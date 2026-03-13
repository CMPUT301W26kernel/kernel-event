/**
 * Create Event Fragment Test
 * Contains UI tests for the CreateEventFragment
 * Last Modified: 2026-03-13 by Grace MacKenzie
 *<p>
 *     Notes:
 *     - TODO: Fix the tests. They currently cannot see the toasts.
 *          -> Option 1. Use Espresso Toast Matcher
 *          -> Option 2.Stop using Toasts for validation and use something easier to test instead.
 *</p>
 *
 * @author Grace MacKenzie
 * @since 2026-03-12
 */
package com.example.eventlottery;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateEventFragmentTest {

    // ATTRIBUTES

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference eventsRef = db.collection("events");

    // CREATE MODE TESTS

    /**
     * Tests that the validation in CREATE mode catches invalid user entries for the event Title.
     */
    @Test
    public void testCreateModeEmptyTitle() {
        FragmentScenario<CreateEventFragment> scenario = launchCreateMode();

        // Check Empty Title
        onView(withId(R.id.edit_event_title)).perform(ViewActions.typeText(""));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_empty_title)).check(matches(isDisplayed()));
    }

    /**
     * Tests that the validation in CREATE mode catches invalid user entries for the event Description.
     */
    @Test
    public void testCreateModeEmptyDescription() {
        FragmentScenario<CreateEventFragment> scenario = launchCreateMode();

        // Check Empty Description
        onView(withId(R.id.edit_event_description)).perform(ViewActions.typeText(""));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_empty_description)).check(matches(isDisplayed()));
    }

    /**
     * Tests that the validation in CREATE mode catches invalid user entries for Registration Open
     * and Close dates.
     */
    @Test
    public void testCreateModeInvalidDate() {
        FragmentScenario<CreateEventFragment> scenario = launchCreateMode();

        // Check empty date
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_date_format)).check(matches(isDisplayed()));

        // Initialize some usable dates (so that these tests run regardless of the current date)
        ZonedDateTime current = ZonedDateTime.now();
        ZonedDateTime earlier = current.minusYears(10);
        ZonedDateTime later = current.plusYears(10);

        // Check impossible date
        setRegOpenDate(later.getYear(), 99, 99);
        setRegCloseDate(later.getYear(), later.getMonthValue(), later.getDayOfMonth());
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_impossible_date)).check(matches(isDisplayed()));

        // Check registration is equal to registration close
        setRegOpenDate(later.getYear(), later.getMonthValue(), later.getDayOfMonth());
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_open_date_equals_close_date)).check(matches(isDisplayed()));

        // Check registration open is not before registration close
        setRegOpenDate(later.getYear() + 10, later.getMonthValue(), later.getDayOfMonth());
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_close_date_before_open_date)).check(matches(isDisplayed()));

        // Check invalid date period (registration opens before current day)
        setRegOpenDate(earlier.getYear(), earlier.getMonthValue(), earlier.getDayOfMonth());
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_registration_period)).check(matches(isDisplayed()));
    }

    /**
     * Tests that the validation in CREATE mode catches invalid user entries for Waiting List Capacity.
     */
    @Test
    public void testCreateModeInvalidCapacity() {
        FragmentScenario<CreateEventFragment> scenario = launchCreateMode();

        // Enter valid dates to prevent date errors
        ZonedDateTime current = ZonedDateTime.now();
        ZonedDateTime validOpenDate = current.plusDays(1);
        ZonedDateTime validCloseDate = current.plusDays(10);
        setRegOpenDate(validOpenDate.getYear(), validOpenDate.getMonthValue(), validOpenDate.getDayOfMonth());
        setRegCloseDate(validCloseDate.getYear(), validCloseDate.getMonthValue(), validCloseDate.getDayOfMonth());

        // Check 0 error
        onView(withId(R.id.edit_capacity)).perform(ViewActions.typeText("0"));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_capacity)).check(matches(isDisplayed()));

        /*
            Note that the edit_capacity EditText view has the input type set to "number",
            so the following checks are somewhat redundant since alphabetical and symbolic
            characters are not accepted by the view when entered by a user.
         */

        // Check alphabetical characters
        onView(withId(R.id.edit_capacity)).perform(ViewActions.typeText("abc"));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_capacity)).check(matches(isDisplayed()));

        // Check symbolic characters
        onView(withId(R.id.edit_capacity)).perform(ViewActions.typeText("*$(%$#"));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_capacity)).check(matches(isDisplayed()));

        // Check negative capacity
        onView(withId(R.id.edit_capacity)).perform(ViewActions.typeText("-7"));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_capacity)).check(matches(isDisplayed()));
    }

    // EDIT MODE TESTS

    /**
     * Tests that the validation in CREATE mode catches invalid user entries for the event Title.
     */
    @Test
    public void testEditModeEmptyTitle() {
        Event testEvent = generateTestEvent();
        addFirebaseEvent(testEvent);
        FragmentScenario<CreateEventFragment> scenario = launchEditMode(testEvent.getEventId());

        // Check Empty Title
        onView(withId(R.id.edit_event_title)).perform(ViewActions.typeText(""));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_empty_title)).check(matches(isDisplayed()));

        deleteFirebaseEvent();
    }

    /**
     * Tests that the validation in CREATE mode catches invalid user entries for the event Description.
     */
    @Test
    public void testEditModeEmptyDescription() {
        Event testEvent = generateTestEvent();
        addFirebaseEvent(testEvent);
        FragmentScenario<CreateEventFragment> scenario = launchCreateMode();

        // Check Empty Description
        onView(withId(R.id.edit_event_description)).perform(ViewActions.typeText(""));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_empty_description)).check(matches(isDisplayed()));

        //
        deleteFirebaseEvent();
    }

    /**
     * Tests that the validation in EDIT mode catches invalid user entries for Registration Open
     * and Close dates.
     */
    @Test
    public void testEditModeInvalidDate() {
        Event testEvent = generateTestEvent();
        addFirebaseEvent(testEvent);
        FragmentScenario<CreateEventFragment> scenario = launchEditMode(testEvent.getEventId());

        // Check empty date
        onView(withId(R.id.edit_reg_open_year)).perform(ViewActions.typeText(""));
        onView(withId(R.id.edit_reg_open_month)).perform(ViewActions.typeText(""));
        onView(withId(R.id.edit_reg_open_day)).perform(ViewActions.typeText(""));

        onView(withId(R.id.edit_reg_close_year)).perform(ViewActions.typeText(""));
        onView(withId(R.id.edit_reg_close_month)).perform(ViewActions.typeText(""));
        onView(withId(R.id.edit_reg_close_day)).perform(ViewActions.typeText(""));

        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_date_format)).check(matches(isDisplayed()));

        // Initialize some usable dates (so that these tests run regardless of the current date)
        ZonedDateTime current = ZonedDateTime.now();
        ZonedDateTime earlier = current.minusYears(10);
        ZonedDateTime later = current.plusYears(10);

        // Check impossible date
        setRegOpenDate(later.getYear(), 99, 99);
        setRegCloseDate(later.getYear(), later.getMonthValue(), later.getDayOfMonth());
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_impossible_date)).check(matches(isDisplayed()));

        // Check registration is equal to registration close
        setRegOpenDate(later.getYear(), later.getMonthValue(), later.getDayOfMonth());
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_open_date_equals_close_date)).check(matches(isDisplayed()));

        // Check registration open is not before registration close
        setRegOpenDate(later.getYear() + 10, later.getMonthValue(), later.getDayOfMonth());
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_close_date_before_open_date)).check(matches(isDisplayed()));

        // Check invalid date period (registration opens before current day)
        setRegOpenDate(earlier.getYear(), earlier.getMonthValue(), earlier.getDayOfMonth());
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_registration_period)).check(matches(isDisplayed()));

        deleteFirebaseEvent();
    }

    /**
     * Tests that the validation in EDIT mode catches invalid user entries for Waiting List Capacity.
     */
    @Test
    public void testEditModeInvalidCapacity() {
        Event testEvent = generateTestEvent();
        addFirebaseEvent(testEvent);
        FragmentScenario<CreateEventFragment> scenario = launchEditMode(testEvent.getEventId());

        // Check 0 error
        onView(withId(R.id.edit_capacity)).perform(ViewActions.typeText("0"));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_capacity)).check(matches(isDisplayed()));

        /*
            Note that the edit_capacity EditText view has the input type set to "number",
            so the following checks are somewhat redundant since alphabetical and symbolic
            characters are not accepted by the view when entered by a user.
         */

        // Check alphabetical characters
        onView(withId(R.id.edit_capacity)).perform(ViewActions.typeText("abc"));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_capacity)).check(matches(isDisplayed()));

        // Check symbolic characters
        onView(withId(R.id.edit_capacity)).perform(ViewActions.typeText("*$(%$#"));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_capacity)).check(matches(isDisplayed()));

        // Check negative capacity
        onView(withId(R.id.edit_capacity)).perform(ViewActions.typeText("-7"));
        onView(withId(R.id.confirm_button)).perform(click());
        onView(withText(R.string.error_invalid_capacity)).check(matches(isDisplayed()));

        deleteFirebaseEvent();
    }

    /**
     * Removes the potential test event left over from Firestore
     */
    @After
    public void cleanup() {
        deleteFirebaseEvent();
    }

    // HELPER FUNCTIONS

    /**
     * Launches a FragmentScenario in Create mode on which to run espresso tests on.
     * This method was made using the help of Microsoft, Copilot using various prompts.
     *
     * @return A FragmentScenario on which to run tests
     */
    private FragmentScenario<CreateEventFragment> launchCreateMode() {
        CreateEventFragment fragment = CreateEventFragment.newInstanceCreateMode("test organizer id");
        Bundle args = fragment.getArguments();

        return FragmentScenario.launchInContainer(
                CreateEventFragment.class,
                args,
                R.style.Theme_EventLottery,
                Lifecycle.State.RESUMED
        );
    }

    /**
     * Launches a FragmentScenario in Edit mode on which to run espresso tests on
     * This method was made using the help of Microsoft, Copilot using various prompts.
     * @param eventId a mock event id required for launching CreateEventFragment in Edit mode
     * @return A FragmentScenario on which to run tests
     */
    private FragmentScenario<CreateEventFragment> launchEditMode(String eventId) {
        CreateEventFragment fragment = CreateEventFragment.newInstanceEditMode(eventId);
        Bundle args = fragment.getArguments();

        return FragmentScenario.launchInContainer(
                CreateEventFragment.class,
                args,
                R.style.Theme_EventLottery,
                Lifecycle.State.RESUMED
        );
    }

    /**
     * Returns a test Event object with valid fields
     * @return a test Event with valid fields
     */
    private Event generateTestEvent() {
        return new Event(
                "Test Event",
                "Test Description",
                "test organizer id",
                ZonedDateTime.now().plusDays(1),
                ZonedDateTime.now().plusDays(10),
                null
        );
    }

    /**
     * Adds a new test Event to the Firestore database with a specific test eventId
     * @param event The Event to add to Firestore
     */
    private void addFirebaseEvent(Event event) {
        DocumentReference docRef = eventsRef.document("TestEventId");
        String generatedId = docRef.getId();
        event.setEventId(generatedId);

        try {
            // This blocks until Firestore has actually written the document
            Tasks.await(docRef.set(event));
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Deletes a test Event from Firestore
     */
    private void deleteFirebaseEvent() {
        DocumentReference docRef = eventsRef.document("TestEventId");
        docRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("DELETE", "City deleted from Firestore"))
                .addOnFailureListener(e -> Log.e("DELETE", "Error deleting city", e));
    }

    /**
     * Small helper to quickly set a Registration Open date
     * @param year the 4-digit year
     * @param month the 2-digit month
     * @param day the 2-digit day within the given month
     */
    private void setRegOpenDate(int year, int month, int day) {
        onView(withId(R.id.edit_reg_open_year)).perform(ViewActions.typeText(String.format("%04d", year)));
        onView(withId(R.id.edit_reg_open_month)).perform(ViewActions.typeText(String.format("%02d", month)));
        onView(withId(R.id.edit_reg_open_day)).perform(ViewActions.typeText(String.format("%02d", day)));
    }

    /**
     * Small helper to quickly set a Registration Close date
     * @param year the 4-digit year
     * @param month the 2-digit month
     * @param day the 2-digit day within the given month
     */
    private void setRegCloseDate(int year, int month, int day) {
        onView(withId(R.id.edit_reg_close_year)).perform(ViewActions.typeText(String.format("%04d", year)));
        onView(withId(R.id.edit_reg_close_month)).perform(ViewActions.typeText(String.format("%02d", month)));
        onView(withId(R.id.edit_reg_close_day)).perform(ViewActions.typeText(String.format("%02d", day)));
    }
}
