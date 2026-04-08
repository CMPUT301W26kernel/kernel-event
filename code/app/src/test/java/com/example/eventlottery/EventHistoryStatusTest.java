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
    public void testStatusStillOnWaitingListAfterDraw() {
        // A draw happened, but users who were not selected remain eligible for future draws.
        List<String> waiting = Arrays.asList(userId);
        List<String> invited = Arrays.asList("winner1");
        String status = EventHistoryStatusUtils.determineStatus(userId, waiting, invited, null, null);
        assertEquals("Lottery results:\nStill on waiting list", status);
    }

    @Test
    public void testStatusStillOnWaitingListAfterAcceptance() {
        // Accepted entrants also indicate that the draw happened, but the remaining pool stays active.
        List<String> waiting = Arrays.asList(userId);
        List<String> accepted = Arrays.asList("winner1");
        String status = EventHistoryStatusUtils.determineStatus(userId, waiting, null, accepted, null);
        assertEquals("Lottery results:\nStill on waiting list", status);
    }

    @Test
    public void testStatusPending() {
        // Before any draw happens, the user is still on the waiting list.
        List<String> waiting = Arrays.asList(userId);
        List<String> invited = new ArrayList<>();
        String status = EventHistoryStatusUtils.determineStatus(userId, waiting, invited, null, null);
        assertEquals("Lottery results:\nStill on waiting list", status);
    }

    @Test
    public void testStatusUnknown() {
        String status = EventHistoryStatusUtils.determineStatus(userId, null, null, null, null);
        assertEquals("Unknown status", status);
    }
}
