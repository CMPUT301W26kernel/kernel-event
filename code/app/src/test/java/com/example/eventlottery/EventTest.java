package com.example.eventlottery;

import static org.junit.Assert.assertEquals;

import com.example.eventlottery.event.Event;

import org.junit.Test;

import java.time.ZonedDateTime;

public class EventTest {

    @Test
    public void testEventConstructorStoresValuesCorrectly() {
        ZonedDateTime regOpen = ZonedDateTime.now().plusDays(3);
        ZonedDateTime regClose = ZonedDateTime.now().plusDays(10);

        Event event = new Event(
                "Paint Night",
                "Fun painting event",
                "Tas",
                regOpen,
                regClose,
                25
        );

        assertEquals("Paint Night", event.getTitle());
        assertEquals("Fun painting event", event.getDescription());
        assertEquals("Tas", event.getOrganizerId());
        assertEquals(regOpen, event.getRegistrationOpen());
        assertEquals(regClose, event.getRegistrationClose());
        assert(25 == event.getWaitingListCapacity());
    }

    @Test
    public void testEventSettersWorkCorrectly() {
        ZonedDateTime regOpen = ZonedDateTime.now().plusDays(3);
        ZonedDateTime regClose = ZonedDateTime.now().plusDays(10);
        Event event = new Event(
                "Paint Night",
                "Fun painting event",
                "Tas",
                regOpen,
                regClose,
                25
        );

        event.setTitle("Badminton Social");
        event.setDescription("Casual badminton games");
        event.setRegistrationOpen(regOpen);
        event.setRegistrationClose(regClose);
        event.setWaitingListCapacity(16);

        assertEquals("Badminton Social", event.getTitle());
        assertEquals("Casual badminton games", event.getDescription());
        assertEquals(regOpen, event.getRegistrationOpen());
        assertEquals(regClose, event.getRegistrationClose());
        assert(16 == event.getWaitingListCapacity());
    }
}