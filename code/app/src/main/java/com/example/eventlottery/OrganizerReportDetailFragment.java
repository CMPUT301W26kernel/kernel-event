package com.example.eventlottery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.profiles.UserProfileFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Admin detail screen for a single organizer report.
 */
public class OrganizerReportDetailFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";
    private static final String ARG_REPORT_ID = "report_id";

    private OrganizerReportRepository reportRepository;
    private String eventId;
    private String reportId;
    private String currentAdminId;
    private String currentAdminRole;
    private String currentAdminName;
    private OrganizerReport currentReport;

    private TextView statusView;
    private TextView summaryView;
    private TextView noteView;
    private TextView resolutionView;
    private EditText resolutionNoteInput;
    private MaterialButton dismissButton;
    private MaterialButton removeOrganizerButton;

    public static OrganizerReportDetailFragment newInstance(String eventId, String reportId) {
        OrganizerReportDetailFragment fragment = new OrganizerReportDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_REPORT_ID, reportId);
        fragment.setArguments(args);
        return fragment;
    }

    public OrganizerReportDetailFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reportRepository = new OrganizerReportRepository();
        currentAdminId = FirebaseAuth.getInstance().getUid();
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            reportId = getArguments().getString(ARG_REPORT_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_report_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        statusView = view.findViewById(R.id.text_report_detail_status);
        summaryView = view.findViewById(R.id.text_report_detail_summary);
        noteView = view.findViewById(R.id.text_report_detail_note);
        resolutionView = view.findViewById(R.id.text_report_detail_resolution);
        resolutionNoteInput = view.findViewById(R.id.edit_admin_resolution_note);
        dismissButton = view.findViewById(R.id.btn_dismiss_report);
        removeOrganizerButton = view.findViewById(R.id.btn_remove_organizer);
        MaterialButton doneButton = view.findViewById(R.id.btn_done_report_detail);

        doneButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        dismissButton.setOnClickListener(v -> resolveAsDismissed());
        removeOrganizerButton.setOnClickListener(v -> resolveWithOrganizerRemoval());

        loadCurrentAdmin();
        loadReport();
    }

    private void loadCurrentAdmin() {
        if (currentAdminId == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentAdminId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    currentAdminRole = snapshot.getString("role");
                    currentAdminName = snapshot.getString("username");
                    bindReport(currentReport);
                });
    }

    private void loadReport() {
        if (eventId == null || reportId == null) {
            showMissingReport();
            return;
        }

        reportRepository.getReportById(eventId, reportId)
                .addOnSuccessListener(report -> {
                    currentReport = report;
                    if (report == null) {
                        showMissingReport();
                        return;
                    }
                    bindReport(report);
                })
                .addOnFailureListener(error -> showMissingReport());
    }

    private void bindReport(@Nullable OrganizerReport report) {
        if (!isAdded() || report == null || statusView == null) {
            return;
        }

        statusView.setText(OrganizerReportPolicy.getStatusLabel(report.getStatus()));
        summaryView.setText(getString(
                R.string.report_history_summary_format,
                fallback(report.getOrganizerNameSnapshot(), "Unknown organizer"),
                fallback(report.getEventTitleSnapshot(), "Unknown event"),
                fallback(report.getReporterUsernameSnapshot(), "Unknown entrant"),
                OrganizerReportPolicy.getReasonLabel(report.getReason()),
                OrganizerReportUiUtils.formatTimestamp(report.getCreatedAt())
        ));

        String entrantNote = OrganizerReportPolicy.normalizeNote(report.getNote());
        noteView.setText(entrantNote == null
                ? getString(R.string.report_history_note_empty)
                : getString(R.string.report_history_note_format, entrantNote));

        if (report.isPending()) {
            resolutionView.setText(R.string.report_resolution_pending);
        } else {
            resolutionView.setText(getString(
                    R.string.report_resolution_format,
                    OrganizerReportPolicy.getStatusLabel(report.getStatus()),
                    OrganizerReportUiUtils.formatResolutionAction(report.getResolutionAction()),
                    fallback(report.getResolvedByAdminNameSnapshot(), "Unknown admin"),
                    OrganizerReportUiUtils.formatTimestamp(report.getResolvedAt()),
                    fallback(report.getResolutionNote(), "None")
            ));
        }

        boolean canResolve = OrganizerReportPolicy.canAdminResolve(currentAdminRole, report);
        resolutionNoteInput.setVisibility(canResolve ? View.VISIBLE : View.GONE);
        dismissButton.setVisibility(canResolve ? View.VISIBLE : View.GONE);
        removeOrganizerButton.setVisibility(canResolve ? View.VISIBLE : View.GONE);
        if (!canResolve) {
            resolutionNoteInput.setEnabled(false);
        }
    }

    private void resolveAsDismissed() {
        if (!OrganizerReportPolicy.canAdminResolve(currentAdminRole, currentReport)) {
            return;
        }

        reportRepository.resolveReport(
                        eventId,
                        reportId,
                        OrganizerReport.STATUS_DISMISSED,
                        OrganizerReport.ACTION_DISMISSED,
                        resolutionNoteInput.getText().toString(),
                        currentAdminId,
                        fallback(currentAdminName, currentAdminId)
                )
                .addOnSuccessListener(unused -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.report_resolved_success, Toast.LENGTH_SHORT).show();
                    }
                    loadReport();
                })
                .addOnFailureListener(error -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.report_resolve_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resolveWithOrganizerRemoval() {
        if (!OrganizerReportPolicy.canAdminResolve(currentAdminRole, currentReport)) {
            return;
        }

        reportRepository.resolveReport(
                        eventId,
                        reportId,
                        OrganizerReport.STATUS_ACTION_TAKEN,
                        OrganizerReport.ACTION_REMOVE_ORGANIZER,
                        resolutionNoteInput.getText().toString(),
                        currentAdminId,
                        fallback(currentAdminName, currentAdminId)
                )
                .addOnSuccessListener(unused -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.report_admin_handoff_success, Toast.LENGTH_SHORT).show();
                    }
                    if (currentReport == null || currentReport.getOrganizerId() == null) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), R.string.report_removed_organizer_missing, Toast.LENGTH_SHORT).show();
                        }
                        loadReport();
                        return;
                    }

                    UserProfileFragment fragment = UserProfileFragment.newInstance(currentReport.getOrganizerId(), true);
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                })
                .addOnFailureListener(error -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.report_resolve_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showMissingReport() {
        if (getContext() != null) {
            Toast.makeText(getContext(), R.string.report_detail_missing, Toast.LENGTH_SHORT).show();
        }
        if (isAdded()) {
            getParentFragmentManager().popBackStack();
        }
    }

    private String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
