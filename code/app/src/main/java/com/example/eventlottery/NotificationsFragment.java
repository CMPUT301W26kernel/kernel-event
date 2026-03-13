/**
 * Notifications Fragment
 * Displays a list of notifications for the current user
 * Last Modified: 2026-03-12 by Radwa Sheikhdon
 * Handles real-time updates and links to NotificationRepository for accept/decline actions.
 * @author Radwa Sheikhdon
 * @version 1.0
 * @since 2023-03-02
 */

package com.example.eventlottery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Notifications Fragment
 * Displays a list of notifications for the current user
 * Last Modified: 2026-03-12 by Radwa Sheikhdon
 * Handles real-time updates and links to NotificationRepository for accept/decline actions.
 * @author Radwa Sheikhdon
 * @version 1.0
 * @since 2023-03-02
 */
public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationRepository repository = new NotificationRepository();
    private String currentUserId;


    /**
     * Called to have the fragment instantiate its user interface view.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }


    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationsAdapter(new ArrayList<>(), repository);
        recyclerView.setAdapter(adapter);

        // Get current user ID safely
        var user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : "anonymous";

        loadNotifications();
    }

    /**
     * Loads notifications for the current user from Firestore.
     * Updates the adapter in real-time when data changes.
     * @return
     * @throws Exception
     */
    private void loadNotifications() {
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;

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

                    if (adapter != null) {
                        adapter.updateList(list);
                    }
                });
    }
}
