package com.saigonphantomlabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test thuần JVM (không Android) cho policy App Open của [MyApplication]:
 * [MyApplication.shouldSkipAppOpen] + [MyApplication.APP_OPEN_BLACKLIST].
 *
 * Phủ MỌI case của predicate: 3 activity blacklisted, các activity được phép,
 * null/empty, case-sensitivity, whitespace, và contract của tập blacklist.
 */
class AppOpenPolicyTest {

    // ── Blacklisted → phải SKIP (true) ───────────────────────────────
    @Test fun skip_splashActivity() {
        assertTrue(MyApplication.shouldSkipAppOpen("SplashActivity"))
    }

    @Test fun skip_chessBoardActivity() {
        assertTrue(MyApplication.shouldSkipAppOpen("ChessBoardActivity"))
    }

    @Test fun skip_vipActivity() {
        // Regression: VipActivity vừa thêm vào blacklist (App Open không đè rewarded).
        assertTrue(MyApplication.shouldSkipAppOpen("VipActivity"))
    }

    // ── Không blacklisted → phải SHOW (false) ────────────────────────
    @Test fun show_mainActivity() {
        assertFalse(MyApplication.shouldSkipAppOpen("MainActivity"))
    }

    @Test fun show_pawnPromotionActivity() {
        assertFalse(MyApplication.shouldSkipAppOpen("PawnPromotionActivity"))
    }

    @Test fun show_unknownActivity() {
        assertFalse(MyApplication.shouldSkipAppOpen("SomeOtherActivity"))
    }

    // ── Null / empty ─────────────────────────────────────────────────
    @Test fun skip_whenNull_failSafe() {
        // Không biết đang ở màn nào → fail-safe: không show.
        assertTrue(MyApplication.shouldSkipAppOpen(null))
    }

    @Test fun show_whenEmptyString() {
        // Chuỗi rỗng không nằm trong blacklist → không skip.
        assertFalse(MyApplication.shouldSkipAppOpen(""))
    }

    // ── Case-sensitivity & whitespace (simpleName phải khớp tuyệt đối) ─
    @Test fun show_lowercaseVariant_notMatched() {
        assertFalse(MyApplication.shouldSkipAppOpen("vipactivity"))
    }

    @Test fun show_uppercaseVariant_notMatched() {
        assertFalse(MyApplication.shouldSkipAppOpen("VIPACTIVITY"))
    }

    @Test fun show_fullyQualifiedName_notMatched() {
        // Predicate so simpleName, không phải FQN.
        assertFalse(MyApplication.shouldSkipAppOpen("com.saigonphantomlabs.feature.vip.VipActivity"))
    }

    @Test fun show_trailingWhitespace_notMatched() {
        assertFalse(MyApplication.shouldSkipAppOpen("VipActivity "))
    }

    // ── Contract của tập blacklist ───────────────────────────────────
    @Test fun blacklist_containsExactlyExpectedThree() {
        assertEquals(
            setOf("SplashActivity", "ChessBoardActivity", "VipActivity"),
            MyApplication.APP_OPEN_BLACKLIST,
        )
    }

    @Test fun blacklist_size_isThree() {
        assertEquals(3, MyApplication.APP_OPEN_BLACKLIST.size)
    }
}
