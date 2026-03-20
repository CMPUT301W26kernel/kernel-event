package com.example.eventlottery;

import static org.junit.Assert.assertNotNull;

import android.view.View;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Test for HomePageFragment basic view inflation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class HomePageFragmentTest {

    @Test
    public void testHomePageFragmentInflatesListViewLayout() {
        FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        HomePageFragment fragment = new HomePageFragment();

        View view = fragment.onCreateView(
                activity.getLayoutInflater(),
                new FrameLayout(activity),
                null
        );

        assertNotNull("Fragment should inflate a view", view);
        assertNotNull("List view should be present in the inflated layout", view.findViewById(R.id.list_view));
    }
}
