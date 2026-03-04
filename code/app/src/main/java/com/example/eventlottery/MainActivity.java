/**
 * Event Lottery Main Activity
 * Provides the main entry point for the app
 * Last Modified: 2026-02-28 by Grace MacKenzie
 *
 * @author author1
 * @author author2
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // This block makes sure the fragment properly fits onto the device. do not remove this. thanks.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // On first launch only: decides where the user should start.
        //Not to be done when restoring state because FragmentManager will restore the last screen.
        if (savedInstanceState == null) {
            // This is the "routing" step: based on auth/profile state, chooses which screen to show.
            StartDestination destination = StartDestinationResolver.resolve();

            if (destination == StartDestination.SETUP) {
                // No authenticated user yet -> show account + profile setup flow.
                showSetUpFragment();
            } else {
                // Authenticated user exists -> send them to the main home page.
                showHomePageFragment();
            }
        }
    }

    private void showSetUpFragment() {
        // Replacing the single fragment container with the setup flow fragment.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new SetUpFragment())
                .commit();
    }

    private void showHomePageFragment() {
        // Replacing the single fragment container with the app "home" fragment.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomePageFragment())
                .commit();
    }

    /**
     * Represents where the app should route a user on launch.
     */
    public enum StartDestination {
        SETUP,
        HOME
    }

    /**
     * Simple static helper for deciding the first screen based on authentication and profile state.
     * Separated here to make it easier to test and extend later.
     */
    public static class StartDestinationResolver {

        /**
         * Resolves the starting destination.
         *
         * Current behaviour:
         *  - If there is no authenticated Firebase user, the user must create an account and profile.
         *  - If a user is authenticated, we optimistically route them to the home page.
         *
         * Future improvement
         *  - Check the Firestore Users collection for a profile document and route based on its presence.
         */
        public static StartDestination resolve() {
            // FirebaseAuth is the source of truth for "is the user signed in?"
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            com.google.firebase.auth.FirebaseUser currentUser = auth.getCurrentUser();

            // If no user is signed in, we must show setup (create account + profile).
            if (currentUser == null) {
                return StartDestination.SETUP;
            }

            // If a user exists, we can proceed to the app.
            return StartDestination.HOME;
        }
    }
}