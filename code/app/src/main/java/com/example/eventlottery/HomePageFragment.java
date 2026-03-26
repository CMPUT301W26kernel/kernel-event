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
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

/**
 * Home page fragment that displays a list of events fetched from Firestore.
 */
public class HomePageFragment extends Fragment {

    private ListView listView;
    private EventAdapter eventAdapter;
    private ArrayList<Event> eventList = new ArrayList<>();

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

        // Reference to the main content views
        listView = view.findViewById(R.id.list_view);
        FrameLayout bottomBar = view.findViewById(R.id.bottom_bar);
        TextView statusText = view.findViewById(R.id.logged_in_status);

        // Setup the event adapter
        eventAdapter = new EventAdapter(requireContext(), eventList);
        listView.setAdapter(eventAdapter);

        // Handle clicking items to see event details
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Event selectedEvent = eventList.get(position);
            navigateToEventOverview(selectedEvent.getEventId());
        });

        // Fetch events from Firestore
        fetchEventsFromFirestore();

        // Get the current user role to decide which bottom bar to load
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

    /**
     * Fetches all events from Firestore and updates the list.
     */
    private void fetchEventsFromFirestore() {
        FirebaseFirestore.getInstance().collection("events")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                eventList.clear();
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
                        eventList.add(event);
                    } catch (Exception e) {
                        Log.e("HomePageFragment", "Error parsing event: " + document.getId(), e);
                    }
                }
                eventAdapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * Loads the correct bottom bar layout based on the user's role and sets up listeners.
     */
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

    /**
     * Navigates to the EventOverviewFragment for a specific event.
     */
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
}