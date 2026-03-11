/**
 * Create Event Fragment
 * Allows Admin and Organizers to create and edit events
 * Last Modified: 2026-03-10 by Grace MacKenzie
 *<p>
 *     Notes
 *      - This fragment has two modes: CREATE and EDIT
 *        @see com.example.eventlottery.EventCreationMode
 *      - In EDIT mode, it is assumed that some event has been passed to the fragment to
 *        be edited.
 *      - In CREATE mode, it is assumed that no such event is present and must be created.
 *        It is also assumed that an organizer id is present instead.
 *</p>

 *
 * @author Grace MacKenzie
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A Fragment which allows users to create and edit events.
 */
public class CreateEventFragment extends Fragment {

    // ATTRIBUTES

    private EventCreationMode mode;

    private FirebaseFirestore db;
    private CollectionReference eventsRef;

    EditText editTitle;
    EditText editDescription;
    // TODO: add edit text fields for registration open/close
    EditText editCapacity;

    Button negativeButton;
    Button positiveButton;

    private Event currentEvent = null;

    // CONSTRUCTORS

    /**
     * DO.NOT.USE.THIS.OUTSIDE.OF.THIS.CLASS.
     * A required empty public constructor for the CreateEventFragment.
     * Please instead use newInstanceCreateMode or newInstanceEditMode.
     *
     * @see CreateEventFragment#newInstanceCreateMode(String)
     * @see CreateEventFragment#newInstanceEditMode(String)
     */
    public CreateEventFragment() {}

    // OVERRIDDEN METHODS

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

        mode = (eventId == null) ? EventCreationMode.CREATE : EventCreationMode.EDIT ;

        negativeButton = view.findViewById(R.id.cancel_button);
        positiveButton = view.findViewById(R.id.confirm_button);

        editTitle = view.findViewById(R.id.edit_event_title);
        editDescription = view.findViewById(R.id.edit_event_description);
        // TODO: initialize views for registration open and close ZonedDateTimes
        editCapacity = view.findViewById(R.id.edit_capacity);

        // FIRESTORE STUFF

        db = FirebaseFirestore.getInstance();
        eventsRef = db.collection("events");

        // UI CHANGES FOR EDIT MODE

        if (mode == EventCreationMode.EDIT) {
            // Set currentEvent to the Event whose id was in the bundle
            db.collection("events")
                    .document(Objects.requireNonNull(eventId)) // not possible for event id to be null, but android studio is android studio
                    .get()
                    .addOnSuccessListener(doc -> {
                        currentEvent = doc.toObject(Event.class);
                    });

            // Set fields to contain the data of currentEvent
            editTitle.setText(currentEvent.title);
            editDescription.setText(currentEvent.description);
            // TODO: add fields for registration open/close ZonedDateTimes
            if (currentEvent.getWaitingListCapacity() != null) {
                editCapacity.setText(currentEvent.getWaitingListCapacity());
            }

            /*
            See the following for time and ZonedDateTime stuff
            https://docs.oracle.com/javase/8/docs/api/java/text/ZonedDateTimeFormat.html
            https://docs.oracle.com/javase/8/docs/api/java/text/SimpleZonedDateTimeFormat.html
            */

            // Change negative button appearance
            negativeButton.setText(R.string.delete);
            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_light));
            negativeButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark));
        }

        // BUTTON LISTENERS

        positiveButton.setOnClickListener(v -> {
            // Gather and valiZonedDateTime user entries
            EventInput input = getInput();
            ValidationContext ctx = new ValidationContext(mode, input, currentEvent, organizerId);
            ValidationResult validationResult = valiZonedDateTimeInput(ctx);
            /*
                TODO: somehow get the organizerId into the event generated by valiZonedDateTimeInput().
                      May want to add it as an EventInput attribute.
             */

            if (validationResult.isValid) {
                if ( mode == EventCreationMode.CREATE) {
                    addEvent(Objects.requireNonNull(validationResult.event));
                } else { // Event mode
                    // validationResult.event just points to currentEvent
                    upZonedDateTimeEvent(Objects.requireNonNull(validationResult.event));
                }

                // Navigate to EventOverviewFragment
                EventOverviewFragment fragment = new EventOverviewFragment(); // TODO: change to newInstance method if one is created
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(requireContext(), validationResult.errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        negativeButton.setOnClickListener(v -> {
            if (mode == EventCreationMode.EDIT) {
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

    // NEW INSTANCE METHODS

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
     * Saves upZonedDateTimed Event to Firestore
     * @param upZonedDateTimedEvent The Event with upZonedDateTimes made to it
     */
    private void upZonedDateTimeEvent(Event upZonedDateTimedEvent) {
        db.collection("events")
                .document(upZonedDateTimedEvent.getEventId())
                .set(upZonedDateTimedEvent);
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

    /**
     * A helper function to parse the user input waitingListCapacity.
     * This function uses a partial result from Microsoft, Copilot, "java toIntOrNull"
     * @param inputCapacity The raw user input string for the waiting list capacity.
     * @return null if empty, or the entered integer if positive and non 0.
     *         throws a IllegalArgumentException otherwise.
     */
    private Integer parseWaitingListCapacity(String inputCapacity) {
        if (inputCapacity == null || inputCapacity.trim().isEmpty()) {
            return null;
        }

        if (!inputCapacity.matches("\\d+")) {
            throw new IllegalArgumentException("Waiting List Capacity must be a positive integer.");
        }

        int value = Integer.parseInt(inputCapacity);

        if (value <= 0) {
            throw new IllegalArgumentException("Waiting List Capacity must be a positive integer.");
        }

        return value;
    }

    /**
     * A helper function which collects raw user input from EditText views.
     * @return Raw user input for to be valiZonedDateTimed and parsed into usable Event attributes
     */
    private EventInput getInput() {
        return new EventInput(
                editTitle.getText().toString(),
                editDescription.getText().toString(),
                "registration open", // TODO: replace temporary var with user input
                "registration close", // TODO: replace temporary var with user input
                editCapacity.getText().toString()
        );
    }

    /**
     * A helper function which valiZonedDateTimes data and returns a non-null event if and only if all
     * validation checks pass, or an error message if and only if at least one validation check fails.
     * @param ctx context of this validation including the mode, the set of inputs to valiZonedDateTime etc
     * @return A ValidationResult containing a true or false value
     */
    private ValidationResult valiZonedDateTimeInput(ValidationContext ctx) {
        Event returnEvent = ctx.event; // Nullable
        Integer capacity;

        // Run checks and parse user input
        if (ctx.input.title.isBlank()) {
            return ValidationResult.invalid("Event title cannot be blank");
        }

        if (ctx.input.description.isBlank()) {
            return ValidationResult.invalid("Event description cannot be blank");
        }

        // TODO: valiZonedDateTime registration open and close ZonedDateTimes
        // see https://docs.oracle.com/javase/8/docs/api/java/time/ZonedDateTime.html
        // see ZoneDateTime.parse(String)

        try {
            capacity = parseWaitingListCapacity(ctx.input.waitingListCapacity);
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid(e.getMessage());
        }

        // Generate Event depending on create/edit modes
        if (mode == EventCreationMode.CREATE) {
            returnEvent = new Event(
                    ctx.input.title,
                    ctx.input.description,
                    ctx.organizerId,
                    ZonedDateTime.now(), // TODO: change from temporary var to user input
                    ZonedDateTime.now(), // TODO: change from temporary var to user input
                    capacity
            );
        } else {
            returnEvent.setTitle(ctx.input.title);
            returnEvent.setDescription(ctx.input.description);
            // TODO: upZonedDateTime registration open/close times
            returnEvent.setWaitingListCapacity(capacity);
        }

        return ValidationResult.valid(returnEvent);
    }

}