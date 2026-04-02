/**
 * Entrant List Fragment
 * Displays a searchable list of all entrants. Organizers and Admins can select an entrant
 * from this list to invite them to become a Co-Organizer for an event.
 * Last Modified: 2026-03-31 by Rebecca OluwaBiyi
 *
 * Notes:
 *      - Accessible by Organizer and Admin roles only via the User Profile.
 *
 * @author Rebecca OluwaBiyi
 * @since 2026-03-31
 */
package com.example.eventlottery.coorganizers;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.example.eventlottery.profiles.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Displays a searchable list of all users with the Entrant role.
 */
public class EntrantListFragment extends Fragment {

    private ListView listView;
    private List<User> allEntrants = new ArrayList<>();
    private List<User> filteredEntrants = new ArrayList<>();
    private EntrantAdapter adapter;
    private String currentUserId;

    public EntrantListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = view.findViewById(R.id.list_view);
        EditText searchInput = view.findViewById(R.id.search_input);
        
        currentUserId = FirebaseAuth.getInstance().getUid();

        adapter = new EntrantAdapter(requireContext(), filteredEntrants);
        listView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntrants(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, v, position, id) -> {
            User selectedUser = filteredEntrants.get(position);
            showEventSelectionDialog(selectedUser);
        });

        view.findViewById(R.id.btn_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        loadEntrants();
    }

    private void loadEntrants() {
        FirebaseFirestore.getInstance().collection("users")
                .whereIn("role", java.util.Arrays.asList("entrant", "Entrant"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEntrants.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUserId(doc.getId());
                            allEntrants.add(user);
                        }
                    }
                    filterEntrants("");
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load entrants.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filterEntrants(String query) {
        filteredEntrants.clear();
        String q = query.toLowerCase();
        for (User u : allEntrants) {
            String name = u.getUsername() != null ? u.getUsername().toLowerCase() : "";
            String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
            if (name.contains(q) || email.contains(q)) {
                filteredEntrants.add(u);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showEventSelectionDialog(User targetUser) {
        if (currentUserId == null) return;

        FirebaseFirestore.getInstance().collection("events")
                .whereEqualTo("organizerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "You have no events.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> eventNames = new ArrayList<>();
                    List<String> eventIds = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        eventNames.add(doc.getString("title"));
                        eventIds.add(doc.getId());
                    }

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Select Event to invite " + targetUser.getUsername())
                            .setItems(eventNames.toArray(new String[0]), (dialog, which) -> {
                                inviteToEvent(eventIds.get(which), targetUser.getUserId());
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to fetch events", Toast.LENGTH_SHORT).show());
    }

    private void inviteToEvent(String eventId, String targetUserId) {
        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .update("pendingCoOrganizers", FieldValue.arrayUnion(targetUserId))
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Invite sent!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to send invite.", Toast.LENGTH_SHORT).show());
    }

    private static class EntrantAdapter extends ArrayAdapter<User> {
        EntrantAdapter(android.content.Context context, List<User> items) {
            super(context, R.layout.item_profile, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_profile, parent, false);
            }

            User user = getItem(position);
            if (user != null) {
                TextView usernameView = convertView.findViewById(R.id.profile_username);
                TextView detailsView = convertView.findViewById(R.id.profile_details);

                usernameView.setText(user.getUsername() != null ? user.getUsername() : "Unknown User");
                String details = user.getRole() != null ? user.getRole() : "No role";
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    details += " • " + user.getEmail();
                }
                detailsView.setText(details);
            }
            return convertView;
        }
    }
}