package com.example.eventlottery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

public class NotificationLogsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private NotificationLogsAdapter adapter;
    private final List<NotificationLog> logList = new ArrayList<>();
    private FirebaseFirestore db;

    public NotificationLogsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_logs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.rv_notification_logs);
        emptyText = view.findViewById(R.id.txt_empty_logs);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationLogsAdapter(logList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadLogs();
    }

    private void loadLogs() {
        db.collection("notification_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText("Failed to load notification logs.");
                        return;
                    }

                    logList.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            NotificationLog log = doc.toObject(NotificationLog.class);
                            if (log != null) {
                                log.setLogId(doc.getId());
                                logList.add(log);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    emptyText.setVisibility(logList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }
}
