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

    /**
     * US 01.05.02 Accept invitation to register
     * US 01.05.07 Entrant accepts private waiting list invitation
     */
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
                    DocumentSnapshot notificationSnap = transaction.get(notificationRef);

                    if (!eventSnap.exists()) {
                        throw new RuntimeException("Event not found");
                    }

                    if (!notificationSnap.exists()) {
                        throw new RuntimeException("Notification not found");
                    }

                    List<String> privateInviteList = getStringList(eventSnap.get("privateEventInvitedList"));
                    boolean isPrivateWaitlistInvite = privateInviteList.contains(notification.getUserId());

                    transaction.update(notificationRef, "status", Notification.STATUS_ACCEPTED);

                    if (isPrivateWaitlistInvite) {
                        // Accepting a private event invite moves entrant onto the waiting list.
                        transaction.update(eventRef, "privateEventInvitedList",
                                FieldValue.arrayRemove(notification.getUserId()));
                        transaction.update(eventRef, "waitingList",
                                FieldValue.arrayUnion(notification.getUserId()));
                    } else {
                        // Accepting a normal invite moves entrant to accepted list.
                        transaction.update(eventRef, "invitedList",
                                FieldValue.arrayRemove(notification.getUserId()));
                        transaction.update(eventRef, "acceptedList",
                                FieldValue.arrayUnion(notification.getUserId()));
                        transaction.update(eventRef, "waitingList",
                                FieldValue.arrayRemove(notification.getUserId()));
                    }

                    return null;
                }).addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Accept invitation failed", e);
                    notifyFailure(callback, e);
                });
    }

    /**
     * US 01.05.03 Decline invitation to register
     * US 01.05.07 Entrant declines private waiting list invitation
     */
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
                    DocumentSnapshot notificationSnap = transaction.get(notificationRef);

                    if (!eventSnap.exists()) {
                        throw new RuntimeException("Event not found");
                    }

                    if (!notificationSnap.exists()) {
                        throw new RuntimeException("Notification not found");
                    }

                    List<String> privateInviteList =
                            getStringList(eventSnap.get("privateEventInvitedList"));

                    boolean isPrivateWaitlistInvite =
                            privateInviteList.contains(notification.getUserId());

                    transaction.update(notificationRef, "status", Notification.STATUS_DECLINED);

                    if (isPrivateWaitlistInvite) {
                        // Declining a private waitlist invite just removes the pending invite.
                        transaction.update(eventRef, "privateEventInvitedList",
                                FieldValue.arrayRemove(notification.getUserId()));
                    } else {
                        // Declining a normal invite removes it and records the entrant as cancelled/not selected.
                        transaction.update(eventRef, "invitedList",
                                FieldValue.arrayRemove(notification.getUserId()));
                        transaction.update(eventRef, "acceptedList",
                                FieldValue.arrayRemove(notification.getUserId()));
                        transaction.update(eventRef, "cancelledList",
                                FieldValue.arrayUnion(notification.getUserId()));
                    }

                    return null;
                }).addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Decline invitation failed", e);
                    notifyFailure(callback, e);
                });
    }

    public void createNotification(Notification notification) {
        createNotificationTask(notification)
                .addOnSuccessListener(aVoid -> {
                    if (notification != null && notification.getNotificationId() != null) {
                        Log.d(TAG, "Notification added: " + notification.getNotificationId());
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error adding notification", e));
    }

    private Task<Void> createNotificationTask(Notification notification) {
        if (notification == null) {
            return Tasks.forException(new IllegalArgumentException("Notification cannot be null"));
        }

        return canReceiveNotification(notification.getUserId(), notification.getType())
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        throw (e != null) ? e : new Exception("Failed to check notification preference");
                    }

                    Boolean allowed = task.getResult();
                    if (allowed == null || !allowed) {
                        return Tasks.forResult(null);
                    }

                    DocumentReference notificationRef = db.collection("notifications").document();
                    notification.setNotificationId(notificationRef.getId());
                    return notificationRef.set(notification);
                });
    }

    public void sendBulkNotification(List<String> userIds,
                                     String eventId,
                                     String message,
                                     String type) {
        if (userIds == null || userIds.isEmpty()) return;

        for (String userId : userIds) {
            createNotification(buildNotification(
                    userId,
                    eventId,
                    type,
                    message,
                    Notification.STATUS_UNREAD
            ));
        }
    }

    /**
     * US 02.07.01 Notify waiting list
     */
    public void sendWaitingListNotification(String eventId, String message) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Event event = doc.toObject(Event.class);

                    if (event == null ||
                            event.getWaitingList() == null ||
                            event.getWaitingList().isEmpty()) return;

                    for (String userId : event.getWaitingList()) {
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

    /**
     * US 01.04.01 Receive notification when selected for event
     * US 2.05.01 Notify selected entrants
     * US 02.07.02 Notify selected entrants
     */
    public Task<Void> sendInvitedUsersNotification(String eventId, String message) {
        return sendNotificationsToListField(
                eventId,
                "invitedList",
                Notification.TYPE_INVITE,
                message
        );
    }

    /**
     * US 01.04.02 Receive notification when not chosen
     * US 02.07.03 Notify cancelled entrants
     */
    public Task<Void> sendCancelledEntrantsNotification(String eventId, String message) {
        return sendNotificationsToListField(
                eventId,
                "cancelledList",
                Notification.TYPE_INFO,
                message
        );
    }

    /**
     * Alias for readability for the "not chosen" story.
     */
    public Task<Void> sendNotSelectedEntrantsNotification(String eventId, String message) {
        return sendCancelledEntrantsNotification(eventId, message);
    }

    /**
     * US 01.05.06 Send entrant notification to join private event
     */
    public Task<Void> sendPrivateWaitlistInviteNotification(String eventId, String message) {
        return sendNotificationsToListField(
                eventId,
                "privateEventInvitedList",
                Notification.TYPE_INVITE,
                message
        );
    }

    private Task<Void> sendNotificationsToListField(String eventId,
                                                    String fieldName,
                                                    String type,
                                                    String message) {
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
                    List<Task<Void>> tasks = new ArrayList<>();

                    for (String userId : userIds) {
                        Notification notification = buildNotification(
                                userId,
                                eventId,
                                type,
                                message,
                                Notification.STATUS_UNREAD
                        );
                        tasks.add(createNotificationTask(notification));
                    }

                    return Tasks.whenAll(tasks);
                });
    }

    private Task<Boolean> canReceiveNotification(String userId, String type) {
        if (Notification.TYPE_INVITE.equals(type)
                || Notification.TYPE_COORGANIZER_INVITE.equals(type)) {
            return Tasks.forResult(true);
        }

        return db.collection("users").document(userId).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        throw (e != null) ? e : new Exception("Failed to load user");
                    }

                    DocumentSnapshot snapshot = task.getResult();
                    if (snapshot == null || !snapshot.exists()) {
                        return true;
                    }

                    Boolean enabled = snapshot.getBoolean("notificationsEnabled");
                    return enabled == null || enabled;
                });
    }

    public void sendCoOrganizerInviteNotification(String userId, String eventId, String message) {
        createNotification(buildNotification(
                userId,
                eventId,
                Notification.TYPE_COORGANIZER_INVITE,
                message,
                Notification.STATUS_UNREAD
        ));
    }

    public void acceptCoOrganizerInvite(Notification notification, NotificationCallback callback) {
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
                    DocumentSnapshot notificationSnap = transaction.get(notificationRef);

                    if (!eventSnap.exists()) {
                        throw new RuntimeException("Event not found");
                    }

                    if (!notificationSnap.exists()) {
                        throw new RuntimeException("Notification not found");
                    }

                    transaction.update(notificationRef, "status", Notification.STATUS_ACCEPTED);
                    transaction.update(eventRef, "coOrganizers", FieldValue.arrayUnion(notification.getUserId()));

                    return null;
                }).addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Accept co-organizer invite failed", e);
                    notifyFailure(callback, e);
                });
    }

    public void declineCoOrganizerInvite(Notification notification, NotificationCallback callback) {
        if (!isValidNotification(notification)) {
            notifyFailure(callback, new RuntimeException("Invalid notification data"));
            return;
        }

        DocumentReference notificationRef = db.collection("notifications")
                .document(notification.getNotificationId());

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot notificationSnap = transaction.get(notificationRef);

                    if (!notificationSnap.exists()) {
                        throw new RuntimeException("Notification not found");
                    }

                    transaction.update(notificationRef, "status", Notification.STATUS_DECLINED);
                    return null;
                }).addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Decline co-organizer invite failed", e);
                    notifyFailure(callback, e);
                });
    }



    private Notification buildNotification(String userId,
                                           String eventId,
                                           String type,
                                           String message,
                                           String status) {
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