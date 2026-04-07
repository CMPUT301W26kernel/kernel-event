package com.example.eventlottery;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying notifications.
 * Supports actionable invites and non-actionable notifications.
 * Last Modified: 2026-04-03 by Radwa Sheikhdon
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private List<Notification> notificationList;
    private final NotificationRepository repository;

    /**
     * Creates a new adapter for displaying notifications.
     *
     * @param notificationList list of notifications to display
     * @param repository repository used for notification actions
     */
    public NotificationsAdapter(List<Notification> notificationList, NotificationRepository repository) {
        this.notificationList = notificationList;
        this.repository = repository;
    }

    /**
     * Replaces the current notification list and refreshes the RecyclerView.
     *
     * @param newList updated list of notifications
     */
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
        showTypeBadge(holder, notification);

        if (isActionableInvite(notification)) {
            showInviteActions(holder, notification);
        } else {
            showStatusBadge(holder, notification);
        }
    }

    /**
     * Displays the notification message.
     */
    private void bindMessage(ViewHolder holder, Notification notification) {
        holder.message.setText(notification.getMessage());
    }

    /**
     * Displays the formatted timestamp if one exists.
     * Otherwise, leaves the time field blank.
     */
    private void bindTimestamp(ViewHolder holder, Notification notification) {
        if (notification.getTimestamp() != null) {
            Date date = notification.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.time.setText(sdf.format(date));
        } else {
            holder.time.setText("");
        }
    }

    /**
     * Resets view state before binding a recycled item.
     * Prevents recycled rows from keeping stale listeners or visibility states.
     */
    private void resetViewState(ViewHolder holder) {
        holder.layoutInviteActions.setVisibility(View.GONE);
        holder.statusBadge.setVisibility(View.VISIBLE);
        holder.btnAccept.setOnClickListener(null);
        holder.btnDecline.setOnClickListener(null);
    }

    /**
     * Marks a notification as read when tapped, but only if it is currently unread.
     */
    private void bindItemClick(ViewHolder holder, Notification notification) {
        holder.itemView.setOnClickListener(v -> {
            if (notification.getStatusEnum() != NotificationStatus.UNREAD) {
                return;
            }

            repository.markAsRead(notification, new NotificationRepository.NotificationCallback() {
                @Override
                public void onSuccess() {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        notification.setStatusEnum(NotificationStatus.READ);
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

    /**
     * Returns true if the notification is an invite or co-organizer invite
     * and is still awaiting user action.
     */
    private boolean isActionableInvite(Notification notification) {
        NotificationType type = notification.getTypeEnum();
        NotificationStatus status = notification.getStatusEnum();

        boolean isInviteType =
                type == NotificationType.INVITE ||
                        type == NotificationType.COORGANIZER_INVITE;

        boolean isStillActionable =
                status == NotificationStatus.UNREAD ||
                        status == NotificationStatus.READ;

        return isInviteType && isStillActionable;
    }

    /**
     * Shows accept/decline buttons for actionable invite notifications.
     */
    private void showInviteActions(ViewHolder holder, Notification notification) {
        holder.layoutInviteActions.setVisibility(View.VISIBLE);
        holder.statusBadge.setVisibility(View.GONE);

        holder.btnAccept.setBackgroundTintList(
                ColorStateList.valueOf(safeColor(holder.itemView, R.color.primary_dark))
        );
        holder.btnDecline.setBackgroundTintList(
                ColorStateList.valueOf(safeColor(holder.itemView, R.color.secondary_dark))
        );

        holder.btnAccept.setTextColor(safeColor(holder.itemView, R.color.white));
        holder.btnDecline.setTextColor(safeColor(holder.itemView, R.color.white));

        holder.btnAccept.setOnClickListener(v -> handleAccept(holder, notification, v));
        holder.btnDecline.setOnClickListener(v -> handleDecline(holder, notification, v));
    }

    /**
     * Handles accepting an invite or co-organizer invite.
     */
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
                    notification.setStatusEnum(NotificationStatus.ACCEPTED);
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

        if (notification.getTypeEnum() == NotificationType.COORGANIZER_INVITE) {
            repository.acceptCoOrganizerInvite(notification, callback);
        } else {
            repository.acceptInvitation(notification, callback);
        }
    }

    /**
     * Handles declining an invite or co-organizer invite.
     */
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
                    notification.setStatusEnum(NotificationStatus.DECLINED);
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

        if (notification.getTypeEnum() == NotificationType.COORGANIZER_INVITE) {
            repository.declineCoOrganizerInvite(notification, callback);
        } else {
            repository.declineInvitation(notification, callback);
        }
    }

    /**
     * Displays the status badge for non-actionable notifications.
     */
    private void showStatusBadge(ViewHolder holder, Notification notification) {
        NotificationStatus status = notification.getStatusEnum();
        String statusText = notification.getStatus();

        holder.statusBadge.setText(
                statusText != null && !statusText.isEmpty() ? statusText : "UNKNOWN"
        );
        holder.statusBadge.setVisibility(View.VISIBLE);

        int color;
        if (status == NotificationStatus.ACCEPTED) {
            color = safeColor(holder.itemView, R.color.primary_dark);
        } else if (status == NotificationStatus.DECLINED) {
            color = safeColor(holder.itemView, R.color.secondary_dark);
        } else if (status == NotificationStatus.UNREAD) {
            color = safeColor(holder.itemView, R.color.primary_mid);
        } else if (status == NotificationStatus.READ) {
            color = safeColor(holder.itemView, R.color.grey_light);
        } else {
            color = safeColor(holder.itemView, R.color.grey_light);
        }

        holder.statusBadge.setBackgroundTintList(ColorStateList.valueOf(color));
        holder.statusBadge.setTextColor(safeColor(holder.itemView, R.color.white));
    }

    /**
     * Displays a badge describing the notification type.
     */
    private void showTypeBadge(ViewHolder holder, Notification notification) {
        NotificationType type = notification.getTypeEnum();

        String text;
        int color;

        if (type == NotificationType.COORGANIZER_INVITE) {
            text = "CO-ORGANIZER";
            color = safeColor(holder.itemView, R.color.primary_deep);
        } else if (type == NotificationType.INVITE) {
            if (notification.getMessage() != null
                    && notification.getMessage().toLowerCase().contains("private")) {
                text = "PRIVATE EVENT";
                color = safeColor(holder.itemView, R.color.secondary_mid);
            } else {
                text = "INVITE";
                color = safeColor(holder.itemView, R.color.primary_mid);
            }
        } else if (type == NotificationType.INFO) {
            text = "INFO";
            color = safeColor(holder.itemView, R.color.primary_dark);
        } else if (type == NotificationType.ADMIN) {
            text = "ADMIN";
            color = safeColor(holder.itemView, R.color.primary_dark);
        } else {
            text = "OTHER";
            color = safeColor(holder.itemView, R.color.grey_light);
        }

        holder.typeBadge.setText(text);
        holder.typeBadge.setBackgroundTintList(ColorStateList.valueOf(color));
        holder.typeBadge.setTextColor(safeColor(holder.itemView, R.color.white));
    }

    /**
     * Safely resolves a color resource.
     */
    private int safeColor(View view, int colorRes) {
        try {
            return ContextCompat.getColor(view.getContext(), colorRes);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns true if the notification contains all IDs required
     * for accept/decline operations.
     */
    private boolean hasRequiredIds(Notification notification) {
        return notification.getNotificationId() != null
                && notification.getEventId() != null
                && notification.getUserId() != null;
    }

    @Override
    public int getItemCount() {
        return notificationList == null ? 0 : notificationList.size();
    }

    /**
     * ViewHolder for notification rows.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView message;
        TextView time;
        TextView statusBadge;
        TextView typeBadge;
        Button btnAccept;
        Button btnDecline;
        View layoutInviteActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.notification_message);
            time = itemView.findViewById(R.id.notification_time);
            statusBadge = itemView.findViewById(R.id.txt_status_badge);
            typeBadge = itemView.findViewById(R.id.txt_type_badge);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
            layoutInviteActions = itemView.findViewById(R.id.layout_invite_actions);
        }
    }
}

