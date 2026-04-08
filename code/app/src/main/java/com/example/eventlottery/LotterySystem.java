package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles organizer lottery draws and invitation state transitions.
 *
 * Waiting-list state model:
 * - Selected entrants move from waitingList to invitedList.
 * - Entrants not selected in a draw stay on waitingList for future replacement draws.
 * - cancelledList is reserved for entrants who were previously invited and then declined
 *   or were cancelled later.
 */
public class LotterySystem {

    private static final String TAG = "LotterySystem";

    private static final String EVENTS_COLLECTION = "events";
    private static final String WAITING_LIST_FIELD = "waitingList";
    private static final String INVITED_LIST_FIELD = "invitedList";
    private static final String ACCEPTED_LIST_FIELD = "acceptedList";
    private static final String CANCELLED_LIST_FIELD = "cancelledList";

    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;

    private static final class DrawResult {
        final List<String> selectedUsers;
        final List<String> notSelectedUsers;

        DrawResult(List<String> selectedUsers, List<String> notSelectedUsers) {
            this.selectedUsers = selectedUsers;
            this.notSelectedUsers = notSelectedUsers;
        }
    }

    public LotterySystem() {
        this(FirebaseFirestore.getInstance());
    }

    private LotterySystem(FirebaseFirestore db) {
        this(db, new NotificationRepository(db));
    }

    LotterySystem(FirebaseFirestore db, NotificationRepository notificationRepository) {
        this.db = db;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Safely extracts a list of strings from a Firestore document field.
     */
    private List<String> getListSafely(DocumentSnapshot snapshot, String field) {
        List<String> result = new ArrayList<>();
        Object value = snapshot.get(field);
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
        }
        return result;
    }

    /**
     * Randomly selects up to {@code count} entrants from the current waiting list.
     *
     * Selected entrants are moved into invitedList. Everyone else stays on waitingList so the
     * organizer can draw replacements later if invitations are declined.
     *
     * @return a task containing only the entrants selected in this draw
     */
    public Task<List<String>> drawEntrants(String eventId, int count) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        Task<DrawResult> drawTask = db.<DrawResult>runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (!snapshot.exists()) {
                throw new RuntimeException("Event does not exist.");
            }

            List<String> waitingList = getListSafely(snapshot, WAITING_LIST_FIELD);
            if (waitingList.isEmpty()) {
                return new DrawResult(new ArrayList<>(), new ArrayList<>());
            }

            List<String> invitedList = getListSafely(snapshot, INVITED_LIST_FIELD);
            List<String> shuffledList = new ArrayList<>(waitingList);
            Collections.shuffle(shuffledList);

            int actualCount = Math.min(count, shuffledList.size());
            List<String> selectedUsers = new ArrayList<>();
            List<String> notSelectedUsers = new ArrayList<>();

            for (int i = 0; i < actualCount; i++) {
                String userId = shuffledList.get(i);
                selectedUsers.add(userId);
                if (!invitedList.contains(userId)) {
                    invitedList.add(userId);
                }
            }

            for (int i = actualCount; i < shuffledList.size(); i++) {
                notSelectedUsers.add(shuffledList.get(i));
            }

            transaction.update(eventRef, WAITING_LIST_FIELD, new ArrayList<>(notSelectedUsers));
            transaction.update(eventRef, INVITED_LIST_FIELD, invitedList);

            return new DrawResult(selectedUsers, notSelectedUsers);
        });

        drawTask.addOnSuccessListener(result -> {
            if (result == null) {
                return;
            }

            Log.d(TAG, "Successfully drew " + result.selectedUsers.size() + " entrants for event: " + eventId);

            if (!result.selectedUsers.isEmpty()) {
                notificationRepository.sendBulkNotification(
                        result.selectedUsers,
                        eventId,
                        "Congratulations! You have been selected from the waiting list. Please accept or decline your invitation.",
                        NotificationType.INVITE
                );
            }

            if (!result.notSelectedUsers.isEmpty()) {
                notificationRepository.sendBulkNotification(
                        result.notSelectedUsers,
                        eventId,
                        "You were not selected for this event.",
                        NotificationType.INFO
                );
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to execute lottery draw", e));

        return drawTask.continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception error = task.getException();
                throw error != null ? error : new RuntimeException("Failed to draw entrants.");
            }

            DrawResult result = task.getResult();
            return result != null ? result.selectedUsers : new ArrayList<>();
        });
    }

    /**
     * Moves a user from invitedList to acceptedList.
     */
    public Task<Void> acceptInvitation(String eventId, String userId) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        return db.<Void>runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            if (!snapshot.exists()) {
                throw new RuntimeException("Event does not exist.");
            }

            transaction.update(eventRef, INVITED_LIST_FIELD, FieldValue.arrayRemove(userId));
            transaction.update(eventRef, ACCEPTED_LIST_FIELD, FieldValue.arrayUnion(userId));
            return null;
        }).addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " accepted invitation to " + eventId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to accept invitation", e));
    }

    /**
     * Removes a user from invitation-related lists and adds them to cancelledList.
     */
    public Task<Void> declineOrCancelInvitation(String eventId, String userId) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        return db.<Void>runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            if (!snapshot.exists()) {
                throw new RuntimeException("Event does not exist.");
            }

            transaction.update(eventRef, WAITING_LIST_FIELD, FieldValue.arrayRemove(userId));
            transaction.update(eventRef, INVITED_LIST_FIELD, FieldValue.arrayRemove(userId));
            transaction.update(eventRef, ACCEPTED_LIST_FIELD, FieldValue.arrayRemove(userId));
            transaction.update(eventRef, CANCELLED_LIST_FIELD, FieldValue.arrayUnion(userId));
            return null;
        }).addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " declined/was cancelled from " + eventId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to decline/cancel invitation", e));
    }
}
