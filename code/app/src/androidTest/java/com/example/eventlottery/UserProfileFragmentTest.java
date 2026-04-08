package com.example.eventlottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static org.hamcrest.Matchers.not;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventlottery.profiles.UserProfileFragment;

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

    /**
     * Verifies User mode UI state: "See Event History" visible, "Notification Logs" hidden.
     */
    @Test
    public void testUI_UserModeInitialState() {
        // Default launch should be User mode
        FragmentScenario.launchInContainer(UserProfileFragment.class, null, R.style.Theme_EventLottery, Lifecycle.State.RESUMED);
        
        onView(withText("Your Profile")).check(matches(isDisplayed()));
        onView(withId(R.id.history_button)).check(matches(isDisplayed()));
        onView(withId(R.id.notification_logs_button)).check(matches(not(isDisplayed())));
    }

    /**
     * Verifies Admin mode UI state: "See Event History" hidden.
     */
    @Test
    public void testUI_AdminModeInitialState() {
        // Launch in Admin Mode using arguments
        Bundle args = new Bundle();
        args.putString("user_id", "test_uid");
        args.putBoolean("is_admin_mode", true);
        
        FragmentScenario.launchInContainer(UserProfileFragment.class, args, R.style.Theme_EventLottery, Lifecycle.State.RESUMED);
        
        onView(withText("Review Profile")).check(matches(isDisplayed()));
        onView(withId(R.id.history_button)).check(matches(not(isDisplayed())));
        
        // Verifies the "See Entrant List" button (for US 03.09.01) does not show up
        // when an Admin is viewing SOMEONE ELSE'S profile in Admin Mode.
        onView(withId(R.id.entrant_list_button)).check(matches(not(isDisplayed())));
    }

    /**
     * Verifies that the Delete Confirmation dialog has the correct title in Admin mode.
     */
    @Test
    public void testDeleteDialog_AdminModeTitle() {
        Bundle args = new Bundle();
        args.putString("user_id", "test_uid");
        args.putBoolean("is_admin_mode", true);
        
        FragmentScenario.launchInContainer(UserProfileFragment.class, args, R.style.Theme_EventLottery, Lifecycle.State.RESUMED);

        onView(withId(R.id.delete_button)).perform(click());

        onView(withText("Are you sure you'd like to delete this profile?"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
    }
}
