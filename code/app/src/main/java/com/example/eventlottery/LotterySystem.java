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
    
    private static final String EVENTS_COLLECTION = "events";
    private static final String WAITING_LIST_FIELD = "waitingList";
    private static final String INVITED_LIST_FIELD = "invitedList";
    private static final String ACCEPTED_LIST_FIELD = "acceptedList";
    private static final String CANCELLED_LIST_FIELD = "cancelledList";

    public LotterySystem() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Draws a specific number of entrants from the waiting list.
     * The drawn users are moved from the 'waitingList' to the 'invitedList' in Firestore.
     *
     * @param eventId The ID of the event
     * @param count   The number of entrants to draw (pass 1 to draw a replacement applicant)
     * @return Task with the list of User IDs that were selected.
     */
    public Task<List<String>> drawEntrants(String eventId, int count) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        return db.runTransaction((Transaction.Function<List<String>>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (!snapshot.exists()) {
                throw new RuntimeException("Event does not exist.");
            }

            List<String> waitingList = (List<String>) snapshot.get(WAITING_LIST_FIELD);
            if (waitingList == null || waitingList.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> invitedList = (List<String>) snapshot.get(INVITED_LIST_FIELD);
            if (invitedList == null) {
                invitedList = new ArrayList<>();
            }

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
        }).addOnSuccessListener(selectedUsers -> Log.d(TAG, "Successfully drew " + selectedUsers.size() + " entrants for event: " + eventId))
          .addOnFailureListener(e -> Log.e(TAG, "Failed to execute lottery draw: ", e));
    }

    /**
     * An invited entrant accepts their invitation.
     * Moves them from 'invitedList' to 'acceptedList'.
     */
    public Task<Void> acceptInvitation(String eventId, String userId) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);
        
        return db.runTransaction((Transaction.Function<Void>) transaction -> {
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
        
        return db.runTransaction((Transaction.Function<Void>) transaction -> {
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