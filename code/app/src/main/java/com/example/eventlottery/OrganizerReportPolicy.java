package com.example.eventlottery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure business rules and label mappings for organizer reporting.
 */
public final class OrganizerReportPolicy {

    private static final LinkedHashMap<String, String> REASON_LABELS = new LinkedHashMap<>();

    static {
        REASON_LABELS.put(OrganizerReport.REASON_HARASSMENT_OR_HATE, "Harassment or hate");
        REASON_LABELS.put(OrganizerReport.REASON_SEXUAL_OR_EXPLICIT_CONTENT, "Sexual or explicit content");
        REASON_LABELS.put(OrganizerReport.REASON_VIOLENCE_OR_THREATS, "Violence or threats");
        REASON_LABELS.put(OrganizerReport.REASON_SCAM_OR_FRAUD, "Scam, fraud, or misleading event");
        REASON_LABELS.put(OrganizerReport.REASON_ILLEGAL_ACTIVITY, "Illegal activity");
        REASON_LABELS.put(OrganizerReport.REASON_SPAM_OR_PROMOTION, "Spam or repeated unwanted promotion");
        REASON_LABELS.put(OrganizerReport.REASON_UNSAFE_EVENT_CONDITIONS, "Unsafe event conditions");
        REASON_LABELS.put(OrganizerReport.REASON_IMPERSONATION_OR_FALSE_IDENTITY, "Impersonation or false identity");
        REASON_LABELS.put(OrganizerReport.REASON_OTHER_POLICY_VIOLATION, "Other policy violation");
    }

    private OrganizerReportPolicy() {
    }

    public static boolean canCreateReport(String currentUserId, String currentUserRole) {
        return currentUserId != null && "entrant".equalsIgnoreCase(currentUserRole);
    }

    public static boolean canEditOrDeleteReport(
            OrganizerReport report,
            String currentUserId,
            String currentUserRole
    ) {
        return report != null
                && report.isActive()
                && report.isPending()
                && currentUserId != null
                && currentUserId.equals(report.getReporterUserId())
                && "entrant".equalsIgnoreCase(currentUserRole);
    }

    public static boolean canAdminResolve(String currentUserRole, OrganizerReport report) {
        return report != null
                && report.isActive()
                && report.isPending()
                && "admin".equalsIgnoreCase(currentUserRole);
    }

    public static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String validateReasonAndNote(String reason, String note) {
        if (!REASON_LABELS.containsKey(reason)) {
            return "Select a valid report reason.";
        }
        if (OrganizerReport.REASON_OTHER_POLICY_VIOLATION.equals(reason) && normalizeNote(note) == null) {
            return "Add a short note when reporting another policy violation.";
        }
        return null;
    }

    public static List<String> getReasonCodes() {
        return new ArrayList<>(REASON_LABELS.keySet());
    }

    public static List<String> getReasonLabels() {
        return new ArrayList<>(REASON_LABELS.values());
    }

    public static String reasonCodeFromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : REASON_LABELS.entrySet()) {
            if (entry.getValue().equals(label)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static String getReasonLabel(String reason) {
        String label = REASON_LABELS.get(reason);
        return label != null ? label : "Unknown reason";
    }

    public static String getStatusLabel(String status) {
        if (OrganizerReport.STATUS_PENDING.equals(status)) {
            return "Pending";
        }
        if (OrganizerReport.STATUS_DISMISSED.equals(status)) {
            return "Dismissed";
        }
        if (OrganizerReport.STATUS_ACTION_TAKEN.equals(status)) {
            return "Action taken";
        }
        return "Unknown";
    }

    public static boolean matchesSearchText(OrganizerReport report, String query) {
        if (report == null) {
            return false;
        }
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.US);
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        return contains(report.getOrganizerNameSnapshot(), normalizedQuery)
                || contains(report.getEventTitleSnapshot(), normalizedQuery)
                || contains(report.getReporterUsernameSnapshot(), normalizedQuery)
                || contains(getReasonLabel(report.getReason()), normalizedQuery)
                || contains(report.getResolutionNote(), normalizedQuery);
    }

    private static boolean contains(String candidate, String query) {
        return candidate != null && candidate.toLowerCase(Locale.US).contains(query);
    }
}
