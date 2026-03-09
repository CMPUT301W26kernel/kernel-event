/**
 * Create Event Fragment
 * Allows Admin and Organizers to create and edit events
 * Last Modified: 2026-03-08 by Grace MacKenzie
 *
 * @author Grace MacKenzie
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.firebase.Firebase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A Fragment which allows users to create and edit events.
 */
public class CreateEventFragment extends Fragment {

    // ATTRIBUTES

    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    // CONSTRUCTORS

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
        super.onViewCreated(view, savedInstanceState);
        String eventId = getArguments() != null ? getArguments().getString("eventId") : null;
        String organizerId = getArguments() != null ? getArguments().getString("organizerId") : null;

        // TODO: Check for create/edit mode and change fragment accordingly

        /*
            See the following for time and date stuff
            https://docs.oracle.com/javase/8/docs/api/java/text/DateFormat.html
            https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
        */

        // BUTTON LOGIC

        Button positiveButton = view.findViewById(R.id.confirm_button);
        positiveButton.setOnClickListener(v -> {

            if ( eventId == null ) { // Creation mode
                Date date = new Date();
                Event newEvent = new Event("test title", "test description", "pretend organizer id", date, date, null); // TODO: create event using data entered by user
                addEvent(newEvent);
            } else { // Event mode
                updateEvent();
            }

            // Navigate to EventOverviewFragment
            EventOverviewFragment fragment = new EventOverviewFragment(); // TODO: change to newInstance method if one is created
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        });

        Button negativeButton = view.findViewById(R.id.cancel_button);
        negativeButton.setOnClickListener(v -> {
            if ( eventId == null ) { // Creation mode
                // TODO: cancel event creation
                // TODO: navigate back to home page
            } else { // Event mode
                // TODO: delete event. Will want a confirmation dialog fragment.
                // TODO: navigate back to event overview page
            }
        });
    }



    /**
     * Returns a new instance of the CreateEventFragment in create mode. To load
     * CreateEventFragment in edit mode, pass an eventId as an argument.
     * <p>
     * Usage:
     * CreateEventFragment fragment = CreateEventFragment.newInstanceCreateMode();
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
     * CreateEventFragment fragment = CreateEventFragment.newInstanceEditMode(myEvent.eventId);
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

    private void addEvent(Event event) {
    }

    private void updateEvent() {
        // TODO: edit event attributes according to what the user has entered
    }
}