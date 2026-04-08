/**
 * Home page fragment that displays a list of events fetched from Firestore.
 * This fragment acts as the main entry point for entrants to browse and select events.
 * Last Modified: 2026-04-04 by Grace MacKenzie
 *
 * @author Pierce
 * @author Rebecca OluwaBiyi
 * @author Grace MacKenzie
 * @since 2026-03-13
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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.eventlottery.creation.CreateEventFragment;
import com.example.eventlottery.map.NearbyEventsMapFragment;
import com.example.eventlottery.profiles.ProfileListFragment;
import com.example.eventlottery.profiles.User;
import com.example.eventlottery.profiles.UserProfileFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Home page fragment that displays a list of events fetched from Firestore.
 */
public class HomePageFragment extends Fragment {
    private final ArrayList<Event> allEvents = new ArrayList<>();
    private final ArrayList<Event> filteredEvents = new ArrayList<>();
    private EventAdapter adapter;

    private TextInputLayout keywordLayout;
    private TextInputLayout tagLayout;
    private TextInputLayout availableDateLayout;
    private TextInputLayout minCapacityLayout;

    private TextInputEditText keywordInput;
    private MaterialAutoCompleteTextView tagInput;
    private TextInputEditText availableDateInput;
    private TextInputEditText minCapacityInput;
    private CheckBox openNowOnlyCheckbox;

    private User currentUser;

    public HomePageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_with_bottom_bar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ListView listView = view.findViewById(R.id.list_view);
        keywordLayout = view.findViewById(R.id.input_search_keyword_layout);
        tagLayout = view.findViewById(R.id.input_search_tag_layout);
        availableDateLayout = view.findViewById(R.id.input_available_date_layout);
        minCapacityLayout = view.findViewById(R.id.input_min_capacity_layout);
        keywordInput = view.findViewById(R.id.input_search_keyword);
        tagInput = view.findViewById(R.id.input_search_tag);
        availableDateInput = view.findViewById(R.id.input_available_date);
        minCapacityInput = view.findViewById(R.id.input_min_capacity);
        openNowOnlyCheckbox = view.findViewById(R.id.checkbox_open_now_only);

        View applyFiltersButton = view.findViewById(R.id.button_apply_filters);
        View clearFiltersButton = view.findViewById(R.id.button_clear_filters);

        adapter = new EventAdapter(requireContext(), filteredEvents);
        listView.setAdapter(adapter);

        applyFiltersButton.setOnClickListener(v -> applyFilters());
        clearFiltersButton.setOnClickListener(v -> {
            if (keywordInput != null) keywordInput.setText("");
            if (tagInput != null) tagInput.setText("");
            if (availableDateInput != null) availableDateInput.setText("");
            if (minCapacityInput != null) minCapacityInput.setText("");
            if (openNowOnlyCheckbox != null) openNowOnlyCheckbox.setChecked(false);
            clearFilterErrors();
            applyFilters();
        });

        // Navigation to EventOverviewFragment
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Event selectedEvent = filteredEvents.get(position);
            navigateToEventOverview(selectedEvent.getEventId());
        });

        // Setup role-based bottom bar and status
        FrameLayout bottomBar = view.findViewById(R.id.bottom_bar);
        TextView statusText = view.findViewById(R.id.logged_in_status);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(firebaseUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {

                            // Update the status bar
                            if (statusText != null) {
                                String userLabel = (currentUser.getUsername() != null ? currentUser.getUsername() : "User");
                                String roleLabel = (currentUser.getRole() != null ? currentUser.getRole() : "Unknown");
                                statusText.setText(String.format("Logged in as: %s (%s)", userLabel, roleLabel));
                            }

                            loadBottomBar(bottomBar, currentUser.getRole());

                            // Fetch all events from Firestore after setting up user
                            // TODO: implement callback pattern to avoid firebase nesting
                            fetchEventsFromFirestore();
                        }
                    });
        }
    }

    private void fetchEventsFromFirestore() {
        FirebaseFirestore.getInstance().collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            boolean isPublic = !event.isPrivate();
                            boolean isOrganizer = event.isOrganizer(currentUser.getUserId());
                            boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());
                            Log.d("HomePageFragment", String.format("Event: %s, public: %b, admin: %b, is organizer: %b",
                                    event.getTitle(), isPublic, isAdmin, isOrganizer));
                            if (isPublic || isOrganizer || isAdmin) {
                                allEvents.add(event);
                            }
                        } catch (Exception e) {
                            Log.e("HomePageFragment", "Error parsing event: " + document.getId(), e);
                        }
                    }
                    refreshTagAutocompleteSuggestions();
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadBottomBar(FrameLayout container, String role) {
        int layoutId;
        if ("admin".equalsIgnoreCase(role)) {
            layoutId = R.layout.bottom_bar_home_admin;
        } else if ("organizer".equalsIgnoreCase(role)) {
            layoutId = R.layout.bottom_bar_home_organizer;
        } else {
            layoutId = R.layout.bottom_bar_home_entrant;
        }

        View bottomBarView = getLayoutInflater().inflate(layoutId, container, false);
        container.removeAllViews();
        container.addView(bottomBarView);

        // Profile button (User icon)
        MaterialButton profileBtn = bottomBarView.findViewById(R.id.btn_profile);
        if (profileBtn != null) {
            profileBtn.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new UserProfileFragment())
                    .addToBackStack(null)
                    .commit());
        }

        MaterialButton nearbyMapBtn = bottomBarView.findViewById(R.id.btn_nearby_map);
        if (nearbyMapBtn != null) {
            nearbyMapBtn.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NearbyEventsMapFragment())
                    .addToBackStack(null)
                    .commit());
        }
        
        // Now allows admin to select either reporting or Notifications
        MaterialButton notificationsBtn = bottomBarView.findViewById(R.id.btn_notifications);
        if (notificationsBtn != null) {
            notificationsBtn.setOnClickListener(v -> {
                if ("admin".equalsIgnoreCase(role)) {
                    String[] options = {"Notifications", "Reporting"};

                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("SELECT")
                            .setItems(options, (dialog, which) -> {
                                Fragment destination = (which == 0)
                                        ? new NotificationsFragment()
                                        : new OrganizerReportCenterFragment();

                                getParentFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, destination)
                                        .addToBackStack(null)
                                        .commit();
                            })
                            .show();
                } else {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new NotificationsFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // Admin Profiles button (Profiles icon)
        MaterialButton profilesBtn = bottomBarView.findViewById(R.id.btn_profiles);
        if (profilesBtn != null) {
            profilesBtn.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileListFragment())
                    .addToBackStack(null)
                    .commit());
        }

        // Admin/Organizer Create Event Button
        MaterialButton createEventBtn = bottomBarView.findViewById(R.id.btn_plus);
        if (createEventBtn != null) {
            createEventBtn.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, CreateEventFragment.newInstanceCreateMode(currentUser.getUserId()))
                    .addToBackStack(null)
                    .commit());
        }

        // QR Scanner Button
        MaterialButton qrScannerBtn = bottomBarView.findViewById(R.id.btn_camera);
        if (qrScannerBtn != null) {
            qrScannerBtn.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, QrScannerFragment.newInstance())
                    .addToBackStack(null)
                    .commit());
        }

        // Admin Notification Logs Button
        MaterialButton notificationLogsBtn = bottomBarView.findViewById(R.id.btn_notification_logs);
        if (notificationLogsBtn != null) {
            notificationLogsBtn.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotificationLogsFragment())
                    .addToBackStack(null)
                    .commit());
        }
    }

    private void navigateToEventOverview(String eventId) {
        EventOverviewFragment fragment = new EventOverviewFragment();
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void clearFilterErrors() {
        if (keywordLayout != null) keywordLayout.setError(null);
        if (tagLayout != null) tagLayout.setError(null);
        if (availableDateLayout != null) availableDateLayout.setError(null);
        if (minCapacityLayout != null) minCapacityLayout.setError(null);
    }

    /**
     * Fills the tag field dropdown with distinct tags from loaded events (lowercase, sorted).
     */
    private void refreshTagAutocompleteSuggestions() {
        if (tagInput == null) {
            return;
        }
        Set<String> tags = new HashSet<>();
        for (Event e : allEvents) {
            if (e.getTags() == null) {
                continue;
            }
            for (String t : e.getTags()) {
                if (t != null && !t.trim().isEmpty()) {
                    tags.add(t.trim().toLowerCase(Locale.US));
                }
            }
        }
        List<String> sorted = new ArrayList<>(tags);
        Collections.sort(sorted);
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                sorted);
        tagInput.setAdapter(tagAdapter);
    }

    private void applyFilters() {
        clearFilterErrors();

        String keyword = valueOf(keywordInput).toLowerCase(Locale.US).trim();
        String tagQuery = valueOf(tagInput).toLowerCase(Locale.US).trim();
        String availableDateRaw = valueOf(availableDateInput).trim();
        String minCapacityRaw = valueOf(minCapacityInput).trim();
        boolean openNowOnly = openNowOnlyCheckbox != null && openNowOnlyCheckbox.isChecked();

        LocalDate availableDate = null;
        if (!availableDateRaw.isEmpty()) {
            try {
                availableDate = LocalDate.parse(availableDateRaw);
            } catch (Exception e) {
                availableDateLayout.setError("Date must be YYYY-MM-DD");
                return;
            }
        }

        Integer minCapacity = null;
        if (!minCapacityRaw.isEmpty()) {
            try {
                minCapacity = Integer.parseInt(minCapacityRaw);
                if (minCapacity < 1) {
                    minCapacityLayout.setError("Capacity must be at least 1");
                    return;
                }
            } catch (NumberFormatException e) {
                minCapacityLayout.setError("Capacity must be a number");
                return;
            }
        }

        ZonedDateTime now = ZonedDateTime.now();
        filteredEvents.clear();
        for (Event event : allEvents) {
            if (!matchesKeyword(event, keyword)) continue;
            if (!matchesTagFilter(event, tagQuery)) continue;
            if (!matchesAvailability(event, availableDate, openNowOnly, now)) continue;
            if (!matchesCapacity(event, minCapacity)) continue;
            filteredEvents.add(event);
        }
        adapter.notifyDataSetChanged();
    }

    private boolean matchesKeyword(Event event, String keyword) {
        if (keyword.isEmpty()) return true;
        String title = event.getTitle() == null ? "" : event.getTitle().toLowerCase(Locale.US);
        String description = event.getDescription() == null ? "" : event.getDescription().toLowerCase(Locale.US);
        if (title.contains(keyword) || description.contains(keyword)) {
            return true;
        }
        if (event.getTags() != null) {
            for (String tag : event.getTags()) {
                if (tag != null && tag.toLowerCase(Locale.US).contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * When non-empty, only events that have at least one tag matching the query (exact or substring, case-insensitive).
     */
    private boolean matchesTagFilter(Event event, String tagQuery) {
        if (tagQuery.isEmpty()) {
            return true;
        }
        if (event.getTags() == null || event.getTags().isEmpty()) {
            return false;
        }
        for (String tag : event.getTags()) {
            if (tag == null) {
                continue;
            }
            String normalized = tag.toLowerCase(Locale.US).trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.equals(tagQuery) || normalized.contains(tagQuery)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAvailability(Event event, LocalDate availableDate, boolean openNowOnly, ZonedDateTime now) {
        ZonedDateTime open = event.getRegistrationOpen();
        ZonedDateTime close = event.getRegistrationClose();

        if (openNowOnly && (now.isBefore(open) || now.isAfter(close))) {
            return false;
        }

        if (availableDate != null) {
            LocalDate openDate = open.toLocalDate();
            LocalDate closeDate = close.toLocalDate();
            if (availableDate.isBefore(openDate) || availableDate.isAfter(closeDate)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesCapacity(Event event, Integer minCapacity) {
        if (minCapacity == null) return true;
        Integer capacity = event.getWaitingListCapacity();
        return capacity != null && capacity >= minCapacity;
    }

    private String valueOf(EditText field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().toString();
    }
}
