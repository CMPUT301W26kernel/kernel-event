package com.example.eventlottery;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repository for entrant organizer reports and admin review actions.
 */
public class OrganizerReportRepository {

    interface ReportListener {
        void onReportsChanged(List<OrganizerReport> reports);
        void onError(Exception error);
    }

    private static final String TAG = "OrganizerReportRepo";
    private static final String REPORTS_COLLECTION = "notifications";
    private static final String EVENTS_COLLECTION = "events";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore db;

    public OrganizerReportRepository() {
        this(FirebaseFirestore.getInstance());
    }

    OrganizerReportRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public ListenerRegistration listenForActiveReports(ReportListener listener) {
        return reportsCollection()
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to listen for organizer reports", error);
                        listener.onError(error);
                        return;
                    }
                    listener.onReportsChanged(sortReportsNewestFirst(filterActiveReports(mapReports(snapshot))));
                });
    }

    public Task<List<OrganizerReport>> getActiveReportsForReporterAndEvent(String reporterUserId, String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            TaskCompletionSource<List<OrganizerReport>> failed = new TaskCompletionSource<>();
            failed.setException(new IllegalArgumentException("The event could not be resolved."));
            return failed.getTask();
        }

        TaskCompletionSource<List<OrganizerReport>> taskSource = new TaskCompletionSource<>();
        reportsCollection()
                .whereEqualTo("userId", reporterUserId)
                .get()
                .addOnSuccessListener(snapshot -> taskSource.setResult(
                        sortReportsNewestFirst(
                                filterActiveReportsForReporterAndEvent(mapReports(snapshot), reporterUserId, eventId)
                        )
                ))
                .addOnFailureListener(taskSource::setException);
        return taskSource.getTask();
    }

    public Task<OrganizerReport> getReportById(String eventId, String reportId) {
        if (eventId == null || eventId.trim().isEmpty() || reportId == null || reportId.trim().isEmpty()) {
            TaskCompletionSource<OrganizerReport> failed = new TaskCompletionSource<>();
            failed.setException(new IllegalArgumentException("The report could not be resolved."));
            return failed.getTask();
        }

        TaskCompletionSource<OrganizerReport> taskSource = new TaskCompletionSource<>();
        reportsCollection()
                .document(reportId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    OrganizerReport report = snapshot.toObject(OrganizerReport.class);
                    if (report != null) {
                        report.setReportId(snapshot.getId());
                    }
                    taskSource.setResult(report);
                })
                .addOnFailureListener(taskSource::setException);
        return taskSource.getTask();
    }

    public Task<Void> savePendingReport(OrganizerReport draft) {
        if (draft == null || draft.getEventId() == null || draft.getEventId().trim().isEmpty()) {
            TaskCompletionSource<Void> failed = new TaskCompletionSource<>();
            failed.setException(new IllegalArgumentException("The event could not be resolved."));
            return failed.getTask();
        }
        if (draft.getReporterUserId() == null || draft.getReporterUserId().trim().isEmpty()) {
            TaskCompletionSource<Void> failed = new TaskCompletionSource<>();
            failed.setException(new IllegalArgumentException("Only signed-in entrants can submit reports."));
            return failed.getTask();
        }
        if (draft.getOrganizerId() == null || draft.getOrganizerId().trim().isEmpty()) {
            TaskCompletionSource<Void> failed = new TaskCompletionSource<>();
            failed.setException(new IllegalArgumentException("The organizer could not be resolved for this report."));
            return failed.getTask();
        }
        if (draft.getReporterUserId() != null && draft.getUserId() == null) {
            draft.setUserId(draft.getReporterUserId());
        }

        String validationError = OrganizerReportPolicy.validateReasonAndNote(draft.getReason(), draft.getNote());
        if (validationError != null) {
            TaskCompletionSource<Void> failed = new TaskCompletionSource<>();
            failed.setException(new IllegalArgumentException(validationError));
            return failed.getTask();
        }

        String reportId = buildReportDocumentId(
                draft.getReporterUserId(),
                draft.getOrganizerId(),
                draft.getEventId()
        );
        DocumentReference reportRef = reportsCollection().document(reportId);

        return db.collection(EVENTS_COLLECTION)
                .document(draft.getEventId())
                .get()
                .continueWithTask(eventTask -> {
                    if (!eventTask.isSuccessful()) {
                        Exception error = eventTask.getException();
                        throw error != null ? error : new IllegalStateException("Failed to load event.");
                    }
                    DocumentSnapshot eventSnapshot = eventTask.getResult();
                    if (eventSnapshot == null || !eventSnapshot.exists()) {
                        throw new IllegalStateException("The event no longer exists.");
                    }
                    return reportsCollection()
                            .whereEqualTo("userId", draft.getReporterUserId())
                            .whereEqualTo("eventId", draft.getEventId())
                            .get();
                })
                .continueWithTask(existingTask -> {
                    if (!existingTask.isSuccessful()) {
                        Exception error = existingTask.getException();
                        throw error != null ? error : new IllegalStateException("Failed to load existing reports.");
                    }

                    OrganizerReport existingReport = findMatchingReport(
                            mapReports(existingTask.getResult()),
                            draft.getOrganizerId()
                    );

                    Timestamp now = Timestamp.now();
                    draft.setReportId(reportRef.getId());
                    draft.setEntryType(OrganizerReport.ENTRY_TYPE_REPORT);
                    draft.setUserId(draft.getReporterUserId());
                    draft.setActive(true);
                    draft.setActiveKey(null);
                    draft.setStatus(OrganizerReport.STATUS_PENDING);
                    draft.setNote(OrganizerReportPolicy.normalizeNote(draft.getNote()));
                    draft.setUpdatedAt(now);

                    if (existingReport != null) {
                        if (!OrganizerReport.STATUS_PENDING.equals(existingReport.getStatus())) {
                            throw new IllegalStateException("Resolved reports cannot be edited by entrants.");
                        }
                        draft.setCreatedAt(existingReport.getCreatedAt());
                        draft.setResolvedAt(existingReport.getResolvedAt());
                        draft.setResolvedByAdminId(existingReport.getResolvedByAdminId());
                        draft.setResolvedByAdminNameSnapshot(existingReport.getResolvedByAdminNameSnapshot());
                    } else {
                        draft.setCreatedAt(now);
                        draft.setResolvedAt(null);
                        draft.setResolvedByAdminId(null);
                        draft.setResolvedByAdminNameSnapshot(null);
                    }

                    draft.setResolutionAction(null);
                    draft.setResolutionNote(null);
                    draft.setDeletedByReporterAt(null);
                    draft.setAnonymizedAt(null);

                    return reportRef.set(draft);
                })
                .addOnFailureListener(error -> Log.e(TAG, "Failed to save organizer report", error));
    }

    public Task<Void> anonymizePendingReport(OrganizerReport report, String reporterUserId) {
        if (report == null || report.getReportId() == null) {
            TaskCompletionSource<Void> failed = new TaskCompletionSource<>();
            failed.setException(new IllegalArgumentException("This report is no longer active."));
            return failed.getTask();
        }

        DocumentReference reportRef = reportsCollection().document(report.getReportId());

        return reportRef.get()
                .continueWithTask(reportTask -> {
                    if (!reportTask.isSuccessful()) {
                        Exception error = reportTask.getException();
                        throw error != null ? error : new IllegalStateException("This report is no longer active.");
                    }

                    DocumentSnapshot reportSnapshot = reportTask.getResult();
                    if (reportSnapshot == null || !reportSnapshot.exists() || !Boolean.TRUE.equals(reportSnapshot.getBoolean("active"))) {
                        throw new IllegalStateException("This report is no longer active.");
                    }
                    if (!OrganizerReport.STATUS_PENDING.equals(reportSnapshot.getString("status"))) {
                        throw new IllegalStateException("Only pending reports can be deleted.");
                    }
                    if (!reporterUserId.equals(reportSnapshot.getString("reporterUserId"))) {
                        throw new IllegalStateException("You can only delete your own report.");
                    }

                    return reportRef.update(buildAnonymizedUpdates(Timestamp.now()));
                })
                .addOnFailureListener(error -> Log.e(TAG, "Failed to anonymize organizer report", error));
    }

    public Task<Void> resolveReport(
            String eventId,
            String reportId,
            String newStatus,
            String resolutionAction,
            String resolutionNote,
            String adminId,
            String adminName
    ) {
        if (eventId == null || eventId.trim().isEmpty() || reportId == null || reportId.trim().isEmpty()) {
            TaskCompletionSource<Void> failed = new TaskCompletionSource<>();
            failed.setException(new IllegalArgumentException("This report is no longer available."));
            return failed.getTask();
        }

        DocumentReference reportRef = reportsCollection().document(reportId);
        return reportRef.get()
                .continueWithTask(reportTask -> {
                    if (!reportTask.isSuccessful()) {
                        Exception error = reportTask.getException();
                        throw error != null ? error : new IllegalStateException("This report is no longer available.");
                    }

                    DocumentSnapshot reportSnapshot = reportTask.getResult();
                    if (reportSnapshot == null || !reportSnapshot.exists() || !Boolean.TRUE.equals(reportSnapshot.getBoolean("active"))) {
                        throw new IllegalStateException("This report is no longer available.");
                    }
                    if (!OrganizerReport.STATUS_PENDING.equals(reportSnapshot.getString("status"))) {
                        throw new IllegalStateException("This report has already been resolved.");
                    }

                    Timestamp now = Timestamp.now();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", newStatus);
                    updates.put("updatedAt", now);
                    updates.put("resolvedAt", now);
                    updates.put("resolvedByAdminId", adminId);
                    updates.put("resolvedByAdminNameSnapshot", adminName);
                    updates.put("resolutionAction", resolutionAction);
                    updates.put("resolutionNote", OrganizerReportPolicy.normalizeNote(resolutionNote));
                    return reportRef.update(updates);
                })
                .addOnFailureListener(error -> Log.e(TAG, "Failed to resolve organizer report", error));
    }

    static String buildReportDocumentId(String reporterUserId, String organizerId, String eventId) {
        return sanitize(reporterUserId) + "__" + sanitize(organizerId) + "__" + sanitize(eventId);
    }

    static Map<String, Object> buildAnonymizedUpdates(Timestamp now) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("active", false);
        updates.put("activeKey", null);
        updates.put("userId", null);
        updates.put("reporterUserId", null);
        updates.put("reporterUsernameSnapshot", null);
        updates.put("organizerId", null);
        updates.put("organizerNameSnapshot", null);
        updates.put("eventId", null);
        updates.put("eventTitleSnapshot", null);
        updates.put("updatedAt", now);
        updates.put("deletedByReporterAt", now);
        updates.put("anonymizedAt", now);
        return updates;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9_-]", "_");
    }

    private List<OrganizerReport> mapReports(QuerySnapshot snapshot) {
        List<OrganizerReport> reports = new ArrayList<>();
        if (snapshot == null) {
            return reports;
        }
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            if (!OrganizerReport.ENTRY_TYPE_REPORT.equals(document.getString("entryType"))) {
                continue;
            }
            OrganizerReport report = document.toObject(OrganizerReport.class);
            if (report != null) {
                report.setReportId(document.getId());
                reports.add(report);
            }
        }
        return reports;
    }

    private OrganizerReport findMatchingReport(List<OrganizerReport> reports, String organizerId) {
        if (reports == null) {
            return null;
        }
        for (OrganizerReport report : reports) {
            if (report != null && safeEquals(organizerId, report.getOrganizerId())) {
                return report;
            }
        }
        return null;
    }

    static List<OrganizerReport> filterActiveReports(List<OrganizerReport> reports) {
        List<OrganizerReport> filtered = new ArrayList<>();
        if (reports == null) {
            return filtered;
        }
        for (OrganizerReport report : reports) {
            if (report != null && report.isActive()) {
                filtered.add(report);
            }
        }
        return filtered;
    }

    static List<OrganizerReport> filterActiveReportsForReporterAndEvent(
            List<OrganizerReport> reports,
            String reporterUserId,
            String eventId
    ) {
        List<OrganizerReport> filtered = new ArrayList<>();
        if (reports == null) {
            return filtered;
        }
        for (OrganizerReport report : reports) {
            if (report == null || !report.isActive()) {
                continue;
            }
            if (!safeEquals(reporterUserId, report.getReporterUserId())) {
                continue;
            }
            if (!safeEquals(eventId, report.getEventId())) {
                continue;
            }
            filtered.add(report);
        }
        return filtered;
    }

    static List<OrganizerReport> sortReportsNewestFirst(List<OrganizerReport> reports) {
        List<OrganizerReport> sorted = new ArrayList<>();
        if (reports == null) {
            return sorted;
        }
        sorted.addAll(reports);
        sorted.sort(
                Comparator.comparing(OrganizerReport::getCreatedAt, OrganizerReportRepository::compareCreatedAtDescending)
        );
        return sorted;
    }

    private static int compareCreatedAtDescending(Timestamp left, Timestamp right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private static boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private CollectionReference reportsCollection() {
        return db.collection(REPORTS_COLLECTION);
    }
}
