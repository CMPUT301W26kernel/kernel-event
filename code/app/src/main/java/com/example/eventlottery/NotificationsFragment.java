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
 * Notifications Fragment
 * Displays a list of notifications for the current user
 * Last Modified: 2026-03-26 by Radwa Sheikhdon
 * Handles real-time updates and links to NotificationRepository for accept/decline actions.
 * @author Radwa Sheikhdon
 * @version 1.3
 * @since 2023-03-02
 */

/**
 * Displays notifications for the currently signed-in user.
 * Supports real-time updates and invitation actions through NotificationRepository.
 */
public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";

    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private ListenerRegistration notificationsListener;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationRepository repository = new NotificationRepository();

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationsAdapter(new ArrayList<>(), repository);
        recyclerView.setAdapter(adapter);

        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "No signed-in user", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        loadNotifications(currentUserId);
    }

    private void loadNotifications(String userId) {
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }

        notificationsListener = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;

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

                    List<Notification> notifications = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notification notification = doc.toObject(Notification.class);
                            if (notification != null) {
                                notification.setNotificationId(doc.getId());
                                notifications.add(notification);
                            }
                        }
                    }

                    adapter.updateList(notifications);
                });
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }

        recyclerView = null;
    }
}
