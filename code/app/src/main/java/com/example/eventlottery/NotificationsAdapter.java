package com.example.eventlottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adapter for the notifications list
 * Handles layout inflation and binding for individual notification items.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private List<Notification> notificationList;
    private final NotificationRepository repository;

    public NotificationsAdapter(List<Notification> list, NotificationRepository repo) {
        this.notificationList = list;
        this.repository = repo;
    }

    public void updateList(List<Notification> newList) {
        this.notificationList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.message.setText(notification.getMessage());

        // Set the timestamp
        if (notification.getTimestamp() != null) {
            holder.time.setText(notification.getTimestamp().toString());
        }

        // Handles different notification types
        if ("INVITE".equals(notification.getType()) && "PENDING".equals(notification.getStatus())) {
            // Show Accept/Decline buttons for pending invites
            holder.layoutInviteActions.setVisibility(View.VISIBLE);
            holder.statusBadge.setVisibility(View.GONE);

            holder.btnAccept.setOnClickListener(v -> repository.acceptInvitation(notification));
            holder.btnDecline.setOnClickListener(v -> repository.declineInvitation(notification));
        } else {
            // Hide Accept/Decline buttons for non-pending invites
            holder.layoutInviteActions.setVisibility(View.GONE);
            holder.statusBadge.setVisibility(View.VISIBLE);
            holder.statusBadge.setText(notification.getStatus());
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView message, time, statusBadge;
        Button btnAccept, btnDecline;
        View layoutInviteActions;

        public ViewHolder(View itemView) {
            super(itemView);
            // Initialize views
            message = itemView.findViewById(R.id.notification_message);
            time = itemView.findViewById(R.id.notification_time);
            statusBadge = itemView.findViewById(R.id.txt_status_badge);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
            layoutInviteActions = itemView.findViewById(R.id.layout_invite_actions);
        }
    }
}

