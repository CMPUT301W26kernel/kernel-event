package com.example.eventlottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for event comments.
 */
public class EventCommentAdapter extends RecyclerView.Adapter<EventCommentAdapter.ViewHolder> {

    /**
     * Callback fired when the user taps the delete button on a comment row.
     */
    interface OnDeleteCommentListener {
        void onDeleteComment(EventComment comment);
    }

    private static final DateTimeFormatter COMMENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault());

    private final List<EventComment> comments = new ArrayList<>();
    private final OnDeleteCommentListener deleteListener;

    private String currentUserId;
    private String currentUserRole;
    private String organizerId;

    public EventCommentAdapter(OnDeleteCommentListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    /**
     * Replaces the adapter contents with the latest comment list.
     *
     * @param newComments Comments in display order.
     */
    public void setComments(List<EventComment> newComments) {
        comments.clear();
        comments.addAll(newComments);
        notifyDataSetChanged();
    }

    /**
     * Updates the viewer role context used to decide whether delete buttons should be visible.
     *
     * @param currentUserId Signed-in user id, if any.
     * @param currentUserRole Viewer role.
     * @param organizerId Event organizer id.
     */
    public void setViewerContext(String currentUserId, String currentUserRole, String organizerId) {
        this.currentUserId = currentUserId;
        this.currentUserRole = currentUserRole;
        this.organizerId = organizerId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventComment comment = comments.get(position);
        holder.authorView.setText(buildAuthorLine(comment));
        holder.metaView.setText(buildMetaLine(comment));
        holder.pinBadgeView.setVisibility(comment.isPinned() ? View.VISIBLE : View.GONE);

        if (comment.hasBeenRemoved()) {
            holder.bodyView.setText(holder.itemView.getContext().getString(R.string.comment_removed_text));
            holder.deleteButton.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.bodyView.setText(comment.getText());
            holder.itemView.setAlpha(1f);

            boolean canDelete = EventCommentPolicy.canDeleteComment(
                    comment,
                    currentUserId,
                    currentUserRole,
                    organizerId
            );
            holder.deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);
            holder.deleteButton.setOnClickListener(v -> deleteListener.onDeleteComment(comment));
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private String buildAuthorLine(EventComment comment) {
        String authorName = isBlank(comment.getAuthorName()) ? "User" : comment.getAuthorName();
        String role = formatRole(comment.getAuthorRole());
        if (role.isEmpty()) {
            return authorName;
        }
        return authorName + " | " + role;
    }

    private String buildMetaLine(EventComment comment) {
        if (comment.hasBeenRemoved()) {
            String reason = comment.getRemovedReason();
            return isBlank(reason) ? "" : reason;
        }

        Timestamp timestamp = comment.getCreatedAt();
        if (timestamp == null) {
            return "";
        }

        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanoseconds());
        return COMMENT_TIME_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }

    private String formatRole(String role) {
        if (isBlank(role)) {
            return "";
        }

        String normalized = role.toLowerCase(Locale.getDefault());
        return normalized.substring(0, 1).toUpperCase(Locale.getDefault()) + normalized.substring(1);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView authorView;
        final TextView metaView;
        final TextView bodyView;
        final TextView pinBadgeView;
        final Button deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            authorView = itemView.findViewById(R.id.tv_comment_author);
            metaView = itemView.findViewById(R.id.tv_comment_meta);
            bodyView = itemView.findViewById(R.id.tv_comment_body);
            pinBadgeView = itemView.findViewById(R.id.tv_comment_pin_badge);
            deleteButton = itemView.findViewById(R.id.btn_delete_comment);
        }
    }
}
