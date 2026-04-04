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
 * LotterySystem
 *
 * Handles the logic for drawing entrants from an event's waiting list.
 *
 * UPDATED DESIGN (04-03):
 * - Separates entrants into two groups:
 *   1. Selected entrants are added to invitedList
 *   2. Non-selected entrants are added to cancelledList
 *
 * WHY:
 * - The notification system requires both groups to be explicitly known so that:
 *     - selected users receive invitation notifications
 *     - non-selected users receive "not selected" notifications
 *
 * NOTE:
 * - This introduces a dependency between lottery logic and notifications.
 *
 * @author Radwa Sheikhdon
 */
public class LotterySystem {

    private static final String TAG = "LotterySystem";
    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;
    
    private static final String EVENTS_COLLECTION = "events";
    private static final String WAITING_LIST_FIELD = "waitingList";
    private static final String INVITED_LIST_FIELD = "invitedList";
    private static final String ACCEPTED_LIST_FIELD = "acceptedList";
    private static final String CANCELLED_LIST_FIELD = "cancelledList";

    public LotterySystem() {
        this(FirebaseFirestore.getInstance());
    }

    private LotterySystem(FirebaseFirestore db) {
        this.db = db;
        this.notificationRepository = new NotificationRepository(this.db);
    }

    LotterySystem(FirebaseFirestore db, NotificationRepository notificationRepository) {
        this.db = db;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Safely extracts a list of Strings from a Firestore document field.
     *
     * Prevents ClassCastException by validating types at runtime.
     *
     * @param snapshot Firestore document snapshot
     * @param field the field name to extract
     * @return a list of strings (empty if field is missing or invalid)
     */
    private List<String> getListSafely(DocumentSnapshot snapshot, String field) {
        List<String> result = new ArrayList<>();
        Object obj = snapshot.get(field);
        if (obj instanceof List<?>) {
            for (Object item : (List<?>) obj) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
        }
        return result;
    }

    /**
     * Draws entrants from the waiting list using a randomized selection.
     *
     * Behavior:
     * - Randomly selects up to `count` users from the waiting list
     * - Splits users into:
     *   - selectedUsers → added to invitedList
     *   - notSelectedUsers → added to cancelledList
     * - Clears the waiting list after the draw
     *
     * Firestore updates:
     * - waitingList → cleared
     * - invitedList → updated with selected users
     * - cancelledList → updated with non-selected users
     *
     * Notification integration:
     * - Selected users receive invitation notifications
     * - Non-selected users receive "not selected" notifications
     *
     * Return value:
     * - Returns a combined list containing:
     *     [selectedUsers, "__LOSERS_SEPARATOR__", notSelectedUsers]
     * - This is used internally to separate results after transaction completion
     *
     * NOTE:
     * - The separator approach is a temporary design to return both groups
     * - A structured result object would be a cleaner long term solution
     *
     * @param eventId the event to perform the draw on
     * @param count the number of users to select
     * @return a Task containing both selected and non-selected users
     */
    public Task<List<String>> drawEntrants(String eventId, int count) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        return db.<List<String>>runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (!snapshot.exists()) {
                throw new RuntimeException("Event does not exist.");
            }

            List<String> waitingList = getListSafely(snapshot, WAITING_LIST_FIELD);
            if (waitingList.isEmpty()) {
                return new ArrayList<>();
            }
            // Separates the lists in the transaction to distinguish between winners and losers
            List<String> invitedList = getListSafely(snapshot, INVITED_LIST_FIELD);
            List<String> cancelledList = getListSafely(snapshot, CANCELLED_LIST_FIELD);

            List<String> selectedUsers = new ArrayList<>();
            List<String> notSelectedUsers = new ArrayList<>();

            List<String> shuffledList = new ArrayList<>(waitingList);
            Collections.shuffle(shuffledList);

            int actualCount = Math.min(count, shuffledList.size());

            for (int i = 0; i < actualCount; i++) {
                String userId = shuffledList.get(i);
                selectedUsers.add(userId);
                if (!invitedList.contains(userId)) {
                    invitedList.add(userId);
                }
            }

            for (int i = actualCount; i < shuffledList.size(); i++) {
                String userId = shuffledList.get(i);
                notSelectedUsers.add(userId);
                if (!cancelledList.contains(userId)) {
                    cancelledList.add(userId);
                }
            }

            waitingList.clear();

            transaction.update(eventRef, WAITING_LIST_FIELD, waitingList);
            transaction.update(eventRef, INVITED_LIST_FIELD, invitedList);
            transaction.update(eventRef, CANCELLED_LIST_FIELD, cancelledList);

            // Returns both groups in one list. winners first, then a separator marker, then losers.
            List<String> result = new ArrayList<>();
            result.addAll(selectedUsers);
            result.add("__LOSERS_SEPARATOR__");
            result.addAll(notSelectedUsers);

            return result;

        }).addOnSuccessListener(result -> {
            int separatorIndex = result.indexOf("__LOSERS_SEPARATOR__");

            List<String> selectedUsers;
            List<String> notSelectedUsers;

            if (separatorIndex >= 0) {
                selectedUsers = new ArrayList<>(result.subList(0, separatorIndex));
                notSelectedUsers = new ArrayList<>(result.subList(separatorIndex + 1, result.size()));
            } else {
                selectedUsers = result;
                notSelectedUsers = new ArrayList<>();
            }

            Log.d(TAG, "Successfully drew " + selectedUsers.size() + " entrants for event: " + eventId);

            if (!selectedUsers.isEmpty()) {
                notificationRepository.sendBulkNotification(
                        selectedUsers,
                        eventId,
                        "Congratulations! You have been selected from the waiting list. Please accept or decline your invitation.",
                        NotificationType.INVITE
                );
            }

            if (!notSelectedUsers.isEmpty()) {
                notificationRepository.sendBulkNotification(
                        notSelectedUsers,
                        eventId,
                        "You were not selected for this event.",
                        NotificationType.INFO
                );
            }

        }).addOnFailureListener(e ->
                Log.e(TAG, "Failed to execute lottery draw: ", e)
        );
    }

    /**
     * Moves a user from invitedList → acceptedList.
     *
     * Called when a user accepts an invitation.
     */
    public Task<Void> acceptInvitation(String eventId, String userId) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);
        
        return db.<Void>runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            if (!snapshot.exists()) throw new RuntimeException("Event does not exist.");
            
            transaction.update(eventRef, INVITED_LIST_FIELD, FieldValue.arrayRemove(userId));
            transaction.update(eventRef, ACCEPTED_LIST_FIELD, FieldValue.arrayUnion(userId));
            return null;
        }).addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " accepted invitation to " + eventId))
          .addOnFailureListener(e -> Log.e(TAG, "Failed to accept invitation", e));
    }

    /**
     * Removes a user from all active lists and adds them to cancelledList.
     *
     * Called when a user declines an invitation or is removed.
     */
    public Task<Void> declineOrCancelInvitation(String eventId, String userId) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);
        
        return db.<Void>runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);
            if (!snapshot.exists()) throw new RuntimeException("Event does not exist.");

            transaction.update(eventRef, WAITING_LIST_FIELD, FieldValue.arrayRemove(userId));
            transaction.update(eventRef, INVITED_LIST_FIELD, FieldValue.arrayRemove(userId));
            transaction.update(eventRef, ACCEPTED_LIST_FIELD, FieldValue.arrayRemove(userId));
            
            transaction.update(eventRef, CANCELLED_LIST_FIELD, FieldValue.arrayUnion(userId));
            return null;
        }).addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " declined/was cancelled from " + eventId))
          .addOnFailureListener(e -> Log.e(TAG, "Failed to decline/cancel invitation", e));
    }
}
