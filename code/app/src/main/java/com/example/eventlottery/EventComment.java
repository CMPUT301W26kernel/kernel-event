package com.example.eventlottery;

import com.google.firebase.Timestamp;

/**
 * Firestore-backed comment attached to a single event.
 */
@SuppressWarnings("unused")
public class EventComment {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_REMOVED = "removed";

    private String commentId;
    private String authorId;
    private String authorName;
    private String authorRole;
    private String text;
    private Timestamp createdAt;
    private String status;
    private boolean isPinned;
    private String removedReason;

    public EventComment() {
        // Required empty constructor for Firestore.
    }

    public EventComment(
            String authorId,
            String authorName,
            String authorRole,
            String text,
            Timestamp createdAt,
            String status,
            boolean isPinned
    ) {
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.text = text;
        this.createdAt = createdAt;
        this.status = status;
        this.isPinned = isPinned;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public String getRemovedReason() {
        return removedReason;
    }

    public void setRemovedReason(String removedReason) {
        this.removedReason = removedReason;
    }

    public boolean hasBeenRemoved() {
        return STATUS_REMOVED.equals(status);
    }
}
