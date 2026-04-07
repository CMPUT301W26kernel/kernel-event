package com.example.eventlottery;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin report center for organizer reports.
 */
public class OrganizerReportCenterFragment extends Fragment {

    private static final int MODE_SUMMARY = 0;
    private static final int MODE_REPORTS = 1;

    private final List<OrganizerReport> allReports = new ArrayList<>();
    private final List<OrganizerSummaryItem> summaryItems = new ArrayList<>();
    private final List<OrganizerReport> reportItems = new ArrayList<>();

    private OrganizerReportRepository reportRepository;
    private ListenerRegistration reportListenerRegistration;

    private int currentMode = MODE_SUMMARY;
    private String selectedOrganizerId;

    private MaterialButton organizersTabButton;
    private MaterialButton reportsTabButton;
    private EditText searchInput;
    private Spinner summaryStatusSpinner;
    private Spinner reportStatusSpinner;
    private Spinner reportReasonSpinner;
    private EditText minReportCountInput;
    private EditText startDateInput;
    private EditText endDateInput;
    private View summaryFilters;
    private View reportFilters;
    private TextView subtitleView;
    private TextView emptyView;
    private ListView listView;

    private SummaryAdapter summaryAdapter;
    private ReportAdapter reportAdapter;

    public OrganizerReportCenterFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reportRepository = new OrganizerReportRepository();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_report_center, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupSpinners();
        setupButtons();
        setupList();
        updateModeUi();
        startListeningForReports();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reportListenerRegistration != null) {
            reportListenerRegistration.remove();
            reportListenerRegistration = null;
        }
    }

    private void bindViews(@NonNull View view) {
        organizersTabButton = view.findViewById(R.id.btn_view_organizer_summaries);
        reportsTabButton = view.findViewById(R.id.btn_view_report_list);
        searchInput = view.findViewById(R.id.edit_report_search);
        summaryStatusSpinner = view.findViewById(R.id.spinner_summary_status_filter);
        reportStatusSpinner = view.findViewById(R.id.spinner_report_status_filter);
        reportReasonSpinner = view.findViewById(R.id.spinner_report_reason_filter);
        minReportCountInput = view.findViewById(R.id.edit_min_report_count);
        startDateInput = view.findViewById(R.id.edit_report_start_date);
        endDateInput = view.findViewById(R.id.edit_report_end_date);
        summaryFilters = view.findViewById(R.id.summary_filter_container);
        reportFilters = view.findViewById(R.id.report_filter_container);
        subtitleView = view.findViewById(R.id.text_report_center_subtitle);
        emptyView = view.findViewById(R.id.text_report_center_empty);
        listView = view.findViewById(R.id.list_report_center);
    }

    private void setupSpinners() {
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                buildStatusOptions()
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        summaryStatusSpinner.setAdapter(statusAdapter);
        reportStatusSpinner.setAdapter(statusAdapter);

        List<String> reasonOptions = new ArrayList<>();
        reasonOptions.add("Any reason");
        reasonOptions.addAll(OrganizerReportPolicy.getReasonLabels());
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                reasonOptions
        );
        reasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportReasonSpinner.setAdapter(reasonAdapter);
    }

    private void setupButtons() {
        organizersTabButton.setOnClickListener(v -> {
            currentMode = MODE_SUMMARY;
            selectedOrganizerId = null;
            updateModeUi();
            applyFilters();
        });
        reportsTabButton.setOnClickListener(v -> {
            currentMode = MODE_REPORTS;
            updateModeUi();
            applyFilters();
        });

        MaterialButton applyButton = requireView().findViewById(R.id.btn_apply_report_filters);
        MaterialButton clearButton = requireView().findViewById(R.id.btn_clear_report_filters);
        MaterialButton doneButton = requireView().findViewById(R.id.btn_done_report_center);

        applyButton.setOnClickListener(v -> applyFilters());
        clearButton.setOnClickListener(v -> {
            selectedOrganizerId = null;
            searchInput.setText("");
            minReportCountInput.setText("");
            startDateInput.setText("");
            endDateInput.setText("");
            summaryStatusSpinner.setSelection(0);
            reportStatusSpinner.setSelection(0);
            reportReasonSpinner.setSelection(0);
            applyFilters();
        });
        doneButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void setupList() {
        summaryAdapter = new SummaryAdapter();
        reportAdapter = new ReportAdapter();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (currentMode == MODE_SUMMARY) {
                OrganizerSummaryItem summary = summaryItems.get(position);
                selectedOrganizerId = summary.organizerId;
                currentMode = MODE_REPORTS;
                updateModeUi();
                applyFilters();
            } else {
                OrganizerReport report = reportItems.get(position);
                OrganizerReportDetailFragment fragment = OrganizerReportDetailFragment.newInstance(
                        report.getEventId(),
                        report.getReportId()
                );
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void startListeningForReports() {
        reportListenerRegistration = reportRepository.listenForActiveReports(new OrganizerReportRepository.ReportListener() {
            @Override
            public void onReportsChanged(List<OrganizerReport> reports) {
                allReports.clear();
                allReports.addAll(reports);
                applyFilters();
            }

            @Override
            public void onError(Exception error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.report_load_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateModeUi() {
        boolean summaryMode = currentMode == MODE_SUMMARY;
        summaryFilters.setVisibility(summaryMode ? View.VISIBLE : View.GONE);
        reportFilters.setVisibility(summaryMode ? View.GONE : View.VISIBLE);
        listView.setAdapter(summaryMode ? summaryAdapter : reportAdapter);

        int activeColor = ContextCompat.getColor(requireContext(), R.color.primary_dark);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.primary_light);
        organizersTabButton.setBackgroundTintList(ColorStateList.valueOf(summaryMode ? activeColor : inactiveColor));
        reportsTabButton.setBackgroundTintList(ColorStateList.valueOf(summaryMode ? inactiveColor : activeColor));

        if (summaryMode) {
            subtitleView.setText(R.string.report_center_subtitle);
        } else if (selectedOrganizerId != null) {
            subtitleView.setText("Showing reports for the selected organizer.");
        } else {
            subtitleView.setText("Showing all submitted reports.");
        }
    }

    private void applyFilters() {
        if (currentMode == MODE_SUMMARY) {
            renderSummaryMode();
        } else {
            renderReportsMode();
        }
    }

    private void renderSummaryMode() {
        String query = valueOf(searchInput).trim().toLowerCase(Locale.US);
        String statusFilter = selectedStatus(summaryStatusSpinner);
        Integer minReportCount = parseOptionalInteger(minReportCountInput);
        if (!TextUtils.isEmpty(valueOf(minReportCountInput)) && minReportCount == null) {
            minReportCountInput.setError(getString(R.string.report_min_count_hint));
            return;
        }
        minReportCountInput.setError(null);

        Map<String, OrganizerSummaryItem> grouped = new HashMap<>();
        for (OrganizerReport report : allReports) {
            if (!matchesStatus(report.getStatus(), statusFilter)) {
                continue;
            }
            if (!query.isEmpty() && !containsIgnoreCase(report.getOrganizerNameSnapshot(), query)) {
                continue;
            }
            if (report.getOrganizerId() == null) {
                continue;
            }

            OrganizerSummaryItem item = grouped.get(report.getOrganizerId());
            if (item == null) {
                item = new OrganizerSummaryItem(
                        report.getOrganizerId(),
                        fallback(report.getOrganizerNameSnapshot(), "Unknown organizer")
                );
                grouped.put(report.getOrganizerId(), item);
            }

            item.totalReports += 1;
            if (OrganizerReport.STATUS_PENDING.equals(report.getStatus())) {
                item.pendingReports += 1;
            } else if (OrganizerReport.STATUS_DISMISSED.equals(report.getStatus())) {
                item.dismissedReports += 1;
            } else if (OrganizerReport.STATUS_ACTION_TAKEN.equals(report.getStatus())) {
                item.actionTakenReports += 1;
            }
        }

        summaryItems.clear();
        summaryItems.addAll(grouped.values());
        if (minReportCount != null) {
            summaryItems.removeIf(item -> item.totalReports < minReportCount);
        }
        summaryItems.sort(Comparator.comparingInt((OrganizerSummaryItem item) -> item.totalReports).reversed()
                .thenComparing(item -> item.organizerName.toLowerCase(Locale.US)));
        summaryAdapter.notifyDataSetChanged();
        emptyView.setVisibility(summaryItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderReportsMode() {
        String query = valueOf(searchInput);
        String statusFilter = selectedStatus(reportStatusSpinner);
        String reasonFilter = selectedReasonCode();
        LocalDate startDate = parseOptionalDate(startDateInput);
        if (!TextUtils.isEmpty(valueOf(startDateInput)) && startDate == null) {
            startDateInput.setError(getString(R.string.report_invalid_date));
            return;
        }
        LocalDate endDate = parseOptionalDate(endDateInput);
        if (!TextUtils.isEmpty(valueOf(endDateInput)) && endDate == null) {
            endDateInput.setError(getString(R.string.report_invalid_date));
            return;
        }
        startDateInput.setError(null);
        endDateInput.setError(null);

        reportItems.clear();
        for (OrganizerReport report : allReports) {
            if (selectedOrganizerId != null && !selectedOrganizerId.equals(report.getOrganizerId())) {
                continue;
            }
            if (!matchesStatus(report.getStatus(), statusFilter)) {
                continue;
            }
            if (reasonFilter != null && !reasonFilter.equals(report.getReason())) {
                continue;
            }
            if (!OrganizerReportPolicy.matchesSearchText(report, query)) {
                continue;
            }
            if (!matchesDateRange(report.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            reportItems.add(report);
        }

        reportItems.sort((left, right) -> {
            Timestamp leftTime = left.getCreatedAt();
            Timestamp rightTime = right.getCreatedAt();
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        });

        reportAdapter.notifyDataSetChanged();
        emptyView.setVisibility(reportItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean matchesDateRange(Timestamp timestamp, LocalDate startDate, LocalDate endDate) {
        if (timestamp == null) {
            return startDate == null && endDate == null;
        }
        LocalDate createdDate = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanoseconds())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        if (startDate != null && createdDate.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !createdDate.isAfter(endDate);
    }

    private String selectedStatus(Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        return selected == null ? "Any status" : selected.toString();
    }

    private String selectedReasonCode() {
        int selectedIndex = reportReasonSpinner.getSelectedItemPosition();
        if (selectedIndex <= 0) {
            return null;
        }
        return OrganizerReportPolicy.getReasonCodes().get(selectedIndex - 1);
    }

    private boolean matchesStatus(String reportStatus, String filterStatus) {
        if ("Any status".equals(filterStatus)) {
            return true;
        }
        if ("Pending".equals(filterStatus)) {
            return OrganizerReport.STATUS_PENDING.equals(reportStatus);
        }
        if ("Dismissed".equals(filterStatus)) {
            return OrganizerReport.STATUS_DISMISSED.equals(reportStatus);
        }
        return OrganizerReport.STATUS_ACTION_TAKEN.equals(reportStatus);
    }

    private Integer parseOptionalInteger(EditText input) {
        String raw = valueOf(input).trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private LocalDate parseOptionalDate(EditText input) {
        String raw = valueOf(input).trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (Exception error) {
            return null;
        }
    }

    private String valueOf(EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private boolean containsIgnoreCase(String candidate, String query) {
        return candidate != null && candidate.toLowerCase(Locale.US).contains(query);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private List<String> buildStatusOptions() {
        List<String> options = new ArrayList<>();
        options.add("Any status");
        options.add("Pending");
        options.add("Dismissed");
        options.add("Action taken");
        return options;
    }

    private static final class OrganizerSummaryItem {
        final String organizerId;
        final String organizerName;
        int totalReports;
        int pendingReports;
        int dismissedReports;
        int actionTakenReports;

        OrganizerSummaryItem(String organizerId, String organizerName) {
            this.organizerId = organizerId;
            this.organizerName = organizerName;
        }
    }

    private final class SummaryAdapter extends ArrayAdapter<OrganizerSummaryItem> {
        SummaryAdapter() {
            super(requireContext(), R.layout.item_organizer_report_summary, summaryItems);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_organizer_report_summary, parent, false);
            }
            OrganizerSummaryItem item = getItem(position);
            if (item != null) {
                TextView nameView = convertView.findViewById(R.id.text_summary_item_name);
                TextView countView = convertView.findViewById(R.id.text_summary_item_count);
                TextView breakdownView = convertView.findViewById(R.id.text_summary_item_breakdown);
                nameView.setText(item.organizerName);
                countView.setText(getString(R.string.report_summary_count_format, item.totalReports));
                breakdownView.setText(getString(
                        R.string.report_summary_breakdown_format,
                        item.pendingReports,
                        item.dismissedReports,
                        item.actionTakenReports
                ));
            }
            return convertView;
        }
    }

    private final class ReportAdapter extends ArrayAdapter<OrganizerReport> {
        ReportAdapter() {
            super(requireContext(), R.layout.item_organizer_report, reportItems);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_organizer_report, parent, false);
            }
            OrganizerReport report = getItem(position);
            if (report != null) {
                TextView titleView = convertView.findViewById(R.id.text_report_item_title);
                TextView metaView = convertView.findViewById(R.id.text_report_item_meta);
                TextView noteView = convertView.findViewById(R.id.text_report_item_note);
                titleView.setText(fallback(report.getOrganizerNameSnapshot(), "Unknown organizer"));
                metaView.setText(getString(
                        R.string.report_item_meta_format,
                        OrganizerReportUiUtils.formatTimestamp(report.getCreatedAt()),
                        OrganizerReportPolicy.getReasonLabel(report.getReason()),
                        OrganizerReportPolicy.getStatusLabel(report.getStatus())
                ));
                String normalizedNote = OrganizerReportPolicy.normalizeNote(report.getNote());
                noteView.setText(
                        "Event: " + fallback(report.getEventTitleSnapshot(), "Unknown event")
                                + "\nEntrant: " + fallback(report.getReporterUsernameSnapshot(), "Unknown entrant")
                                + "\nNote: " + (normalizedNote == null ? getString(R.string.report_item_note_empty) : normalizedNote)
                );
            }
            return convertView;
        }
    }
}
