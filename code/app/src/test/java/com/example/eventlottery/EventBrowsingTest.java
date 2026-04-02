package com.example.eventlottery;

import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Unit tests for Event Browsing & Overview feature.
 */
public class EventBrowsingTest {

    // Test 1: Event fields populate correctly (US 01.01.03)
    @Test
    public void testEventFieldsPopulate() {
        Event event = new Event(
                "Swimming Lessons",
                "Beginner swim class",
                "organizer123",
                ZonedDateTime.now().plusDays(1),
                ZonedDateTime.now().plusDays(7),
                20
        );
        assertEquals("Swimming Lessons", event.getTitle());
        assertEquals("Beginner swim class", event.getDescription());
        assertEquals("organizer123", event.getOrganizerId());
        assertEquals(Integer.valueOf(20), event.getWaitingListCapacity());
    }

    // Test 2: Keyword filter matches title and description (US 01.01.05)
    @Test
    public void testKeywordFilterMatchesTitleAndDescription() {
        Event event1 = new Event("Swimming Lessons", "Fun water class", "org1",
                ZonedDateTime.now().plusDays(1), ZonedDateTime.now().plusDays(7), null);
        Event event2 = new Event("Dance Class", "Learn to swim gracefully", "org1",
                ZonedDateTime.now().plusDays(1), ZonedDateTime.now().plusDays(7), null);
        Event event3 = new Event("Cooking Class", "Make great food", "org1",
                ZonedDateTime.now().plusDays(1), ZonedDateTime.now().plusDays(7), null);

        String keyword = "swim";
        ArrayList<Event> matched = new ArrayList<>();
        for (Event e : new Event[]{event1, event2, event3}) {
            String title = e.getTitle() == null ? "" : e.getTitle().toLowerCase(Locale.US);
            String desc  = e.getDescription() == null ? "" : e.getDescription().toLowerCase(Locale.US);
            if (title.contains(keyword) || desc.contains(keyword)) {
                matched.add(e);
            }
        }

        assertEquals(2, matched.size());
        assertEquals("Swimming Lessons", matched.get(0).getTitle());
        assertEquals("Dance Class", matched.get(1).getTitle());
    }

    // Test 3: Filter open registration only returns currently open events (US 01.01.04)
    @Test
    public void testFilterOpenRegistration() {
        ZonedDateTime now = ZonedDateTime.now();

        Event open = new Event("Open Event", "desc", "org1",
                now.minusDays(1), now.plusDays(5), null);
        Event notYetOpen = new Event("Future Event", "desc", "org1",
                now.plusDays(2), now.plusDays(10), null);
        Event closed = new Event("Closed Event", "desc", "org1",
                now.minusDays(10), now.minusDays(1), null);

        ArrayList<Event> filtered = new ArrayList<>();
        for (Event e : new Event[]{open, notYetOpen, closed}) {
            if (e.getRegistrationOpen().isBefore(now) && e.getRegistrationClose().isAfter(now)) {
                filtered.add(e);
            }
        }

        assertEquals(1, filtered.size());
        assertEquals("Open Event", filtered.get(0).getTitle());
    }

    // Test 4: Capacity filter works correctly (US 01.01.04)
    @Test
    public void testFilterByMinCapacity() {
        Event small  = new Event("Small Event",  "desc", "org1",
                ZonedDateTime.now(), ZonedDateTime.now().plusDays(1), 5);
        Event medium = new Event("Medium Event", "desc", "org1",
                ZonedDateTime.now(), ZonedDateTime.now().plusDays(1), 20);
        Event large  = new Event("Large Event",  "desc", "org1",
                ZonedDateTime.now(), ZonedDateTime.now().plusDays(1), 100);

        int minCapacity = 15;
        ArrayList<Event> filtered = new ArrayList<>();
        for (Event e : new Event[]{small, medium, large}) {
            Integer cap = e.getWaitingListCapacity();
            if (cap != null && cap >= minCapacity) {
                filtered.add(e);
            }
        }

        assertEquals(2, filtered.size());
        assertEquals("Medium Event", filtered.get(0).getTitle());
        assertEquals("Large Event",  filtered.get(1).getTitle());
    }
}
