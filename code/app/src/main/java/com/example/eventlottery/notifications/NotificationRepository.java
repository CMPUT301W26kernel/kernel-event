package com.example.eventlottery.notifications;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.List;

/**
 * Notification Repository
 * Last Modified: 2026-03-12 by Radwa Sheikhdon
 * Handles creation and response logic for notifications.
 * @author Radwa
 * @version 1.0
 * @since 2023-03-02
 */
public class NotificationRepository {

    
    private final FirebaseFirestore db;

    
    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    
    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    /**
     * Create a new notification in Firestore
     * @param notification
     */
    public void createNotification(Notification notification) {
        getDb().collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef ->
                        Log.d("NotificationRepo", "Notification added: " + docRef.getId()))
                .addOnFailureListener(e ->
                        Log.e("NotificationRepo", "Error adding notification", e));
    }

    /**
     * Accept an invitation to an event
     * @param notification
     */
    public void acceptInvitation(Notification notification) {
        getDb().runTransaction((Transaction.Function<Void>) transaction -> {

                    DocumentReference selectedRef = getDb().collection("events")
                            .document(notification.getEventId())
                            .collection("selected")
                            .document(notification.getUserId());

                    DocumentReference attendeeRef = getDb().collection("events")
                            .document(notification.getEventId())
                            .collection("attendees")
                            .document(notification.getUserId());

                    DocumentReference notificationRef = getDb().collection("notifications")
                            .document(notification.getNotificationId());

                    // Add to attendees
                    transaction.set(attendeeRef, new HashMap<>());

                    // Remove from selected
                    transaction.delete(selectedRef);

                    // Update notification status
                    transaction.update(notificationRef, "status", Notification.STATUS_ACCEPTED);

                    return null;
                }).addOnSuccessListener(aVoid ->
                        Log.d("NotificationRepo", "Invitation accepted successfully"))
                .addOnFailureListener(e ->
                        Log.e("NotificationRepo", "Failed to accept invitation", e));
    }

    /**
     * Decline an invitation to an event
     * @param notification
     */
    public void declineInvitation(Notification notification) {
        getDb().runTransaction((Transaction.Function<Void>) transaction -> {

                    DocumentReference selectedRef = getDb().collection("events")
                            .document(notification.getEventId())
                            .collection("selected")
                            .document(notification.getUserId());

                    DocumentReference notificationRef = getDb().collection("notifications")
                            .document(notification.getNotificationId());

                    // Remove from selected
                    transaction.delete(selectedRef);

                    // Update notification status
                    transaction.update(notificationRef, "status", Notification.STATUS_DECLINED);

                    return null;
                }).addOnSuccessListener(aVoid ->
                        Log.d("NotificationRepo", "Invitation declined successfully"))
                .addOnFailureListener(e ->
                        Log.e("NotificationRepo", "Failed to decline invitation", e));
    }

    /**
     * Send a bulk notification to a list of users
     * @param userIds
     * @param eventId
     * @param message
     */
    public void sendBulkNotification(List<String> userIds, String eventId, String message) {
        for (String userId : userIds) {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setEventId(eventId);
            n.setType(Notification.TYPE_INFO);
            n.setMessage(message);
            n.setStatus(Notification.STATUS_UNREAD);
            n.setTimestamp(Timestamp.now());

            createNotification(n);
        }
    }
}
