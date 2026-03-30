package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for creating, updating, and responding to notifications.
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";

    private final FirebaseFirestore db;

    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public interface NotificationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void markAsRead(Notification notification, NotificationCallback callback) {
        if (notification == null || notification.getNotificationId() == null) {
            notifyFailure(callback, new Exception("Invalid notificationId"));
            return;
        }

        db.collection("notifications")
                .document(notification.getNotificationId())
                .update("status", Notification.STATUS_READ)
                .addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyFailure(callback, e));
    }

    public void acceptInvitation(Notification notification, NotificationCallback callback) {
        if (!isValidNotification(notification)) {
            notifyFailure(callback, new RuntimeException("Invalid notification data"));
            return;
        }

        DocumentReference notificationRef = db.collection("notifications")
                .document(notification.getNotificationId());

        DocumentReference eventRef = db.collection("events")
                .document(notification.getEventId());

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot eventSnap = transaction.get(eventRef);

                    if (!eventSnap.exists()) {
                        throw new RuntimeException("Event not found");
                    }

                    List<String> privateInviteList =
                            getStringList(eventSnap.get("privateEventInvitedList"));

                    boolean isPrivateWaitlistInvite =
                            privateInviteList.contains(notification.getUserId());

                    transaction.update(notificationRef, "status", Notification.STATUS_ACCEPTED);

                    if (isPrivateWaitlistInvite) {
                        transaction.update(eventRef, "privateEventInvitedList",
                                FieldValue.arrayRemove(notification.getUserId()));
                        transaction.update(eventRef, "waitingList",
                                FieldValue.arrayUnion(notification.getUserId()));
                    } else {
                        transaction.update(eventRef, "invitedList",
                                FieldValue.arrayRemove(notification.getUserId()));
                        transaction.update(eventRef, "acceptedList",
                                FieldValue.arrayUnion(notification.getUserId()));
                    }

                    return null;
                }).addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Accept invitation failed", e);
                    notifyFailure(callback, e);
                });
    }

    public void declineInvitation(Notification notification, NotificationCallback callback) {
        if (!isValidNotification(notification)) {
            notifyFailure(callback, new RuntimeException("Invalid notification data"));
            return;
        }

        DocumentReference notificationRef = db.collection("notifications")
                .document(notification.getNotificationId());

        DocumentReference eventRef = db.collection("events")
                .document(notification.getEventId());

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot eventSnap = transaction.get(eventRef);

                    if (!eventSnap.exists()) {
                        throw new RuntimeException("Event not found");
                    }

                    List<String> privateInviteList =
                            getStringList(eventSnap.get("privateEventInvitedList"));

                    boolean isPrivateWaitlistInvite =
                            privateInviteList.contains(notification.getUserId());

                    transaction.update(notificationRef, "status", Notification.STATUS_DECLINED);

                    if (isPrivateWaitlistInvite) {
                        transaction.update(eventRef, "privateEventInvitedList",
                                FieldValue.arrayRemove(notification.getUserId()));
                    }

                    return null;
                }).addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Decline invitation failed", e);
                    notifyFailure(callback, e);
                });
    }

    public void createNotification(Notification notification) {
        if (notification == null) return;

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> notification.setNotificationId(docRef.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding notification", e));
    }

    public void sendBulkNotification(List<String> userIds, String eventId, String message) {
        if (userIds == null || userIds.isEmpty()) return;

        for (String userId : userIds) {
            createNotification(buildNotification(
                    userId,
                    eventId,
                    Notification.TYPE_INFO,
                    message,
                    Notification.STATUS_UNREAD
            ));
        }
    }

    public void sendWaitingListNotification(String eventId, String message) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    List<String> waitingList = getStringList(doc.get("waitingList"));
                    if (waitingList.isEmpty()) return;

                    for (String userId : waitingList) {
                        createNotification(buildNotification(
                                userId,
                                eventId,
                                Notification.TYPE_INFO,
                                message,
                                Notification.STATUS_UNREAD
                        ));
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to read waiting list", e));
    }

    public Task<Void> sendInvitedUsersNotification(String eventId, String message) {
        return sendNotificationsToListField(
                eventId, "invitedList", Notification.TYPE_INVITE, message
        );
    }

    public Task<Void> sendCancelledEntrantsNotification(String eventId, String message) {
        return sendNotificationsToListField(
                eventId, "cancelledList", Notification.TYPE_INFO, message
        );
    }

    public Task<Void> sendPrivateWaitlistInviteNotification(String eventId, String message) {
        return sendNotificationsToListField(
                eventId, "privateEventInvitedList", Notification.TYPE_INVITE, message
        );
    }

    private Task<Void> sendNotificationsToListField(String eventId, String fieldName,
                                                    String type, String message) {
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

                    List<String> userIds = getStringList(snapshot.get(fieldName));
                    List<Task<DocumentReference>> tasks = new ArrayList<>();

                    for (String userId : userIds) {
                        Notification notification = buildNotification(
                                userId,
                                eventId,
                                type,
                                message,
                                Notification.STATUS_UNREAD
                        );
                        tasks.add(db.collection("notifications").add(notification));
                    }

                    return Tasks.whenAll(tasks);
                });
    }

    private Notification buildNotification(String userId, String eventId,
                                           String type, String message, String status) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setEventId(eventId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setStatus(status);
        notification.setTimestamp(Timestamp.now());
        return notification;
    }

    private List<String> getStringList(Object fieldValue) {
        List<String> result = new ArrayList<>();

        if (fieldValue instanceof List<?>) {
            for (Object item : (List<?>) fieldValue) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
        }

        return result;
    }

    private boolean isValidNotification(Notification notification) {
        return notification != null
                && notification.getNotificationId() != null
                && notification.getEventId() != null
                && notification.getUserId() != null;
    }

    private void notifySuccess(NotificationCallback callback) {
        if (callback != null) {
            callback.onSuccess();
        }
    }

    private void notifyFailure(NotificationCallback callback, Exception e) {
        if (callback != null) {
            callback.onFailure(e);
        }
    }
}