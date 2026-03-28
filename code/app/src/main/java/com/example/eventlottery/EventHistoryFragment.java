/**
 * Event History Fragment
 * Displays a users event history and lottery outcome.
 * Last Modified: 2026-03-23 by Rebecca OluwaBiyi
 *
 * @author Rebecca OluwaBiyi
 * @author Grace MacKenzie
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Event History Fragment
 * Displays a user's event history and lottery outcomes.
 */
public class EventHistoryFragment extends Fragment {

    private ListView listView;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyItems = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    public EventHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list_with_bottom_bar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = view.findViewById(R.id.list_view);
        FrameLayout bottomBar = view.findViewById(R.id.bottom_bar);

        // Hide the status bar and filters which are only for home page
        View statusText = view.findViewById(R.id.logged_in_status);
        if (statusText != null) {
            statusText.setVisibility(View.GONE);
        }
        View filterContainer = view.findViewById(R.id.filter_container);
        if (filterContainer != null) {
            filterContainer.setVisibility(View.GONE);
        }

        // Adjust bottom bar height for this fragment
        ViewGroup.LayoutParams params = bottomBar.getLayoutParams();
        params.height = (int) (100 * getResources().getDisplayMetrics().density);
        bottomBar.setLayoutParams(params);

        // Load bottom bar
        View bottomBarView = getLayoutInflater().inflate(R.layout.bottom_bar_event_history, bottomBar, false);
        bottomBar.addView(bottomBarView);

        MaterialButton doneButton = bottomBarView.findViewById(R.id.btn_done);
        doneButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        adapter = new HistoryAdapter(requireContext(), historyItems);
        listView.setAdapter(adapter);

        if (currentUserId != null) {
            loadEventHistory();
        }
    }

    private void loadEventHistory() {
        // Query for events where the user is in any of the 4 relevant lists
        Task<QuerySnapshot> q1 = db.collection("events").whereArrayContains("waitingList", currentUserId).get();
        Task<QuerySnapshot> q2 = db.collection("events").whereArrayContains("invitedList", currentUserId).get();
        Task<QuerySnapshot> q3 = db.collection("events").whereArrayContains("acceptedList", currentUserId).get();
        Task<QuerySnapshot> q4 = db.collection("events").whereArrayContains("cancelledList", currentUserId).get();

        Tasks.whenAllSuccess(q1, q2, q3, q4).addOnSuccessListener(results -> {
            historyItems.clear();
            Set<String> processedEventIds = new HashSet<>();

            for (Object result : results) {
                QuerySnapshot snapshot = (QuerySnapshot) result;
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    if (processedEventIds.contains(doc.getId())) continue;
                    processedEventIds.add(doc.getId());

                    String title = doc.getString("title");

                    List<String> waitingList = (List<String>) doc.get("waitingList");
                    List<String> invitedList = (List<String>) doc.get("invitedList");
                    List<String> acceptedList = (List<String>) doc.get("acceptedList");
                    List<String> cancelledList = (List<String>) doc.get("cancelledList");

                    String status = EventHistoryStatusUtils.determineStatus(currentUserId, waitingList, invitedList, acceptedList, cancelledList);
                    historyItems.add(new HistoryItem(doc.getId(), title, status));
                }
            }
            adapter.notifyDataSetChanged();
            if (historyItems.isEmpty()) {
                Toast.makeText(getContext(), "No event history found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load history.", Toast.LENGTH_SHORT).show();
        });
    }

    private static class HistoryItem {
        String eventId;
        String title;
        String status;

        HistoryItem(String eventId, String title, String status) {
            this.eventId = eventId;
            this.title = title;
            this.status = status;
        }
    }

    private static class HistoryAdapter extends ArrayAdapter<HistoryItem> {
        HistoryAdapter(android.content.Context context, List<HistoryItem> items) {
            super(context, R.layout.item_event_history, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_event_history, parent, false);
            }

            HistoryItem item = getItem(position);
            if (item != null) {
                TextView titleView = convertView.findViewById(R.id.history_event_title);
                TextView statusView = convertView.findViewById(R.id.history_event_status);
                titleView.setText(item.title);
                statusView.setText(item.status);
            }
            return convertView;
        }
    }
}
