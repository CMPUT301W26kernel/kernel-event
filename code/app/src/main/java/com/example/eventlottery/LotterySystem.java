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
 * Handles the logic for the lottery to draw attendees from an event's waiting list.
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

            // Return both groups in one list:
            // winners first, then a separator marker, then losers
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
                        Notification.TYPE_INVITE
                );
            }

            if (!notSelectedUsers.isEmpty()) {
                notificationRepository.sendBulkNotification(
                        notSelectedUsers,
                        eventId,
                        "You were not selected for this event.",
                        Notification.TYPE_INFO
                );
            }

        }).addOnFailureListener(e ->
                Log.e(TAG, "Failed to execute lottery draw: ", e)
        );
    }

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