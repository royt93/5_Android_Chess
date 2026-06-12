package com.saigonphantomlabs.feature.vip

import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.saigonphantomlabs.chess.R
import com.saigonphantomlabs.common.consts.AdKeys
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSdkConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * Widget test (Robolectric) cho [VipActivity] ở state ĐÃ VIP — seed VIP qua
 * [AdManager.activateVipByKey] (key = vipKeySecret = key 30 ngày). Phủ render active-state,
 * grace vs redeemed label, ẩn acquire-controls, và luồng revoke dialog.
 *
 * AdManager là singleton → dọn state ở [Before]/[After] để cô lập với các test class khác.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class VipActivityActiveStateTest {

    private fun ctx(): Context = ApplicationProvider.getApplicationContext()

    private fun cleanState() {
        AdManager.clearVipByKey()
        ctx().getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Before fun setup() {
        AdManager.setConfig(
            AdSdkConfig(isEnableAdmob = false, isDebug = true, vipKeySecret = AdKeys.VIP_SECRET)
        )
        cleanState()
    }

    @After fun tearDown() = cleanState()

    /** Seed VIP 30 ngày qua lib (key == vipKeySecret). */
    private fun activate30d() {
        assertTrue(AdManager.activateVipByKey(ctx(), AdKeys.VIP_SECRET, 30))
        assertTrue(AdManager.isVipByKeyActive())
    }

    @Test fun active_showsCountdownProgress_hidesAcquireControls() {
        activate30d()
        Robolectric.buildActivity(VipActivity::class.java).setup().use { c ->
            val a = c.get()
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.tvCountdown).visibility)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.progressVip).visibility)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.cardActiveVip).visibility)
            assertEquals(View.GONE, a.findViewById<View>(R.id.btnWatchAd).visibility)
            assertEquals(View.GONE, a.findViewById<View>(R.id.tilKey).visibility)
            assertEquals(View.GONE, a.findViewById<View>(R.id.btnActivate).visibility)
            assertTrue(a.findViewById<View>(R.id.btnRevokeAll).isEnabled)
            assertEquals(
                a.getString(R.string.vip_active),
                a.findViewById<TextView>(R.id.tvStatusTitle).text.toString(),
            )
        }
    }

    @Test fun graceEntry_showsGiftLabel() {
        // chưa redeem bao giờ (prefs đã clear) → grace entry
        activate30d()
        Robolectric.buildActivity(VipActivity::class.java).setup().use { c ->
            val a = c.get()
            assertEquals(
                a.getString(R.string.vip_entry_first_install),
                a.findViewById<TextView>(R.id.tvEntryLabel).text.toString(),
            )
        }
    }

    @Test fun redeemedEntry_showsDaysLabel() {
        val now = System.currentTimeMillis()
        VipPrefs(ctx()).markUserRedeemed()
        VipPrefs(ctx()).saveGrantedAtMs(now)   // để progress/days tính đúng 30
        activate30d()
        Robolectric.buildActivity(VipActivity::class.java).setup().use { c ->
            val a = c.get()
            assertEquals(
                a.getString(R.string.vip_entry_redeemed, 30),
                a.findViewById<TextView>(R.id.tvEntryLabel).text.toString(),
            )
        }
    }

    /**
     * REGRESSION (single-secret bug): gõ key 3-ngày vào field → Activate → phải VIP active.
     * Lib chỉ chấp nhận vipKeySecret(=30d) nên app PHẢI grant bằng VIP_SECRET + days(=3),
     * KHÔNG truyền VIP_3D_KEY thẳng vào lib (sẽ fail). Test này fail với code cũ.
     */
    @Test fun redeem3dKeyViaField_activatesVip() {
        Robolectric.buildActivity(VipActivity::class.java).setup().use { c ->
            val a = c.get()
            assertEquals(View.GONE, a.findViewById<View>(R.id.cardActiveVip).visibility)
            a.findViewById<EditText>(R.id.etKey).setText(VipKeys.VIP_3D_KEY)
            a.findViewById<View>(R.id.btnActivate).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(AdManager.isVipByKeyActive())
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.cardActiveVip).visibility)
        }
    }

    @Test fun redeem30dKeyViaField_activatesVip() {
        Robolectric.buildActivity(VipActivity::class.java).setup().use { c ->
            val a = c.get()
            a.findViewById<EditText>(R.id.etKey).setText(VipKeys.VIP_30D_KEY)
            a.findViewById<View>(R.id.btnActivate).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(AdManager.isVipByKeyActive())
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.cardActiveVip).visibility)
        }
    }

    /**
     * REGRESSION (watch-ad không work): bấm "Xem QC → 3 ngày VIP" phải grant VIP.
     * Không có rewarded provider trong test → showRewarded callback(earned=false) → app
     * vẫn grant 3 ngày (thiết kế AD.MD). Grant dùng applicationContext + persist, KHÔNG
     * gate theo _binding (bug cũ: callback fire khi activity recreate → grant bị nuốt).
     */
    @Test fun watchAd_grantsVip() {
        Robolectric.buildActivity(VipActivity::class.java).setup().use { c ->
            val a = c.get()
            assertEquals(View.GONE, a.findViewById<View>(R.id.cardActiveVip).visibility)
            a.findViewById<View>(R.id.btnWatchAd).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(AdManager.isVipByKeyActive())
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.cardActiveVip).visibility)
        }
    }

    @Test fun revoke_confirmDialog_returnsToFreeState() {
        activate30d()
        Robolectric.buildActivity(VipActivity::class.java).setup().use { c ->
            val a = c.get()
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.cardActiveVip).visibility)

            a.findViewById<View>(R.id.btnRevokeAll).performClick()
            shadowOf(Looper.getMainLooper()).idle()

            val dialog = ShadowDialog.getLatestDialog()
            assertNotNull(dialog)
            (dialog as androidx.appcompat.app.AlertDialog)
                .getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()

            // Sau revoke → quay về free-state
            assertEquals(View.GONE, a.findViewById<View>(R.id.cardActiveVip).visibility)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.btnWatchAd).visibility)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.tilKey).visibility)
        }
    }
}
