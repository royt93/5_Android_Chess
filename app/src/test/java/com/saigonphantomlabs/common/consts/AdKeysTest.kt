package com.saigonphantomlabs.common.consts

import android.app.Application
import com.saigonphantomlabs.feature.vip.VipKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric — verify VIP secret decode + single-secret contract + privacy URL. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class AdKeysTest {

    @Test fun vipSecret_decodesTo30dPlain() {
        assertEquals("9fA0q7eN!27cLx04@21993Y2u0I7#Q0", AdKeys.VIP_SECRET)
    }

    @Test fun vipSecret_equalsVip30dKey_singleSecretDesign() {
        // Lib single-secret: AdSdkConfig.vipKeySecret = key 30 ngày
        assertEquals(VipKeys.VIP_30D_KEY, AdKeys.VIP_SECRET)
    }

    @Test fun privacyPolicyUrl_isHttps() {
        assertTrue(AdKeys.PRIVACY_POLICY_URL.startsWith("https://"))
    }
}
