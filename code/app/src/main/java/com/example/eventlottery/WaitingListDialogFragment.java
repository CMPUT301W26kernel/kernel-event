/**
 * Waiting List Dialog Fragment
 * Displays the users in a the waiting list of an event
 * Last Modified: 2026-03-04 by Pierce Hampton
 *
 * @author Pierce Hampton
 * @author Grace Mackenzie
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

/**
 * A DialogFragment that lets a user join or leave the waiting list of an event.
 */
public class WaitingListDialogFragment extends DialogFragment {

    public interface WaitingListDialogListener {
        void onJoinWaitingList(String eventId);
        void onLeaveWaitingList(String eventId);
        void onViewWaitingList(String eventId);
    }

    private WaitingListDialogListener listener;

    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_EVENT_NAME = "eventName";
    private static final String ARG_ENTRANT_COUNT = "entrantCount";
    private static final String ARG_IN_WAITING_LIST = "inWaitingList";
    private static final String ARG_REQUIRE_GEO = "requireGeo";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        
        // Try to attach listener to parent fragment or the activity
        Fragment parent = getParentFragment();
        if (parent instanceof WaitingListDialogListener) {
            listener = (WaitingListDialogListener) parent;
        } else if (context instanceof WaitingListDialogListener) {
            listener = (WaitingListDialogListener) context;
        } else {
            throw new RuntimeException(context + " or parent fragment must implement WaitingListDialogListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_waiting_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String eventId = "";
        String eventName = "Event";
        int entrantCount = 0;
        boolean inWaitingList = false;
        boolean requireGeo = false;

        if (args != null) {
            eventId = args.getString(ARG_EVENT_ID, "");
            eventName = args.getString(ARG_EVENT_NAME, "this event");
            entrantCount = args.getInt(ARG_ENTRANT_COUNT, 0);
            inWaitingList = args.getBoolean(ARG_IN_WAITING_LIST, false);
            requireGeo = args.getBoolean(ARG_REQUIRE_GEO, false);
        }

        final String finalEventId = eventId;
        final boolean finalInWaitingList = inWaitingList;

        TextView titleText = view.findViewById(R.id.dialog_title);
        TextView descText = view.findViewById(R.id.dialog_description);
        ImageButton backButton = view.findViewById(R.id.btn_back);
        Button actionButton = view.findViewById(R.id.btn_action);
        Button viewListButton = view.findViewById(R.id.btn_view_list);

        backButton.setOnClickListener(v -> dismiss());
        
        if (finalInWaitingList) {
            titleText.setText(R.string.waiting_list_dialog_title_leave);
            descText.setText(getString(R.string.waiting_list_dialog_desc_leave, eventName));
            actionButton.setText(R.string.waiting_list_dialog_action_leave);
        } else {
            titleText.setText(R.string.waiting_list_dialog_title_join);
            String base = getString(R.string.waiting_list_dialog_desc_join, entrantCount, eventName);
            if (requireGeo) {
                base = base + "\n\n" + getString(R.string.waiting_list_geo_notice);
            }
            descText.setText(base);
            actionButton.setText(R.string.waiting_list_dialog_action_join);
        }
        
        actionButton.setOnClickListener(v -> {
            if (listener != null) {
                if (finalInWaitingList) {
                    listener.onLeaveWaitingList(finalEventId);
                } else {
                    listener.onJoinWaitingList(finalEventId);
                }
            }
            dismiss();
        });

        viewListButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewWaitingList(finalEventId);
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    /**
     * Creates a new instance of WaitingListDialogFragment.
     * @param eventId The ID of the event
     * @param eventName The name of the event for display purposes
     * @param entrantCount Number of people already on the waiting list
     * @param inWaitingList True if the user is already on the waiting list, false otherwise
     * @return A new instance of WaitingListDialogFragment
     */
    public static WaitingListDialogFragment newInstance(String eventId, String eventName, int entrantCount, boolean inWaitingList) {
        return newInstance(eventId, eventName, entrantCount, inWaitingList, false);
    }

    /**
     * @param requireGeolocationForWaitlist when true, joining may use device location for verification
     */
    public static WaitingListDialogFragment newInstance(
            String eventId,
            String eventName,
            int entrantCount,
            boolean inWaitingList,
            boolean requireGeolocationForWaitlist
    ) {
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_NAME, eventName);
        args.putInt(ARG_ENTRANT_COUNT, entrantCount);
        args.putBoolean(ARG_IN_WAITING_LIST, inWaitingList);
        args.putBoolean(ARG_REQUIRE_GEO, requireGeolocationForWaitlist);

        WaitingListDialogFragment fragment = new WaitingListDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
