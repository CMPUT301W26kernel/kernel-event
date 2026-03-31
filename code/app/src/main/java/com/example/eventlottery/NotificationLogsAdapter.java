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

public class NotificationLogsAdapter extends RecyclerView.Adapter<NotificationLogsAdapter.ViewHolder> {

    private final List<NotificationLog> logs;

    public NotificationLogsAdapter(List<NotificationLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_log, parent, false);
        return new ViewHolder(view);
    }

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

    @Override
    public int getItemCount() {
        return logs == null ? 0 : logs.size();
    }

    private String safe(String value) {
        return value == null ? "Unknown" : value;
    }

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
