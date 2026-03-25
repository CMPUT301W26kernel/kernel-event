package com.example.eventlottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Instrumentation tests for the comment functionality inside {@link EventOverviewFragment}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventOverviewCommentsUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Verifies that an entrant can view the existing thread and submit a new comment.
     */
    @Test
    public void entrantCanViewAndPostComments() {
        FakeCommentRepository repository = new FakeCommentRepository(Arrays.asList(
                buildComment("comment-1", "organizer-1", "Olivia", "organizer", "Welcome", 100, true)
        ));

        launchFragment(buildTestState("entrant-1", "entrant", "Evan"), repository);

        onView(withText("Welcome")).check(matches(isDisplayed()));
        onView(withId(R.id.comment_composer_container))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));

        onView(withId(R.id.edit_comment_input)).perform(typeText("Looking forward to it"), closeSoftKeyboard());
        onView(withId(R.id.btn_post_comment)).perform(click());

        onView(withText("Looking forward to it")).check(matches(isDisplayed()));
    }

    /**
     * Verifies that an organizer can post on their own event and the comment is pinned at the top.
     */
    @Test
    public void organizerCanPostPinnedCommentOnOwnEvent() {
        FakeCommentRepository repository = new FakeCommentRepository(Arrays.asList(
                buildComment("comment-1", "entrant-1", "Evan", "entrant", "Existing entrant comment", 200, false)
        ));

        launchFragment(buildTestState("organizer-1", "organizer", "Olivia"), repository);

        onView(withId(R.id.edit_comment_input)).perform(typeText("Organizer update"), closeSoftKeyboard());
        onView(withId(R.id.btn_post_comment)).perform(click());

        onView(nthChildOfRecyclerView(R.id.rv_event_comments, 0, R.id.tv_comment_author))
                .check(matches(withText("Olivia | Organizer")));
        onView(nthChildOfRecyclerView(R.id.rv_event_comments, 0, R.id.tv_comment_pin_badge))
                .check(matches(isDisplayed()));
        onView(withText("Organizer update")).check(matches(isDisplayed()));
    }

    /**
     * Verifies that an organizer can remove entrant comments on their own event.
     */
    @Test
    public void organizerCanRemoveEntrantComment() {
        FakeCommentRepository repository = new FakeCommentRepository(Arrays.asList(
                buildComment("comment-1", "entrant-1", "Evan", "entrant", "Needs moderation", 100, false)
        ));

        launchFragment(buildTestState("organizer-1", "organizer", "Olivia"), repository);

        onView(withId(R.id.btn_delete_comment)).perform(click());

        onView(withText(R.string.comment_removed_text)).check(matches(isDisplayed()));
        onView(withText(R.string.comment_removed_reason_organizer)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that admins cannot post comments but can remove violating comments.
     */
    @Test
    public void adminCanRemoveCommentButCannotPost() {
        FakeCommentRepository repository = new FakeCommentRepository(Arrays.asList(
                buildComment("comment-1", "entrant-1", "Evan", "entrant", "Bad comment", 100, false)
        ));

        launchFragment(buildTestState("admin-1", "admin", "Ada"), repository);

        onView(withId(R.id.comment_composer_container))
                .check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.text_comment_permissions))
                .check(matches(withText(R.string.comment_admin_read_only)));

        onView(withId(R.id.btn_delete_comment)).perform(click());

        onView(withText(R.string.comment_removed_text)).check(matches(isDisplayed()));
        onView(withText(R.string.comment_removed_reason_admin)).check(matches(isDisplayed()));
    }

    /**
     * Launches the fragment inside the main activity with injected in-memory test dependencies.
     *
     * @param state Synthetic event and viewer state.
     * @param repository Fake repository backing the visible thread.
     */
    private void launchFragment(EventOverviewFragment.TestState state, FakeCommentRepository repository) {
        activityRule.getScenario().onActivity(activity -> {
            EventOverviewFragment fragment = new EventOverviewFragment();
            Bundle args = new Bundle();
            args.putString("eventId", "test-event");
            fragment.setArguments(args);
            fragment.setTestState(state);
            fragment.setCommentRepositoryForTesting(repository);

            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitNow();
        });
    }

    /**
     * Returns a consistent event/viewer state for the comment flow tests.
     *
     * @param currentUserId Signed-in user id.
     * @param currentUserRole Signed-in user role.
     * @param currentUsername Signed-in username.
     * @return Synthetic test state.
     */
    private EventOverviewFragment.TestState buildTestState(
            String currentUserId,
            String currentUserRole,
            String currentUsername
    ) {
        ZonedDateTime now = ZonedDateTime.now();
        return new EventOverviewFragment.TestState(
                "Comment Test Event",
                "organizer-1",
                "Event used for UI tests",
                now.plusDays(1),
                now.plusDays(5),
                3,
                false,
                currentUserId,
                currentUserRole,
                currentUsername
        );
    }

    /**
     * Builds a test comment with deterministic ordering fields.
     *
     * @param commentId Comment document id.
     * @param authorId Comment author id.
     * @param authorName Comment author name.
     * @param authorRole Comment author role.
     * @param text Comment body.
     * @param seconds Timestamp seconds.
     * @param pinned Whether the comment should be pinned.
     * @return Materialized comment instance.
     */
    private EventComment buildComment(
            String commentId,
            String authorId,
            String authorName,
            String authorRole,
            String text,
            long seconds,
            boolean pinned
    ) {
        EventComment comment = new EventComment();
        comment.setCommentId(commentId);
        comment.setAuthorId(authorId);
        comment.setAuthorName(authorName);
        comment.setAuthorRole(authorRole);
        comment.setText(text);
        comment.setCreatedAt(new Timestamp(seconds, 0));
        comment.setStatus(EventComment.STATUS_ACTIVE);
        comment.setPinned(pinned);
        return comment;
    }

    /**
     * Returns a matcher that targets a child view inside a RecyclerView item at a fixed position.
     *
     * @param recyclerViewId RecyclerView resource id.
     * @param position Adapter position.
     * @param targetViewId Child view inside the row.
     * @return Matcher targeting that child view.
     */
    private static Matcher<View> nthChildOfRecyclerView(int recyclerViewId, int position, int targetViewId) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                RecyclerView recyclerView = view.getRootView().findViewById(recyclerViewId);
                if (recyclerView == null || recyclerView.getChildCount() <= position) {
                    return false;
                }

                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
                if (viewHolder == null) {
                    return false;
                }

                View targetView = viewHolder.itemView.findViewById(targetViewId);
                return view.equals(targetView);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("child view ")
                        .appendValue(targetViewId)
                        .appendText(" at position ")
                        .appendValue(position)
                        .appendText(" in recycler view ")
                        .appendValue(recyclerViewId);
            }
        };
    }

    /**
     * In-memory repository used to drive the fragment UI tests without Firebase.
     */
    private static class FakeCommentRepository extends EventCommentRepository {
        private final List<EventComment> comments = new ArrayList<>();
        private CommentListener activeListener;

        FakeCommentRepository(List<EventComment> initialComments) {
            super((FirebaseFirestore) null);
            comments.addAll(EventCommentRepository.sortComments(initialComments));
        }

        @Override
        public ListenerRegistration listenForComments(String eventId, CommentListener listener) {
            activeListener = listener;
            notifyListener();
            return () -> activeListener = null;
        }

        @Override
        public Task<Void> postComment(String eventId, EventComment comment) {
            if (comment.getCommentId() == null) {
                comment.setCommentId("comment-" + (comments.size() + 1));
            }
            List<EventComment> updatedComments = new ArrayList<>(comments);
            updatedComments.add(comment);
            comments.clear();
            comments.addAll(EventCommentRepository.sortComments(updatedComments));
            notifyListener();
            return Tasks.forResult(null);
        }

        @Override
        public Task<Void> removeComment(String eventId, String commentId, String removalReason) {
            for (EventComment comment : comments) {
                if (commentId.equals(comment.getCommentId())) {
                    comment.setStatus(EventComment.STATUS_REMOVED);
                    comment.setRemovedReason(removalReason);
                }
            }
            notifyListener();
            return Tasks.forResult(null);
        }

        private void notifyListener() {
            if (activeListener != null) {
                activeListener.onCommentsChanged(new ArrayList<>(comments));
            }
        }
    }
}
