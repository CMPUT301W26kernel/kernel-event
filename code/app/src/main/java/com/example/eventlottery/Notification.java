package com.example.eventlottery;

import com.google.firebase.Timestamp;

/**
 * Notification Model
 *
 * Represents a notification stored in Firestore.
 *
 * Firestore stores type and status as Strings.
 * Enum helper methods are provided for type-safe use in code.
 *
 * Last Modified: 2026-04-03 by Radwa Sheikhdon
 * @author Radwa
 */
public class Notification {

    // Firestore fields
    private String notificationId;
    private String userId;
    private String eventId;
    private String type;
    private String message;
    private String status;
    private Timestamp timestamp;

    /**
     * Required empty constructor for Firestore deserialization.
     */
    public Notification() {}

    /**
     * Constructor for creating a new notification.
     */
    public Notification(String notificationId,
                        String userId,
                        String eventId,
                        String type,
                        String message,
                        String status,
                        Timestamp timestamp) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.eventId = eventId;
        this.type = type;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }

    /**
     * Converts stored String type to enum safely.
     */
    public NotificationType getTypeEnum() {
        if (type == null) return null;
        try {
            return NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Convenience helper for setting type from enum.
     * Not named setType to avoid Firestore setter conflicts.
     */
    public void setTypeEnum(NotificationType type) {
        this.type = (type == null) ? null : type.name();
    }

    /**
     * Backward-compatible enum setter kept for tests and older call sites.
     */
    public void setType(NotificationType type) {
        setTypeEnum(type);
    }

    /**
     * Converts stored String status to enum safely.
     */
    public NotificationStatus getStatusEnum() {
        if (status == null) return null;
        try {
            return NotificationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Convenience helper for setting status from enum.
     * Not named setStatus to avoid Firestore setter conflicts.
     */
    public void setStatusEnum(NotificationStatus status) {
        this.status = (status == null) ? null : status.name();
    }

    /**
     * Backward-compatible enum setter kept for tests and older call sites.
     */
    public void setStatus(NotificationStatus status) {
        setStatusEnum(status);
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Raw getter used by Firestore.
     */
    public String getType() {
        return type;
    }

    /**
     * Raw setter used by Firestore.
     */
    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Raw getter used by Firestore.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Raw setter used by Firestore.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
