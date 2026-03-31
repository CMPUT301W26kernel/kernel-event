/**
 * Event Overview Fragment
 * Displays the details of an event.
 * Last Modified: 2026-03-30
 */
package com.example.eventlottery;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class EventOverviewFragment extends Fragment implements
        WaitingListDialogFragment.WaitingListDialogListener,
        EventCommentAdapter.OnDeleteCommentListener {

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
    private String currentUserId;
    private String currentUserRole;
    private String currentUsername;
    private int waitlistCount;
    private boolean inWaitingList;

    private TextView eventTitleView;
    private TextView eventOrganizerView;
    private TextView eventDateView;
    private TextView eventWaitlistView;
    private TextView eventCapacityView;
    private TextView eventDescriptionView;
    private TextView commentPermissionView;
    private TextView emptyCommentsView;

    private Button backButton;
    private Button postCommentButton;
    private Button joinWaitlistButton;
    private Button manageWaitlistButton;

    private EditText commentInput;
    private LinearLayout commentComposerContainer;

    private EventCommentAdapter commentAdapter;
    private TestState testState;

    public EventOverviewFragment() {
        // Required empty public constructor
    }

    void setCommentRepositoryForTesting(@NonNull EventCommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

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
        eventCapacityView = view.findViewById(R.id.text_event_capacity);
        eventDescriptionView = view.findViewById(R.id.text_event_description);

        commentPermissionView = view.findViewById(R.id.text_comment_permissions);
        emptyCommentsView = view.findViewById(R.id.text_comments_empty);

        backButton = view.findViewById(R.id.btn_back_to_events);
        postCommentButton = view.findViewById(R.id.btn_post_comment);
        joinWaitlistButton = view.findViewById(R.id.btn_join_waitlist);
        manageWaitlistButton = view.findViewById(R.id.btn_manage_waitlist);

        commentInput = view.findViewById(R.id.edit_comment_input);
        commentComposerContainer = view.findViewById(R.id.comment_composer_container);
    }

    private void setupCommentsList(@NonNull View view) {
        RecyclerView commentsRecyclerView = view.findViewById(R.id.rv_event_comments);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        commentAdapter = new EventCommentAdapter(this);
        commentsRecyclerView.setAdapter(commentAdapter);
    }

    private void loadEventData() {
        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        showLoadError();
                        return;
                    }

                    bindEvent(documentSnapshot);
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
                formatEventDate(state.registrationOpen),
                formatEventDate(state.registrationClose)
        ));
        eventDescriptionView.setText(fallbackText(
                state.eventDescription,
                getString(R.string.default_event_description)
        ));
        eventWaitlistView.setText(getString(R.string.event_waitlist_count_format, waitlistCount));

        if (eventCapacityView != null) {
            eventCapacityView.setText(getString(R.string.event_capacity_unknown));
        }

        listenForComments();
        refreshActionState();
    }

    private void bindEvent(DocumentSnapshot documentSnapshot) {
        String eventNameRaw = documentSnapshot.getString("title");
        eventTitle = eventNameRaw == null ? getString(R.string.default_event_title) : eventNameRaw;

        String description = documentSnapshot.getString("description");
        eventOrganizerId = documentSnapshot.getString("organizerId");

        eventTitleView.setText(eventTitle);
        eventOrganizerView.setText(getString(
                R.string.event_organizer_format,
                fallbackText(eventOrganizerId, getString(R.string.event_unknown_organizer))
        ));

        eventDateView.setText(getString(
                R.string.event_registration_window_format,
                formatEventDate(readEventDate(documentSnapshot, "registrationOpen")),
                formatEventDate(readEventDate(documentSnapshot, "registrationClose"))
        ));

        eventDescriptionView.setText(
                fallbackText(description, getString(R.string.default_event_description))
        );

        if (eventCapacityView != null) {
            Object rawCapacity = documentSnapshot.get("waitingListCapacity");
            if (rawCapacity instanceof Number) {
                eventCapacityView.setText(
                        getString(R.string.event_capacity_format, ((Number) rawCapacity).intValue())
                );
            } else {
                eventCapacityView.setText(getString(R.string.event_capacity_unlimited));
            }
        }
    }

    private void updateWaitlistState(DocumentSnapshot documentSnapshot) {
        List<String> waitlist = extractStringList(documentSnapshot.get("waitingList"));
        waitlistCount = waitlist.size();
        inWaitingList = currentUserId != null && waitlist.contains(currentUserId);
        eventWaitlistView.setText(getString(R.string.event_waitlist_count_format, waitlistCount));
    }

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

    private void refreshActionState() {
        commentAdapter.setViewerContext(currentUserId, currentUserRole, eventOrganizerId);

        if (canManageWaitlist()) {
            showManageWaitlistButton();
        } else {
            showJoinWaitlistButton();
        }

        boolean canPostComment = EventCommentPolicy.canPostComment(currentUserId, currentUserRole, eventOrganizerId);
        commentComposerContainer.setVisibility(canPostComment ? View.VISIBLE : View.GONE);
        postCommentButton.setEnabled(canPostComment);

        if (canPostComment) {
            commentPermissionView.setVisibility(View.GONE);
        } else {
            commentPermissionView.setVisibility(View.VISIBLE);
            commentPermissionView.setText(resolveCommentPermissionMessage());
        }
    }

    private boolean canManageWaitlist() {
        boolean isOrganizer = currentUserId != null && currentUserId.equals(eventOrganizerId);
        boolean isAdmin = "admin".equalsIgnoreCase(currentUserRole);
        return isOrganizer || isAdmin;
    }

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

    private void submitComment() {
        String text = commentInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            commentInput.setError(getString(R.string.error_empty_comment));
            return;
        }

        if (!EventCommentPolicy.canPostComment(currentUserId, currentUserRole, eventOrganizerId)) {
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
                EventCommentPolicy.shouldPinComment(currentUserId, eventOrganizerId)
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

    private void openWaitlistDialog() {
        WaitingListDialogFragment dialog =
                WaitingListDialogFragment.newInstance(eventId, eventTitle, waitlistCount, inWaitingList);
        dialog.show(getChildFragmentManager(), "WaitingListDialog");
    }

    private void openWaitlistManagementDialog() {
        WaitlistManagementFragment dialog = WaitlistManagementFragment.newInstance(eventId);
        dialog.show(getChildFragmentManager(), "WaitlistManagementDialog");
    }

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

    private void showJoinWaitlistButton() {
        manageWaitlistButton.setVisibility(View.GONE);
        joinWaitlistButton.setVisibility(View.VISIBLE);
    }

    private void showManageWaitlistButton() {
        joinWaitlistButton.setVisibility(View.GONE);
        manageWaitlistButton.setVisibility(View.VISIBLE);
    }

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

        waitlistRepo.joinWaitingList(eventId, userId).addOnSuccessListener(aVoid -> {
            inWaitingList = true;
            waitlistCount += 1;
            eventWaitlistView.setText(getString(R.string.event_waitlist_count_format, waitlistCount));
            refreshActionState();

            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.join_success), Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.action_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    @Override
    public void onViewWaitingList(String eventId) {
        WaitlistManagementFragment dialog = WaitlistManagementFragment.newInstance(eventId);
        dialog.show(getChildFragmentManager(), "WaitlistManagementDialog");
    }

    @Override
    public void onDeleteComment(EventComment comment) {
        if (!EventCommentPolicy.canDeleteComment(comment, currentUserId, currentUserRole, eventOrganizerId)) {
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

    private void showLoadError() {
        if (getContext() != null) {
            Toast.makeText(getContext(), getString(R.string.error_load_event_failed), Toast.LENGTH_SHORT).show();
        }
        navigateToFallbackScreen();
    }

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
                // Fall through
            }
        }
        return ZonedDateTime.now();
    }

    private String formatEventDate(ZonedDateTime date) {
        return EVENT_DATE_FORMATTER.format(date);
    }

    private String fallbackText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
