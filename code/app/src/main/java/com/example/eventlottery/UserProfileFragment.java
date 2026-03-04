/**
 * User Profile Fragment
 * Displays a user profile.
 * Last Modified: 2026-02-28 by Grace MacKenzie
 *
 * Notes:
 *      - This is intended to contain modes for both the admin view
 *          and when the user is viewing their own profile.
 *
 * @author author1
 * @author author2
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A simple {@link Fragment} subclass.
 */
public class UserProfileFragment extends Fragment {

    // UI labels for displaying the current user's profile.
    private TextView usernameView;
    private TextView emailView;
    private TextView phoneView;
    private TextView roleView;

    public UserProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflates the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Binds view references.
        usernameView = view.findViewById(R.id.profile_username);
        emailView = view.findViewById(R.id.profile_email);
        phoneView = view.findViewById(R.id.profile_phone);
        roleView = view.findViewById(R.id.profile_role);

        // Authentication check: if we don't have a signed-in user, we shouldn't attempt a profile lookup.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // If somehow we reach this screen without authentication, we cannot show a profile.
            usernameView.setText("Not signed in");
            emailView.setText("");
            phoneView.setText("");
            roleView.setText("");
            return;
        }

        // Looks up the profile document in Firestore at `users/{uid}` and bind it to the screen.
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(this::bindProfile)
                .addOnFailureListener(e -> {
                    // Shows a simple failure message; this keeps the UI resilient even if Firestore is unavailable.
                    usernameView.setText("Failed to load profile");
                    emailView.setText("");
                    phoneView.setText("");
                    roleView.setText("");
                });
    }

    private void bindProfile(DocumentSnapshot snapshot) {
        // If there is no profile document, there's nothing to display.
        if (snapshot == null || !snapshot.exists()) {
            usernameView.setText("Profile not found");
            emailView.setText("");
            phoneView.setText("");
            roleView.setText("");
            return;
        }

        // Converts the Firestore document into our User model.
        User user = snapshot.toObject(User.class);
        if (user == null) {
            usernameView.setText("Profile not found");
            emailView.setText("");
            phoneView.setText("");
            roleView.setText("");
            return;
        }

        // Renders the data.
        usernameView.setText("Username: " + valueOrEmpty(user.getUsername()));
        emailView.setText("Email: " + valueOrEmpty(user.getEmail()));
        phoneView.setText("Phone: " + valueOrEmpty(user.getPhoneNumber()));
        roleView.setText("Role: " + valueOrEmpty(user.getRole()));
    }

    private String valueOrEmpty(String value) {
        // Guards against null values in Firestore.
        return value == null ? "" : value;
    }
}