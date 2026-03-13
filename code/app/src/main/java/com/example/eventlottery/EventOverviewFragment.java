/**
 * Event Overview Fragment
 * Displays the Details of an event
 * Last Modified: 2026-03-12
 *
 * Notes:
 *      - This fragment can take an event id from firebase as a Bundle argument
 *          and load the event directly from firebase. This prevents the overhead of making
 *          an event parseable or serializable and keeps the fragment light weight.
 *
 * @author Pierce Hampton
 * @author author2
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Displays event details and allows entrants to interact with the Waitlist.
 */
public class EventOverviewFragment extends Fragment implements WaitingListDialogFragment.WaitingListDialogListener {

    private WaitingListRepository waitlistRepo;
    private String eventId;

    public EventOverviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        waitlistRepo = new WaitingListRepository();
        
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
        /*
            TODO: Load the proper content xml, data set, and bottom bar button set
                depending on if the user is an admin, organizer, or standard entrant
         */

        if (eventId == null) {
            Toast.makeText(getContext(), "Error: No Event ID provided", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Button btnJoinWaitlist = view.findViewById(R.id.btn_join_waitlist);
        Button btnManageWaitlist = view.findViewById(R.id.btn_manage_waitlist);

        // Load event data from Firestore to check permissions and waitlist status
        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String eventNameRaw = documentSnapshot.getString("title");
                    final String eventName = (eventNameRaw == null) ? "Event" : eventNameRaw;
                    
                    List<String> waitlist = new ArrayList<>();
                    Object waitlistObj = documentSnapshot.get("waitingList");
                    if (waitlistObj instanceof List<?>) {
                        for (Object item : (List<?>) waitlistObj) {
                            if (item instanceof String) {
                                waitlist.add((String) item);
                            }
                        }
                    }

                    final int count = waitlist.size();
                    final String currentUserId = getCurrentUserId();
                    final boolean inWaitingList = waitlist.contains(currentUserId);
                    
                    // Permission Check: Is the current user the organizer of this event?
                    String organizerId = documentSnapshot.getString("organizerId");
                    boolean isOrganizer = (organizerId != null && organizerId.equals(currentUserId));
                    
                    if (isOrganizer) {
                        // Organizers see the Manage button
                        btnManageWaitlist.setVisibility(View.VISIBLE);
                        btnManageWaitlist.setOnClickListener(v -> {
                            WaitlistManagementFragment dialog = WaitlistManagementFragment.newInstance(eventId);
                            dialog.show(getChildFragmentManager(), "WaitlistManagementDialog");
                        });
                    } else {
                        // Entrants see the Join/Leave button
                        btnJoinWaitlist.setVisibility(View.VISIBLE);
                        btnJoinWaitlist.setOnClickListener(v -> {
                            WaitingListDialogFragment dialog = WaitingListDialogFragment.newInstance(eventId, eventName, count, inWaitingList);
                            dialog.show(getChildFragmentManager(), "WaitingListDialog");
                        });
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load event data", Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * Helper method to get the current authenticated user's ID.
     * @return The ID of the current user, or "unauthenticated_user" if not authenticated.
     */
    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return "unauthenticated_user";
    }

    @Override
    public void onJoinWaitingList(String eventId) {
        waitlistRepo.joinWaitingList(eventId, getCurrentUserId()).addOnSuccessListener(aVoid -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Joined waitlist successfully!", Toast.LENGTH_SHORT).show();
                // TODO: Refresh data by recreating the view or relying on a SnapshotListener in a final implementation
            }
        });
    }

    @Override
    public void onLeaveWaitingList(String eventId) {
        waitlistRepo.leaveWaitingList(eventId, getCurrentUserId()).addOnSuccessListener(aVoid -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Left waitlist successfully!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewWaitingList(String eventId) {
        WaitlistManagementFragment dialog = WaitlistManagementFragment.newInstance(eventId);
        dialog.show(getChildFragmentManager(), "WaitlistManagementDialog");
    }
}
