package com.example.eventlottery;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying notifications.
 * Supports marking notifications as read and responding to invitation notifications.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private List<Notification> notificationList;
    private final NotificationRepository repository;

    public NotificationsAdapter(List<Notification> notificationList, NotificationRepository repository) {
        this.notificationList = notificationList;
        this.repository = repository;
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

        bindMessage(holder, notification);
        bindTimestamp(holder, notification);
        resetViewState(holder);
        bindItemClick(holder, notification);

        if (isUnreadInvite(notification)) {
            showInviteActions(holder, notification);
        } else {
            showStatusBadge(holder, notification);
        }
    }

    private void bindMessage(ViewHolder holder, Notification notification) {
        holder.message.setText(notification.getMessage());
    }

    private void bindTimestamp(ViewHolder holder, Notification notification) {
        if (notification.getTimestamp() != null) {
            Date date = notification.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.time.setText(sdf.format(date));
        } else {
            holder.time.setText("");
        }
    }

    private void resetViewState(ViewHolder holder) {
        holder.layoutInviteActions.setVisibility(View.GONE);
        holder.statusBadge.setVisibility(View.VISIBLE);
        holder.btnAccept.setOnClickListener(null);
        holder.btnDecline.setOnClickListener(null);
    }

    private void bindItemClick(ViewHolder holder, Notification notification) {
        holder.itemView.setOnClickListener(v -> {
            if (!Notification.STATUS_UNREAD.equals(notification.getStatus())) return;

            repository.markAsRead(notification, new NotificationRepository.NotificationCallback() {
                @Override
                public void onSuccess() {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        notification.setStatus(Notification.STATUS_READ);
                        notifyItemChanged(pos);
                        Toast.makeText(v.getContext(), "Marked as read", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(v.getContext(), "Failed to mark as read", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean isUnreadInvite(Notification notification) {
        String type = notification.getType();
        return (Notification.TYPE_INVITE.equals(type)
                || Notification.TYPE_COORGANIZER_INVITE.equals(type))
                && Notification.STATUS_UNREAD.equals(notification.getStatus());
    }

    private void showInviteActions(ViewHolder holder, Notification notification) {
        holder.layoutInviteActions.setVisibility(View.VISIBLE);
        holder.statusBadge.setVisibility(View.GONE);

        holder.btnAccept.setBackgroundTintList(
                ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.primary_dark))
        );
        holder.btnDecline.setBackgroundTintList(
                ColorStateList.valueOf(holder.itemView.getContext().getColor(R.color.secondary_dark))
        );

        holder.btnAccept.setTextColor(holder.itemView.getContext().getColor(R.color.white));
        holder.btnDecline.setTextColor(holder.itemView.getContext().getColor(R.color.white));

        holder.btnAccept.setOnClickListener(v -> handleAccept(holder, notification, v));
        holder.btnDecline.setOnClickListener(v -> handleDecline(holder, notification, v));
    }

    private void handleAccept(ViewHolder holder, Notification notification, View view) {
        if (!hasRequiredIds(notification)) {
            Toast.makeText(view.getContext(), "Invalid notification data", Toast.LENGTH_SHORT).show();
            return;
        }

        NotificationRepository.NotificationCallback callback = new NotificationRepository.NotificationCallback() {
            @Override
            public void onSuccess() {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    notification.setStatus(Notification.STATUS_ACCEPTED);
                    notifyItemChanged(pos);
                    Toast.makeText(view.getContext(), "Invitation accepted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        view.getContext(),
                        "Failed to accept invitation: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        if (Notification.TYPE_COORGANIZER_INVITE.equals(notification.getType())) {
            repository.acceptCoOrganizerInvite(notification, callback);
        } else {
            repository.acceptInvitation(notification, callback);
        }
    }

    private void handleDecline(ViewHolder holder, Notification notification, View view) {
        if (!hasRequiredIds(notification)) {
            Toast.makeText(view.getContext(), "Invalid notification data", Toast.LENGTH_SHORT).show();
            return;
        }

        NotificationRepository.NotificationCallback callback = new NotificationRepository.NotificationCallback() {
            @Override
            public void onSuccess() {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    notification.setStatus(Notification.STATUS_DECLINED);
                    notifyItemChanged(pos);
                    Toast.makeText(view.getContext(), "Invitation declined", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        view.getContext(),
                        "Failed to decline invitation: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        if (Notification.TYPE_COORGANIZER_INVITE.equals(notification.getType())) {
            repository.declineCoOrganizerInvite(notification, callback);
        } else {
            repository.declineInvitation(notification, callback);
        }
    }

    private void showStatusBadge(ViewHolder holder, Notification notification) {
        String status = notification.getStatus();
        holder.statusBadge.setText(status != null && !status.isEmpty() ? status : "UNKNOWN");

        int color;
        if (Notification.STATUS_ACCEPTED.equals(status)) {
            color = holder.itemView.getContext().getColor(R.color.primary_dark);
        } else if (Notification.STATUS_DECLINED.equals(status)) {
            color = holder.itemView.getContext().getColor(R.color.secondary_dark);
        } else if (Notification.STATUS_UNREAD.equals(status)) {
            color = holder.itemView.getContext().getColor(R.color.primary_mid);
        } else {
            color = holder.itemView.getContext().getColor(R.color.grey_light);
        }

        holder.statusBadge.setBackgroundTintList(ColorStateList.valueOf(color));
        holder.statusBadge.setTextColor(holder.itemView.getContext().getColor(R.color.white));
    }

    private boolean hasRequiredIds(Notification notification) {
        return notification.getNotificationId() != null
                && notification.getEventId() != null
                && notification.getUserId() != null;
    }

    @Override
    public int getItemCount() {
        return notificationList == null ? 0 : notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView message;
        TextView time;
        TextView statusBadge;
        Button btnAccept;
        Button btnDecline;
        View layoutInviteActions;

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


