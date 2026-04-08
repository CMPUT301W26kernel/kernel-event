package com.example.eventlottery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.profiles.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A DialogFragment representing the view of an event's waiting list.
 * Displays entrants in a simple popup and allows the organizer to trigger the lottery draw.
 */
public class WaitlistManagementFragment extends DialogFragment {

    private static final String TAG = "WaitlistManagement";
    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_CAN_MANAGE_WAITLIST = "canManageWaitlist";

    private String eventId;
    private boolean canManageWaitlist;
    private EntrantAdapter adapter;

    private FirebaseFirestore db;
    private LotterySystem lotterySystem;
    private List<User> currentDisplayList = new ArrayList<>();
    private RecyclerView recyclerView;
    private Button drawLotteryButton;
    private TextView emptyStateView;

    public static WaitlistManagementFragment newInstance(String eventId) {
        return newInstance(eventId, false);
    }

    public static WaitlistManagementFragment newInstance(String eventId, boolean canManageWaitlist) {
        WaitlistManagementFragment fragment = new WaitlistManagementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putBoolean(ARG_CAN_MANAGE_WAITLIST, canManageWaitlist);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            canManageWaitlist = getArguments().getBoolean(ARG_CAN_MANAGE_WAITLIST, false);
        }
        db = FirebaseFirestore.getInstance();
        lotterySystem = new LotterySystem();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_waitlist_management, container, false);

        recyclerView = view.findViewById(R.id.rv_entrants);
        ImageButton btnClose = view.findViewById(R.id.btn_close);
        drawLotteryButton = view.findViewById(R.id.btn_draw_lottery);
        emptyStateView = view.findViewById(R.id.tv_empty_state);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EntrantAdapter();
        recyclerView.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dismiss());
        drawLotteryButton.setOnClickListener(v -> promptDrawLottery());

        if (!canManageWaitlist) {
            showInfoState(R.string.waitlist_view_restricted, false);
            return view;
        }

        showInfoState(R.string.waitlist_management_loading, false);
        fetchEventData();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Listens to the specific event document in Firestore for real-time updates to the waiting list.
     */
    private void fetchEventData() {
        if (eventId == null) return;

        db.collection("events").document(eventId).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.w(TAG, "Listen failed.", error);
                showInfoState(R.string.waitlist_management_load_failed, false);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                List<String> waitingListIds = (List<String>) snapshot.get("waitingList");
                if (waitingListIds == null) {
                    waitingListIds = new ArrayList<>();
                }
                fetchUsersByIds(waitingListIds);
            } else {
                showInfoState(R.string.waitlist_management_load_failed, false);
            }
        });
    }

    /**
     * Fetches User objects from Firestore given the list of IDs from the waitlist.
     */
    private void fetchUsersByIds(List<String> ids) {
        currentDisplayList.clear();
        adapter.notifyDataSetChanged();

        if (ids.isEmpty()) {
            showInfoState(R.string.waitlist_management_empty, false);
            return;
        }

        showInfoState(R.string.waitlist_management_loading, false);

        Map<String, Integer> idOrder = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            idOrder.put(ids.get(i), i);
        }

        List<User> loadedUsers = new ArrayList<>();
        AtomicInteger remaining = new AtomicInteger(ids.size());

        for (String id : ids) {
            db.collection("users").document(id).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                user.setUserId(id);
                                loadedUsers.add(user);
                            }
                        }
                    })
                    .addOnFailureListener(error -> Log.w(TAG, "Failed to load user profile for waitlist entry " + id, error))
                    .addOnCompleteListener(task -> {
                        if (remaining.decrementAndGet() == 0) {
                            loadedUsers.sort((left, right) -> {
                                Integer leftIndex = idOrder.get(left.getUserId());
                                Integer rightIndex = idOrder.get(right.getUserId());
                                return Integer.compare(leftIndex != null ? leftIndex : Integer.MAX_VALUE,
                                        rightIndex != null ? rightIndex : Integer.MAX_VALUE);
                            });

                            currentDisplayList.clear();
                            currentDisplayList.addAll(loadedUsers);
                            adapter.notifyDataSetChanged();

                            if (currentDisplayList.isEmpty()) {
                                showInfoState(R.string.waitlist_management_missing_profiles, false);
                            } else {
                                showInfoState(null, true);
                            }
                        }
                    });
        }
    }

    private void showInfoState(@Nullable Integer messageResId, boolean showRecycler) {
        if (recyclerView == null || drawLotteryButton == null || emptyStateView == null) {
            return;
        }

        recyclerView.setVisibility(showRecycler ? View.VISIBLE : View.GONE);
        emptyStateView.setVisibility(showRecycler ? View.GONE : View.VISIBLE);
        if (!showRecycler && messageResId != null) {
            emptyStateView.setText(messageResId);
        }
        drawLotteryButton.setEnabled(showRecycler && !currentDisplayList.isEmpty());
    }

    /**
     * Pops up a dialog for the Organizer to type how many people they want to select.
     */
    private void promptDrawLottery() {
        if (!canManageWaitlist) {
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.waitlist_view_restricted, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.run_lottery_btn);
        builder.setMessage(R.string.draw_lottery_prompt);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(R.string.draw_btn, (dialog, which) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                int count = Integer.parseInt(val);
                drawEntrants(count);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void drawEntrants(int count) {
        if (count <= 0) return;
        
        lotterySystem.drawEntrants(eventId, count).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> winners = task.getResult();
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.success_draw_toast, winners.size()), Toast.LENGTH_SHORT).show();
                }
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.fail_draw_toast, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = currentDisplayList.get(position);
            String displayName = user.getUsername() != null && !user.getUsername().isEmpty() ? user.getUsername() : "User";
            holder.tvName.setText(displayName);
        }

        @Override
        public int getItemCount() {
            return currentDisplayList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = (TextView) itemView;
            }
        }
    }
}
