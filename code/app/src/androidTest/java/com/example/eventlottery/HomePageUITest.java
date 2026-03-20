package com.example.eventlottery;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Android Instrumented intent tests for the HomePageFragment UI.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HomePageUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testHomePageIsDisplayed() {
        // This test ensures that the Home Page (with the ListView) is correctly displayed
        // within the main activity.
        onView(withId(R.id.list_view)).check(matches(isDisplayed()));
    }
}
