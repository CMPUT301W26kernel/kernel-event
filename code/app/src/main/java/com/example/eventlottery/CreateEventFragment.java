/**
 * Create Event Fragment
 * Allows Admin and Organizers to create and edit events
 * Last Modified: 2026-03-12 by Grace MacKenzie
 *<p>
 *     Notes
 *      - This fragment has two modes: CREATE and EDIT
 *        @see com.example.eventlottery.EventCreationMode
 *      - In EDIT mode, it is assumed that some event has been passed to the fragment to
 *        be edited.
 *      - In CREATE mode, it is assumed that no such event is present and must be created.
 *        It is also assumed that an organizer id is present instead.
 *      - TODO: Generate a QR code upon Event creation
 *        see https://www.geeksforgeeks.org/android/how-to-generate-qr-code-in-android/
 *        and https://reintech.io/blog/implementing-android-app-qr-code-scanner
 *      - TODO: upload an optional image to firebase and store a reference to that image.
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
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * A Fragment which allows users to create and edit events.
 */
public class CreateEventFragment extends Fragment {

    // ATTRIBUTES

    private EventCreationMode mode;

    private FirebaseFirestore db;
    private CollectionReference eventsRef;

    private ImageView editPosterImage; // TODO: optionally upload image to Firestore and store reference in Event
    private EditText editTitle;
    private EditText editDescription;
    private EditText editRegOpenYear;
    private EditText editRegOpenMonth;
    private EditText editRegOpenDay;
    private EditText editRegCloseYear;
    private EditText editRegCloseMonth;
    private EditText editRegCloseDay;
    private EditText editCapacity;

    private Button negativeButton;
    private Button positiveButton;

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

        editPosterImage = view.findViewById(R.id.poster_image);
        editTitle = view.findViewById(R.id.edit_event_title);
        editDescription = view.findViewById(R.id.edit_event_description);
        editRegOpenYear = view.findViewById(R.id.edit_reg_open_year);
        editRegOpenMonth = view.findViewById(R.id.edit_reg_open_month);
        editRegOpenDay = view.findViewById(R.id.edit_reg_open_day);
        editRegCloseYear = view.findViewById(R.id.edit_reg_close_year);
        editRegCloseMonth = view.findViewById(R.id.edit_reg_close_month);
        editRegCloseDay = view.findViewById(R.id.edit_reg_close_day);
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
                    .addOnSuccessListener(doc -> currentEvent = doc.toObject(Event.class));

            // Set fields to contain the data of currentEvent
            editTitle.setText(currentEvent.title);
            editDescription.setText(currentEvent.description);
            editRegOpenYear.setText(currentEvent.getRegistrationOpen().getYear());
            editRegOpenMonth.setText(currentEvent.getRegistrationOpen().getMonthValue());
            editRegOpenDay.setText(currentEvent.getRegistrationOpen().getDayOfMonth());
            editRegCloseYear.setText(currentEvent.getRegistrationClose().getYear());
            editRegCloseMonth.setText(currentEvent.getRegistrationClose().getMonthValue());
            editRegCloseDay.setText(currentEvent.getRegistrationClose().getDayOfMonth());
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
            ValidationResult validationResult = validateInput(ctx);

            if (validationResult.isValid) {
                if ( mode == EventCreationMode.CREATE) {
                    addEvent(Objects.requireNonNull(validationResult.event));
                } else { // Event mode
                    // validationResult.event just points to currentEvent
                    upZonedDateTimeEvent(Objects.requireNonNull(validationResult.event));
                }

                // Navigate to EventOverviewFragment
                EventOverviewFragment fragment = new EventOverviewFragment(); // TODO: change to newInstance method if one is created
                Bundle bundle = new Bundle();
                bundle.putString("eventId", validationResult.event.getEventId());
                fragment.setArguments(bundle);
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
            throw new IllegalArgumentException(getString(R.string.error_invalid_capacity));
        }

        int value = Integer.parseInt(inputCapacity);

        if (value <= 0) {
            throw new IllegalArgumentException(getString(R.string.error_invalid_capacity));
        }

        return value;
    }

    /**
     * A helper function which collects raw user input from EditText views.
     * @return Raw user input for to be validated and parsed into usable Event attributes
     */
    private EventInput getInput() {
        String regOpen = String.format(
                "%s-%s-%s",
                editRegOpenYear.getText().toString(),
                editRegOpenMonth.getText().toString(),
                editRegOpenDay.getText().toString());
        String regClose = String.format(
                "%s-%s-%s",
                editRegCloseYear.getText().toString(),
                editRegCloseMonth.getText().toString(),
                editRegCloseDay.getText().toString());
        return new EventInput(
                editTitle.getText().toString(),
                editDescription.getText().toString(),
                regOpen,
                regClose,
                editCapacity.getText().toString()
        );
    }

    /*
     I'm considering making this validation process its own dedicated class.
     I already have 2 helper classes for it after all. 3 if you count EventInput
     as part of the validation pipeline. which it pretty much is.
     */

    /**
     * A helper function which valiZonedDateTimes data and returns a non-null event if and only if all
     * validation checks pass, or an error message if and only if at least one validation check fails.
     * @param ctx context of this validation including the mode, the set of inputs to valiZonedDateTime etc
     * @return A ValidationResult containing a true or false value
     */
    private ValidationResult validateInput(ValidationContext ctx) {
        Event returnEvent = ctx.event; // Nullable
        ZonedDateTime registrationOpen, registrationClose;
        Integer capacity;

        // Title Check
        if (ctx.input.title.isBlank()) {
            return ValidationResult.invalid(getString(R.string.error_empty_title));
        }

        // Description Check
        if (ctx.input.description.isBlank()) {
            return ValidationResult.invalid(getString(R.string.error_empty_description));
        }

        // Registration Open/Close Checks

        if(!ctx.input.registrationOpen.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return ValidationResult.invalid(getString(R.string.error_invalid_date_format));
        }

        if(!ctx.input.registrationClose.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return ValidationResult.invalid(getString(R.string.error_invalid_date_format));
        }

        try {
            // Generate registration open/close ZonedDateTimes
            ZoneId zone = ZoneId.systemDefault();

            LocalDate date = LocalDate.parse(ctx.input.registrationOpen);
            LocalTime time = LocalTime.of(12, 0); // TODO: change from default to user input time
            registrationOpen = ZonedDateTime.of(date, time, zone);

            date = LocalDate.parse(ctx.input.registrationClose);
            time = LocalTime.of(12, 0); // TODO: change from default to user input time
            registrationClose = ZonedDateTime.of(date, time, zone);

        } catch (DateTimeParseException e) {
            return ValidationResult.invalid(getString(R.string.error_impossible_date));
        }

        /*
            TODO: fix event period check to let existing events have registration open dates
             earlier than the current date. I may wish to hide the registration Open field from
             editing users entirely if the registration period has already begun
         */
        if (registrationOpen.equals(registrationClose)) {
            return ValidationResult.invalid(getString(R.string.error_open_date_equals_close_date));
        } else if (registrationClose.isBefore(registrationOpen)) {
            return ValidationResult.invalid(getString(R.string.error_close_date_before_open_date));
        } else if (registrationOpen.isBefore(ZonedDateTime.now())) {
            return ValidationResult.invalid(getString(R.string.error_registration_period));
        }

        // Waiting List Capacity Check
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
                    registrationOpen,
                    registrationClose,
                    capacity
            );
        } else {
            returnEvent.setTitle(ctx.input.title);
            returnEvent.setDescription(ctx.input.description);
            returnEvent.setRegistrationOpen(registrationOpen);
            returnEvent.setRegistrationClose(registrationClose);
            returnEvent.setWaitingListCapacity(capacity);
        }

        return ValidationResult.valid(returnEvent);
    }

}