package com.example.eventlottery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PendingInvitesFragment extends Fragment {

    private LinearLayout pendingInvitesList;
    private String currentUserId;

    public PendingInvitesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pending_invites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pendingInvitesList = view.findViewById(R.id.pending_invites_list);
        currentUserId = FirebaseAuth.getInstance().getUid();

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        loadPendingCoOrganizerInvites();
    }

    private void loadPendingCoOrganizerInvites() {
        if (currentUserId == null) return;
        FirebaseFirestore.getInstance().collection("events")
                .whereArrayContains("pendingCoOrganizers", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    pendingInvitesList.removeAllViews();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No pending invites.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String eventName = doc.getString("title");
                        String eventId = doc.getId();
                        String organizerId = doc.getString("organizerId");

                        View inviteView = getLayoutInflater().inflate(R.layout.item_pending_invite, pendingInvitesList, false);
                        TextView titleView = inviteView.findViewById(R.id.invite_event_title);
                        TextView fromView = inviteView.findViewById(R.id.invite_from);
                        TextView roleView = inviteView.findViewById(R.id.invite_role);
                        
                        titleView.setText("Invite For: " + (eventName != null ? eventName : "Unknown Event"));

                        if (organizerId != null) {
                            FirebaseFirestore.getInstance().collection("users").document(organizerId)
                                    .get().addOnSuccessListener(userDoc -> {
                                        String username = userDoc.getString("username");
                                        String role = userDoc.getString("role");
                                        fromView.setText("From: " + (username != null ? username : "Unknown Organizer"));
                                        
                                        String formattedRole = role != null && !role.isEmpty() 
                                            ? role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase() 
                                            : "Unknown Role";
                                        roleView.setText("Role: " + formattedRole);
                                    }).addOnFailureListener(e -> {
                                        fromView.setText("From: Unknown Organizer");
                                        roleView.setText("Role: Unknown Role");
                                    });
                        } else {
                            fromView.setText("From: Unknown Organizer");
                            roleView.setText("Role: Unknown Role");
                        }

                        Button acceptBtn = inviteView.findViewById(R.id.btn_accept_invite);
                        Button declineBtn = inviteView.findViewById(R.id.btn_decline_invite);

                        acceptBtn.setOnClickListener(v -> handleInviteResponse(eventId, true));
                        declineBtn.setOnClickListener(v -> handleInviteResponse(eventId, false));

                        pendingInvitesList.addView(inviteView);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load invites", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleInviteResponse(String eventId, boolean accept) {
        if (currentUserId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("pendingCoOrganizers", FieldValue.arrayRemove(currentUserId));
        if (accept) {
            updates.put("coOrganizers", FieldValue.arrayUnion(currentUserId));
            removeUserFromWaitlist(eventId, currentUserId);
        }

        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), accept ? "Invite Accepted" : "Invite Declined", Toast.LENGTH_SHORT).show();
                    loadPendingCoOrganizerInvites(); // Refresh list
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to respond to invite", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeUserFromWaitlist(String eventId, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String[] lists = {"waitingList", "invitedList", "acceptedList", "cancelledList"};

        for (String listName : lists) {
            db.collection("events").document(eventId)
                    .update(listName, FieldValue.arrayRemove(userId));
        }
    }
}