package com.example.eventlottery.profiles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.NotificationLogsFragment;
import com.example.eventlottery.R;

/**
 * Admin Notification Logs Fragment
 * A wrapper fragment that displays the NotificationLogsFragment with an additional
 * back button for admin navigation purposes.
 * Last Modified: 2026-04-05 by Rebecca
 *
 * Notes:
 *      - Used by admins when viewing an organizer's profile to review sent notifications.
 *
 * @author Rebecca
 * @since 2026-04-05
 */
public class AdminNotificationLogsFragment extends Fragment {

    /**
     * Required empty public constructor for Fragment instantiation.
     */
    public AdminNotificationLogsFragment() {
        // Required empty public constructor
    }

    /**
     * Inflates the layout for this fragment.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_notification_logs, container, false);
    }

    /**
     * Initializes UI components, sets up the back button, and embeds the NotificationLogsFragment.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup back button
        ImageButton backButton = view.findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        // Add the base NotificationLogsFragment inside the container
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.notification_logs_container, new NotificationLogsFragment())
                    .commit();
        }
    }
}
