package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.ArrayList;

public class MockEventDataTest {

    @Test
    public void testSampleEventsIsNotEmpty() {
        ArrayList<Event> events = MockEventData.getSampleEvents();
        assertFalse(events.isEmpty());
    }

    @Test
    public void testSampleEventsContainsThreeEvents() {
        ArrayList<Event> events = MockEventData.getSampleEvents();
        assertEquals(3, events.size());
    }

    @Test
    public void testFirstSampleEventTitleIsCorrect() {
        ArrayList<Event> events = MockEventData.getSampleEvents();
        assertEquals("Paint & Chill Night", events.get(0).getTitle());
    }
}