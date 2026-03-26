/**
 * Home page fragment that displays a list of events fetched from Firestore.
 * This fragment acts as the main entry point for entrants to browse and select events.
 * 
 * Last Modified: 2026-03-25 by Rebecca OluwaBiyi
 * @author Pierce
 * @author Rebecca OluwaBiyi
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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Home page fragment that displays a list of events fetched from Firestore.
 */
public class HomePageFragment extends Fragment {
    private final ArrayList<Event> allEvents = new ArrayList<>();
    private final ArrayList<Event> filteredEvents = new ArrayList<>();
    private EventAdapter adapter;

    private TextInputLayout keywordLayout;
    private TextInputLayout availableDateLayout;
    private TextInputLayout minCapacityLayout;

    private TextInputEditText keywordInput;
    private TextInputEditText availableDateInput;
    private TextInputEditText minCapacityInput;
    private CheckBox openNowOnlyCheckbox;

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
        availableDateLayout = view.findViewById(R.id.input_available_date_layout);
        minCapacityLayout = view.findViewById(R.id.input_min_capacity_layout);
        keywordInput = view.findViewById(R.id.input_search_keyword);
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

        // Fetch all events from Firestore
        fetchEventsFromFirestore();

        // Setup role-based bottom bar and status
        FrameLayout bottomBar = view.findViewById(R.id.bottom_bar);
        TextView statusText = view.findViewById(R.id.logged_in_status);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String role = documentSnapshot.getString("role");
                            
                            // Update the status bar
                            if (statusText != null) {
                                String userLabel = (username != null ? username : "User");
                                String roleLabel = (role != null ? role : "Unknown");
                                statusText.setText(String.format("Logged in as: %s (%s)", userLabel, roleLabel));
                            }

                            loadBottomBar(bottomBar, role);
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
                        String title = document.getString("title");
                        String description = document.getString("description");
                        String organizerId = document.getString("organizerId");
                        Long capacityLong = document.getLong("waitingListCapacity");
                        Integer capacity = (capacityLong != null) ? capacityLong.intValue() : null;

                        ZonedDateTime openDate = ZonedDateTime.now();
                        ZonedDateTime closeDate = ZonedDateTime.now().plusDays(1);
                        
                        Object openObj = document.get("registrationOpen");
                        if (openObj instanceof com.google.firebase.Timestamp) {
                            com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) openObj;
                            openDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanoseconds()), ZoneId.systemDefault());
                        }
                        
                        Object closeObj = document.get("registrationClose");
                        if (closeObj instanceof com.google.firebase.Timestamp) {
                            com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) closeObj;
                            closeDate = ZonedDateTime.ofInstant(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanoseconds()), ZoneId.systemDefault());
                        }

                        Event event = new Event(
                            title != null ? title : "Untitled Event",
                            description != null ? description : "No description",
                            organizerId, openDate, closeDate, capacity
                        );
                        event.setEventId(document.getId());
                        allEvents.add(event);
                    } catch (Exception e) {
                        Log.e("HomePageFragment", "Error parsing event: " + document.getId(), e);
                    }
                }
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

        // Admin Profiles button (Profiles icon)
        MaterialButton profilesBtn = bottomBarView.findViewById(R.id.btn_profiles);
        if (profilesBtn != null) {
            profilesBtn.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileListFragment())
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
        if (availableDateLayout != null) availableDateLayout.setError(null);
        if (minCapacityLayout != null) minCapacityLayout.setError(null);
    }

    private void applyFilters() {
        clearFilterErrors();

        String keyword = valueOf(keywordInput).toLowerCase(Locale.US).trim();
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
        return title.contains(keyword) || description.contains(keyword);
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

    private String valueOf(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString();
    }
}