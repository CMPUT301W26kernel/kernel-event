package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

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
        this(FirebaseFirestore.getInstance(), new NotificationRepository(FirebaseFirestore.getInstance()));
    }

    LotterySystem(FirebaseFirestore db, NotificationRepository notificationRepository) {
        this.db = db;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Helper to safely extract a list of strings from a Firestore snapshot 
     * without triggering unchecked cast warnings.
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
     * Draws a specified number of entrants from the waiting list.
     * The drawn users are moved from the 'waitingList' to the 'invitedList' in Firestore.
     *
     * @param eventId The ID of the event
     * @param count   The number of entrants to draw (pass 1 to draw a replacement applicant)
     * @return Task with the list of User IDs that were selected.
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

            List<String> invitedList = getListSafely(snapshot, INVITED_LIST_FIELD);
            List<String> selectedUsers = new ArrayList<>();

            if (count >= waitingList.size()) {
                selectedUsers.addAll(waitingList);
                invitedList.addAll(waitingList);
                waitingList.clear();
            } else {
                List<String> shuffledList = new ArrayList<>(waitingList);
                Collections.shuffle(shuffledList);

                for (int i = 0; i < count; i++) {
                    String chosenUserId = shuffledList.get(i);
                    selectedUsers.add(chosenUserId);
                    invitedList.add(chosenUserId);
                    waitingList.remove(chosenUserId);
                }
            }

            transaction.update(eventRef, WAITING_LIST_FIELD, waitingList);
            transaction.update(eventRef, INVITED_LIST_FIELD, invitedList);

            return selectedUsers;
        }).addOnSuccessListener(selectedUsers -> {
            Log.d(TAG, "Successfully drew " + selectedUsers.size() + " entrants for event: " + eventId);
            
            if (!selectedUsers.isEmpty()) {
                notificationRepository.sendBulkNotification(
                    selectedUsers, 
                    eventId, 
                    "Congratulations! You have been selected from the waiting list. Please accept or decline your invitation."
                );
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to execute lottery draw: ", e));
    }

    /**
     * An invited entrant accepts their invitation.
     * Moves them from 'invitedList' to 'acceptedList'.
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
     * An invited entrant declines their invitation OR an Organizer cancels them.
     * Moves them from 'invitedList' to 'cancelledList'.
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