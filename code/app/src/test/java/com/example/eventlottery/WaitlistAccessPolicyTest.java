package com.example.eventlottery;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Collections;

public class WaitlistAccessPolicyTest {

    @Test
    public void canManageWaitlist_allowsEventOrganizer() {
        Event event = createEvent("organizer-1");

        assertTrue(WaitlistAccessPolicy.canManageWaitlist("organizer-1", "organizer", event));
    }

    @Test
    public void canManageWaitlist_allowsCoOrganizer() {
        Event event = createEvent("organizer-1");
        event.setCoOrganizers(Collections.singletonList("co-organizer-1"));

        assertTrue(WaitlistAccessPolicy.canManageWaitlist("co-organizer-1", "organizer", event));
    }

    @Test
    public void canManageWaitlist_allowsAdmin() {
        Event event = createEvent("organizer-1");

        assertTrue(WaitlistAccessPolicy.canManageWaitlist("admin-1", "admin", event));
    }

    @Test
    public void canManageWaitlist_rejectsEntrant() {
        Event event = createEvent("organizer-1");

        assertFalse(WaitlistAccessPolicy.canManageWaitlist("entrant-1", "entrant", event));
    }

    @Test
    public void canManageWaitlist_requiresEventAndUser() {
        Event event = createEvent("organizer-1");

        assertFalse(WaitlistAccessPolicy.canManageWaitlist(null, "admin", event));
        assertFalse(WaitlistAccessPolicy.canManageWaitlist("admin-1", "admin", null));
    }

    private static Event createEvent(String organizerId) {
        return new Event(
                "Event",
                "Description",
                organizerId,
                ZonedDateTime.now().plusDays(1),
                ZonedDateTime.now().plusDays(2),
                10
        );
    }
}
