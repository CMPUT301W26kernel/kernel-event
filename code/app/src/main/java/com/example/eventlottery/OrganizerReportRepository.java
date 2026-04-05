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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
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
    private static final String REPORTS_COLLECTION = "organizerReports";
    private static final String ACTIVE_KEYS_COLLECTION = "organizerReportActiveKeys";
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
                .whereEqualTo("active", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to listen for organizer reports", error);
                        listener.onError(error);
                        return;
                    }
                    listener.onReportsChanged(mapReports(snapshot));
                });
    }

    public Task<List<OrganizerReport>> getActiveReportsForReporterAndEvent(String reporterUserId, String eventId) {
        TaskCompletionSource<List<OrganizerReport>> taskSource = new TaskCompletionSource<>();
        reportsCollection()
                .whereEqualTo("active", true)
                .whereEqualTo("reporterUserId", reporterUserId)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> taskSource.setResult(mapReports(snapshot)))
                .addOnFailureListener(taskSource::setException);
        return taskSource.getTask();
    }

    public Task<OrganizerReport> getReportById(String reportId) {
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
        String validationError = OrganizerReportPolicy.validateReasonAndNote(draft.getReason(), draft.getNote());
        if (validationError != null) {
            TaskCompletionSource<Void> failed = new TaskCompletionSource<>();
            failed.setException(new IllegalArgumentException(validationError));
            return failed.getTask();
        }

        String activeKey = buildActiveKey(
                draft.getReporterUserId(),
                draft.getOrganizerId(),
                draft.getEventId()
        );
        DocumentReference activeKeyRef = activeKeysCollection().document(activeKey);
        DocumentReference newReportRef = reportsCollection().document();

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot eventSnapshot = transaction.get(db.collection(EVENTS_COLLECTION).document(draft.getEventId()));
            DocumentSnapshot organizerSnapshot = transaction.get(db.collection(USERS_COLLECTION).document(draft.getOrganizerId()));
            DocumentSnapshot reporterSnapshot = transaction.get(db.collection(USERS_COLLECTION).document(draft.getReporterUserId()));
            if (!eventSnapshot.exists() || !organizerSnapshot.exists()) {
                throw new IllegalStateException("The event or organizer no longer exists.");
            }
            if (!reporterSnapshot.exists()) {
                throw new IllegalStateException("Only signed-in entrants can submit reports.");
            }

            Timestamp now = Timestamp.now();
            DocumentSnapshot activeKeySnapshot = transaction.get(activeKeyRef);
            if (activeKeySnapshot.exists()) {
                String existingReportId = activeKeySnapshot.getString("reportId");
                if (existingReportId != null) {
                    DocumentReference existingReportRef = reportsCollection().document(existingReportId);
                    DocumentSnapshot existingReportSnapshot = transaction.get(existingReportRef);
                    if (existingReportSnapshot.exists() && Boolean.TRUE.equals(existingReportSnapshot.getBoolean("active"))) {
                        String existingStatus = existingReportSnapshot.getString("status");
                        if (!OrganizerReport.STATUS_PENDING.equals(existingStatus)) {
                            throw new IllegalStateException("Resolved reports cannot be edited by entrants.");
                        }
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("reason", draft.getReason());
                        updates.put("note", OrganizerReportPolicy.normalizeNote(draft.getNote()));
                        updates.put("updatedAt", now);
                        updates.put("organizerNameSnapshot", draft.getOrganizerNameSnapshot());
                        updates.put("eventTitleSnapshot", draft.getEventTitleSnapshot());
                        updates.put("reporterUsernameSnapshot", draft.getReporterUsernameSnapshot());
                        transaction.update(existingReportRef, updates);
                        return null;
                    }
                }
                transaction.delete(activeKeyRef);
            }

            draft.setReportId(newReportRef.getId());
            draft.setActive(true);
            draft.setActiveKey(activeKey);
            draft.setStatus(OrganizerReport.STATUS_PENDING);
            draft.setNote(OrganizerReportPolicy.normalizeNote(draft.getNote()));
            draft.setCreatedAt(now);
            draft.setUpdatedAt(now);
            draft.setResolvedAt(null);
            draft.setResolvedByAdminId(null);
            draft.setResolvedByAdminNameSnapshot(null);
            draft.setResolutionAction(null);
            draft.setResolutionNote(null);
            draft.setDeletedByReporterAt(null);
            draft.setAnonymizedAt(null);

            transaction.set(newReportRef, draft);
            transaction.set(activeKeyRef, buildActiveKeyDocument(newReportRef.getId(), draft, now));
            return null;
        }).addOnFailureListener(error -> Log.e(TAG, "Failed to save organizer report", error));
    }

    public Task<Void> anonymizePendingReport(OrganizerReport report, String reporterUserId) {
        DocumentReference reportRef = reportsCollection().document(report.getReportId());
        DocumentReference activeKeyRef = activeKeysCollection().document(report.getActiveKey());

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot reportSnapshot = transaction.get(reportRef);
            if (!reportSnapshot.exists() || !Boolean.TRUE.equals(reportSnapshot.getBoolean("active"))) {
                throw new IllegalStateException("This report is no longer active.");
            }
            if (!OrganizerReport.STATUS_PENDING.equals(reportSnapshot.getString("status"))) {
                throw new IllegalStateException("Only pending reports can be deleted.");
            }
            if (!reporterUserId.equals(reportSnapshot.getString("reporterUserId"))) {
                throw new IllegalStateException("You can only delete your own report.");
            }

            Timestamp now = Timestamp.now();
            transaction.update(reportRef, buildAnonymizedUpdates(now));
            transaction.delete(activeKeyRef);
            return null;
        }).addOnFailureListener(error -> Log.e(TAG, "Failed to anonymize organizer report", error));
    }

    public Task<Void> resolveReport(
            String reportId,
            String newStatus,
            String resolutionAction,
            String resolutionNote,
            String adminId,
            String adminName
    ) {
        DocumentReference reportRef = reportsCollection().document(reportId);
        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot reportSnapshot = transaction.get(reportRef);
            if (!reportSnapshot.exists() || !Boolean.TRUE.equals(reportSnapshot.getBoolean("active"))) {
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
            transaction.update(reportRef, updates);
            return null;
        }).addOnFailureListener(error -> Log.e(TAG, "Failed to resolve organizer report", error));
    }

    static String buildActiveKey(String reporterUserId, String organizerId, String eventId) {
        return sanitize(reporterUserId) + "__" + sanitize(organizerId) + "__" + sanitize(eventId);
    }

    static Map<String, Object> buildAnonymizedUpdates(Timestamp now) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("active", false);
        updates.put("activeKey", null);
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

    private Map<String, Object> buildActiveKeyDocument(String reportId, OrganizerReport report, Timestamp now) {
        Map<String, Object> activeKeyDocument = new HashMap<>();
        activeKeyDocument.put("reportId", reportId);
        activeKeyDocument.put("reporterUserId", report.getReporterUserId());
        activeKeyDocument.put("organizerId", report.getOrganizerId());
        activeKeyDocument.put("eventId", report.getEventId());
        activeKeyDocument.put("createdAt", now);
        return activeKeyDocument;
    }

    private List<OrganizerReport> mapReports(QuerySnapshot snapshot) {
        List<OrganizerReport> reports = new ArrayList<>();
        if (snapshot == null) {
            return reports;
        }
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            OrganizerReport report = document.toObject(OrganizerReport.class);
            if (report != null) {
                report.setReportId(document.getId());
                reports.add(report);
            }
        }
        return reports;
    }

    private CollectionReference reportsCollection() {
        return db.collection(REPORTS_COLLECTION);
    }

    private CollectionReference activeKeysCollection() {
        return db.collection(ACTIVE_KEYS_COLLECTION);
    }
}
