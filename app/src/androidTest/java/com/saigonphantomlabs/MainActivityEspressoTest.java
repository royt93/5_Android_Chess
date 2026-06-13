package com.saigonphantomlabs;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.app.Activity;
import android.app.Instrumentation;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.saigonphantomlabs.chess.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI test (instrumented — CẦN emulator/thiết bị: ./gradlew connectedDebugAndroidTest)
 * cho {@link MainActivity}: hiển thị menu + điều hướng sang ChessBoardActivity với extra đúng.
 *
 * Dùng {@link Intents#intending} để CHẶN việc thật sự mở ChessBoardActivity (tránh khởi tạo
 * quảng cáo / Storage) — chỉ xác minh intent phát ra.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityEspressoTest {

    @Rule
    public ActivityScenarioRule<MainActivity> rule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        Intents.init();
        // Chặn mọi intent mở ChessBoardActivity, trả về kết quả rỗng (không mở thật)
        intending(hasComponent(ChessBoardActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void menuButtons_areDisplayed() {
        onView(withId(R.id.btnPlayPvP)).check(matches(isDisplayed()));
        onView(withId(R.id.btnPlayPvE)).check(matches(isDisplayed()));
        onView(withId(R.id.btnStats)).check(matches(isDisplayed()));
        onView(withId(R.id.btnRules)).check(matches(isDisplayed()));
    }

    @Test
    public void clickPvP_launchesChessBoardAsHumanVsHuman() {
        onView(withId(R.id.btnPlayPvP)).perform(click());
        // PvP nay hiện dialog chọn thời gian trước → chọn "No clock" để khởi ván
        onView(withText(R.string.time_off)).perform(click());
        intended(allOf(
                hasComponent(ChessBoardActivity.class.getName()),
                hasExtra("IS_VS_AI", false)));
    }
}
