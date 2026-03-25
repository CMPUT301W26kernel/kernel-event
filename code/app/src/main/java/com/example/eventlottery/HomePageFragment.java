/**
 * Home page fragment that displays a list of events fetched from Firestore.
 * This fragment acts as the main entry point for entrants to browse and select events.
 * 
 * @author Pierce
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
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

        // Fetch all events from Firestore
        FirebaseFirestore.getInstance().collection("events")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                allEvents.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        // Since Event.java uses ZonedDateTime and lacks an empty constructor, 
                        // we cannot use document.toObject(Event.class). We must build it manually.
                        
                        String title = document.getString("title");
                        String description = document.getString("description");
                        String organizerId = document.getString("organizerId");
                        
                        // Parse capacity safely
                        Long capacityLong = document.getLong("waitingListCapacity");
                        Integer capacity = (capacityLong != null) ? capacityLong.intValue() : null;

                        // Parse dates safely
                        ZonedDateTime openDate = ZonedDateTime.now(); // Fallback
                        ZonedDateTime closeDate = ZonedDateTime.now().plusDays(1); // Fallback
                        
                        Object openObj = document.get("registrationOpen");
                        if (openObj instanceof com.google.firebase.Timestamp) {
                            com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) openObj;
                            openDate = ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(ts.getSeconds(), ts.getNanoseconds()), 
                                ZoneId.systemDefault()
                            );
                        } else if (openObj instanceof String) {
                            try { openDate = ZonedDateTime.parse((String) openObj); } catch (Exception ignored) {}
                        }

                        Object closeObj = document.get("registrationClose");
                        if (closeObj instanceof com.google.firebase.Timestamp) {
                            com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) closeObj;
                            closeDate = ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(ts.getSeconds(), ts.getNanoseconds()), 
                                ZoneId.systemDefault()
                            );
                        } else if (closeObj instanceof String) {
                            try { closeDate = ZonedDateTime.parse((String) closeObj); } catch (Exception ignored) {}
                        }

                        // Construct the event using the strict 6 parameter constructor
                        Event event = new Event(
                            title != null ? title : "Untitled Event",
                            description != null ? description : "No description",
                            organizerId,
                            openDate,
                            closeDate,
                            capacity
                        );
                        
                        event.setEventId(document.getId());
                        allEvents.add(event);
                        
                    } catch (Exception e) {
                        Log.e("HomePageFragment", "Error parsing event document: " + document.getId(), e);
                    }
                }
                applyFilters();
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_events), Toast.LENGTH_SHORT).show();
                }
            });

        // Navigation to EventOverviewFragment
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Event selectedEvent = filteredEvents.get(position);
            
            // Navigate to EventOverviewFragment
            EventOverviewFragment fragment = new EventOverviewFragment();
            Bundle args = new Bundle();
            args.putString("eventId", selectedEvent.getEventId());
            fragment.setArguments(args);
            
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void clearFilterErrors() {
        keywordLayout.setError(null);
        availableDateLayout.setError(null);
        minCapacityLayout.setError(null);
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