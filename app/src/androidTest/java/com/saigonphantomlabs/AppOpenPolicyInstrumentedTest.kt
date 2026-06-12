package com.saigonphantomlabs

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saigonphantomlabs.feature.vip.VipActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test (instrumented, cần device/emulator: `./gradlew connectedDebugAndroidTest`).
 *
 * Xác minh App Open blacklist contract trên RUNTIME THẬT của thiết bị: lấy
 * `javaClass.simpleName` đúng như observer trong [MyApplication] dùng lúc resume,
 * rồi đối chiếu [MyApplication.shouldSkipAppOpen]. Bắt được sai lệch tên class
 * ở môi trường thật (vd nếu cấu hình build/keep-rule đổi simpleName).
 *
 * Không launch activity để tránh kích hoạt App Open/consent SDK — chỉ kiểm tra
 * hợp đồng tên-lớp ↔ policy.
 */
@RunWith(AndroidJUnit4::class)
class AppOpenPolicyInstrumentedTest {

    @Test fun blacklistedActivities_areSkipped_onDevice() {
        assertTrue(MyApplication.shouldSkipAppOpen(SplashActivity::class.java.simpleName))
        assertTrue(MyApplication.shouldSkipAppOpen(ChessBoardActivity::class.java.simpleName))
        assertTrue(MyApplication.shouldSkipAppOpen(VipActivity::class.java.simpleName))
    }

    @Test fun allowedActivities_areShown_onDevice() {
        assertFalse(MyApplication.shouldSkipAppOpen(MainActivity::class.java.simpleName))
        assertFalse(MyApplication.shouldSkipAppOpen(PawnPromotionActivity::class.java.simpleName))
    }

    @Test fun runtimeSimpleNames_matchBlacklistLiterals() {
        // simpleName thật trên device phải đúng bằng string literal trong blacklist.
        assertEquals("SplashActivity", SplashActivity::class.java.simpleName)
        assertEquals("ChessBoardActivity", ChessBoardActivity::class.java.simpleName)
        assertEquals("VipActivity", VipActivity::class.java.simpleName)
        assertTrue(MyApplication.APP_OPEN_BLACKLIST.contains(SplashActivity::class.java.simpleName))
        assertTrue(MyApplication.APP_OPEN_BLACKLIST.contains(ChessBoardActivity::class.java.simpleName))
        assertTrue(MyApplication.APP_OPEN_BLACKLIST.contains(VipActivity::class.java.simpleName))
    }
}
