package com.example.eventlottery.notifications;


import com.google.firebase.Timestamp;

/**
 * This class represents a notification in the application.
 * It has fields for notificationId, userId, eventId, type, message, status, and timestamp.
 * It also has getters and setters for each field.
 * The class also has a constructor for creating a notification.
 * Last Modified: 2026-03-12 by Radwa Sheikhdon
 * @author Radwa Sheikhdon
 * @version 1.0
 * @since 2023-03-02
 */
public class Notification {

    //  Types of Notifications
    public static final String TYPE_INVITE = "INVITE";
    public static final String TYPE_ADMIN = "ADMIN";
    public static final String TYPE_INFO = "INFO";

    // Notification Status
    public static final String STATUS_UNREAD = "UNREAD";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private String notificationId;
    private String userId;
    private String eventId;
    private String type;
    private String message;
    private String status;
    private Timestamp timestamp;

    // This is a default constructor for creating a notification
    public Notification() {
    }

    /**
     * This is a constructor for creating a notification.
     * It takes in the following parameters: notificationId, userId, eventId, type, message, status, and timestamp.
     * @param notificationId
     * @param userId
     * @param eventId
     * @param type
     * @param message
     * @param status
     * @param timestamp
     */
    public Notification(String notificationId, String userId, String eventId,
                        String type, String message, String status, Timestamp timestamp) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.eventId = eventId;
        this.type = type;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
    }


    // Returns the notificationId of the notification.
    public String getNotificationId() {
        return notificationId;
    }

    // Sets the notificationId of the notification.
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    // Returns the userId of the notification.
    public String getUserId() {
        return userId;
    }

    // Sets the userId of the notification.
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Returns the eventId of the notification.
    public String getEventId() {
        return eventId;
    }

    // Sets the eventId of the notification.
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    // Returns the type of the notification.
    public String getType() {
        return type;
    }

    // Sets the type of the notification.
    public void setType(String type) {
        this.type = type;
    }

    // Returns the message of the notification.
    public String getMessage() {
        return message;
    }

    // Sets the message of the notification.
    public void setMessage(String message) {
        this.message = message;
    }

    // Returns the status of the notification.
    public String getStatus() {
        return status;
    }

    // Sets the status of the notification.
    public void setStatus(String status) {
        this.status = status;
    }

    // Returns the timestamp of the notification.
    public Timestamp getTimestamp() {
        return timestamp;
    }

    // Sets the timestamp of the notification.
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}