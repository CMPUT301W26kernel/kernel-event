/**
 * Event Overview Fragment
 * Displays the details of an event.
 * Last Modified: 2026-04-04 by Grace MacKenzie
 */
package com.example.eventlottery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.example.eventlottery.creation.CreateEventFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A fragment which displays the details of a particular event and allows users to
 * interact with events.
 * <p>
 *     Allows Entrants to join/leave a waitlist and comment on events.
 * </p>
 * <p>
 *     Allows Organizers of the event to manage their waitlist, post comments,
 *     moderate comments, edit their event, and generate an event QR code.
 *     Organizers who are not contributors of an event are treated as Entrants.
 * </p>
 * <p>
 *     Allows Admin to act as both an Entrant and an Organizer of the Event.
 * </p>
 * <p>
 *     The fragment normally loads its event, role, and comments from Firebase.
 *     Tests may inject a {@link TestState} and a fake {@link EventCommentRepository}
 *     to test the UI without depending on Firebase state.
 * </p>
 */
public class EventOverviewFragment extends Fragment implements
        WaitingListDialogFragment.WaitingListDialogListener,
        EventCommentAdapter.OnDeleteCommentListener {

    /**
     * State used by instrumentation tests to bypass Firebase reads.
     */
    static class TestState {
        final String eventTitle;
        final String eventOrganizerId;
        final String eventDescription;
        final ZonedDateTime registrationOpen;
        final ZonedDateTime registrationClose;
        final int waitlistCount;
        final boolean inWaitingList;
        final String currentUserId;
        final String currentUserRole;
        final String currentUsername;

        TestState(
                String eventTitle,
                String eventOrganizerId,
                String eventDescription,
                ZonedDateTime registrationOpen,
                ZonedDateTime registrationClose,
                int waitlistCount,
                boolean inWaitingList,
                String currentUserId,
                String currentUserRole,
                String currentUsername
        ) {
            this.eventTitle = eventTitle;
            this.eventOrganizerId = eventOrganizerId;
            this.eventDescription = eventDescription;
            this.registrationOpen = registrationOpen;
            this.registrationClose = registrationClose;
            this.waitlistCount = waitlistCount;
            this.inWaitingList = inWaitingList;
            this.currentUserId = currentUserId;
            this.currentUserRole = currentUserRole;
            this.currentUsername = currentUsername;
        }
    }

    private static final DateTimeFormatter EVENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault());

    private WaitingListRepository waitlistRepo;
    private EventCommentRepository commentRepository;
    private ListenerRegistration commentListenerRegistration;

    private String eventId;
    private String eventTitle = "Event";
    private String eventOrganizerId;
    private List<String> eventCoOrganizers = new ArrayList<>();
    private String currentUserId;
    private String currentUserRole;
    private String currentUsername;
    private Event currentEvent;
    private int waitlistCount;
    private boolean inWaitingList;
    private boolean eventRequiresGeolocationForWaitlist;
    private String pendingJoinEventId;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean ok = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (ok && pendingJoinEventId != null) {
                    String id = pendingJoinEventId;
                    pendingJoinEventId = null;
                    joinWaitlistWithDeviceLocation(id);
                } else {
                    pendingJoinEventId = null;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.location_permission_required, Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private TextView eventTitleView;
    private TextView eventOrganizerView;
    private TextView eventDateView;
    private TextView eventWaitlistView;
    private TextView eventDescriptionView;
    private TextView commentPermissionView;
    private TextView emptyCommentsView;
    private ImageView posterImageView;
    private Button backButton;
    private Button qrGenerateButton;
    private Button editButton;
    private EditText commentInput;
    private Button postCommentButton;
    private Button joinWaitlistButton;
    private Button manageWaitlistButton;
    private LinearLayout commentComposerContainer;
    private EventCommentAdapter commentAdapter;
    private TestState testState;

    public EventOverviewFragment() {
        // Required empty public constructor
    }

    /**
     * Injects a comment repository for tests before the fragment is attached.
     *
     * @param commentRepository Repository implementation to use for comment actions and updates.
     */
    void setCommentRepositoryForTesting(@NonNull EventCommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    /**
     * Injects an in-memory event and viewer state for UI tests.
     *
     * @param testState Synthetic fragment state used instead of Firebase reads.
     */
    void setTestState(@NonNull TestState testState) {
        this.testState = testState;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        waitlistRepo = new WaitingListRepository();
        if (commentRepository == null) {
            commentRepository = new EventCommentRepository();
        }
        currentUserId = getCurrentUserId();

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindViews(view);
        setupCommentsList(view);
        backButton.setOnClickListener(v -> navigateBackToPreviousScreen());
        joinWaitlistButton.setOnClickListener(v -> openWaitlistDialog());
        manageWaitlistButton.setOnClickListener(v -> openWaitlistManagementDialog());
        postCommentButton.setOnClickListener(v -> submitComment());
        qrGenerateButton.setOnClickListener(v -> navigateToQrGeneration());
        editButton.setOnClickListener(v -> navigateToEditFragment());

        if (eventId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.error_no_event_id), Toast.LENGTH_SHORT).show();
            }
            navigateToFallbackScreen();
            return;
        }

        if (testState != null) {
            applyTestState(testState);
            return;
        }

        loadEventData();
    }

    /**
     * A custom implementation of the onResume() method which reloads the event data in the
     * EventOverviewFragment if returning from a fragment which updates the event data
     * (e.g. the CreateEventFragment in edit mode).
     */
    @Override
    public void onResume() {
        super.onResume();

        getParentFragmentManager().setFragmentResultListener(
                "editEventResult",
                this,
                (requestKey, bundle) -> {
                    if (bundle.getBoolean("eventUpdated", false)) {
                        loadEventData(); // reload updated event from Firestore
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (commentListenerRegistration != null) {
            commentListenerRegistration.remove();
            commentListenerRegistration = null;
        }
    }

    private void bindViews(@NonNull View view) {
        eventTitleView = view.findViewById(R.id.text_event_title);
        eventOrganizerView = view.findViewById(R.id.text_event_organizer);
        eventDateView = view.findViewById(R.id.text_event_date);
        eventWaitlistView = view.findViewById(R.id.text_event_waitlist);
        eventDescriptionView = view.findViewById(R.id.text_event_description);
        commentPermissionView = view.findViewById(R.id.text_comment_permissions);
        emptyCommentsView = view.findViewById(R.id.text_comments_empty);
        posterImageView = view.findViewById(R.id.image_event_poster);
        backButton = view.findViewById(R.id.btn_back_to_events);
        commentInput = view.findViewById(R.id.edit_comment_input);
        postCommentButton = view.findViewById(R.id.btn_post_comment);
        joinWaitlistButton = view.findViewById(R.id.btn_join_waitlist);
        manageWaitlistButton = view.findViewById(R.id.btn_manage_waitlist);
        commentComposerContainer = view.findViewById(R.id.comment_composer_container);
        qrGenerateButton = view.findViewById(R.id.btn_qr_generate);
        editButton = view.findViewById(R.id.btn_edit);
    }

    /**
     * Sets up the RecyclerView used for the live comment thread.
     *
     * @param view Fragment root view.
     */
    private void setupCommentsList(@NonNull View view) {
        RecyclerView commentsRecyclerView = view.findViewById(R.id.rv_event_comments);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        commentAdapter = new EventCommentAdapter(this);
        commentsRecyclerView.setAdapter(commentAdapter);
    }

    /**
     * Loads the current event document and then resolves the viewer profile to decide which
     * actions should be available on the screen.
     */
    private void loadEventData() {
        FirebaseFirestore.getInstance().collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Deserialize event. Load error if it doesn't exist.
                    currentEvent = documentSnapshot.toObject(Event.class);
                    if (currentEvent == null) {
                        showLoadError();
                        return;
                    }

                    bindEvent(currentEvent);
                    updateWaitlistState(documentSnapshot);
                    listenForComments();

                    if (currentUserId == null) {
                        refreshActionState();
                        return;
                    }

                    loadCurrentUserProfile();
                })
                .addOnFailureListener(error -> showLoadError());
    }

    /**
     * Applies a fully in-memory state for instrumentation tests and starts the comment listener
     * against the injected repository.
     *
     * @param state The synthetic state to render.
     */
    private void applyTestState(@NonNull TestState state) {
        eventTitle = state.eventTitle;
        eventOrganizerId = state.eventOrganizerId;
        currentUserId = state.currentUserId;
        currentUserRole = state.currentUserRole;
        currentUsername = state.currentUsername;
        waitlistCount = state.waitlistCount;
        inWaitingList = state.inWaitingList;

        eventTitleView.setText(eventTitle);
        eventOrganizerView.setText(getString(
                R.string.event_organizer_format,
                fallbackText(eventOrganizerId, getString(R.string.event_unknown_organizer))
        ));
        eventDateView.setText(getString(
                R.string.event_registration_window_format,
                state.registrationOpen.format(EVENT_DATE_FORMATTER),
                state.registrationClose.format(EVENT_DATE_FORMATTER)
        ));
        eventDescriptionView.setText(fallbackText(
                state.eventDescription,
                getString(R.string.default_event_description)
        ));
        eventWaitlistView.setText(getString(R.string.event_waitlist_count_format, waitlistCount));
        eventRequiresGeolocationForWaitlist = false;

        listenForComments();
        refreshActionState();
    }

    /**
     * Copies event-level display data from Firestore into the UI.
     *
     * @param event The current event being overviewed
     */
    private void bindEvent(Event event) {
        eventTitle = event.getTitle();
        eventOrganizerId = event.getOrganizerId();
        
        List<String> rawCoOrganizers = (List<String>) event.getCoOrganizers();
        if (rawCoOrganizers != null) {
            eventCoOrganizers = new ArrayList<>(rawCoOrganizers);
        } else {
            eventCoOrganizers.clear();
        }

        eventTitleView.setText(eventTitle);
        eventOrganizerView.setText(getString(
                R.string.event_organizer_format,
                fallbackText(eventOrganizerId, getString(R.string.event_unknown_organizer))
        ));
        eventDateView.setText(getString(
                R.string.event_registration_window_format,
                event.getRegistrationOpen().format(EVENT_DATE_FORMATTER),
                event.getRegistrationClose().format(EVENT_DATE_FORMATTER)
        ));
        eventDescriptionView.setText(fallbackText(event.getDescription(), getString(R.string.default_event_description)));
        posterImageView.setImageBitmap(event.getPosterImage());
        eventRequiresGeolocationForWaitlist = event.isRequireGeolocationForWaitlist();
    }

    /**
     * Extracts waitlist data needed by the waitlist dialog controls.
     *
     * @param documentSnapshot The Firestore event document.
     */
    private void updateWaitlistState(DocumentSnapshot documentSnapshot) {
        List<String> waitlist = extractStringList(documentSnapshot.get("waitingList"));
        waitlistCount = waitlist.size();
        inWaitingList = currentUserId != null && waitlist.contains(currentUserId);
        eventWaitlistView.setText(getString(R.string.event_waitlist_count_format, waitlistCount));
    }

    /**
     * Loads the signed-in user's profile so the fragment can decide whether the viewer is an
     * entrant, the event organizer, or an admin.
     */
    private void loadCurrentUserProfile() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    currentUserRole = userDoc.exists() ? userDoc.getString("role") : null;
                    currentUsername = userDoc.exists() ? userDoc.getString("username") : null;
                    refreshActionState();
                })
                .addOnFailureListener(error -> refreshActionState());
    }

    /**
     * Subscribes to the event comment stream and updates the adapter when the thread changes.
     */
    private void listenForComments() {
        if (commentListenerRegistration != null) {
            commentListenerRegistration.remove();
        }

        commentListenerRegistration = commentRepository.listenForComments(
                eventId,
                new EventCommentRepository.CommentListener() {
                    @Override
                    public void onCommentsChanged(List<EventComment> comments) {
                        if (!isAdded()) {
                            return;
                        }

                        commentAdapter.setComments(comments);
                        emptyCommentsView.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(Exception error) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), R.string.error_load_comments_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * Updates button visibility, composer visibility, and moderation controls based on the
     * current viewer role and event ownership.
     */
    private void refreshActionState() {
        commentAdapter.setViewerContext(currentUserId, currentUserRole, eventOrganizerId, eventCoOrganizers);

        boolean isOrganizer = currentUserId != null && currentEvent.isOrganizer(currentUserId);
        boolean isAdmin = "admin".equalsIgnoreCase(currentUserRole);

        if (isOrganizer) {
            // Allow organizer/admin of event to manage waitlist; cannot join waitlist.
            showOrganizerActions();
        } else if (isAdmin) {
            //Allow admin to both join and manage events they're not organizing.
            showAdminActions();
        } else {
            showEntrantActions();
        }

        boolean canPostComment = EventCommentPolicy.canPostComment(currentUserId, currentUserRole, eventOrganizerId, eventCoOrganizers);
        commentComposerContainer.setVisibility(canPostComment ? View.VISIBLE : View.GONE);
        postCommentButton.setEnabled(canPostComment);

        if (canPostComment) {
            commentPermissionView.setVisibility(View.GONE);
        } else {
            commentPermissionView.setVisibility(View.VISIBLE);
            commentPermissionView.setText(resolveCommentPermissionMessage());
        }
    }

    /**
     * Builds message shown when the comment composer is hidden.
     *
     * @return Human-readable reason the viewer cannot post a comment.
     */
    private String resolveCommentPermissionMessage() {
        if (currentUserId == null) {
            return getString(R.string.comment_sign_in_required);
        }
        if ("admin".equalsIgnoreCase(currentUserRole)) {
            return getString(R.string.comment_admin_read_only);
        }
        if ("organizer".equalsIgnoreCase(currentUserRole)) {
            return getString(R.string.comment_only_event_organizer);
        }
        return getString(R.string.comment_post_unavailable);
    }

    /**
     * Validates and posts a new comment for the current viewer.
     */
    private void submitComment() {
        String text = commentInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            commentInput.setError(getString(R.string.error_empty_comment));
            return;
        }

        if (!EventCommentPolicy.canPostComment(currentUserId, currentUserRole, eventOrganizerId, eventCoOrganizers)) {
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.comment_post_unavailable, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        EventComment comment = new EventComment(
                currentUserId,
                fallbackText(currentUsername, getString(R.string.comment_default_author)),
                currentUserRole,
                text,
                Timestamp.now(),
                EventComment.STATUS_ACTIVE,
                EventCommentPolicy.shouldPinComment(currentUserId, eventOrganizerId, eventCoOrganizers)
        );

        postCommentButton.setEnabled(false);
        commentRepository.postComment(eventId, comment)
                .addOnSuccessListener(unused -> {
                    commentInput.setText("");
                    commentInput.setError(null);
                    postCommentButton.setEnabled(true);
                })
                .addOnFailureListener(error -> {
                    postCommentButton.setEnabled(true);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.error_post_comment_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Opens the entrant facing join/leave waitlist dialog for the current event state.
     */
    private void openWaitlistDialog() {
        WaitingListDialogFragment dialog =
                WaitingListDialogFragment.newInstance(
                        eventId,
                        eventTitle,
                        waitlistCount,
                        inWaitingList,
                        eventRequiresGeolocationForWaitlist);
        dialog.show(getChildFragmentManager(), "WaitingListDialog");
    }

    /**
     * Opens the organizer/admin waitlist management dialog.
     */
    private void openWaitlistManagementDialog() {
        WaitlistManagementFragment dialog = WaitlistManagementFragment.newInstance(eventId);
        dialog.show(getChildFragmentManager(), "WaitlistManagementDialog");
    }

    /**
     * Returns the user to the previous screen or the home screen when the event cannot be shown.
     */
    private void navigateToFallbackScreen() {
        if (!isAdded()) {
            return;
        }

        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomePageFragment())
                .commit();
    }

    /**
     * Returns to the previous screen from the event overview page.
     */
    private void navigateBackToPreviousScreen() {
        if (!isAdded()) {
            return;
        }

        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            navigateToFallbackScreen();
        }
    }

    /**
     * Navigates to a Qr Code Generation fragment
     */
    private void navigateToQrGeneration() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, QrGeneratorFragment.newInstance(eventId));
        transaction.addToBackStack("qr_generation");
        transaction.commit();
    }

    private void navigateToEditFragment() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, CreateEventFragment.newInstanceEditMode(eventId));
        transaction.addToBackStack("edit_event");
        transaction.commit();
    }

    /**
     * Shows the join/leave waitlist action and hides event organizer management.
     */
    private void showEntrantActions() {
        joinWaitlistButton.setVisibility(View.VISIBLE);
        manageWaitlistButton.setVisibility(View.GONE);
        qrGenerateButton.setVisibility(View.GONE);
        editButton.setVisibility(View.GONE);
    }

    /**
     * Shows event management buttons and hides the button to join/leave the waitlist.
     */
    private void showOrganizerActions() {
        joinWaitlistButton.setVisibility(View.GONE);
        manageWaitlistButton.setVisibility(View.VISIBLE);
        if (!currentEvent.isPrivate()) {
            qrGenerateButton.setVisibility(View.VISIBLE);
        } else {
            qrGenerateButton.setVisibility(View.GONE);
        }
        editButton.setVisibility(View.VISIBLE);
    }

    /**
     * Shows join/leave waitlist, waitlist management, qr generation, and edit buttons to admin.
     */
    private void showAdminActions() {
        joinWaitlistButton.setVisibility(View.VISIBLE);
        manageWaitlistButton.setVisibility(View.VISIBLE);
        if (!currentEvent.isPrivate()) {
            qrGenerateButton.setVisibility(View.VISIBLE);
        } else {
            qrGenerateButton.setVisibility(View.GONE);
        }
        editButton.setVisibility(View.VISIBLE);
    }

    /**
     * Helper method to get the current authenticated user's ID.
     * @return The ID of the current user, or null if not authenticated.
     */
    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    @Override
    public void onJoinWaitingList(String eventId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), getString(R.string.error_must_be_signed_in), Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventRequiresGeolocationForWaitlist) {
            if (!hasLocationPermission()) {
                pendingJoinEventId = eventId;
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
                return;
            }
            joinWaitlistWithDeviceLocation(eventId);
            return;
        }

        performJoinWaitlist(eventId, null, null);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void joinWaitlistWithDeviceLocation(String joinEventId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), getString(R.string.error_must_be_signed_in), Toast.LENGTH_SHORT).show();
            return;
        }
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(requireActivity());
        CancellationTokenSource cts = new CancellationTokenSource();
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (loc == null) {
                        Toast.makeText(getContext(), R.string.location_unavailable, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    performJoinWaitlist(joinEventId, loc.getLatitude(), loc.getLongitude());
                });
    }

    private void performJoinWaitlist(String joinEventId, Double lat, Double lng) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), getString(R.string.error_must_be_signed_in), Toast.LENGTH_SHORT).show();
            return;
        }

        waitlistRepo.joinWaitingList(joinEventId, userId, lat, lng).addOnSuccessListener(aVoid -> {
            inWaitingList = true;
            waitlistCount += 1;
            eventWaitlistView.setText(getString(R.string.event_waitlist_count_format, waitlistCount));
            refreshActionState();

            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.join_success), Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), resolveJoinFailureMessage(e), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String resolveJoinFailureMessage(Exception e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        String m = t.getMessage();
        if (m != null && !m.trim().isEmpty()) {
            return m;
        }
        return getString(R.string.action_failed);
    }

    /**
     * Handles the entrant action to leave the waiting list and refreshes the local screen state.
     *
     * @param eventId Event whose waiting list should be updated.
     */
    @Override
    public void onLeaveWaitingList(String eventId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getContext(), getString(R.string.error_must_be_signed_in), Toast.LENGTH_SHORT).show();
            return;
        }

        waitlistRepo.leaveWaitingList(eventId, userId).addOnSuccessListener(aVoid -> {
            inWaitingList = false;
            waitlistCount = Math.max(0, waitlistCount - 1);
            eventWaitlistView.setText(getString(R.string.event_waitlist_count_format, waitlistCount));
            refreshActionState();

            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.leave_success), Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.action_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Opens the waitlist management view from the waitlist dialog callback.
     *
     * @param eventId Event whose waiting list should be shown.
     */
    @Override
    public void onViewWaitingList(String eventId) {
        WaitlistManagementFragment dialog = WaitlistManagementFragment.newInstance(eventId);
        dialog.show(getChildFragmentManager(), "WaitlistManagementDialog");
    }

    /**
     * Removes a visible comment when the current viewer is allowed to moderate it.
     *
     * @param comment Comment selected for removal.
     */
    @Override
    public void onDeleteComment(EventComment comment) {
        if (!EventCommentPolicy.canDeleteComment(comment, currentUserId, currentUserRole, eventOrganizerId, eventCoOrganizers)) {
            return;
        }

        String removalReason = "admin".equalsIgnoreCase(currentUserRole)
                ? getString(R.string.comment_removed_reason_admin)
                : getString(R.string.comment_removed_reason_organizer);

        commentRepository.removeComment(
                        eventId,
                        comment.getCommentId(),
                        removalReason
                )
                .addOnFailureListener(error -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.action_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Shows a load failure toast and navigates away from the fragment.
     */
    private void showLoadError() {
        if (getContext() != null) {
            Toast.makeText(getContext(), getString(R.string.error_load_event_failed), Toast.LENGTH_SHORT).show();
        }
        navigateToFallbackScreen();
    }

    /**
     * Converts a Firestore array value into a typed list of strings.
     *
     * @param value Raw Firestore field value.
     * @return String entries extracted from the value, or an empty list.
     */
    private List<String> extractStringList(Object value) {
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
     * Parses an event timestamp field from Firestore into the local time zone.
     *
     * @param documentSnapshot Firestore event document.
     * @param fieldName Name of the date field to parse.
     * @return Parsed date, or the current time as a fallback.
     */
    private ZonedDateTime readEventDate(DocumentSnapshot documentSnapshot, String fieldName) {
        Object rawValue = documentSnapshot.get(fieldName);
        if (rawValue instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) rawValue;
            return ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanoseconds()),
                    ZoneId.systemDefault()
            );
        }
        if (rawValue instanceof String) {
            try {
                return ZonedDateTime.parse((String) rawValue);
            } catch (Exception ignored) {
                // Fall through to the default below.
            }
        }
        return ZonedDateTime.now();
    }

    /**
     * Returns the input string unless it is blank, in which case the fallback is returned.
     *
     * @param value Candidate value.
     * @param fallback Replacement for null or blank values.
     * @return Display-safe string value.
     */
    private String fallbackText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
