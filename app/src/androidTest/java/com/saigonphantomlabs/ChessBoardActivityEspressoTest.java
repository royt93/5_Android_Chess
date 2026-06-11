package com.saigonphantomlabs;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.saigonphantomlabs.chess.R;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI test (instrumented — CẦN device/emulator) cho {@link ChessBoardActivity}:
 * khởi chạy chế độ PvP, kiểm tra bàn cờ + các nút hiển thị, nút Undo ẩn lúc đầu ván.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ChessBoardActivityEspressoTest {

    private Intent pvpIntent() {
        Intent i = new Intent(ApplicationProvider.getApplicationContext(), ChessBoardActivity.class);
        i.putExtra("IS_VS_AI", false);
        return i;
    }

    @Test
    public void board_isDisplayed() {
        try (ActivityScenario<ChessBoardActivity> scenario = ActivityScenario.launch(pvpIntent())) {
            onView(withId(R.id.boardImage)).check(matches(isDisplayed()));
            onView(withId(R.id.boardLayout)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void actionButtons_hiddenAtGameStart() {
        try (ActivityScenario<ChessBoardActivity> scenario = ActivityScenario.launch(pvpIntent())) {
            // Đầu ván: chưa có nước để Undo, ván chưa kết thúc nên nút "Play Again" cũng ẩn
            onView(withId(R.id.btnUndo)).check(matches(not(isDisplayed())));
            onView(withId(R.id.btnRestart)).check(matches(not(isDisplayed())));
        }
    }
}
