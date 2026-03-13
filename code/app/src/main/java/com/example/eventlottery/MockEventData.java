package com.example.eventlottery;

import java.util.ArrayList;

/**
 * Provides temporary sample event data for development before Firebase is connected.
 */
public class MockEventData {

    public static ArrayList<Event> getSampleEvents() {
        ArrayList<Event> events = new ArrayList<>();

        events.add(new Event(
                "Paint & Chill Night",
                "Relaxed crafts night with snacks and board games.",
                "Team Organizer",
                "March 20, 2026",
                "March 18, 2026",
                30
        ));

        events.add(new Event(
                "Badminton Social",
                "Casual badminton games for all skill levels.",
                "Sports Lead",
                "March 22, 2026",
                "March 20, 2026",
                16
        ));

        events.add(new Event(
                "Coffee Chat + Study Jam",
                "Meet new people and do a chill study session.",
                "Community Lead",
                "March 25, 2026",
                "March 24, 2026",
                40
        ));

        return events;
    }
}