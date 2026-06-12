package com.saigonphantomlabs

import android.app.Application
import com.saigonphantomlabs.feature.vip.VipActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Widget test (Robolectric) — RENAME-GUARD cho App Open blacklist.
 *
 * Blacklist của [MyApplication] so khớp bằng [Class.getSimpleName] (string literal),
 * còn observer thực tế lấy tên qua `activity.javaClass.simpleName`. Test này nối
 * predicate với CHÍNH các activity class thật: nếu ai đó đổi tên class (vd
 * VipActivity → PremiumActivity) mà quên cập nhật blacklist, test fail ngay —
 * bảo vệ khỏi việc App Open âm thầm show lại trên màn lẽ ra phải skip.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class AppOpenPolicyRobolectricTest {

    // ── Các activity blacklisted: simpleName runtime → phải SKIP ──────
    @Test fun splashActivity_realSimpleName_isSkipped() {
        assertTrue(MyApplication.shouldSkipAppOpen(SplashActivity::class.java.simpleName))
    }

    @Test fun chessBoardActivity_realSimpleName_isSkipped() {
        assertTrue(MyApplication.shouldSkipAppOpen(ChessBoardActivity::class.java.simpleName))
    }

    @Test fun vipActivity_realSimpleName_isSkipped() {
        assertTrue(MyApplication.shouldSkipAppOpen(VipActivity::class.java.simpleName))
    }

    // ── Các activity được phép: simpleName runtime → phải SHOW ────────
    @Test fun mainActivity_realSimpleName_isNotSkipped() {
        assertFalse(MyApplication.shouldSkipAppOpen(MainActivity::class.java.simpleName))
    }

    @Test fun pawnPromotionActivity_realSimpleName_isNotSkipped() {
        assertFalse(MyApplication.shouldSkipAppOpen(PawnPromotionActivity::class.java.simpleName))
    }

    // ── Mọi entry trong blacklist phải ứng với 1 class thật (không chết) ─
    @Test fun everyBlacklistedEntry_mapsToARealActivityClass() {
        val realSimpleNames = setOf(
            SplashActivity::class.java.simpleName,
            ChessBoardActivity::class.java.simpleName,
            VipActivity::class.java.simpleName,
        )
        // Không còn entry "mồ côi" (class đã đổi tên/xoá nhưng string còn sót).
        assertTrue(realSimpleNames.containsAll(MyApplication.APP_OPEN_BLACKLIST))
        assertTrue(MyApplication.APP_OPEN_BLACKLIST.containsAll(realSimpleNames))
    }
}
