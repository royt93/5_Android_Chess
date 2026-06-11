package com.saigonphantomlabs.feature.vip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test thuần JVM (không Android) cho [VipMath] — phủ mọi edge case của
 * progress elapsed-semantic, countdown decompose, days-left ceil, extend-guard.
 */
class VipMathTest {

    private val DAY = VipMath.ONE_DAY_MS

    // ── elapsedProgress ──────────────────────────────────────────────
    @Test fun progress_atGrant_isZero() {
        assertEquals(0, VipMath.elapsedProgress(grantedMs = 1_000L, expiryMs = 1_000L + DAY, nowMs = 1_000L))
    }

    @Test fun progress_atExpiry_is100() {
        assertEquals(100, VipMath.elapsedProgress(0L, DAY, DAY))
    }

    @Test fun progress_midpoint_is50() {
        assertEquals(50, VipMath.elapsedProgress(0L, DAY, DAY / 2))
    }

    @Test fun progress_quarter_is25() {
        assertEquals(25, VipMath.elapsedProgress(0L, 4 * DAY, DAY))
    }

    @Test fun progress_beforeGrant_clampsTo0() {
        assertEquals(0, VipMath.elapsedProgress(DAY, 2 * DAY, 0L))
    }

    @Test fun progress_afterExpiry_clampsTo100() {
        assertEquals(100, VipMath.elapsedProgress(0L, DAY, 3 * DAY))
    }

    @Test fun progress_zeroTotal_returns100() {
        assertEquals(100, VipMath.elapsedProgress(5_000L, 5_000L, 5_000L))
    }

    @Test fun progress_negativeTotal_clockSkew_returns100() {
        assertEquals(100, VipMath.elapsedProgress(grantedMs = 10_000L, expiryMs = 5_000L, nowMs = 7_000L))
    }

    // ── remaining ────────────────────────────────────────────────────
    @Test fun remaining_exact_1d2h3m4s() {
        val ms = (1 * 86_400L + 2 * 3_600L + 3 * 60L + 4L) * 1000L
        val r = VipMath.remaining(ms)
        assertEquals(1L, r.days); assertEquals(2L, r.hours); assertEquals(3L, r.minutes); assertEquals(4L, r.seconds)
    }

    @Test fun remaining_zero_allZero() {
        val r = VipMath.remaining(0L)
        assertEquals(0L, r.days); assertEquals(0L, r.hours); assertEquals(0L, r.minutes); assertEquals(0L, r.seconds)
    }

    @Test fun remaining_negative_treatedAsZero() {
        val r = VipMath.remaining(-123_456L)
        assertEquals(0L, r.days); assertEquals(0L, r.seconds)
    }

    @Test fun remaining_subSecond_floorsToZero() {
        val r = VipMath.remaining(999L)
        assertEquals(0L, r.seconds)
    }

    // ── daysLeftCeil ─────────────────────────────────────────────────
    @Test fun daysLeft_exact3Days_is3() {
        assertEquals(3, VipMath.daysLeftCeil(expiryMs = 3 * DAY, nowMs = 0L))
    }

    @Test fun daysLeft_partial_ceils() {
        assertEquals(3, VipMath.daysLeftCeil(expiryMs = (2 * DAY) + 1L, nowMs = 0L))
    }

    @Test fun daysLeft_oneMs_is1() {
        assertEquals(1, VipMath.daysLeftCeil(expiryMs = 1L, nowMs = 0L))
    }

    @Test fun daysLeft_expired_is0() {
        assertEquals(0, VipMath.daysLeftCeil(expiryMs = 0L, nowMs = DAY))
    }

    @Test fun daysLeft_atExpiry_is0() {
        assertEquals(0, VipMath.daysLeftCeil(expiryMs = DAY, nowMs = DAY))
    }

    // ── isExtension (extend-guard) ───────────────────────────────────
    @Test fun extension_whenNotActive_alwaysTrue() {
        assertTrue(VipMath.isExtension(isCurrentlyActive = false, currentExpiryMs = 999_999L, newExpiryMs = 1L))
    }

    @Test fun extension_activeAndLonger_true() {
        assertTrue(VipMath.isExtension(true, currentExpiryMs = DAY, newExpiryMs = 2 * DAY))
    }

    @Test fun extension_activeAndShorter_false() {
        assertFalse(VipMath.isExtension(true, currentExpiryMs = 30 * DAY, newExpiryMs = 3 * DAY))
    }

    @Test fun extension_activeAndEqual_false() {
        assertFalse(VipMath.isExtension(true, currentExpiryMs = DAY, newExpiryMs = DAY))
    }
}
