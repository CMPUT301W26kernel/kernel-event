/**
 * User Profile Fragment
 * Displays a user profile.
 * Last Modified: 2026-02-28 by Grace MacKenzie
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass for managing user profiles.
 */
public class UserProfileFragment extends Fragment {

    private EditText usernameEdit, emailEdit, phoneEdit;
    private TextView roleText, deviceIdText;
    private Button deleteButton, historyButton, doneButton;

    public UserProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        usernameEdit = view.findViewById(R.id.username_edit);
        emailEdit = view.findViewById(R.id.email_edit);
        phoneEdit = view.findViewById(R.id.phone_edit);
        roleText = view.findViewById(R.id.role_text);
        deviceIdText = view.findViewById(R.id.device_id_text);
        deleteButton = view.findViewById(R.id.delete_button);
        historyButton = view.findViewById(R.id.history_button);
        doneButton = view.findViewById(R.id.done_button);

        // Fetch Device ID (Always stays the same)
        String androidId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceIdText.setText("Device ID: " + androidId);

        // Load data - This is the part that will be replaced by Firebase later
        fetchUserProfile();

        // Button Listeners
        deleteButton.setOnClickListener(v -> {
            showDeleteConfirmation();
        });

        historyButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Opening Event History...", Toast.LENGTH_SHORT).show();
        });

        doneButton.setOnClickListener(v -> {
            validateAndSave();
        });
    }

    /**
     * Shows a custom confirmation dialog for profile deletion.
     */
    private void showDeleteConfirmation() {
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fragment_user_profile_delete_confirmation);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button confirmDelete = dialog.findViewById(R.id.dialog_delete_button);
        Button cancelDelete = dialog.findViewById(R.id.dialog_cancel_button);

        confirmDelete.setOnClickListener(v -> {
            // TODO: Add Firebase deletion logic here
            // FirebaseFirestore.getInstance().collection("users").document(deviceId).delete()...
            
            Toast.makeText(getContext(), "Profile Deleted Successfully", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            
            // Logic to navigate back to "Set Up Account" would go here
        });

        cancelDelete.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Validates input fields and proceeds to save if successful.
     */
    private void validateAndSave() {
        String username = usernameEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim();
        String phone = phoneEdit.getText().toString().trim();

        // Validation: Username cannot be empty
        if (TextUtils.isEmpty(username)) {
            usernameEdit.setError("Username is required");
            usernameEdit.requestFocus();
            return;
        }

        // Optional: Simple email validation
        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEdit.setError("Invalid email format");
            emailEdit.requestFocus();
            return;
        }

        // If validation passes, call save logic
        saveUserProfile(username, email, phone);
    }

    /**
     * Mock function to simulate fetching data from Firebase.
     * Replace the hardcoded values with a Firestore query later.
     */
    private void fetchUserProfile() {
        // MOCK DATA
        String mockUsername = "JohnDoe2024";
        String mockEmail = "john.doe@example.com";
        String mockPhone = "(555) 123-4567";
        String mockRole = "Entrant";

        // Populate fields
        usernameEdit.setText(mockUsername);
        emailEdit.setText(mockEmail);
        phoneEdit.setText(mockPhone);
        roleText.setText(mockRole);
    }

    /**
     * Mock function to simulate saving data to Firebase.
     * Replace the Toast with a Firestore update/set call later.
     */
    private void saveUserProfile(String username, String email, String phone) {
        // TODO: Add Firebase save logic here
        Toast.makeText(getContext(), "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
    }
}