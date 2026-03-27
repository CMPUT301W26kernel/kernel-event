package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Notification Repository
 * Handles creation and response logic for notifications.
 * Last Modified: 2026-03-25 by Radwa Sheikhdon
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
     * Callback interface for notification operations
     */
    public interface NotificationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void markAsRead(Notification notification, NotificationCallback callback) {
        if (notification.getNotificationId() == null) {
            if (callback != null) callback.onFailure(new Exception("Invalid notificationId"));
            return;
        }

        DocumentReference notificationRef = db.collection("notifications")
                .document(notification.getNotificationId());

        notificationRef.update("status", Notification.STATUS_READ)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }
    public void acceptInvitation(Notification notification, NotificationCallback callback) {
        if (notification.getNotificationId() == null ||
                notification.getEventId() == null ||
                notification.getUserId() == null) {
            if (callback != null) {
                callback.onFailure(new RuntimeException("Invalid notification data"));
            }
            return;
        }

        DocumentReference notificationRef = db.collection("notifications")
                .document(notification.getNotificationId());

        DocumentReference eventRef = db.collection("events")
                .document(notification.getEventId());

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            com.google.firebase.firestore.DocumentSnapshot eventSnap = transaction.get(eventRef);

            if (!eventSnap.exists()) {
                throw new RuntimeException("Event not found");
            }

            transaction.update(notificationRef, "status", Notification.STATUS_ACCEPTED);
            transaction.update(eventRef, "invitedList",
                    com.google.firebase.firestore.FieldValue.arrayRemove(notification.getUserId()));
            transaction.update(eventRef, "acceptedList",
                    com.google.firebase.firestore.FieldValue.arrayUnion(notification.getUserId()));

            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e("NotificationRepo", "Accept invitation failed", e);
            if (callback != null) callback.onFailure(e);
        });
    }

    public void declineInvitation(Notification notification, NotificationCallback callback) {
        DocumentReference notificationRef = db.collection("notifications")
                .document(notification.getNotificationId());

        notificationRef.update("status", Notification.STATUS_DECLINED)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void createNotification(Notification notification) {
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    // Set the notificationId in the local object
                    notification.setNotificationId(docRef.getId());
                    Log.d("NotificationRepo", "Notification added: " + docRef.getId());
                })
                .addOnFailureListener(e -> Log.e("NotificationRepo", "Error adding notification", e));
    }

    /**
     * Sends bulk notifications to a list of users
     * @param userIds List of user IDs
     * @param eventId Event ID
     * @param message Message content
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

    public void sendWaitingListNotification(String eventId, String message) {
        Timestamp now = Timestamp.now();

        // Get the event to read the waitingList array
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    List<String> waitingList = (List<String>) doc.get("waitingList");
                    if (waitingList == null || waitingList.isEmpty()) return;

                    for (String userId : waitingList) {
                        Notification n = new Notification();
                        n.setUserId(userId);
                        n.setEventId(eventId);
                        n.setType(Notification.TYPE_INFO); // can also use TYPE_ADMIN
                        n.setMessage(message);
                        n.setStatus(Notification.STATUS_UNREAD);
                        n.setTimestamp(now);

                        // Add to Firestore
                        createNotification(n);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("NotificationRepo", "Failed to read waiting list: " + e.getMessage());
                });
    }

    public Task<Void> sendInvitedUsersNotification(String eventId, String message) {
        return db.collection("events")
                .document(eventId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        throw (e != null) ? e : new Exception("Failed to load event");
                    }

                    DocumentSnapshot snapshot = task.getResult();
                    if (snapshot == null || !snapshot.exists()) {
                        throw new Exception("Event not found");
                    }

                    Object invitedObj = snapshot.get("invitedList");
                    List<String> invitedList = new ArrayList<>();

                    if (invitedObj instanceof List<?>) {
                        for (Object item : (List<?>) invitedObj) {
                            if (item instanceof String) {
                                invitedList.add((String) item);
                            }
                        }
                    }

                    List<Task<DocumentReference>> notificationTasks = new ArrayList<>();

                    for (String userId : invitedList) {
                        Notification n = new Notification();
                        n.setUserId(userId);
                        n.setEventId(eventId);
                        n.setType(Notification.TYPE_INVITE);
                        n.setMessage(message);
                        n.setStatus(Notification.STATUS_UNREAD);
                        n.setTimestamp(Timestamp.now());

                        notificationTasks.add(db.collection("notifications").add(n));
                    }

                    return Tasks.whenAll(notificationTasks);
                });
    }

    public Task<Void> sendCancelledEntrantsNotification(String eventId, String message) {
        return db.collection("events")
                .document(eventId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        throw (e != null) ? e : new Exception("Failed to load event");
                    }

                    DocumentSnapshot snapshot = task.getResult();
                    if (snapshot == null || !snapshot.exists()) {
                        throw new Exception("Event not found");
                    }

                    Object cancelledObj = snapshot.get("cancelledList");
                    List<String> cancelledList = new ArrayList<>();

                    if (cancelledObj instanceof List<?>) {
                        for (Object item : (List<?>) cancelledObj) {
                            if (item instanceof String) {
                                cancelledList.add((String) item);
                            }
                        }
                    }

                    List<Task<DocumentReference>> notificationTasks = new ArrayList<>();

                    for (String userId : cancelledList) {
                        Notification n = new Notification();
                        n.setUserId(userId);
                        n.setEventId(eventId);
                        n.setType(Notification.TYPE_INFO);
                        n.setMessage(message);
                        n.setStatus(Notification.STATUS_UNREAD);
                        n.setTimestamp(Timestamp.now());

                        notificationTasks.add(db.collection("notifications").add(n));
                    }

                    return Tasks.whenAll(notificationTasks);
                });
    }
}