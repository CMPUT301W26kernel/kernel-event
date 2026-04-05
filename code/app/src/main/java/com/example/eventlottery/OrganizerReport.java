package com.example.eventlottery;

import com.google.firebase.Timestamp;

/**
 * Firestore-backed report submitted by an entrant against an organizer for a specific event.
 */
public class OrganizerReport {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_DISMISSED = "dismissed";
    public static final String STATUS_ACTION_TAKEN = "action_taken";

    public static final String ACTION_DISMISSED = "dismiss";
    public static final String ACTION_REMOVE_ORGANIZER = "remove_organizer";

    public static final String REASON_HARASSMENT_OR_HATE = "HARASSMENT_OR_HATE";
    public static final String REASON_SEXUAL_OR_EXPLICIT_CONTENT = "SEXUAL_OR_EXPLICIT_CONTENT";
    public static final String REASON_VIOLENCE_OR_THREATS = "VIOLENCE_OR_THREATS";
    public static final String REASON_SCAM_OR_FRAUD = "SCAM_OR_FRAUD";
    public static final String REASON_ILLEGAL_ACTIVITY = "ILLEGAL_ACTIVITY";
    public static final String REASON_SPAM_OR_PROMOTION = "SPAM_OR_PROMOTION";
    public static final String REASON_UNSAFE_EVENT_CONDITIONS = "UNSAFE_EVENT_CONDITIONS";
    public static final String REASON_IMPERSONATION_OR_FALSE_IDENTITY = "IMPERSONATION_OR_FALSE_IDENTITY";
    public static final String REASON_OTHER_POLICY_VIOLATION = "OTHER_POLICY_VIOLATION";

    private String reportId;
    private boolean active = true;
    private String activeKey;
    private String status = STATUS_PENDING;
    private String reason;
    private String note;
    private String reporterUserId;
    private String reporterUsernameSnapshot;
    private String organizerId;
    private String organizerNameSnapshot;
    private String eventId;
    private String eventTitleSnapshot;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp resolvedAt;
    private String resolvedByAdminId;
    private String resolvedByAdminNameSnapshot;
    private String resolutionAction;
    private String resolutionNote;
    private Timestamp deletedByReporterAt;
    private Timestamp anonymizedAt;

    public OrganizerReport() {
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getActiveKey() {
        return activeKey;
    }

    public void setActiveKey(String activeKey) {
        this.activeKey = activeKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(String reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    public String getReporterUsernameSnapshot() {
        return reporterUsernameSnapshot;
    }

    public void setReporterUsernameSnapshot(String reporterUsernameSnapshot) {
        this.reporterUsernameSnapshot = reporterUsernameSnapshot;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getOrganizerNameSnapshot() {
        return organizerNameSnapshot;
    }

    public void setOrganizerNameSnapshot(String organizerNameSnapshot) {
        this.organizerNameSnapshot = organizerNameSnapshot;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventTitleSnapshot() {
        return eventTitleSnapshot;
    }

    public void setEventTitleSnapshot(String eventTitleSnapshot) {
        this.eventTitleSnapshot = eventTitleSnapshot;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedByAdminId() {
        return resolvedByAdminId;
    }

    public void setResolvedByAdminId(String resolvedByAdminId) {
        this.resolvedByAdminId = resolvedByAdminId;
    }

    public String getResolvedByAdminNameSnapshot() {
        return resolvedByAdminNameSnapshot;
    }

    public void setResolvedByAdminNameSnapshot(String resolvedByAdminNameSnapshot) {
        this.resolvedByAdminNameSnapshot = resolvedByAdminNameSnapshot;
    }

    public String getResolutionAction() {
        return resolutionAction;
    }

    public void setResolutionAction(String resolutionAction) {
        this.resolutionAction = resolutionAction;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public Timestamp getDeletedByReporterAt() {
        return deletedByReporterAt;
    }

    public void setDeletedByReporterAt(Timestamp deletedByReporterAt) {
        this.deletedByReporterAt = deletedByReporterAt;
    }

    public Timestamp getAnonymizedAt() {
        return anonymizedAt;
    }

    public void setAnonymizedAt(Timestamp anonymizedAt) {
        this.anonymizedAt = anonymizedAt;
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }
}
