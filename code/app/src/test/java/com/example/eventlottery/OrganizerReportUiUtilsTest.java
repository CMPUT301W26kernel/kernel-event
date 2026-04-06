package com.example.eventlottery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

public class OrganizerReportUiUtilsTest {

    @Test
    public void formatResolutionActionUsesFriendlyLabels() {
        assertEquals("Remove organizer", OrganizerReportUiUtils.formatResolutionAction(
                OrganizerReport.ACTION_REMOVE_ORGANIZER
        ));
        assertEquals("Dismiss", OrganizerReportUiUtils.formatResolutionAction(
                OrganizerReport.ACTION_DISMISSED
        ));
        assertEquals("None", OrganizerReportUiUtils.formatResolutionAction("unknown"));
    }

    @Test
    public void formatTimestampProducesReadableValue() {
        String formatted = OrganizerReportUiUtils.formatTimestamp(new Timestamp(1712250000L, 0));
        assertTrue(formatted.contains("-"));
        assertTrue(formatted.contains(":"));
    }
}
