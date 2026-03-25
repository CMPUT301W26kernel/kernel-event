package com.example.eventlottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for notifications list
 * Last Modified: 2026-03-12 by Radwa Sheikhdon
 * @author Radwa Sheikhdon
 * @version 1.0
 * @since 2023-03-02
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private List<Notification> notificationList;
    private final NotificationRepository repository;

    /**
     * Constructor
     * @param list List of notifications
     * @param repo Notification repository
     */
    public NotificationsAdapter(List<Notification> list, NotificationRepository repo) {
        this.notificationList = list;
        this.repository = repo;
    }

    /**
     * Updates the list of notifications
     * @param newList New list of notifications
     */
    public void updateList(List<Notification> newList) {
        this.notificationList = newList;
        notifyDataSetChanged();
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
     * position.
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Notification notification = notificationList.get(position);

        holder.message.setText(notification.getMessage());

        // Format timestamp nicely
        if (notification.getTimestamp() != null) {
            Date date = notification.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.time.setText(sdf.format(date));
        }

        // Handle invite notifications that require a response
        if (Notification.TYPE_INVITE.equals(notification.getType()) &&
                Notification.STATUS_UNREAD.equals(notification.getStatus())) {

            holder.layoutInviteActions.setVisibility(View.VISIBLE);
            holder.statusBadge.setVisibility(View.GONE);

            holder.btnAccept.setOnClickListener(v -> {
                repository.acceptInvitation(notification);
                holder.btnAccept.setEnabled(false);
                holder.btnDecline.setEnabled(false);
            });

            holder.btnDecline.setOnClickListener(v -> {
                repository.declineInvitation(notification);
                holder.btnAccept.setEnabled(false);
                holder.btnDecline.setEnabled(false);
            });

        } else {
            holder.layoutInviteActions.setVisibility(View.GONE);
            holder.statusBadge.setVisibility(View.VISIBLE);
            holder.statusBadge.setText(notification.getStatus());
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return
     */
    @Override
    public int getItemCount() {
        return notificationList == null ? 0 : notificationList.size();
    }

    /**
     * ViewHolder for notification items
     *
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView message;
        TextView time;
        public TextView statusBadge;
        public Button btnAccept;
        public Button btnDecline;
        public View layoutInviteActions;

        /**
         * Constructor for ViewHolder
         * @param itemView
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.notification_message);
            time = itemView.findViewById(R.id.notification_time);
            statusBadge = itemView.findViewById(R.id.txt_status_badge);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
            layoutInviteActions = itemView.findViewById(R.id.layout_invite_actions);
        }
    }
}
