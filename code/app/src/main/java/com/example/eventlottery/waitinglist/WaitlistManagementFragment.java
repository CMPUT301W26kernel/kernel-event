package com.example.eventlottery.waitinglist;

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

import com.example.eventlottery.lottery.LotterySystem;
import com.example.eventlottery.R;
import com.example.eventlottery.profile.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * A DialogFragment representing the view of an event's waiting list.
 * Displays entrants in a simple popup and allows the organizer to trigger the lottery draw.
 */
public class WaitlistManagementFragment extends DialogFragment {

    private static final String TAG = "WaitlistManagement";
    private static final String ARG_EVENT_ID = "eventId";

    private String eventId;
    private EntrantAdapter adapter;

    private FirebaseFirestore db;
    private LotterySystem lotterySystem;
    private List<User> currentDisplayList = new ArrayList<>();

    public static WaitlistManagementFragment newInstance(String eventId) {
        WaitlistManagementFragment fragment = new WaitlistManagementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
        db = FirebaseFirestore.getInstance();
        lotterySystem = new LotterySystem();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_waitlist_management, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.rv_entrants);
        ImageButton btnClose = view.findViewById(R.id.btn_close);
        Button btnDrawLottery = view.findViewById(R.id.btn_draw_lottery);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EntrantAdapter();
        recyclerView.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dismiss());
        btnDrawLottery.setOnClickListener(v -> promptDrawLottery());

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
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                List<String> waitingListIds = (List<String>) snapshot.get("waitingList");
                if (waitingListIds == null) {
                    waitingListIds = new ArrayList<>();
                }
                fetchUsersByIds(waitingListIds);
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
            return;
        }

        for (String id : ids) {
            db.collection("users").document(id).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        user.setUserId(id); 
                        currentDisplayList.add(user);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    /**
     * Pops up a dialog for the Organizer to type how many people they want to select.
     */
    private void promptDrawLottery() {
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
