package com.example.eventlottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for UserProfileFragment.
 * These tests require an Android device or emulator to run.
 */
@RunWith(AndroidJUnit4.class)
public class UserProfileFragmentTest {

    /**
     * Verifies that the Delete Confirmation dialog appears when the delete button is clicked
     * and is dismissed when the cancel button is clicked.
     */
    @Test
    public void testDeleteDialog_CancelButtonDismissesDialog() {
        // Launch the fragment in a mock container with the app theme
        FragmentScenario<UserProfileFragment> scenario = FragmentScenario.launchInContainer(
                UserProfileFragment.class, null, R.style.Theme_EventLottery, Lifecycle.State.RESUMED);

        // Click the delete button in the fragment
        onView(withId(R.id.delete_button)).perform(click());

        // Verify the dialog's title/text or one of its buttons is displayed
        onView(withText("Are you sure you'd like to delete your profile?"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
        onView(withId(R.id.dialog_cancel_button))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));

        // Click the cancel button in the dialog
        onView(withId(R.id.dialog_cancel_button))
                .inRoot(isDialog())
                .perform(click());

        // Verify the dialog is no longer displayed
        onView(withText("Are you sure you'd like to delete your profile?")).check(doesNotExist());
    }

    /**
     * Verifies that the Done button is displayed.
     */
    @Test
    public void testUI_DoneButtonIsDisplayed() {
        FragmentScenario.launchInContainer(UserProfileFragment.class, null, R.style.Theme_EventLottery, Lifecycle.State.RESUMED);
        onView(withId(R.id.done_button)).check(matches(isDisplayed()));
    }
}
