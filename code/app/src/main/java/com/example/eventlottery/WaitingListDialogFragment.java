/**
 * Waiting List Dialog Fragment
 * Displays the users in a the waiting list of an event
 * Last Modified: 2026-02-28 by Grace MacKenzie
 *
 * @author author1
 * @author author2
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;

/**
 * A simple {@link Fragment} subclass.
 */
public class WaitingListDialogFragment extends DialogFragment {

    public interface WaitingListDialogListener {
        void editEmoticon(String newEmoticon);
    }

    private WaitingListDialogListener listener;

    // FUNCTIONS

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        Fragment parent = getParentFragment();
        if (parent instanceof WaitingListDialogListener) {
            listener = (WaitingListDialogListener) parent;
        } else {
            throw new RuntimeException(parent + " must implement WaitingListDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // get the emoticon data if we passed any to it
        Bundle args = getArguments();

        View view = getLayoutInflater().inflate(R.layout.fragment_waiting_list_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        return builder
                .setView(view)
                .setTitle("Add/edit a city")
                .setNegativeButton("Cancel", null)
                .create();
    }

    /**
     * Creates a new instance of EditEmoticonFragment and passes an emoticon to it.
     * @param arg1 the selected emoticon before editing
     * @return a new WaitingListDialogFragment with the selected emoticon data passed to it
     */
    static WaitingListDialogFragment newInstance(String arg1) {
        Bundle args = new Bundle();
        args.putString("emoticon", arg1);

        WaitingListDialogFragment fragment = new WaitingListDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }
}