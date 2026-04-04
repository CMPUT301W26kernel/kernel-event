package com.example.eventlottery;

import com.google.firebase.Timestamp;
import com.example.eventlottery.NotificationType;
import com.example.eventlottery.NotificationStatus;

/**
 * Notification Model
 *
 * Represents a notification stored in Firestore.
 *
 * Firestore stores `type` and `status` as Strings
 * We use enums in code for type safety
 * Conversion is handled via helper methods
 *
 * Last Modified: 2026-04-03 by Radwa Sheikhdon
 * @author Radwa
 */
public class Notification {


    // Firestore fields (stay as Strings for serialization)
    private String notificationId;
    private String userId;
    private String eventId;
    private String type;    // stored as String in Firestore
    private String message;
    private String status;  // stored as String in Firestore
    private Timestamp timestamp;

    /**
     * Required empty constructor for Firestore deserialization
     */
    public Notification() {}

    /**
     * Constructor for creating a new notification
     *
     * @param notificationId
     * @param userId
     * @param eventId
     * @param type
     * @param message
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

    // ENUM HELPERS

    /**
     * Converts stored String type to enum
     * This prevents crashes if data is invalid/null
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
     * Sets notification type using enum
     * Converts enum to a String for Firestore
     */
    public void setType(NotificationType type) {
        this.type = (type == null) ? null : type.name();
    }

    /**
     * Converts stored String status to enum
     * This prevents crashes if data is invalid/null
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
     * Sets status using an enum
     */
    public void setStatus(NotificationStatus status) {
        this.status = (status == null) ? null : status.name();
    }

    /**
     * Standard GETTERS and SETTERS
     * @return
     */

    // Gets notificationId as a String
    public String getNotificationId() {
        return notificationId;
    }

    // Sets notificationId as a String
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    // Gets userId as a String
    public String getUserId() {
        return userId;
    }

    // Sets userId as a String
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Gets eventId as a String
    public String getEventId() {
        return eventId;
    }

    // Sets eventId as a String
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    // Raw getter (used by Firestore) to get type as a String
    public String getType() {
        return type;
    }

    // Raw setter (used by Firestore) to set type as a String
    public void setType(String type) {
        this.type = type;
    }

    // Gets message as a String
    public String getMessage() {
        return message;
    }

    // Sets message as a String
    public void setMessage(String message) {
        this.message = message;
    }

    // Raw getter (used by Firestore)
    public String getStatus() {
        return status;
    }

    // Raw setter (used by Firestore)
    public void setStatus(String status) {
        this.status = status;
    }

    // Gets timestamp as a Timestamp
    public Timestamp getTimestamp() {
        return timestamp;
    }

    // Sets timestamp as a Timestamp
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}