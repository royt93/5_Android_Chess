package com.saigonphantomlabs.feature.vip

import android.app.Application
import android.content.Context
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import com.saigonphantomlabs.chess.R
import com.roy.sdkadbmob.AdManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Widget test (Robolectric) cho [VipActivity] — state FREE (chưa VIP).
 * Dùng stub [Application] → AdManager không có config → isVipByKeyActive()=false,
 * loadRewarded() no-op (provider null). Verify UI free-state + extend-guard branch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class VipActivityRobolectricTest {

    @Before fun cleanVipState() {
        // Cô lập với VipActivityActiveStateTest (AdManager singleton có thể leak state)
        AdManager.clearVipByKey()
        ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun freeState_showsAcquireControls_hidesActiveCard() {
        Robolectric.buildActivity(VipActivity::class.java).setup().use { controller ->
            val a = controller.get()
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.btnWatchAd).visibility)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.tilKey).visibility)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.btnActivate).visibility)
            assertEquals(View.GONE, a.findViewById<View>(R.id.cardActiveVip).visibility)
            assertEquals(View.GONE, a.findViewById<View>(R.id.progressVip).visibility)
            assertEquals(View.GONE, a.findViewById<View>(R.id.tvCountdown).visibility)
            assertFalse(a.findViewById<View>(R.id.btnRevokeAll).isEnabled)
        }
    }

    @Test fun invalidKey_showsInvalidToast() {
        Robolectric.buildActivity(VipActivity::class.java).setup().use { controller ->
            val a = controller.get()
            a.findViewById<EditText>(R.id.etKey).setText("NOT-A-VALID-KEY")
            a.findViewById<View>(R.id.btnActivate).performClick()
            assertEquals(a.getString(R.string.vip_key_invalid), ShadowToast.getTextOfLatestToast())
            // free-state vẫn giữ (không có config nên không thể activate)
            assertEquals(View.GONE, a.findViewById<View>(R.id.cardActiveVip).visibility)
        }
    }

    @Test fun statusHeader_freeUser_label() {
        Robolectric.buildActivity(VipActivity::class.java).setup().use { controller ->
            val a = controller.get()
            val title = a.findViewById<android.widget.TextView>(R.id.tvStatusTitle)
            assertEquals(a.getString(R.string.vip_free_user), title.text.toString())
        }
    }
}
