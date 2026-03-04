/**
 * Set Up Fragment
 * Sets up a new user with a profile.
 * Last Modified: 2026-02-28 by Grace MacKenzie
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * A simple {@link Fragment} subclass.
 */
public class SetUpFragment extends Fragment {

    // UI references (TextInputLayouts let us show validation errors inline).
    private TextInputLayout usernameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout passwordLayout;

    // UI references (TextInputEditTexts hold the user's typed values).
    private TextInputEditText usernameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;

    // Role selection UI (must choose exactly one of Entrant / Organizer / Admin).
    private RadioGroup roleGroup;
    private RadioButton roleEntrant;
    private RadioButton roleOrganizer;
    private RadioButton roleAdmin;

    // Inline error text + loading UI so the user can see what's happening.
    private TextView errorText;
    private ProgressBar progressBar;
    private Button createAccountButton;

    // Firebase services used by this feature:
    // - FirebaseAuth: create/sign-in users
    // - Firestore: store the User profile document including role
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public SetUpFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflating  the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initializing Firebase instances once the fragment has a valid context.
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Binding view references
        usernameLayout = view.findViewById(R.id.input_username_layout);
        emailLayout = view.findViewById(R.id.input_email_layout);
        phoneLayout = view.findViewById(R.id.input_phone_layout);
        passwordLayout = view.findViewById(R.id.input_password_layout);

        usernameInput = view.findViewById(R.id.input_username);
        emailInput = view.findViewById(R.id.input_email);
        phoneInput = view.findViewById(R.id.input_phone);
        passwordInput = view.findViewById(R.id.input_password);

        roleGroup = view.findViewById(R.id.role_group);
        roleEntrant = view.findViewById(R.id.role_entrant);
        roleOrganizer = view.findViewById(R.id.role_organizer);
        roleAdmin = view.findViewById(R.id.role_admin);

        errorText = view.findViewById(R.id.setup_error);
        progressBar = view.findViewById(R.id.setup_progress);
        createAccountButton = view.findViewById(R.id.button_create_account);

        // Primary action: validates input, creates Firebase account, writes profile to Firestore, then route to home.
        createAccountButton.setOnClickListener(v -> attemptCreateAccount());
    }

    private void attemptCreateAccount() {
        // Step 1: resets previous errors so we only show current validation results.
        clearErrors();

        // Step 2: reads and normalize user inputs.
        String username = getText(usernameInput);
        String email = getText(emailInput);
        String phone = getText(phoneInput);
        String password = getText(passwordInput);
        String role = resolveSelectedRole();

        // Step 3: validates locally before we call Firebase (faster feedback, fewer network requests).
        boolean valid = true;

        if (TextUtils.isEmpty(username)) {
            usernameLayout.setError("Username is required");
            valid = false;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email");
            valid = false;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneLayout.setError("Phone number is required");
            valid = false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            valid = false;
        }

        if (role == null) {
            showError("Please select a role");
            valid = false;
        }

        if (!valid) {
            // If validation fails, stop here. The user can correct the highlighted fields.
            return;
        }

        // Step 4: show loading and prevent duplicate submissions.
        setLoading(true);

        // Step 5: creates the Firebase Authentication account.
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        // Account creation failed (e.g., weak password, email already in use, network issue).
                        setLoading(false);
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Account creation failed";
                        showError(message);
                        return;
                    }

                    // Step 6: on success, Firebase assigns a unique UID.
                    // We use that UID as the Firestore document id so profile and auth stay linked.
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        setLoading(false);
                        showError("Account created but user is not available. Please try signing in again.");
                        return;
                    }

                    String userId = firebaseUser.getUid();
                    User userProfile = new User(userId, username, email, role, phone);

                    // Step 7: stores the user profile (including role) in Firestore under `users/{uid}`.
                    firestore.collection("users")
                            .document(userId)
                            .set(userProfile)
                            .addOnSuccessListener(unused -> {
                                // Profile successfully stored -> user can enter the app.
                                setLoading(false);
                                Toast.makeText(requireContext(), "Profile created", Toast.LENGTH_SHORT).show();
                                navigateToHome();
                            })
                            .addOnFailureListener(e -> {
                                // Account exists but profile write failed (e.g., Firestore rules or network).
                                setLoading(false);
                                showError("Failed to save profile: " + e.getMessage());
                            });
                });
    }

    private void navigateToHome() {
        if (getActivity() == null) {
            return;
        }

        // Role-based routing expanded here later.
        // For now, all roles go to the same HomePageFragment (which can render role-specific UI).
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomePageFragment())
                .commit();
    }

    private String resolveSelectedRole() {
        // Converts the selected radio button to the canonical role string we store in Firestore.
        int checkedId = roleGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.role_entrant) {
            return "entrant";
        } else if (checkedId == R.id.role_organizer) {
            return "organizer";
        } else if (checkedId == R.id.role_admin) {
            return "admin";
        }
        return null;
    }

    private void clearErrors() {
        // Clears per-field errors and hide any form-level error message.
        usernameLayout.setError(null);
        emailLayout.setError(null);
        phoneLayout.setError(null);
        passwordLayout.setError(null);
        errorText.setVisibility(View.GONE);
    }

    private void showError(String message) {
        // Shows a form-level error (used for things not tied to one specific field, e.g. "choose a role").
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        // Toggle loading UI while network calls are in progress.
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        createAccountButton.setEnabled(!loading);
    }

    private String getText(TextInputEditText editText) {
        // Safe helper to avoid NPEs and trim whitespace from user input.
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}