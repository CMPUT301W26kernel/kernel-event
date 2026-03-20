package com.example.eventlottery;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

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

    @Test
    public void testHomePageIsDisplayed() {
        // Use FragmentScenario to launch HomePageFragment directly, 
        // bypassing MainActivity's authentication routing.
        FragmentScenario.launchInContainer(HomePageFragment.class);

        onView(withId(R.id.list_view)).check(matches(isDisplayed()));
    }
}
