/**
 * Create Event Fragment
 * Allows Admin and Organizers to create and edit events
 * Last Modified: 2026-03-09 by Grace MacKenzie
 *
 * @author Grace MacKenzie
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A Fragment which allows users to create and edit events.
 */
public class CreateEventFragment extends Fragment {

    // ATTRIBUTES

    private FirebaseFirestore db;
    private CollectionReference eventsRef;

    private Event currentEvent;

    // CONSTRUCTORS

    /**
     * An empty constructor for the CreateEventFragment. DO.NOT.USE.THIS.OUTSIDE.OF.THIS.CLASS.
     * Please instead use newInstanceCreateMode or newInstanceEditMode.
     *
     * @see CreateEventFragment#newInstanceCreateMode(String)
     * @see CreateEventFragment#newInstanceEditMode(String)
     */
    public CreateEventFragment() {
        // Required empty public constructor
    }

    // FUNCTIONS

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // DECLARE VARS
        super.onViewCreated(view, savedInstanceState);
        String eventId = getArguments() != null ? getArguments().getString("eventId") : null;
        String organizerId = getArguments() != null ? getArguments().getString("organizerId") : null;

        // FIRESTORE STUFF

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");

        // UI CHANGES FOR EDIT MODE

        if (eventId != null) { // Edit Mode
            // Set current Event to the Event whose id was in the bundle
            db.collection("events")
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        currentEvent = doc.toObject(Event.class);
                    });

            // TODO: set fields to contain the data of currentEvent

            /*
            See the following for time and date stuff
            https://docs.oracle.com/javase/8/docs/api/java/text/DateFormat.html
            https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
            */

            // TODO: change cancel button to delete button and positive button to save button

        } // else: leave fragment as is in creation mode

        // BUTTON LISTENERS

        Button positiveButton = view.findViewById(R.id.confirm_button);
        positiveButton.setOnClickListener(v -> {

            if ( eventId == null ) { // Creation mode
                // TODO: create an event using the user input and add it to firebase
                Date date = new Date();
                Event newEvent = new Event("test title", "test description", "pretend organizer id", date, date, null);
                addEvent(newEvent);
            } else { // Event mode
                // TODO: set the attributes of currentEvent to whatever the user has input
                updateEvent(currentEvent);
            }

            // Navigate to EventOverviewFragment
            EventOverviewFragment fragment = new EventOverviewFragment(); // TODO: change to newInstance method if one is created
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        Button negativeButton = view.findViewById(R.id.cancel_button);
        negativeButton.setOnClickListener(v -> {
            if (eventId != null) { // Edit mode
                deleteEvent(eventId);
                // TODO: Set up confirmation dialog fragment
            }

            HomePageFragment fragment = new HomePageFragment(); // TODO: change to newInstance method if one is created
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    /**
     * Returns a new instance of the CreateEventFragment in create mode. To load
     * CreateEventFragment in edit mode, pass an eventId as an argument.
     * <p>
     * Usage:
     * CreateEventFragment fragment = CreateEventFragment
     *                     .newInstanceCreateMode(Organizer.getOrganizerId());
     * <p>
     * // Then use the fragment in your fragment transaction
     * requireActivity().getSupportFragmentManager()
     *                     .beginTransaction()
     *                     .replace(R.id.fragment_container, fragment)
     *                     .addToBackStack(null)
     *                     .commit();
     *
     * @param organizerId The Firestore organizer id used to reference the relevant organizer
     * @return A new CreateEventFragment which can be used in fragment transactions
     */
    public static CreateEventFragment newInstanceCreateMode(String organizerId) {
        CreateEventFragment fragment = new CreateEventFragment();
        Bundle bundle = new Bundle();
        bundle.putString("organizerId", organizerId);
        fragment.setArguments(bundle);
        return fragment;
    }

    /**
     * Returns a new instance of the CreateEventFragment in edit mode. to load
     * CreateEventFragment in create mode, pass this function without an argument.
     * <p>
     * Usage:
     * CreateEventFragment fragment = CreateEventFragment
     *                     .newInstanceEditMode(myEvent.getEventId());
     * <p>
     * // Then use the fragment in your fragment transaction
     * requireActivity().getSupportFragmentManager()
     *                     .beginTransaction()
     *                     .replace(R.id.fragment_container, fragment)
     *                     .addToBackStack(null)
     *                     .commit();
     *
     * @param eventId The Firestore event id used to load an event from Firestore
     * @return A new CreateEventFragment which can be used in fragment transactions
     */
    public static CreateEventFragment newInstanceEditMode(String eventId) {
        CreateEventFragment fragment = new CreateEventFragment();
        Bundle bundle = new Bundle();
        bundle.putString("eventId", eventId);
        fragment.setArguments(bundle);
        return fragment;
    }

    // HELPER METHODS

    /**
     * Adds a new Event to the Firestore database and generates a unique eventId for the added Event
     * @param event The Event to add to Firestore
     */
    private void addEvent(Event event) {
        DocumentReference docRef = eventsRef.document();
        String generatedId = docRef.getId();
        event.setEventId(generatedId);
        docRef.set(event);
    }

    /**
     * Saves updated Event to Firestore
     * @param updatedEvent The Event with updates made to it
     */
    private void updateEvent(Event updatedEvent) {
        db.collection("events")
                .document(updatedEvent.getEventId())
                .set(updatedEvent);
    }

    /**
     * Deletes an Event from Firestore
     * @param eventId The Firestore id of the Event
     */
    private void deleteEvent(String eventId) {
        DocumentReference docRef = eventsRef.document(eventId);
        docRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("DELETE", "City deleted from Firestore"))
                .addOnFailureListener(e -> Log.e("DELETE", "Error deleting city", e));
    }

}