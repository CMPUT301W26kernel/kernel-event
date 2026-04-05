package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Map;

public class OrganizerReportRepositoryTest {

    @Test
    public void buildActiveKeyNormalizesIdentifiers() {
        assertEquals(
                "entrant_1__organizer-1__event_1",
                OrganizerReportRepository.buildActiveKey("Entrant 1", "Organizer-1", "Event/1")
        );
    }

    @Test
    public void anonymizedUpdatesDetachReportLinks() {
        Timestamp now = Timestamp.now();
        Map<String, Object> updates = OrganizerReportRepository.buildAnonymizedUpdates(now);

        assertEquals(false, updates.get("active"));
        assertNull(updates.get("reporterUserId"));
        assertNull(updates.get("organizerId"));
        assertNull(updates.get("eventId"));
        assertEquals(now, updates.get("deletedByReporterAt"));
        assertEquals(now, updates.get("anonymizedAt"));
    }
}
