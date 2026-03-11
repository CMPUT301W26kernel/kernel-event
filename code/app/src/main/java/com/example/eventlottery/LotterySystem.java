package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles the logic for drawing attendees from an event's waiting list.
 */
public class LotterySystem {

    private static final String TAG = "LotterySystem";
    private final FirebaseFirestore db;

    // Firestore collection and field names
    private static final String EVENTS_COLLECTION = "events";
    private static final String WAITING_LIST_FIELD = "waitingList";
    private static final String INVITED_LIST_FIELD = "invitedList"; // Users who "won" the lottery

    public LotterySystem() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Draws a specified number of entrants from the waiting list.
     * The drawn users are moved from the 'waitingList' to the 'invitedList' in Firestore.
     *
     * @param eventId The ID of the event
     * @param count   The number of entrants to draw
     * @return A Task containing the list of User IDs that were selected
     */
    public Task<List<String>> drawEntrants(String eventId, int count) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        return db.runTransaction((Transaction.Function<List<String>>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (!snapshot.exists()) {
                throw new RuntimeException("Event does not exist.");
            }

            // Get the current waiting list
            List<String> waitingList = (List<String>) snapshot.get(WAITING_LIST_FIELD);
            if (waitingList == null || waitingList.isEmpty()) {
                return new ArrayList<>(); // Nobody to draw
            }

            // Get the current invited list (to append to)
            List<String> invitedList = (List<String>) snapshot.get(INVITED_LIST_FIELD);
            if (invitedList == null) {
                invitedList = new ArrayList<>();
            }

            List<String> selectedUsers = new ArrayList<>();

            // If we want to draw more people than are actually on the list, just select everyone
            if (count >= waitingList.size()) {
                selectedUsers.addAll(waitingList);
                invitedList.addAll(waitingList);
                waitingList.clear();
            } else {
                // Shuffle a copy of the list to randomize selection
                List<String> shuffledList = new ArrayList<>(waitingList);
                Collections.shuffle(shuffledList);

                // Pick the top 'count' users
                for (int i = 0; i < count; i++) {
                    String chosenUserId = shuffledList.get(i);
                    selectedUsers.add(chosenUserId);
                    invitedList.add(chosenUserId);
                    waitingList.remove(chosenUserId);
                }
            }

            // Update Firestore: Save the modified arrays
            transaction.update(eventRef, WAITING_LIST_FIELD, waitingList);
            transaction.update(eventRef, INVITED_LIST_FIELD, invitedList);

            return selectedUsers; // Return the winners so notifications can be sent later
        }).addOnSuccessListener(selectedUsers -> {
            Log.d(TAG, "Successfully drew " + selectedUsers.size() + " entrants for event: " + eventId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to execute lottery draw: ", e);
        });
    }
}