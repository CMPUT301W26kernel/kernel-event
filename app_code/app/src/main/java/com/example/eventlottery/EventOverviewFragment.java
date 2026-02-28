/**
 * Event Overview Fragment
 * Displays the Details of an event
 * Last Modified: 2026-02-28 by Grace MacKenzie
 *
 * Notes:
 *      - This fragment can take an event id from firebase as a Bundle argument
 *          and load the event directly from firebase. This prevents the overhead of making
 *          an event parseable or serializable and keeps the fragment light weight.
 *
 * @author author1
 * @author author2
 * @since 2026-02-28
 */
package com.example.eventlottery;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 */
public class EventOverviewFragment extends Fragment {


    public EventOverviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        /*
            TODO: Load the proper content xml, data set, and bottom bar button set
                depending on if the user is an admin, organizer, or standard entrant
         */
    }
}