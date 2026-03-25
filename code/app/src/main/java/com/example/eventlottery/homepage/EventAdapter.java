package com.example.eventlottery.homepage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventlottery.event.Event;
import com.example.eventlottery.R;

import java.util.ArrayList;

/**
 * Adapter used to display Event objects in the home page ListView.
 */
public class EventAdapter extends ArrayAdapter<Event> {

    public EventAdapter(Context context, ArrayList<Event> events) {
        super(context, 0, events);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Event event = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_event, parent, false);
        }

        TextView titleText = convertView.findViewById(R.id.text_event_title);
        TextView organizerText = convertView.findViewById(R.id.text_event_organizer);
        TextView dateText = convertView.findViewById(R.id.text_event_date);

        if (event != null) {
            titleText.setText(event.getTitle());
            organizerText.setText(String.format("Organizer:\t%s", event.getOrganizerId()));
            dateText.setText(String.format("Start Date:\t%s/%s/%s",
                    event.getRegistrationOpen().getYear(),
                    event.getRegistrationOpen().getMonthValue(),
                    event.getRegistrationOpen().getDayOfMonth()));
        }

        return convertView;
    }
}

