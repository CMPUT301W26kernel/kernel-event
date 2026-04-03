package com.example.eventlottery;

import com.google.firebase.Timestamp;

import java.util.List;

/**
 * This class represents a notification log entry.
 * It has fields for logId, senderId, eventId, recipientIds, recipientCount, type, message, and timestamp.
 * It also has getters and setters for each field.
 * The class also has a constructor for creating a notification log entry.
 * Last Modified: 2026-04-03 by Radwa Sheikhdon
 */
public class NotificationLog {
    private String logId;
    private String senderId;
    private String eventId;
    private List<String> recipientIds;
    private int recipientCount;
    private String type;
    private String message;
    private Timestamp timestamp;

    public NotificationLog() {
        // Required empty constructor for Firestore
    }

    /**
     * Creates a new notification log entry.
     *
     * @param logId
     * @param senderId
     * @param eventId
     * @param recipientIds
     * @param recipientCount
     * @param type
     * @param message
     * @param timestamp
     */
    public NotificationLog(String logId,
                           String senderId,
                           String eventId,
                           List<String> recipientIds,
                           int recipientCount,
                           String type,
                           String message,
                           Timestamp timestamp) {
        this.logId = logId;
        this.senderId = senderId;
        this.eventId = eventId;
        this.recipientIds = recipientIds;
        this.recipientCount = recipientCount;
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and setters

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public List<String> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(List<String> recipientIds) {
        this.recipientIds = recipientIds;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
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

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
