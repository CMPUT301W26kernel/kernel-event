package com.example.eventlottery;

import com.google.firebase.Timestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Small UI formatting helpers shared by organizer report screens.
 */
public final class OrganizerReportUiUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());

    private OrganizerReportUiUtils() {
    }

    public static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "Unknown";
        }
        return DATE_TIME_FORMATTER.format(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanoseconds())
                        .atZone(ZoneId.systemDefault())
        );
    }

    public static String formatResolutionAction(String resolutionAction) {
        if (OrganizerReport.ACTION_REMOVE_ORGANIZER.equals(resolutionAction)) {
            return "Remove organizer";
        }
        if (OrganizerReport.ACTION_DISMISSED.equals(resolutionAction)) {
            return "Dismiss";
        }
        return "None";
    }
}
