/**
 * Create Event Fragment
 * Allows Admin and Organizers to create and edit events
 * Last Modified: 2026-03-08 by Grace MacKenzie
 * Notes:
 *      - Has modes for creating and editing events
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

/**
 * A simple {@link Fragment} subclass.
 */
public class CreateEventFragment extends Fragment {

    public CreateEventFragment() {
        // Required empty public constructor
    }

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
        /*
            TODO: Check for create/edit mode and change fragment accordingly

            - I will need a bundle passed to me which includes the event data.
            - If the bundle contains an Event -> I will load the data in edit mode.
            - Else, the bundle is empty -> I know I'm in creation mode and will need to
              create an event.
        */

        Button positiveButton = view.findViewById(R.id.confirm_button);
        positiveButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();

            // TODO: Check that user has filled in all required fields

            if ( 1 == 1 ) {  // TODO: check if in creation mode
                // TODO: Create new event object based on user input
                Event newEvent = new Event();
                bundle.putString("eventId", "placeholder Firestore Id");
            } else {
                // save edits to the loaded Event
                // use the id of the existing Event
                bundle.putString("eventId", "placeholder Firestore Id");
            }

            // Navigate to EventOverviewFragment
            EventOverviewFragment fragment = new EventOverviewFragment();
            fragment.setArguments(bundle);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        });

        Button negativeButton = view.findViewById(R.id.cancel_button);
        negativeButton.setOnClickListener(v -> {
            if ( 1 == 1 ) {  // TODO: check if in creation mode
                // cancel event creation
            } else {
                // delete event. Will want a confirmation dialog fragment.
            }
        });
    }

    /**
     * Returns a new instance of the CreateEventFragment in create mode. To load
     * CreateEventFragment in edit mode, pass an eventId as an argument.
     * <p>
     * Usage:
     * CreateEventFragment fragment = CreateEventFragment.newInstance();
     * <p>
     * // Then use the fragment in your fragment transaction
     * requireActivity().getSupportFragmentManager()
     *                     .beginTransaction()
     *                     .replace(R.id.fragment_container, fragment)
     *                     .addToBackStack(null)
     *                     .commit();
     *
     * @return A new CreateEventFragment which can be used in fragment transactions
     */
    public static CreateEventFragment newInstance() {
        return new CreateEventFragment();
    }

    /**
     * Returns a new instance of the CreateEventFragment in edit mode. to load
     * CreateEventFragment in create mode, pass this function without an argument.
     * <p>
     * Usage:
     * CreateEventFragment fragment = CreateEventFragment.newInstance(myEvent.eventId);
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
    public static CreateEventFragment newInstance(String eventId) {
        CreateEventFragment fragment = new CreateEventFragment();
        Bundle bundle = new Bundle();
        bundle.putString("eventId", eventId);
        fragment.setArguments(bundle);
        return fragment;
    }
}