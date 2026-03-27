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
public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";

    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationRepository repository = new NotificationRepository();
    private String currentUserId;
    private ListenerRegistration notificationsListener;

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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "No signed-in user", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No signed-in user");
            return;
        }

        currentUserId = currentUser.getUid();
        Log.d(TAG, "Current user UID: " + currentUserId);
        Toast.makeText(getContext(), "UID: " + currentUserId, Toast.LENGTH_LONG).show();

        loadNotifications();
    }

    private void loadNotifications() {
        if (currentUserId == null) {
            Log.e(TAG, "loadNotifications called with null currentUserId");
            return;
        }

        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }

        notificationsListener = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (error != null) {
                        Log.e(TAG, "Failed to load notifications", error);
                        Toast.makeText(
                                getContext(),
                                "Failed to load notifications: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    List<Notification> list = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notification n = doc.toObject(Notification.class);
                            if (n != null) {
                                n.setNotificationId(doc.getId());
                                list.add(n);
                            }
                        }
                    }

                    Log.d(TAG, "Loaded notifications count: " + list.size());
                    Toast.makeText(getContext(), "Loaded " + list.size() + " notifications", Toast.LENGTH_SHORT).show();

                    adapter.updateList(list);
                });
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
