/**
 * Notifications Fragment
 * Displays a list of notifications
 * Last Modified: 2026-03-02 by Radwa Sheikhdon
 *
 * @author author1
 * @author author2
 * @since 2026-02-28
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationRepository repository = new NotificationRepository();
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationsAdapter(new ArrayList<>(), repository);
        recyclerView.setAdapter(adapter);

        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            // If the user is not logged in, use an anonymous user ID
            currentUserId = "anonymous";
        }

        loadNotifications();
    }

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
