/**
 * User Profile Fragment
 * Displays a user profile.
 * Last Modified: 2026-03-11 by Rebecca OluwaBiyi
 *
 * Notes:
 *      - This is intended to contain modes for both the admin view
 *          and when the user is viewing their own profile.
 *
 * @author Rebecca OluwaBiyi
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
import android.provider.Settings;
import android.util.Log;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class UserProfileFragment extends Fragment {

    // UI labels for displaying the current user's profile.
    // Uses EditText to match the layout and allow editing.
    private EditText usernameView;
    private EditText emailView;
    private EditText phoneView;
    // KEPT: This variable remains unchanged.
    private TextView roleView;

    //New variables for buttons and Device ID text.
    private Button deleteButton, historyButton, doneButton, notificationLogsButton, signOutButton;
    private TextView deviceIdText, profileTitle;

    // Mode flags
    private String targetUserId;
    private boolean isAdminMode = false;

    // Constants for arguments
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_IS_ADMIN_MODE = "is_admin_mode";

    /**
     * Creates a new instance of UserProfileFragment for a specific user.
     * @param userId The ID of the user whose profile to display.
     * @param isAdminMode Whether the fragment is in admin mode.
     * @return A new instance of UserProfileFragment.
     */
    public static UserProfileFragment newInstance(String userId, boolean isAdminMode) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putBoolean(ARG_IS_ADMIN_MODE, isAdminMode);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public UserProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Called to do initial creation of the fragment.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Inflates the layout for this fragment.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflates the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    /**
     * Initializes UI components, sets up Firebase authentication checks, 
     * and fetches the user profile from Firestore once the view is created.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Binds view references.
        profileTitle = view.findViewById(R.id.profile_title);
        usernameView = view.findViewById(R.id.profile_username);
        emailView = view.findViewById(R.id.profile_email);
        phoneView = view.findViewById(R.id.profile_phone);
        roleView = view.findViewById(R.id.profile_role);

        //Bindings for new UI elements.
        deleteButton = view.findViewById(R.id.delete_button);
        signOutButton = view.findViewById(R.id.sign_out_button);
        historyButton = view.findViewById(R.id.history_button);
        notificationLogsButton = view.findViewById(R.id.notification_logs_button);
        doneButton = view.findViewById(R.id.done_button);
        deviceIdText = view.findViewById(R.id.device_id_text);

        // Check for arguments to determine mode and target user.
        if (getArguments() != null) {
            targetUserId = getArguments().getString(ARG_USER_ID);
            isAdminMode = getArguments().getBoolean(ARG_IS_ADMIN_MODE, false);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (targetUserId == null && currentUser != null) {
            targetUserId = currentUser.getUid();
        }

        // Configure UI based on mode
        if (isAdminMode) {
            profileTitle.setText("Review Profile"); // Will be updated to username once loaded
            historyButton.setVisibility(View.GONE);
            signOutButton.setVisibility(View.GONE);
            // Notification logs button visibility will be set in bindProfile based on role
        } else {
            profileTitle.setText("Your Profile");
            historyButton.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.VISIBLE);
            notificationLogsButton.setVisibility(View.GONE);
        }

        //Listeners for button clicks.
        doneButton.setOnClickListener(v -> validateAndSave());
        deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        signOutButton.setOnClickListener(v -> signOut());
        historyButton.setOnClickListener(v -> {
            // Navigate to EventHistoryFragment
            if (getActivity() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new EventHistoryFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
        notificationLogsButton.setOnClickListener(v ->
                Toast.makeText(getContext(), "Opening Notification Logs...", Toast.LENGTH_SHORT).show()
        );

        // Authentication check: if we don't have a signed-in user or target user, we shouldn't attempt a profile lookup.
        if (targetUserId == null) {
            usernameView.setHint("Not signed in");
            emailView.setText("");
            phoneView.setText("");
            roleView.setText("");
            return;
        }
        
        //Set the Device ID text.
        if (getContext() != null) {
            String androidId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            deviceIdText.setText("Device ID: " + (androidId != null ? androidId : "Unknown"));
        } else {
            deviceIdText.setText("Device ID: " + targetUserId);
        }

        // Looks up the profile document in Firestore at `users/{uid}` and bind it to the screen.
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(this::bindProfile)
                .addOnFailureListener(e -> {
                    // Shows a simple failure message; this keeps the UI resilient even if Firestore is unavailable.
                    usernameView.setHint("Failed to load profile");
                    emailView.setText("");
                    phoneView.setText("");
                    roleView.setText("");
                });
    }

    /**
     * Binds the retrieved Firestore profile document to the UI components.
     * @param snapshot The DocumentSnapshot retrieved from Firestore.
     */
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
        roleView.setText(capitalize(user.getRole()));

        if (isAdminMode) {
            profileTitle.setText(valueOrEmpty(user.getUsername()));
            // Show notification logs button ONLY for Organizers in Admin mode
            if ("Organizer".equalsIgnoreCase(user.getRole())) {
                notificationLogsButton.setVisibility(View.VISIBLE);
            } else {
                notificationLogsButton.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Capitalizes the first letter of a string.
     * @param str The string to capitalize.
     * @return The capitalized string.
     */
    public String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Returns the provided string if not null, or an empty string otherwise.
     * @param value The string to check.
     * @return The original string or an empty string.
     */
    public String valueOrEmpty(String value) {
        // Guards against null values in Firestore.
        return value == null ? "" : value;
    }


    /**
     * Validates user input and proceeds to save the profile if valid.
     */
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

    /**
     * Saves or updates the user profile data in the Firestore database.
     * @param username The updated username.
     * @param email The updated email address.
     * @param phone The updated phone number.
     */
    private void saveUserProfile(String username, String email, String phone) {
        if (targetUserId == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("phoneNumber", phone);
        userData.put("role", roleView.getText().toString());
        userData.put("userId", targetUserId); // Ensure userId is also saved/updated

        FirebaseFirestore.getInstance().collection("users").document(targetUserId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save profile.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Navigates back to the previous screen (Home or Admin List).
     */
    private void navigateBack() {
        if (getActivity() != null) {
            getParentFragmentManager().popBackStack();
        }
    }

    /**
     * Displays a confirmation dialog before deleting the user profile.
     */
    private void showDeleteConfirmation() {
        if (getContext() == null) return;
        
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.fragment_user_profile_delete_confirmation);
        
        // Adjust dialog text if in Admin mode
        if (isAdminMode) {
            TextView dialogTitle = dialog.findViewById(R.id.dialog_title); // I should add this ID
            if (dialogTitle != null) {
                dialogTitle.setText("Are you sure you'd like to delete this profile?");
            }
        }

        Button confirmDelete = dialog.findViewById(R.id.dialog_delete_button);
        Button cancelDelete = dialog.findViewById(R.id.dialog_cancel_button);

        confirmDelete.setOnClickListener(v -> {
            deleteUserProfile();
            dialog.dismiss();
        });
        cancelDelete.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Deletes the user profile document from Firestore and navigates back to the setup screen.
     */
    private void deleteUserProfile() {
        if (targetUserId == null) return;

        FirebaseFirestore.getInstance().collection("users").document(targetUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    removeUserFromEvents(targetUserId);
                    Toast.makeText(getContext(), "Profile Deleted", Toast.LENGTH_LONG).show();
                    if (isAdminMode) {
                        navigateBack(); // Admin goes back to the list
                    } else {
                        // User goes back to setup since they deleted their own account
                        usernameView.setText("");
                        emailView.setText("");
                        phoneView.setText("");
                        roleView.setText("Entrant");
                        navigateToSetUpFragment();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete profile.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Removes the user from all events they are registered for (all lottery lists).
     * @param userId The ID of the user to remove.
     */
    private void removeUserFromEvents(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String[] lists = {"waitingList", "invitedList", "acceptedList", "cancelledList"};

        for (String listName : lists) {
            db.collection("events")
                    .whereArrayContains(listName, userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            doc.getReference().update(listName, FieldValue.arrayRemove(userId));
                        }
                    })
                    .addOnFailureListener(e -> Log.e("UserProfileFragment", "Error removing user from " + listName, e));
        }
    }

    /**
     * Signs the user out of Firebase and navigates to the setup screen.
     */
    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(getContext(), "Signed Out", Toast.LENGTH_SHORT).show();
        navigateToSetUpFragment();
    }

    /**
     * Navigates the user back to the SetUpFragment after profile deletion or sign out.
     */
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
