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
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.error_no_event_id), Toast.LENGTH_SHORT).show();
            }
            navigateToFallbackScreen();
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
                    
                    // Check: Is the current user the organizer of this event?
                    String organizerId = documentSnapshot.getString("organizerId");
                    boolean isOrganizer = (organizerId != null && organizerId.equals(currentUserId));
                    
                    if (currentUserId == null) {
                        showJoinWaitlistButton(btnJoinWaitlist, btnManageWaitlist, eventName, count, inWaitingList);
                        return;
                    }

                    // Also, fetch the current user's profile to see if they are an admin
                    FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                        .addOnSuccessListener(userDoc -> {
                            String role = userDoc.exists() ? userDoc.getString("role") : null;
                            boolean isAdmin = "admin".equals(role);

                            if (isOrganizer || isAdmin) {
                                showManageWaitlistButton(btnJoinWaitlist, btnManageWaitlist);
                            } else {
                                showJoinWaitlistButton(btnJoinWaitlist, btnManageWaitlist, eventName, count, inWaitingList);
                            }
                        })
                        .addOnFailureListener(e ->
                            showJoinWaitlistButton(btnJoinWaitlist, btnManageWaitlist, eventName, count, inWaitingList)
                        );
                } else if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.error_load_event_failed), Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.error_load_event_failed), Toast.LENGTH_SHORT).show();
                }
            });
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

    private void showJoinWaitlistButton(
            Button btnJoinWaitlist,
            Button btnManageWaitlist,
            String eventName,
            int count,
            boolean inWaitingList
    ) {
        btnManageWaitlist.setVisibility(View.GONE);
        btnJoinWaitlist.setVisibility(View.VISIBLE);
        btnJoinWaitlist.setOnClickListener(v -> {
            WaitingListDialogFragment dialog =
                    WaitingListDialogFragment.newInstance(eventId, eventName, count, inWaitingList);
            dialog.show(getChildFragmentManager(), "WaitingListDialog");
        });
    }

    private void showManageWaitlistButton(Button btnJoinWaitlist, Button btnManageWaitlist) {
        btnJoinWaitlist.setVisibility(View.GONE);
        btnManageWaitlist.setVisibility(View.VISIBLE);
        btnManageWaitlist.setOnClickListener(v -> {
            WaitlistManagementFragment dialog = WaitlistManagementFragment.newInstance(eventId);
            dialog.show(getChildFragmentManager(), "WaitlistManagementDialog");
        });
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

        waitlistRepo.joinWaitingList(eventId, userId).addOnSuccessListener(aVoid -> {
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
}
