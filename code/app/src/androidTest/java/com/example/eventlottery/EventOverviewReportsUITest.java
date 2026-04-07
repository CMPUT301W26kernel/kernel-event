package com.example.eventlottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;

import android.os.Bundle;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Instrumentation tests for the entrant organizer report flow inside {@link EventOverviewFragment}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventOverviewReportsUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void configureDevice() {
        AndroidTestDeviceUtil.disableSystemAnimations();
    }

    @Test
    public void entrantCanSubmitAndDeleteReport() {
        FakeCommentRepository commentRepository = new FakeCommentRepository();
        FakeReportRepository reportRepository = new FakeReportRepository();

        launchFragment(buildTestState("entrant-1", "entrant", "Evan"), commentRepository, reportRepository);

        onView(withId(R.id.btn_report_organizer))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
                .perform(scrollTo())
                .perform(click());

        onView(withText(R.string.report_dialog_title)).check(matches(isDisplayed()));
        onView(withText(R.string.report_dialog_submit)).perform(click());

        onView(withId(R.id.btn_report_organizer))
                .check(matches(withText(R.string.report_button_edit_label)));
        onView(withId(R.id.text_report_status))
                .check(matches(withText(R.string.report_status_pending)));

        onView(withId(R.id.btn_report_organizer)).perform(click());
        onView(withText(R.string.report_dialog_update_title)).check(matches(isDisplayed()));
        onView(withText(R.string.report_dialog_delete)).perform(click());

        onView(withId(R.id.btn_report_organizer))
                .check(matches(withText(R.string.report_button_label)));
        onView(withId(R.id.text_report_status))
                .check(matches(withEffectiveVisibility(Visibility.GONE)));
    }

    private void launchFragment(
            EventOverviewFragment.TestState state,
            FakeCommentRepository commentRepository,
            FakeReportRepository reportRepository
    ) {
        activityRule.getScenario().onActivity(activity -> {
            EventOverviewFragment fragment = new EventOverviewFragment();
            Bundle args = new Bundle();
            args.putString("eventId", "test-event");
            fragment.setArguments(args);
            fragment.setTestState(state);
            fragment.setCommentRepositoryForTesting(commentRepository);
            fragment.setReportRepositoryForTesting(reportRepository);

            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitNow();
        });
    }

    private EventOverviewFragment.TestState buildTestState(
            String currentUserId,
            String currentUserRole,
            String currentUsername
    ) {
        ZonedDateTime now = ZonedDateTime.now();
        return new EventOverviewFragment.TestState(
                "Report Test Event",
                "organizer-1",
                "Event used for report UI tests",
                now.plusDays(1),
                now.plusDays(5),
                3,
                false,
                currentUserId,
                currentUserRole,
                currentUsername
        );
    }

    private static class FakeCommentRepository extends EventCommentRepository {
        FakeCommentRepository() {
            super((FirebaseFirestore) null);
        }

        @Override
        public ListenerRegistration listenForComments(String eventId, CommentListener listener) {
            listener.onCommentsChanged(new ArrayList<>());
            return () -> { };
        }
    }

    private static class FakeReportRepository extends OrganizerReportRepository {
        private final List<OrganizerReport> reports = new ArrayList<>();

        FakeReportRepository() {
            super((FirebaseFirestore) null);
        }

        @Override
        public Task<List<OrganizerReport>> getActiveReportsForReporterAndEvent(String reporterUserId, String eventId) {
            return Tasks.forResult(OrganizerReportRepository.sortReportsNewestFirst(
                    OrganizerReportRepository.filterActiveReportsForReporterAndEvent(
                            new ArrayList<>(reports),
                            reporterUserId,
                            eventId
                    )
            ));
        }

        @Override
        public Task<Void> savePendingReport(OrganizerReport draft) {
            OrganizerReport existing = null;
            for (OrganizerReport report : reports) {
                if (report.isActive()
                        && safeEquals(report.getEventId(), draft.getEventId())
                        && safeEquals(report.getReporterUserId(), draft.getReporterUserId())
                        && safeEquals(report.getOrganizerId(), draft.getOrganizerId())) {
                    existing = report;
                    break;
                }
            }

            Timestamp now = Timestamp.now();
            if (existing != null) {
                existing.setReason(draft.getReason());
                existing.setNote(draft.getNote());
                existing.setUpdatedAt(now);
                existing.setOrganizerNameSnapshot(draft.getOrganizerNameSnapshot());
                existing.setEventTitleSnapshot(draft.getEventTitleSnapshot());
                existing.setReporterUsernameSnapshot(draft.getReporterUsernameSnapshot());
            } else {
                draft.setReportId("report-" + (reports.size() + 1));
                draft.setEntryType(OrganizerReport.ENTRY_TYPE_REPORT);
                draft.setUserId(draft.getReporterUserId());
                draft.setActive(true);
                draft.setStatus(OrganizerReport.STATUS_PENDING);
                draft.setCreatedAt(now);
                draft.setUpdatedAt(now);
                reports.add(draft);
            }
            return Tasks.forResult(null);
        }

        @Override
        public Task<Void> anonymizePendingReport(OrganizerReport report, String reporterUserId) {
            for (OrganizerReport candidate : reports) {
                if (safeEquals(candidate.getReportId(), report.getReportId())) {
                    Timestamp now = Timestamp.now();
                    candidate.setActive(false);
                    candidate.setUserId(null);
                    candidate.setReporterUserId(null);
                    candidate.setReporterUsernameSnapshot(null);
                    candidate.setOrganizerId(null);
                    candidate.setOrganizerNameSnapshot(null);
                    candidate.setEventId(null);
                    candidate.setEventTitleSnapshot(null);
                    candidate.setDeletedByReporterAt(now);
                    candidate.setAnonymizedAt(now);
                    candidate.setUpdatedAt(now);
                }
            }
            return Tasks.forResult(null);
        }

        private boolean safeEquals(String left, String right) {
            return left == null ? right == null : left.equals(right);
        }
    }
}
