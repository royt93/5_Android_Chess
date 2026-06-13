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
 * Espresso UI test (instrumented — CẦN device/emulator) cho {@link PuzzlesActivity}:
 * màn hiển thị header tiến độ + list câu đố (kể cả câu mate-in-2 duy nhất).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PuzzlesActivityEspressoTest {

    @Test
    public void header_andList_areDisplayed() {
        try (ActivityScenario<PuzzlesActivity> scenario =
                     ActivityScenario.launch(PuzzlesActivity.class)) {
            onView(withId(R.id.tvProgress)).check(matches(isDisplayed()));
            onView(withId(R.id.puzzleList)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void mateInTwoSubtitle_isDisplayed() {
        // "Mate in 2" chỉ xuất hiện ở câu mate-in-2 duy nhất → match không nhập nhằng.
        try (ActivityScenario<PuzzlesActivity> scenario =
                     ActivityScenario.launch(PuzzlesActivity.class)) {
            onView(withText(R.string.puzzle_mate_in_2)).check(matches(isDisplayed()));
        }
    }
}
