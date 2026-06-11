package com.saigonphantomlabs.feature.vip

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saigonphantomlabs.chess.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test (Espresso, cần device/emulator: `./gradlew connectedDebugAndroidTest`).
 * Khởi chạy thật [VipActivity] qua MyApplication → verify UI end-to-end.
 */
@RunWith(AndroidJUnit4::class)
class VipActivityEspressoTest {

    @Test fun launch_showsAcquireControls() {
        ActivityScenario.launch(VipActivity::class.java).use {
            onView(withId(R.id.btnWatchAd)).check(matches(isDisplayed()))
            onView(withId(R.id.tilKey)).check(matches(isDisplayed()))
            onView(withId(R.id.btnActivate)).check(matches(isDisplayed()))
        }
    }

    @Test fun invalidKey_screenStaysOnVip() {
        ActivityScenario.launch(VipActivity::class.java).use {
            onView(withId(R.id.etKey)).perform(typeText("INVALID-CODE"), closeSoftKeyboard())
            onView(withId(R.id.btnActivate)).perform(click())
            // Không kích hoạt được → màn VIP vẫn còn, acquire controls vẫn hiển thị
            onView(withId(R.id.btnActivate)).check(matches(isDisplayed()))
            onView(withId(R.id.btnWatchAd)).check(matches(isDisplayed()))
        }
    }

    @Test fun privacyFooter_isVisible() {
        ActivityScenario.launch(VipActivity::class.java).use {
            onView(withId(R.id.tvPrivacy)).check(matches(isDisplayed()))
            onView(withId(R.id.tvPrivacyOptions)).check(matches(isDisplayed()))
        }
    }
}
