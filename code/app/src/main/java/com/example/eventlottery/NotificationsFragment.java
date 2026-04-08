package com.example.eventlottery;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationsFragment
 * Last Modified: 2026-04-03 by Radwa Sheikhdon
 *
 * Displays notifications for the currently signed-in user.
 * Uses Firestore real-time listeners to keep the UI updated.
 * Connects to NotificationRepository for handling invitation actions.
 */
public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";

    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;

    // Listener used to observe Firestore changes in real-time
    private ListenerRegistration notificationsListener;

    // Firestore database instance
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Repository used for handling notification actions (accept/decline/read)
    private final NotificationRepository repository = new NotificationRepository();

    /**
     * Required empty public constructor.
     */
    public NotificationsFragment() {}

    /**
     * Inflates the fragment layout.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    /**
     * Initializes RecyclerView and loads notifications for the current user.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.recycler_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter with empty list
        adapter = new NotificationsAdapter(new ArrayList<>(), repository);
        recyclerView.setAdapter(adapter);

        // Get current user ID
        String currentUserId = getCurrentUserId();

        // If no user is logged in, show error and stop
        if (currentUserId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "No signed-in user", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Load notifications for the user
        loadNotifications(currentUserId);
        View backBtn = view.findViewById(R.id.btn_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }
    }

    /**
     * Loads notifications from Firestore and attaches a real-time listener.
     * Updates the adapter whenever data changes.
     *
     * @param userId ID of the current user
     */
    private void loadNotifications(String userId) {

        // Remove any existing listener to prevent duplicate listeners or memory leaks
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }

        // Attach Firestore snapshot listener for real time updates
        notificationsListener = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    // Prevent UI updates if fragment is no longer attached
                    if (!isAdded()) return;

                    // Handle Firestore errors
                    if (error != null) {
                        Log.e(TAG, "Failed to load notifications", error);
                        if (getContext() != null) {
                            Toast.makeText(
                                    getContext(),
                                    "Failed to load notifications",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                        return;
                    }

                    // Convert Firestore documents into Notification objects
                    List<Notification> notifications = new ArrayList<>();

                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            if (OrganizerReport.ENTRY_TYPE_REPORT.equals(doc.getString("entryType"))) {
                                continue;
                            }
                            Notification notification = doc.toObject(Notification.class);

                            if (notification != null) {
                                // Store document ID for future operations (accept/decline/read)
                                notification.setNotificationId(doc.getId());
                                notifications.add(notification);
                            }
                        }
                    }

                    // Update RecyclerView with new data
                    adapter.updateList(notifications);
                });
    }

    /**
     * Retrieves the currently authenticated user's ID.
     *
     * @return user ID if logged in, otherwise null
     */
    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    /**
     * Cleans up resources when the fragment view is destroyed.
     * Removes Firestore listener to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove Firestore listener if active
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }

        // Clear RecyclerView reference to avoid leaks
        recyclerView = null;
    }
}
