package com.saigonphantomlabs;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.saigonphantomlabs.chess.R;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI test (instrumented — CẦN device/emulator) cho {@link AchievementsActivity}:
 * màn hiển thị header tiến độ + list huy hiệu (mọi huy hiệu luôn render, khoá thì mờ).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AchievementsActivityEspressoTest {

    @Test
    public void progressHeader_isDisplayed() {
        try (ActivityScenario<AchievementsActivity> scenario =
                     ActivityScenario.launch(AchievementsActivity.class)) {
            onView(withId(R.id.tvProgress)).check(matches(isDisplayed()));
            onView(withId(R.id.achList)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void firstAchievementRow_titleIsDisplayed() {
        try (ActivityScenario<AchievementsActivity> scenario =
                     ActivityScenario.launch(AchievementsActivity.class)) {
            // Huy hiệu luôn render dù khoá → tiêu đề "First Victory" (default locale) phải hiện.
            onView(withText(R.string.ach_first_win_title)).check(matches(isDisplayed()));
        }
    }
}
