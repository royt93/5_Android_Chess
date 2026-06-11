package com.saigonphantomlabs.feature.vip

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric (cần [android.util.Base64] thật) — verify whitelist key + decode.
 * Dùng stub [Application] để KHÔNG chạy MyApplication.setupAd (tránh init AppLovin trong test).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class VipKeysTest {

    @Test fun decode_30dKey_matchesPlain() {
        assertEquals("9fA0q7eN!27cLx04@21993Y2u0I7#Q0", VipKeys.VIP_30D_KEY)
    }

    @Test fun decode_3dKey_matchesPlain() {
        assertEquals("eQ7@93L0f!2Y2707xN04021993u0I#2aK", VipKeys.VIP_3D_KEY)
    }

    @Test fun lookup_30dKey_returns30() {
        assertEquals(30, VipKeys.lookupDays(VipKeys.VIP_30D_KEY))
    }

    @Test fun lookup_3dKey_returns3() {
        assertEquals(3, VipKeys.lookupDays(VipKeys.VIP_3D_KEY))
    }

    @Test fun lookup_trimsWhitespace() {
        assertEquals(30, VipKeys.lookupDays("  " + VipKeys.VIP_30D_KEY + "  "))
    }

    @Test fun lookup_invalid_returnsNull() {
        assertNull(VipKeys.lookupDays("NOT-A-KEY"))
    }

    @Test fun lookup_empty_returnsNull() {
        assertNull(VipKeys.lookupDays(""))
    }

    @Test fun lookup_caseSensitive_returnsNull() {
        assertNull(VipKeys.lookupDays(VipKeys.VIP_30D_KEY.lowercase()))
    }
}
