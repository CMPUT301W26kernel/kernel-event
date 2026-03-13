package com.example.eventlottery;

import java.time.ZonedDateTime;
import java.util.ArrayList;

/**
 * Provides temporary sample event data for development before Firebase is connected.
 * <p>
 *     Firebase already has an "events" collection which I created
 *                                                          - Grace
 * </p>
 */
public class MockEventData {

    public static ArrayList<Event> getSampleEvents() {
        ArrayList<Event> events = new ArrayList<>();

        events.add(new Event(
                "Paint & Chill Night",
                "Relaxed crafts night with snacks and board games.",
                "Team Organizer",
                ZonedDateTime.now().plusDays(3),
                ZonedDateTime.now().plusDays(15),
                30
        ));

        events.add(new Event(
                "Badminton Social",
                "Casual badminton games for all skill levels.",
                "Sports Lead",
                ZonedDateTime.now().plusDays(3),
                ZonedDateTime.now().plusDays(15),
                16
        ));

        events.add(new Event(
                "Coffee Chat + Study Jam",
                "Meet new people and do a chill study session.",
                "Community Lead",
                ZonedDateTime.now().plusDays(3),
                ZonedDateTime.now().plusDays(15),
                40
        ));

        return events;
    }
}