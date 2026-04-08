package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final String INVITED_LIST_FIELD = "invitedList";
    private static final String ACCEPTED_LIST_FIELD = "acceptedList";
    private static final String CANCELLED_LIST_FIELD = "cancelledList";
    private static final String WAITING_LIST_JOIN_GEO = "waitingListJoinGeo";
    private static final double DEFAULT_RADIUS_METERS = 500.0;

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
        return joinWaitingList(eventId, userId, null, null);
    }

    /**
     * Adds a user to the waiting list. When the event requires geolocation verification,
     * {@code userLat} and {@code userLng} must be the device-reported WGS84 coordinates.
     */
    public Task<Void> joinWaitingList(String eventId, String userId, Double userLat, Double userLng) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        return db.<Void>runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (!snapshot.exists()) {
                throw new RuntimeException("Event does not exist.");
            }

            List<String> waitingList = getStringList(snapshot, WAITING_LIST_FIELD);
            List<String> invitedList = getStringList(snapshot, INVITED_LIST_FIELD);
            List<String> acceptedList = getStringList(snapshot, ACCEPTED_LIST_FIELD);
            List<String> cancelledList = getStringList(snapshot, CANCELLED_LIST_FIELD);
            List<String> coOrganizers = getStringList(snapshot, "coOrganizers");

            String joinValidationError = validateJoinEligibility(
                    userId,
                    snapshot.getString("organizerId"),
                    waitingList,
                    invitedList,
                    acceptedList,
                    cancelledList,
                    coOrganizers
            );
            if (joinValidationError != null) {
                throw new RuntimeException(joinValidationError);
            }

            Long capacityLong = snapshot.getLong("waitingListCapacity");
            if (capacityLong != null && capacityLong > 0) {
                if (waitingList.size() >= capacityLong) {
                    throw new RuntimeException("Waiting list is full.");
                }
            }

            boolean requireGeo = Boolean.TRUE.equals(snapshot.getBoolean("requireGeolocationForWaitlist"));
            Double venueLat = readNumericField(snapshot, "venueLatitude");
            Double venueLng = readNumericField(snapshot, "venueLongitude");
            if (requireGeo) {
                if (venueLat == null || venueLng == null) {
                    throw new RuntimeException("Organizer must set a venue location for geolocation verification.");
                }
                if (userLat == null || userLng == null) {
                    throw new RuntimeException("Location permission and device location are required to join this waitlist.");
                }
                double radiusMeters = DEFAULT_RADIUS_METERS;
                Double r = readNumericField(snapshot, "geolocationRadiusMeters");
                if (r != null && r > 0) {
                    radiusMeters = r;
                }
                double distance = GeoUtils.haversineMeters(userLat, userLng, venueLat, venueLng);
                if (Double.isNaN(distance) || distance > radiusMeters) {
                    throw new RuntimeException("You are too far from the event location to join the waiting list.");
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put(WAITING_LIST_FIELD, FieldValue.arrayUnion(userId));
            if (userLat != null && userLng != null) {
                Map<String, Object> geo = new HashMap<>();
                geo.put("lat", userLat);
                geo.put("lng", userLng);
                geo.put("at", System.currentTimeMillis());
                updates.put(WAITING_LIST_JOIN_GEO + "." + userId, geo);
            }
            transaction.update(eventRef, updates);
            return null;
        }).addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully joined waiting list for event: " + eventId))
          .addOnFailureListener(e -> Log.e(TAG, "Failed to join waiting list: ", e));
    }

    private static Double readNumericField(DocumentSnapshot snapshot, String key) {
        if (!snapshot.contains(key)) {
            return null;
        }
        Object v = snapshot.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return null;
    }

    static String validateJoinEligibility(
            String userId,
            String organizerId,
            List<String> waitingList,
            List<String> invitedList,
            List<String> acceptedList,
            List<String> cancelledList,
            List<String> coOrganizers
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            return "A signed-in user is required to join the waiting list.";
        }
        if (userId.equals(organizerId) || (coOrganizers != null && coOrganizers.contains(userId))) {
            return "Organizers cannot join their own event's waiting list.";
        }
        if (waitingList != null && waitingList.contains(userId)) {
            return "User is already on the waiting list.";
        }
        if (invitedList != null && invitedList.contains(userId)) {
            return "User has already been invited to this event.";
        }
        if (acceptedList != null && acceptedList.contains(userId)) {
            return "User has already accepted an invitation to this event.";
        }
        if (cancelledList != null && cancelledList.contains(userId)) {
            return "User has already responded to this event and cannot rejoin.";
        }
        return null;
    }

    private static List<String> getStringList(DocumentSnapshot snapshot, String key) {
        Object value = snapshot.get(key);
        List<String> result = new ArrayList<>();
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
     * Removes a user from the waiting list for an event.
     *
     * @param eventId The ID of the event
     * @param userId  The ID of the user leaving
     * @return Task of async operation
     */
    public Task<Void> leaveWaitingList(String eventId, String userId) {
        DocumentReference eventRef = db.collection(EVENTS_COLLECTION).document(eventId);

        Map<String, Object> updates = new HashMap<>();
        updates.put(WAITING_LIST_FIELD, FieldValue.arrayRemove(userId));
        updates.put(WAITING_LIST_JOIN_GEO + "." + userId, FieldValue.delete());
        return eventRef.update(updates)
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
