/**
 * Profile List Fragment
 * Displays a list of all profiles.
 * Last Modified: 2026-03-23 by Rebecca OluwaBiyi
 *
 * Notes:
 *      - Accessible by Admin only.
 *
 * @author Rebecca OluwaBiyi
 * @author Grace MacKenzie
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Displays a list of all user profiles for administration.
 */
public class ProfileListFragment extends Fragment {

    private ListView listView;
    private List<User> userList = new ArrayList<>();
    private ProfileAdapter adapter;

    public ProfileListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_with_bottom_bar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = view.findViewById(R.id.list_view);
        FrameLayout bottomBar = view.findViewById(R.id.bottom_bar);

        // Adjust bottom bar height to fit the large Done button
        ViewGroup.LayoutParams params = bottomBar.getLayoutParams();
        params.height = (int) (100 * getResources().getDisplayMetrics().density);
        bottomBar.setLayoutParams(params);

        // Hide status bar and filters which are only for home page
        View statusText = view.findViewById(R.id.logged_in_status);
        if (statusText != null) {
            statusText.setVisibility(View.GONE);
        }
        View filterContainer = view.findViewById(R.id.filter_container);
        if (filterContainer != null) {
            filterContainer.setVisibility(View.GONE);
        }

        adapter = new ProfileAdapter(requireContext(), userList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, v, position, id) -> {
            User selectedUser = userList.get(position);
            navigateToProfileReview(selectedUser.getUserId());
        });

        loadAllProfiles();
        setupBottomBar(bottomBar);
    }

    private void loadAllProfiles() {
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUserId(doc.getId()); // ensure ID is set
                            userList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load profiles.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBottomBar(FrameLayout container) {
        // Load the single "Done" button bottom bar
        View bottomBarView = getLayoutInflater().inflate(R.layout.bottom_bar_profile_list, container, false);
        container.removeAllViews();
        container.addView(bottomBarView);

        MaterialButton doneBtn = bottomBarView.findViewById(R.id.btn_done);
        if (doneBtn != null) {
            doneBtn.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }
    }

    private void navigateToProfileReview(String userId) {
        UserProfileFragment adminReviewFragment = UserProfileFragment.newInstance(userId, true);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, adminReviewFragment)
                .addToBackStack(null)
                .commit();
    }

    private static class ProfileAdapter extends ArrayAdapter<User> {
        ProfileAdapter(android.content.Context context, List<User> items) {
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