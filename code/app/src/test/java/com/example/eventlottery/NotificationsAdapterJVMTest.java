package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * NotificationsAdapterJVMTest
 * Last Modified: 2026-04-03 by Radwa Sheikhdon
 *
 * Unit tests for NotificationsAdapter.
 * Verifies correct binding of UI elements and behavior based on notification type and status.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class NotificationsAdapterJVMTest {

    private NotificationRepository mockRepo;
    private NotificationsAdapter adapter;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        mockRepo = mock(NotificationRepository.class);
    }

    /**
     * Builds a mock ViewHolder with required views for testing binding logic.
     * @return a ViewHolder with initialized child views
     */
    private NotificationsAdapter.ViewHolder buildHolder() {
        LinearLayout root = new LinearLayout(context);

        TextView message = new TextView(context);
        message.setId(R.id.notification_message);

        TextView time = new TextView(context);
        time.setId(R.id.notification_time);

        TextView statusBadge = new TextView(context);
        statusBadge.setId(R.id.txt_status_badge);

        TextView typeBadge = new TextView(context);
        typeBadge.setId(R.id.txt_type_badge);

        Button btnAccept = new Button(context);
        btnAccept.setId(R.id.btn_accept);

        Button btnDecline = new Button(context);
        btnDecline.setId(R.id.btn_decline);

        LinearLayout inviteActions = new LinearLayout(context);
        inviteActions.setId(R.id.layout_invite_actions);

        root.addView(message);
        root.addView(time);
        root.addView(statusBadge);
        root.addView(typeBadge);
        root.addView(btnAccept);
        root.addView(btnDecline);
        root.addView(inviteActions);

        return spy(new NotificationsAdapter.ViewHolder(root));
    }

    /**
     * Tests that INFO notifications display message, type badge, and status badge correctly.
     */
    @Test
    public void testBindMessageAndTypeBadge_forInfoNotification() {
        Notification notification = new Notification();
        notification.setMessage("You won!");
        notification.setType(NotificationType.INFO);
        notification.setStatus(NotificationStatus.UNREAD);

        adapter = new NotificationsAdapter(
                Collections.singletonList(notification),
                mockRepo
        );

        NotificationsAdapter.ViewHolder holder = buildHolder();
        adapter.onBindViewHolder(holder, 0);

        assertEquals("You won!", holder.message.getText().toString());
        assertEquals("INFO", holder.typeBadge.getText().toString());
        assertEquals("UNREAD", holder.statusBadge.getText().toString());
        assertEquals(View.GONE, holder.layoutInviteActions.getVisibility());
        assertEquals(View.VISIBLE, holder.statusBadge.getVisibility());
    }

    /**
     * Tests that unread invite notifications show accept/decline buttons
     * and hide the status badge.
     */
    @Test
    public void testUnreadInviteShowsActionButtons() {
        Notification notification = new Notification();
        notification.setNotificationId("n1");
        notification.setEventId("e1");
        notification.setUserId("u1");
        notification.setMessage("You have been invited");
        notification.setType(NotificationType.INVITE);
        notification.setStatus(NotificationStatus.UNREAD);

        adapter = new NotificationsAdapter(
                Collections.singletonList(notification),
                mockRepo
        );

        NotificationsAdapter.ViewHolder holder = buildHolder();
        adapter.onBindViewHolder(holder, 0);

        assertEquals("INVITE", holder.typeBadge.getText().toString());
        assertEquals(View.VISIBLE, holder.layoutInviteActions.getVisibility());
        assertEquals(View.GONE, holder.statusBadge.getVisibility());
    }

    /**
     * Tests that read invite notifications still show action buttons
     * and don't display a READ status badge.
     */
    @Test
    public void testReadInviteHidesReadBadgeAndShowsActions() {
        Notification notification = new Notification();
        notification.setNotificationId("n2");
        notification.setEventId("e2");
        notification.setUserId("u2");
        notification.setMessage("Invitation reminder");
        notification.setType(NotificationType.INVITE);
        notification.setStatus(NotificationStatus.READ);

        adapter = new NotificationsAdapter(
                Collections.singletonList(notification),
                mockRepo
        );

        NotificationsAdapter.ViewHolder holder = buildHolder();
        adapter.onBindViewHolder(holder, 0);

        // Invite actions should be visible
        assertEquals(View.VISIBLE, holder.layoutInviteActions.getVisibility());

        // READ badge should be hidden
        assertEquals(View.GONE, holder.statusBadge.getVisibility());
    }

    /**
     * Tests that co-organizer invites display the correct badge label.
     */
    @Test
    public void testCoOrganizerInviteShowsCorrectBadge() {
        Notification notification = new Notification();
        notification.setNotificationId("n3");
        notification.setEventId("e3");
        notification.setUserId("u3");
        notification.setMessage("Please join as co-organizer");
        notification.setType(NotificationType.COORGANIZER_INVITE);
        notification.setStatus(NotificationStatus.UNREAD);

        adapter = new NotificationsAdapter(
                Collections.singletonList(notification),
                mockRepo
        );

        NotificationsAdapter.ViewHolder holder = buildHolder();
        adapter.onBindViewHolder(holder, 0);

        assertEquals("CO-ORGANIZER", holder.typeBadge.getText().toString());
        assertEquals(View.VISIBLE, holder.layoutInviteActions.getVisibility());
    }

    /**
     * Tests that private event invitations display the "PRIVATE EVENT" badge.
     */
    @Test
    public void testPrivateInviteShowsPrivateEventBadge() {
        Notification notification = new Notification();
        notification.setNotificationId("n4");
        notification.setEventId("e4");
        notification.setUserId("u4");
        notification.setMessage("You are invited to a private event");
        notification.setType(NotificationType.INVITE);
        notification.setStatus(NotificationStatus.UNREAD);

        adapter = new NotificationsAdapter(
                Collections.singletonList(notification),
                mockRepo
        );

        NotificationsAdapter.ViewHolder holder = buildHolder();
        adapter.onBindViewHolder(holder, 0);

        assertEquals("PRIVATE EVENT", holder.typeBadge.getText().toString());
        assertEquals(View.VISIBLE, holder.layoutInviteActions.getVisibility());
    }

    /**
     * Tests that notifications with null timestamps display an empty time field.
     */
    @Test
    public void testNullTimestampLeavesTimeBlank() {
        Notification notification = new Notification();
        notification.setMessage("No timestamp");
        notification.setType(NotificationType.INFO);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setTimestamp(null);

        adapter = new NotificationsAdapter(
                Collections.singletonList(notification),
                mockRepo
        );

        NotificationsAdapter.ViewHolder holder = buildHolder();
        adapter.onBindViewHolder(holder, 0);

        assertEquals("", holder.time.getText().toString());
    }

    /**
     * Tests that notifications with a valid timestamp display formatted time.
     */
    @Test
    public void testNonNullTimestampDisplaysTime() {
        Notification notification = new Notification();
        notification.setMessage("Has timestamp");
        notification.setType(NotificationType.INFO);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setTimestamp(new Timestamp(new Date()));

        adapter = new NotificationsAdapter(
                Collections.singletonList(notification),
                mockRepo
        );

        NotificationsAdapter.ViewHolder holder = buildHolder();
        adapter.onBindViewHolder(holder, 0);

        assertTrue(holder.time.getText().toString().length() > 0);
    }

    /**
     * Tests that getItemCount returns the correct number of items.
     */
    @Test
    public void testGetItemCount() {
        Notification notification = new Notification();
        notification.setMessage("Test");
        notification.setType(NotificationType.INFO);
        notification.setStatus(NotificationStatus.UNREAD);

        adapter = new NotificationsAdapter(
                Collections.singletonList(notification),
                mockRepo
        );

        assertEquals(1, adapter.getItemCount());
    }

    /**
     * Tests that updateList correctly updates the adapter's dataset size.
     */
    @Test
    public void testUpdateListChangesItemCount() {
        Notification n1 = new Notification();
        n1.setMessage("First");
        n1.setType(NotificationType.INFO);
        n1.setStatus(NotificationStatus.UNREAD);

        adapter = new NotificationsAdapter(
                Collections.singletonList(n1),
                mockRepo
        );

        assertEquals(1, adapter.getItemCount());

        Notification n2 = new Notification();
        n2.setMessage("Second");
        n2.setType(NotificationType.ADMIN);
        n2.setStatus(NotificationStatus.READ);

        List<Notification> newList = new ArrayList<>();
        newList.add(n1);
        newList.add(n2);

        adapter.updateList(newList);

        assertEquals(2, adapter.getItemCount());
    }
}

