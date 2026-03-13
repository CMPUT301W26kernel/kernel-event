package com.example.eventlottery;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EventTest {

    @Test
    public void testEventConstructorStoresValuesCorrectly() {
        Event event = new Event(
                "Paint Night",
                "Fun painting event",
                "Tas",
                "March 20, 2026",
                "March 18, 2026",
                25
        );

        assertEquals("Paint Night", event.getTitle());
        assertEquals("Fun painting event", event.getDescription());
        assertEquals("Tas", event.getOrganizerName());
        assertEquals("March 20, 2026", event.getStartDate());
        assertEquals("March 18, 2026", event.getRegistrationDeadline());
        assertEquals(25, event.getMaxParticipants());
    }

    @Test
    public void testEventSettersWorkCorrectly() {
        Event event = new Event();

        event.setTitle("Badminton Social");
        event.setDescription("Casual badminton games");
        event.setOrganizerName("Sports Lead");
        event.setStartDate("March 22, 2026");
        event.setRegistrationDeadline("March 20, 2026");
        event.setMaxParticipants(16);

        assertEquals("Badminton Social", event.getTitle());
        assertEquals("Casual badminton games", event.getDescription());
        assertEquals("Sports Lead", event.getOrganizerName());
        assertEquals("March 22, 2026", event.getStartDate());
        assertEquals("March 20, 2026", event.getRegistrationDeadline());
        assertEquals(16, event.getMaxParticipants());
    }
}