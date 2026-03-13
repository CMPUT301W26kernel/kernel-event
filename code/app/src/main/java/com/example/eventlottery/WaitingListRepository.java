package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for managing the waiting list in Firestore.
 * Handles adding/removing users from an event's waiting list, checking capacity,
 * and fetching the current entrants.
 */
public class WaitingListRepository {

    private static final String TAG = "WaitingListRepo";
    private final FirebaseFirestore db;
    private static final String EVENTS_COLLECTION = "events";
    private static final String WAITING_LIST_FIELD = "waitingList";

    public WaitingListRepository() {
        this(FirebaseFirestore.getInstance());
    }

    WaitingListRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Attempts to add a user to the waiting list for an event.
     * Checks if the user is already on the list and if the list has reached the optional limit.
     *
     * @param eventId The ID of the event
     * @param userId  The ID of the user joining
     * @return Task as true on success and an exception on failure.
     */
    public Task<Void> joinWaitingList(String eventId, String userId) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        return db.<Void>runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (!snapshot.exists()) {
                throw new RuntimeException("Event does not exist.");
            }

            @SuppressWarnings("unchecked")
            List<String> currentList = (List<String>) snapshot.get(WAITING_LIST_FIELD);
            if (currentList == null) {
                currentList = new ArrayList<>();
            }

            if (currentList.contains(userId)) {
                throw new RuntimeException("User is already on the waiting list.");
            }

            Long capacityLong = snapshot.getLong("waitingListCapacity");
            if (capacityLong != null && capacityLong > 0) {
                if (currentList.size() >= capacityLong) {
                    throw new RuntimeException("Waiting list is full.");
                }
            }

            transaction.update(eventRef, WAITING_LIST_FIELD, FieldValue.arrayUnion(userId));
            return null;
        }).addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully joined waiting list for event: " + eventId))
          .addOnFailureListener(e -> Log.e(TAG, "Failed to join waiting list: ", e));
    }

    /**
     * Removes a user from the waiting list for an event.
     *
     * @param eventId The ID of the event
     * @param userId  The ID of the user leaving
     * @return Task of async operation
     */
    public Task<Void> leaveWaitingList(String eventId, String userId) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        return eventRef.update(WAITING_LIST_FIELD, FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully left waiting list for event: " + eventId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to leave waiting list: ", e));
    }

    /**
     * Fetches the current waiting list for a specific event.
     *
     * @param eventId The ID of the event
     * @return Task of the list of User IDs on the waiting list
     */
    public Task<List<String>> getWaitingList(String eventId) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        return eventRef.get().continueWith(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) snapshot.get(WAITING_LIST_FIELD);
                    return list != null ? list : new ArrayList<>();
                }
            }
            return new ArrayList<>();
        });
    }
}