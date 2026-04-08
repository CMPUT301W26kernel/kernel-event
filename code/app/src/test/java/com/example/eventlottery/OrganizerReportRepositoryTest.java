package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OrganizerReportRepositoryTest {

    @Test
    public void buildReportDocumentIdNormalizesIdentifiers() {
        assertEquals(
                "entrant_1__organizer-1__event_1",
                OrganizerReportRepository.buildReportDocumentId("Entrant 1", "Organizer-1", "Event/1")
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

    @Test
    public void filterActiveReportsForReporterAndEventKeepsOnlyMatchingActiveReports() {
        OrganizerReport matching = buildReport(true, "entrant-1", "event-1", 300L);
        OrganizerReport differentEvent = buildReport(true, "entrant-1", "event-2", 200L);
        OrganizerReport inactive = buildReport(false, "entrant-1", "event-1", 100L);
        OrganizerReport differentReporter = buildReport(true, "entrant-2", "event-1", 400L);

        List<OrganizerReport> filtered = OrganizerReportRepository.filterActiveReportsForReporterAndEvent(
                Arrays.asList(matching, differentEvent, inactive, differentReporter),
                "entrant-1",
                "event-1"
        );

        assertEquals(1, filtered.size());
        assertTrue(filtered.contains(matching));
    }

    @Test
    public void sortReportsNewestFirstPlacesNullTimestampsLast() {
        OrganizerReport newest = buildReport(true, "entrant-1", "event-1", 300L);
        OrganizerReport older = buildReport(true, "entrant-1", "event-1", 100L);
        OrganizerReport noTimestamp = buildReport(true, "entrant-1", "event-1", null);

        List<OrganizerReport> sorted = OrganizerReportRepository.sortReportsNewestFirst(
                Arrays.asList(older, noTimestamp, newest)
        );

        assertEquals(newest, sorted.get(0));
        assertEquals(older, sorted.get(1));
        assertEquals(noTimestamp, sorted.get(2));
    }

    private OrganizerReport buildReport(boolean active, String reporterUserId, String eventId, Long seconds) {
        OrganizerReport report = new OrganizerReport();
        report.setActive(active);
        report.setReporterUserId(reporterUserId);
        report.setEventId(eventId);
        if (seconds != null) {
            report.setCreatedAt(new Timestamp(seconds, 0));
        }
        return report;
    }
}
