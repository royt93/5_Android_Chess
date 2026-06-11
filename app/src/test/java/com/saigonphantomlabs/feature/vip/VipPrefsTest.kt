package com.saigonphantomlabs.feature.vip

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric — verify VipPrefs persist grantedAt + userRedeemed (SharedPreferences). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class VipPrefsTest {

    private lateinit var prefs: VipPrefs

    @Before fun setup() {
        prefs = VipPrefs(ApplicationProvider.getApplicationContext())
        // sạch state giữa các test
        prefs.clearGrantedAtMs()
    }

    @Test fun default_grantedAt_isZero() {
        assertEquals(0L, prefs.getGrantedAtMs())
    }

    @Test fun saveAndGet_grantedAt() {
        prefs.saveGrantedAtMs(123_456_789L)
        assertEquals(123_456_789L, prefs.getGrantedAtMs())
    }

    @Test fun clear_resetsGrantedAt() {
        prefs.saveGrantedAtMs(999L)
        prefs.clearGrantedAtMs()
        assertEquals(0L, prefs.getGrantedAtMs())
    }

    @Test fun default_userRedeemed_isFalse() {
        assertFalse(prefs.userRedeemedAtLeastOnce())
    }

    @Test fun markUserRedeemed_persists() {
        prefs.markUserRedeemed()
        assertTrue(prefs.userRedeemedAtLeastOnce())
    }
}
