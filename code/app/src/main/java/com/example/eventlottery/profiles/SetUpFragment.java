/**
 * Set Up Fragment
 * Sets up a new user with a profile.
 * Last Modified: 2026-03-13 by Pierce Hampton
 *
 * @author Pierce Hampton
 * @author Grace MacKenzie
 * @since 2026-02-28
 */
package com.example.eventlottery.profiles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.eventlottery.HomePageFragment;
import com.example.eventlottery.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.TextUtils;
import android.util.Patterns;

import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass for user setup.
 */
public class SetUpFragment extends Fragment {
    private TextInputLayout usernameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout passwordLayout;

    private TextInputEditText usernameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;

    // Role selection UI (must choose exactly one of Entrant / Organizer / Admin).
    private RadioGroup roleGroup;
    private View roleLabel;
    private TextView errorText;
    private ProgressBar progressBar;
    private Button createAccountButton;
    private Button toggleAuthModeButton;

    // Firebase services used by this feature:
    // - FirebaseAuth: create/sign-in users
    // - Firestore: store the User profile document including role
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private boolean signInMode = false;

    // Name: letters, spaces, apostrophes, and hyphens only (2 to 50 chars).
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z\\s'\\-]{1,49}$");
    // Phone: optional leading +, 10-15 digits, spaces/dashes/parentheses allowed.
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\-()\\s]{10,20}$");

    public SetUpFragment() {
        // Required empty public constructor
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
        roleLabel = view.findViewById(R.id.role_label);
        errorText = view.findViewById(R.id.setup_error);
        progressBar = view.findViewById(R.id.setup_progress);
        createAccountButton = view.findViewById(R.id.button_create_account);
        toggleAuthModeButton = view.findViewById(R.id.button_toggle_auth_mode);

        // Primary action: validates input, creates Firebase account, writes profile to Firestore, then route to home.
        createAccountButton.setOnClickListener(v -> {
            if (signInMode) {
                attemptSignIn();
            } else {
                attemptCreateAccount();
            }
        });
        toggleAuthModeButton.setOnClickListener(v -> {
            signInMode = !signInMode;
            updateAuthModeUi();
        });

        updateAuthModeUi();
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
            usernameLayout.setError("Name is required");
            valid = false;
        } else if (!NAME_PATTERN.matcher(username).matches()) {
            usernameLayout.setError("Use 2-50 letters, spaces, apostrophes, or hyphens");
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
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            phoneLayout.setError("Enter a valid phone number");
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

    private void attemptSignIn() {
        clearErrors();

        String email = getText(emailInput);
        String password = getText(passwordInput);
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email");
            valid = false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            valid = false;
        }

        if (!valid) {
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Signed in", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Sign in failed: " + e.getMessage());
                });
    }

    private void navigateToHome() {
        // Fix: Added check for isAdded() and getActivity() to prevent crashes if fragment is detached.
        if (isAdded() && getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomePageFragment())
                    .commit();
        }
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
        toggleAuthModeButton.setEnabled(!loading);
    }

    private String getText(TextInputEditText editText) {
        // Safe helper to avoid NPEs and trim whitespace from user input.
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private void updateAuthModeUi() {
        TextView title = requireView().findViewById(R.id.setup_title);
        TextView subtitle = requireView().findViewById(R.id.setup_subtitle);

        if (signInMode) {
            title.setText("Welcome back");
            subtitle.setText("Sign in with your existing account.");
            createAccountButton.setText("Sign in");
            toggleAuthModeButton.setText("Need an account? Sign up");

            usernameLayout.setVisibility(View.GONE);
            phoneLayout.setVisibility(View.GONE);
            if (roleLabel != null) roleLabel.setVisibility(View.GONE);
            roleGroup.setVisibility(View.GONE);
        } else {
            title.setText("Let’s get you set up.");
            subtitle.setText("Create an account and pick a role to continue.");
            createAccountButton.setText("Create account");
            toggleAuthModeButton.setText("Already have an account? Sign in");

            usernameLayout.setVisibility(View.VISIBLE);
            phoneLayout.setVisibility(View.VISIBLE);
            if (roleLabel != null) roleLabel.setVisibility(View.VISIBLE);
            roleGroup.setVisibility(View.VISIBLE);
        }

        clearErrors();
    }
}