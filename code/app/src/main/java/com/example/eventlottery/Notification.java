package com.example.eventlottery;


import com.google.firebase.Timestamp;

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

    // Empty constructor for Firestore
    public Notification() {
    }

    // This is a constructor for creating a notification
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

    // Getters and Setters

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

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