package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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

    /**
     * Marks a notification as read.
     */
    public void markAsRead(Notification notification, NotificationCallback callback) {
        if (notification == null || notification.getNotificationId() == null) {
            notifyFailure(callback, new Exception("Invalid notificationId"));
            return;
        }

        db.collection("notifications")
                .document(notification.getNotificationId())
                .update("status", NotificationStatus.READ.name())
                .addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> notifyFailure(callback, e));
    }

    /**
     * Entrant can accept invitation to register and
     * accept private waiting list invitation.
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

                    List<String> privateInviteList =
                            getStringList(eventSnap.get("privateEventInvitedList"));

                    boolean isPrivateWaitlistInvite =
                            privateInviteList.contains(notification.getUserId());

                    transaction.update(notificationRef, "status", NotificationStatus.ACCEPTED.name());

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
     * Entrant can decline invitation to register
     * and decline private waiting list invitation.
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

                    transaction.update(notificationRef, "status", NotificationStatus.DECLINED.name());

                    if (isPrivateWaitlistInvite) {
                        transaction.update(eventRef, "privateEventInvitedList",
                                FieldValue.arrayRemove(notification.getUserId()));
                    } else {
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

    /**
     * Creates a notification.
     */
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

    /**
     * Creates a notification only if the user can receive it.
     */
    private Task<Void> createNotificationTask(Notification notification) {
        if (notification == null) {
            return Tasks.forException(new IllegalArgumentException("Notification cannot be null"));
        }

        return canReceiveNotification(notification.getUserId(), notification.getTypeEnum())
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

    /**
     * Sends the same notification to multiple users.
     */
    public void sendBulkNotification(List<String> userIds,
                                     String eventId,
                                     String message,
                                     NotificationType type) {
        if (userIds == null || userIds.isEmpty()) return;

        for (String userId : userIds) {
            createNotification(buildNotification(
                    userId,
                    eventId,
                    type,
                    message,
                    NotificationStatus.UNREAD
            ));
        }
    }

    /**
     * Organizer notifies the waiting list.
     */
    public void sendWaitingListNotification(String eventId, String message) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    List<String> waitingList = getStringList(doc.get("waitingList"));
                    if (waitingList.isEmpty()) return;

                    List<Task<String>> tasks = new ArrayList<>();

                    for (String userId : waitingList) {
                        tasks.add(createNotificationTaskWithResult(buildNotification(
                                userId,
                                eventId,
                                NotificationType.INFO,
                                message,
                                NotificationStatus.UNREAD
                        )));
                    }

                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(results -> {
                                List<String> successfulRecipients = new ArrayList<>();

                                for (Object result : results) {
                                    if (result instanceof String) {
                                        String userId = (String) result;
                                        if (userId != null) {
                                            successfulRecipients.add(userId);
                                        }
                                    }
                                }

                                String senderId = FirebaseAuth.getInstance().getCurrentUser() != null
                                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                                        : null;

                                if (senderId != null && !successfulRecipients.isEmpty()) {
                                    createNotificationLog(
                                            senderId,
                                            eventId,
                                            successfulRecipients,
                                            NotificationType.INFO,
                                            message
                                    ).addOnFailureListener(e ->
                                            Log.e(TAG, "Failed to create notification log", e));
                                }
                            })
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed to send waiting list notifications", e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to read waiting list", e));
    }

    /**
     * Organizer notifies selected entrants.
     */
    public Task<Void> sendInvitedUsersNotification(String eventId, String message) {
        return sendNotificationsToListField(
                eventId,
                "invitedList",
                NotificationType.INVITE,
                message
        );
    }

    /**
     * Organizer notifies cancelled entrants.
     */
    public Task<Void> sendCancelledEntrantsNotification(String eventId, String message) {
        return sendNotificationsToListField(
                eventId,
                "cancelledList",
                NotificationType.INFO,
                message
        );
    }

    public Task<Void> sendNotSelectedEntrantsNotification(String eventId, String message) {
        return sendCancelledEntrantsNotification(eventId, message);
    }

    /**
     * Sends private waitlist invite notifications.
     */
    public Task<Void> sendPrivateWaitlistInviteNotification(String eventId, String message) {
        return sendNotificationsToListField(
                eventId,
                "privateEventInvitedList",
                NotificationType.INVITE,
                message
        );
    }

    /**
     * Creates a notification task and returns the recipient ID if successful.
     */
    private Task<String> createNotificationTaskWithResult(Notification notification) {
        if (notification == null) {
            return Tasks.forException(new IllegalArgumentException("Notification cannot be null"));
        }

        return canReceiveNotification(notification.getUserId(), notification.getTypeEnum())
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

                    return notificationRef.set(notification)
                            .continueWith(setTask -> {
                                if (!setTask.isSuccessful()) {
                                    Exception e = setTask.getException();
                                    throw (e != null) ? e : new Exception("Failed to create notification");
                                }
                                return notification.getUserId();
                            });
                });
    }

    /**
     * Sends notifications to every user ID stored in a given event list field.
     */
    private Task<Void> sendNotificationsToListField(String eventId,
                                                    String fieldName,
                                                    NotificationType type,
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
                    List<Task<String>> tasks = new ArrayList<>();

                    for (String userId : userIds) {
                        Notification notification = buildNotification(
                                userId,
                                eventId,
                                type,
                                message,
                                NotificationStatus.UNREAD
                        );
                        tasks.add(createNotificationTaskWithResult(notification));
                    }

                    return Tasks.whenAllSuccess(tasks).continueWithTask(resultsTask -> {
                        List<?> results = resultsTask.getResult();
                        List<String> successfulRecipients = new ArrayList<>();

                        if (results != null) {
                            for (Object result : results) {
                                if (result instanceof String) {
                                    String userId = (String) result;
                                    if (userId != null) {
                                        successfulRecipients.add(userId);
                                    }
                                }
                            }
                        }

                        String senderId = FirebaseAuth.getInstance().getCurrentUser() != null
                                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                                : null;

                        if (senderId != null && !successfulRecipients.isEmpty()) {
                            return createNotificationLog(
                                    senderId,
                                    eventId,
                                    successfulRecipients,
                                    type,
                                    message
                            );
                        }

                        return Tasks.forResult(null);
                    });
                });
    }

    /**
     * Checks whether a user can receive a given notification type.
     * Invite types always bypass the user preference toggle.
     */
    private Task<Boolean> canReceiveNotification(String userId, NotificationType type) {
        if (type == NotificationType.INVITE
                || type == NotificationType.COORGANIZER_INVITE) {
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

    /**
     * Sends a co-organizer invite notification.
     */
    public void sendCoOrganizerInviteNotification(String userId, String eventId, String message) {
        Notification notification = buildNotification(
                userId,
                eventId,
                NotificationType.COORGANIZER_INVITE,
                message,
                NotificationStatus.UNREAD
        );

        createNotification(notification);

        String senderId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (senderId != null) {
            List<String> recipients = new ArrayList<>();
            recipients.add(userId);

            createNotificationLog(
                    senderId,
                    eventId,
                    recipients,
                    NotificationType.COORGANIZER_INVITE,
                    message
            ).addOnFailureListener(e ->
                    Log.e(TAG, "Failed to log co-organizer invite", e));
        }
    }

    /**
     * Accepts a co-organizer invite.
     */
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

                    transaction.update(notificationRef, "status", NotificationStatus.ACCEPTED.name());
                    transaction.update(eventRef, "coOrganizers", FieldValue.arrayUnion(notification.getUserId()));

                    return null;
                }).addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Accept co-organizer invite failed", e);
                    notifyFailure(callback, e);
                });
    }

    /**
     * Declines a co-organizer invite.
     */
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

                    transaction.update(notificationRef, "status", NotificationStatus.DECLINED.name());
                    return null;
                }).addOnSuccessListener(aVoid -> notifySuccess(callback))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Decline co-organizer invite failed", e);
                    notifyFailure(callback, e);
                });
    }

    /**
     * Creates a notification log entry.
     */
    private Task<Void> createNotificationLog(String senderId,
                                             String eventId,
                                             List<String> recipientIds,
                                             NotificationType type,
                                             String message) {
        if (senderId == null || eventId == null || recipientIds == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid log data"));
        }

        DocumentReference logRef = db.collection("notification_logs").document();

        NotificationLog log = new NotificationLog();
        log.setLogId(logRef.getId());
        log.setSenderId(senderId);
        log.setEventId(eventId);
        log.setRecipientIds(new ArrayList<>(recipientIds));
        log.setRecipientCount(recipientIds.size());
        log.setType(type.name());
        log.setMessage(message);
        log.setTimestamp(Timestamp.now());

        return logRef.set(log);
    }

    /**
     * Builds a notification object.
     */
    private Notification buildNotification(String userId,
                                           String eventId,
                                           NotificationType type,
                                           String message,
                                           NotificationStatus status) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setEventId(eventId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setStatus(status);
        notification.setTimestamp(Timestamp.now());
        return notification;
    }

    /**
     * Safely converts an Object field to a list of strings.
     */
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

    /**
     * Checks that a notification contains the IDs required for actions.
     */
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