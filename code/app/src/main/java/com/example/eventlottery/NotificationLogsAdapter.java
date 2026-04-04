package com.example.eventlottery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying notification logs.
 * Last Modified: 2026-04-03 by Radwa Sheikhdon
 */
public class NotificationLogsAdapter extends RecyclerView.Adapter<NotificationLogsAdapter.ViewHolder> {

    private final List<NotificationLog> logs;

    /**
     * Creates a new adapter for displaying notification logs.
     *
     * @param logs
     */
    public NotificationLogsAdapter(List<NotificationLog> logs) {
        this.logs = logs;
    }

    /**
     * Replaces the current log list and refreshes the RecyclerView.

     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_log, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Updates the contents of the ViewHolder to reflect the item at the given position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationLog log = logs.get(position);

        holder.message.setText(log.getMessage());
        holder.type.setText(log.getType() != null ? log.getType() : "UNKNOWN");
        holder.sender.setText("Sender: " + safe(log.getSenderId()));
        holder.event.setText("Event ID: " + safe(log.getEventId()));
        holder.recipients.setText("Recipients: " + log.getRecipientCount());

        if (log.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            holder.time.setText(sdf.format(log.getTimestamp().toDate()));
        } else {
            holder.time.setText("");
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return
     */
    @Override
    public int getItemCount() {
        return logs == null ? 0 : logs.size();
    }

    /**
     * Returns a safe string for displaying unknown values.
     *
     * @param value
     * @return
     */
    private String safe(String value) {
        return value == null ? "Unknown" : value;
    }

    /**
     * ViewHolder for displaying notification logs.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView message;
        TextView type;
        TextView sender;
        TextView event;
        TextView recipients;
        TextView time;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.txt_log_message);
            type = itemView.findViewById(R.id.txt_log_type);
            sender = itemView.findViewById(R.id.txt_log_sender);
            event = itemView.findViewById(R.id.txt_log_event);
            recipients = itemView.findViewById(R.id.txt_log_recipients);
            time = itemView.findViewById(R.id.txt_log_time);
        }
    }
}
