package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class NotificationsAdapterJVMTest {

    private NotificationRepository mockRepo;
    private NotificationsAdapter adapter;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        mockRepo = mock(NotificationRepository.class);
    }

    @Test
    public void testBindMessageAndTimestamp() {
        Notification notification = new Notification();
        notification.setMessage("You won!");
        notification.setType("INFO");
        notification.setStatus("UNREAD");

        adapter = new NotificationsAdapter(Collections.singletonList(notification), mockRepo);


        View itemView = new View(context);
        NotificationsAdapter.ViewHolder holder = new NotificationsAdapter.ViewHolder(itemView);

        holder.message = new TextView(context);
        holder.time = new TextView(context);
        holder.statusBadge = new TextView(context);
        holder.btnAccept = new Button(context);
        holder.btnDecline = new Button(context);
        holder.layoutInviteActions = new LinearLayout(context);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("You won!", holder.message.getText().toString());
    }

    @Test
    public void testBindInviteClickCallsRepository() {
        Notification notification = new Notification();
        notification.setType(Notification.TYPE_INVITE);
        notification.setStatus(Notification.STATUS_UNREAD);

        adapter = new NotificationsAdapter(Collections.singletonList(notification), mockRepo);

        View itemView = new View(context);
        NotificationsAdapter.ViewHolder holder = new NotificationsAdapter.ViewHolder(itemView);

        holder.message = new TextView(context);
        holder.time = new TextView(context);
        holder.statusBadge = new TextView(context);
        holder.btnAccept = new Button(context);
        holder.btnDecline = new Button(context);
        holder.layoutInviteActions = new LinearLayout(context);

        adapter.onBindViewHolder(holder, 0);

        // Robolectric handles the click and listener execution
        holder.btnAccept.performClick();
        holder.btnDecline.performClick();

        // Verify repository interactions
        verify(mockRepo).acceptInvitation(notification);
        verify(mockRepo).declineInvitation(notification);

        // Verify UI state changes
        assertEquals(false, holder.btnAccept.isEnabled());
        assertEquals(false, holder.btnDecline.isEnabled());
    }
}

