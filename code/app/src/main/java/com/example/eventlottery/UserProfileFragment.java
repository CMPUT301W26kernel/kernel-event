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

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class UserProfileFragment extends Fragment {

    // UI labels for displaying the current user's profile.
    // MODIFIED: Changed to EditText to match the layout and allow editing.
    private EditText usernameView;
    private EditText emailView;
    private EditText phoneView;
    // KEPT: This variable remains unchanged.
    private TextView roleView;

    // ADDED: New variables for buttons and Device ID text.
    private Button deleteButton, historyButton, doneButton;
    private TextView deviceIdText;

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

        // ADDED: Bindings for new UI elements.
        deleteButton = view.findViewById(R.id.delete_button);
        historyButton = view.findViewById(R.id.history_button);
        doneButton = view.findViewById(R.id.done_button);
        deviceIdText = view.findViewById(R.id.device_id_text);


        // Authentication check: if we don't have a signed-in user, we shouldn't attempt a profile lookup.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // If somehow we reach this screen without authentication, we cannot show a profile.
            usernameView.setHint("Not signed in");
            emailView.setText("");
            phoneView.setText("");
            roleView.setText("");
            return;
        }
        
        // ADDED: Set the Device ID text.
        deviceIdText.setText("Device ID: " + currentUser.getUid());

        // Looks up the profile document in Firestore at `users/{uid}` and bind it to the screen.
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(this::bindProfile)
                .addOnFailureListener(e -> {
                    // Shows a simple failure message; this keeps the UI resilient even if Firestore is unavailable.
                    usernameView.setHint("Failed to load profile");
                    emailView.setText("");
                    phoneView.setText("");
                    roleView.setText("");
                });

        // ADDED: Listeners for button clicks.
        doneButton.setOnClickListener(v -> validateAndSave());
        deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        historyButton.setOnClickListener(v ->
                Toast.makeText(getContext(), "Opening Event History...", Toast.LENGTH_SHORT).show()
        );
    }

    private void bindProfile(DocumentSnapshot snapshot) {
        // If there is no profile document, there's nothing to display.
        if (snapshot == null || !snapshot.exists()) {
            usernameView.setHint("Profile not found");
            emailView.setText("");
            phoneView.setText("");
            roleView.setText("");
            return;
        }

        // Converts the Firestore document into our User model.
        User user = snapshot.toObject(User.class);
        if (user == null) {
            usernameView.setHint("Profile not found");
            emailView.setText("");
            phoneView.setText("");
            roleView.setText("");
            return;
        }

        // Renders the data.
        usernameView.setText(valueOrEmpty(user.getUsername()));
        emailView.setText(valueOrEmpty(user.getEmail()));
        phoneView.setText(valueOrEmpty(user.getPhoneNumber()));
        roleView.setText(valueOrEmpty(user.getRole()));
    }

    private String valueOrEmpty(String value) {
        // Guards against null values in Firestore.
        return value == null ? "" : value;
    }

    // --- ADDED: All code below this line is new. ---

    private void validateAndSave() {
        String username = usernameView.getText().toString().trim();
        String email = emailView.getText().toString().trim();
        String phone = phoneView.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            usernameView.setError("Username is required");
            return;
        }
        
        saveUserProfile(username, email, phone);
    }

    private void saveUserProfile(String username, String email, String phone) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("phoneNumber", phone);
        userData.put("role", roleView.getText().toString());

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save profile.", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmation() {
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.fragment_user_profile_delete_confirmation);

        Button confirmDelete = dialog.findViewById(R.id.dialog_delete_button);
        Button cancelDelete = dialog.findViewById(R.id.dialog_cancel_button);

        confirmDelete.setOnClickListener(v -> {
            deleteUserProfile();
            dialog.dismiss();
        });
        cancelDelete.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void deleteUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile Deleted", Toast.LENGTH_LONG).show();
                    usernameView.setText("");
                    emailView.setText("");
                    phoneView.setText("");
                    roleView.setText("Entrant");
                    navigateToSetUpFragment(); // ADDED
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete profile.", Toast.LENGTH_SHORT).show());
    }

    // ADDED
    private void navigateToSetUpFragment() {
        if (getActivity() != null) {
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SetUpFragment setUpFragment = new SetUpFragment();
            fragmentTransaction.replace(R.id.fragment_container, setUpFragment);
            fragmentTransaction.commit();
        }
    }
}