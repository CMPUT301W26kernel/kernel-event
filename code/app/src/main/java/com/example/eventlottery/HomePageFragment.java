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
import android.widget.ListView;
import android.widget.Toast;

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
        ArrayList<Event> eventList = new ArrayList<>();
        EventAdapter adapter = new EventAdapter(requireContext(), eventList);
        listView.setAdapter(adapter);

        // Fetch all events from Firestore
        FirebaseFirestore.getInstance().collection("events")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                eventList.clear();
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
                            openDate = ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(((com.google.firebase.Timestamp) openObj).getSeconds()), 
                                ZoneId.systemDefault()
                            );
                        } else if (openObj instanceof String) {
                            try { openDate = ZonedDateTime.parse((String) openObj); } catch (Exception ignored) {}
                        }

                        Object closeObj = document.get("registrationClose");
                        if (closeObj instanceof com.google.firebase.Timestamp) {
                            closeDate = ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(((com.google.firebase.Timestamp) closeObj).getSeconds()), 
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
                        eventList.add(event);
                        
                    } catch (Exception e) {
                        Log.e("HomePageFragment", "Error parsing event document: " + document.getId(), e);
                    }
                }
                adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
                }
            });

        // Navigation to EventOverviewFragment
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Event selectedEvent = eventList.get(position);
            
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
}