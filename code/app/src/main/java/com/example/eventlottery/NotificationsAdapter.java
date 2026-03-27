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
 * Adapter for notifications list
 * Last Modified: 2026-03-26 by Radwa Sheikhdon
 * Handles safe Accept/Decline with Firestore repository callbacks.
 * @author Radwa
 * @version 1.5
 * @since 2023-03-02
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

        if (notification.getTimestamp() != null) {
            Date date = notification.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.time.setText(sdf.format(date));
        } else {
            holder.time.setText("");
        }

        // Reset recycled view state
        holder.layoutInviteActions.setVisibility(View.GONE);
        holder.statusBadge.setVisibility(View.VISIBLE);
        holder.btnAccept.setOnClickListener(null);
        holder.btnDecline.setOnClickListener(null);

        // Default item click: mark unread notifications as read
        holder.itemView.setOnClickListener(v -> {
            if (Notification.STATUS_UNREAD.equals(notification.getStatus())) {
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
            }
        });

        // Unread invite: show buttons, hide badge
        if (Notification.TYPE_INVITE.equals(notification.getType()) &&
                Notification.STATUS_UNREAD.equals(notification.getStatus())) {

            holder.layoutInviteActions.setVisibility(View.VISIBLE);
            holder.statusBadge.setVisibility(View.GONE);

            // Match button colors to final accepted/declined tag colors
            holder.btnAccept.setBackgroundTintList(
                    ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(R.color.primary_dark)
                    )
            );
            holder.btnDecline.setBackgroundTintList(
                    ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(R.color.secondary_dark)
                    )
            );

            holder.btnAccept.setTextColor(
                    holder.itemView.getContext().getColor(R.color.white)
            );
            holder.btnDecline.setTextColor(
                    holder.itemView.getContext().getColor(R.color.white)
            );

            holder.btnAccept.setOnClickListener(v -> {
                if (notification.getNotificationId() == null ||
                        notification.getEventId() == null ||
                        notification.getUserId() == null) {
                    Toast.makeText(v.getContext(), "Invalid notification data", Toast.LENGTH_SHORT).show();
                    return;
                }

                repository.acceptInvitation(notification, new NotificationRepository.NotificationCallback() {
                    @Override
                    public void onSuccess() {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            notification.setStatus(Notification.STATUS_ACCEPTED);
                            notifyItemChanged(pos);
                            Toast.makeText(v.getContext(), "Invitation Accepted!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(
                                v.getContext(),
                                "Failed to accept invitation: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            });

            holder.btnDecline.setOnClickListener(v -> {
                if (notification.getNotificationId() == null ||
                        notification.getEventId() == null ||
                        notification.getUserId() == null) {
                    Toast.makeText(v.getContext(), "Invalid notification data", Toast.LENGTH_SHORT).show();
                    return;
                }

                repository.declineInvitation(notification, new NotificationRepository.NotificationCallback() {
                    @Override
                    public void onSuccess() {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            notification.setStatus(Notification.STATUS_DECLINED);
                            notifyItemChanged(pos);
                            Toast.makeText(v.getContext(), "Invitation Declined!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(v.getContext(), "Failed to decline invitation", Toast.LENGTH_SHORT).show();
                    }
                });
            });

        } else {
            // All non-unread-invite notifications: show colored status badge
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
            holder.statusBadge.setTextColor(
                    holder.itemView.getContext().getColor(R.color.white)
            );
        }
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


