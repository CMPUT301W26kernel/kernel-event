package com.example.eventlottery;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.Date;

/**
 * NotificationLogsAdapterJVMTest
 * Last Modified: 2026-04-03 by Radwa Sheikhdon
 *
 * Unit tests for NotificationLogsAdapter.
 * Verifies correct binding of log data to UI elements.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class NotificationLogsAdapterJVMTest {

    private Context context;
    private NotificationLogsAdapter adapter;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
    }

    /**
     * Builds a ViewHolder with all required views for adapter binding tests.
     * @return a ViewHolder with initialized child views
     */
    private NotificationLogsAdapter.ViewHolder buildHolder() {
        LinearLayout root = new LinearLayout(context);

        TextView message = new TextView(context);
        message.setId(R.id.txt_log_message);

        TextView type = new TextView(context);
        type.setId(R.id.txt_log_type);

        TextView sender = new TextView(context);
        sender.setId(R.id.txt_log_sender);

        TextView event = new TextView(context);
        event.setId(R.id.txt_log_event);

        TextView recipients = new TextView(context);
        recipients.setId(R.id.txt_log_recipients);

        TextView time = new TextView(context);
        time.setId(R.id.txt_log_time);

        root.addView(message);
        root.addView(type);
        root.addView(sender);
        root.addView(event);
        root.addView(recipients);
        root.addView(time);

        return new NotificationLogsAdapter.ViewHolder(root);
    }

    /**
     * Tests that all log fields are correctly bound when valid data is present.
     */
    @Test
    public void testBindLogFields_withValidData() {
        NotificationLog log = new NotificationLog();
        log.setMessage("Bulk message sent");
        log.setType("INFO");
        log.setSenderId("organizer1");
        log.setEventId("event123");
        log.setRecipientCount(5);
        log.setTimestamp(new Timestamp(new Date()));

        adapter = new NotificationLogsAdapter(Collections.singletonList(log));
        NotificationLogsAdapter.ViewHolder holder = buildHolder();

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Bulk message sent", holder.message.getText().toString());
        assertEquals("INFO", holder.type.getText().toString());
        assertEquals("Sender: organizer1", holder.sender.getText().toString());
        assertEquals("Event ID: event123", holder.event.getText().toString());
        assertEquals("Recipients: 5", holder.recipients.getText().toString());
        assertTrue(holder.time.getText().toString().length() > 0);
    }

    /**
     * Tests that null type is displayed as UNKNOWN.
     */
    @Test
    public void testNullTypeDisplaysUnknown() {
        NotificationLog log = new NotificationLog();
        log.setMessage("Test");
        log.setType(null);
        log.setSenderId("sender1");
        log.setEventId("event1");
        log.setRecipientCount(1);

        adapter = new NotificationLogsAdapter(Collections.singletonList(log));
        NotificationLogsAdapter.ViewHolder holder = buildHolder();

        adapter.onBindViewHolder(holder, 0);

        assertEquals("UNKNOWN", holder.type.getText().toString());
    }

    /**
     * Tests that null sender and event IDs are displayed as Unknown.
     */
    @Test
    public void testNullSenderAndEventDisplayUnknown() {
        NotificationLog log = new NotificationLog();
        log.setMessage("Test");
        log.setType("INFO");
        log.setSenderId(null);
        log.setEventId(null);
        log.setRecipientCount(2);

        adapter = new NotificationLogsAdapter(Collections.singletonList(log));
        NotificationLogsAdapter.ViewHolder holder = buildHolder();

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Sender: Unknown", holder.sender.getText().toString());
        assertEquals("Event ID: Unknown", holder.event.getText().toString());
    }

    /**
     * Tests that null timestamp results in an empty time field.
     */
    @Test
    public void testNullTimestampLeavesTimeBlank() {
        NotificationLog log = new NotificationLog();
        log.setMessage("No timestamp");
        log.setType("INFO");
        log.setSenderId("sender1");
        log.setEventId("event1");
        log.setRecipientCount(1);
        log.setTimestamp(null);

        adapter = new NotificationLogsAdapter(Collections.singletonList(log));
        NotificationLogsAdapter.ViewHolder holder = buildHolder();

        adapter.onBindViewHolder(holder, 0);

        assertEquals("", holder.time.getText().toString());
    }

    /**
     * Tests that getItemCount returns the correct number of logs.
     */
    @Test
    public void testGetItemCount() {
        NotificationLog log = new NotificationLog();
        log.setMessage("Test log");

        adapter = new NotificationLogsAdapter(Collections.singletonList(log));

        assertEquals(1, adapter.getItemCount());
    }

    /**
     * Tests that getItemCount returns 0 when the log list is null.
     */
    @Test
    public void testGetItemCount_withNullList() {
        adapter = new NotificationLogsAdapter(null);

        assertEquals(0, adapter.getItemCount());
    }
}
