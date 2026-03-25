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
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility;

import com.example.eventlottery.core.MainActivity;
import com.example.eventlottery.waitinglist.WaitlistManagementFragment;

/**
 * Android Instrumented intent tests for the WaitlistManagementFragment UI.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitlistManagementUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testWaitlistManagementFragmentIsDisplayed() {
        // Because WaitlistManagementFragment is a DialogFragment that needs an event ID, 
        // we programmatically launch it using the test activity's FragmentManager.
        
        activityRule.getScenario().onActivity(activity -> {
            WaitlistManagementFragment fragment = WaitlistManagementFragment.newInstance("testEventId");
            fragment.show(activity.getSupportFragmentManager(), "WaitlistManagement");
        });
        
        // Wait for the fragment to appear and ensure the main components are visible
        // Use withEffectiveVisibility to ensure the view is in the layout hierarchy
        // even if it has no data and zero height.
        onView(withId(R.id.tv_title)).check(matches(isDisplayed()));
        onView(withId(R.id.rv_entrants)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withId(R.id.btn_draw_lottery)).check(matches(isDisplayed()));
    }
}
