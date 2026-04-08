package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OrganizerReportPolicyTest {

    @Test
    public void onlyEntrantsCanCreateReports() {
        assertTrue(OrganizerReportPolicy.canCreateReport("entrant-1", "entrant"));
        assertFalse(OrganizerReportPolicy.canCreateReport("admin-1", "admin"));
        assertFalse(OrganizerReportPolicy.canCreateReport("organizer-1", "organizer"));
        assertFalse(OrganizerReportPolicy.canCreateReport(null, "entrant"));
    }

    @Test
    public void otherReasonRequiresNote() {
        assertEquals(
                "Add a short note when reporting another policy violation.",
                OrganizerReportPolicy.validateReasonAndNote(
                        OrganizerReport.REASON_OTHER_POLICY_VIOLATION,
                        "   "
                )
        );
        assertNull(OrganizerReportPolicy.validateReasonAndNote(
                OrganizerReport.REASON_OTHER_POLICY_VIOLATION,
                "A detailed explanation"
        ));
    }

    @Test
    public void entrantCanOnlyEditOwnPendingActiveReport() {
        OrganizerReport report = new OrganizerReport();
        report.setActive(true);
        report.setStatus(OrganizerReport.STATUS_PENDING);
        report.setReporterUserId("entrant-1");

        assertTrue(OrganizerReportPolicy.canEditOrDeleteReport(report, "entrant-1", "entrant"));

        report.setStatus(OrganizerReport.STATUS_DISMISSED);
        assertFalse(OrganizerReportPolicy.canEditOrDeleteReport(report, "entrant-1", "entrant"));

        report.setStatus(OrganizerReport.STATUS_PENDING);
        report.setActive(false);
        assertFalse(OrganizerReportPolicy.canEditOrDeleteReport(report, "entrant-1", "entrant"));
    }

    @Test
    public void adminCanOnlyResolvePendingActiveReports() {
        OrganizerReport report = new OrganizerReport();
        report.setActive(true);
        report.setStatus(OrganizerReport.STATUS_PENDING);

        assertTrue(OrganizerReportPolicy.canAdminResolve("admin", report));

        report.setStatus(OrganizerReport.STATUS_ACTION_TAKEN);
        assertFalse(OrganizerReportPolicy.canAdminResolve("admin", report));

        report.setStatus(OrganizerReport.STATUS_PENDING);
        report.setActive(false);
        assertFalse(OrganizerReportPolicy.canAdminResolve("admin", report));
        assertFalse(OrganizerReportPolicy.canAdminResolve("entrant", report));
    }

    @Test
    public void invalidReasonIsRejected() {
        assertEquals(
                "Select a valid report reason.",
                OrganizerReportPolicy.validateReasonAndNote("NOT_A_REASON", null)
        );
    }

    @Test
    public void reasonLabelMappingsRoundTrip() {
        String label = OrganizerReportPolicy.getReasonLabel(OrganizerReport.REASON_SCAM_OR_FRAUD);
        assertEquals("Scam, fraud, or misleading event", label);
        assertEquals(OrganizerReport.REASON_SCAM_OR_FRAUD, OrganizerReportPolicy.reasonCodeFromLabel(label));
    }

    @Test
    public void searchMatchesRelevantReportFields() {
        OrganizerReport report = new OrganizerReport();
        report.setOrganizerNameSnapshot("Jordan Host");
        report.setEventTitleSnapshot("Night Market");
        report.setReporterUsernameSnapshot("sam_entrant");
        report.setReason(OrganizerReport.REASON_UNSAFE_EVENT_CONDITIONS);
        report.setResolutionNote("Escalated for venue review");

        assertTrue(OrganizerReportPolicy.matchesSearchText(report, "Jordan"));
        assertTrue(OrganizerReportPolicy.matchesSearchText(report, "market"));
        assertTrue(OrganizerReportPolicy.matchesSearchText(report, "sam_entrant"));
        assertTrue(OrganizerReportPolicy.matchesSearchText(report, "unsafe"));
        assertTrue(OrganizerReportPolicy.matchesSearchText(report, "venue review"));
        assertFalse(OrganizerReportPolicy.matchesSearchText(report, "completely unrelated"));
    }
}
