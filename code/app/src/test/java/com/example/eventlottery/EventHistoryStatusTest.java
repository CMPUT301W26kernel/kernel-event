package com.example.eventlottery;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for Event History status determination logic.
 * Ensures that entrants see the correct status based on lottery outcomes.
 */
public class EventHistoryStatusTest {

    private final String userId = "user123";

    @Test
    public void testStatusAccepted() {
        List<String> accepted = Arrays.asList("otherUser", userId);
        String status = EventHistoryStatusUtils.determineStatus(userId, null, null, accepted, null);
        assertEquals("Lottery results:\nSelected (Accepted)", status);
    }

    @Test
    public void testStatusDeclined() {
        List<String> cancelled = Arrays.asList(userId);
        String status = EventHistoryStatusUtils.determineStatus(userId, null, null, null, cancelled);
        assertEquals("Lottery results:\nSelected (Declined)", status);
    }

    @Test
    public void testStatusInvited() {
        List<String> invited = Arrays.asList(userId);
        String status = EventHistoryStatusUtils.determineStatus(userId, null, invited, null, null);
        assertEquals("Lottery results:\nSelected (Waiting on your response)", status);
    }

    @Test
    public void testStatusNotSelected() {
        // Draw happened (invited list not empty), but user still in waiting list
        List<String> waiting = Arrays.asList(userId);
        List<String> invited = Arrays.asList("winner1");
        String status = EventHistoryStatusUtils.determineStatus(userId, waiting, invited, null, null);
        assertEquals("Lottery results:\nNot selected", status);
    }

    @Test
    public void testStatusNotSelectedAfterDraw() {
        // Draw happened (someone else accepted), user in waiting list
        List<String> waiting = Arrays.asList(userId);
        List<String> accepted = Arrays.asList("winner1");
        String status = EventHistoryStatusUtils.determineStatus(userId, waiting, null, accepted, null);
        assertEquals("Lottery results:\nNot selected", status);
    }

    @Test
    public void testStatusPending() {
        // No draw happened yet (invited list empty), user in waiting list
        List<String> waiting = Arrays.asList(userId);
        List<String> invited = new ArrayList<>();
        String status = EventHistoryStatusUtils.determineStatus(userId, waiting, invited, null, null);
        assertEquals("Lottery results:\nPending...", status);
    }

    @Test
    public void testStatusUnknown() {
        String status = EventHistoryStatusUtils.determineStatus(userId, null, null, null, null);
        assertEquals("Unknown status", status);
    }
}
