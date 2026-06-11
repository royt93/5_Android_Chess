package com.saigonphantomlabs.feature.vip

import kotlin.math.ceil

/**
 * Logic thuần (không phụ thuộc Android) cho VIP screen — tách ra để unit-test trực tiếp trên JVM.
 */
object VipMath {

    const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    /** Các thành phần countdown đã tách (để format ở UI layer). */
    data class Remaining(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

    /**
     * progress elapsed-semantic = (now − granted) / (expiry − granted) × 100, clamp [0,100].
     * total ≤ 0 (clock skew / đã hết hạn) → 100 (bar đầy = hết hạn).
     */
    fun elapsedProgress(grantedMs: Long, expiryMs: Long, nowMs: Long): Int {
        val total = expiryMs - grantedMs
        if (total <= 0L) return 100
        val elapsed = nowMs - grantedMs
        return ((elapsed.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    }

    /** Tách ms còn lại thành d/h/m/s. ms âm coi như 0. */
    fun remaining(ms: Long): Remaining {
        val totalSec = (if (ms > 0L) ms else 0L) / 1000L
        return Remaining(
            days = totalSec / 86_400L,
            hours = (totalSec % 86_400L) / 3_600L,
            minutes = (totalSec % 3_600L) / 60L,
            seconds = totalSec % 60L,
        )
    }

    /** Số ngày còn lại làm tròn lên (cho card "Còn X ngày"); không âm. */
    fun daysLeftCeil(expiryMs: Long, nowMs: Long): Int {
        val remain = expiryMs - nowMs
        if (remain <= 0L) return 0
        return ceil(remain.toDouble() / ONE_DAY_MS).toInt()
    }

    /**
     * Activate có làm KÉO DÀI thời hạn không? Lib ghi đè `expiry = now + days*86400000`
     * (không cộng dồn) → chỉ nên apply khi `newExpiry > currentExpiry` (hoặc chưa VIP).
     */
    fun isExtension(isCurrentlyActive: Boolean, currentExpiryMs: Long, newExpiryMs: Long): Boolean {
        if (!isCurrentlyActive) return true
        return newExpiryMs > currentExpiryMs
    }
}
