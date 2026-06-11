package com.saigonphantomlabs.feature.vip

import android.util.Base64

/**
 * Whitelist VIP key → số ngày. Plain key KHÔNG hardcode trong source — chỉ lưu Base64,
 * decode runtime (mức che giấu đã thống nhất, xem doc/AD.MD §4).
 *
 * - 30 ngày = key dùng chung với [com.roy.sdkadbmob.AdSdkConfig.vipKeySecret] (lib single-secret).
 * - 3 ngày  = dùng cho nút "Xem quảng cáo → 3 ngày VIP" (rewarded grant).
 */
object VipKeys {

    // Base64(NO_WRAP) của plain key 30-ngày (plain không ghi ở source — xem doc/AD.MD §4)
    private const val VIP_30D_B64 = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA=="

    // Base64(NO_WRAP) của plain key 3-ngày
    private const val VIP_3D_B64 = "ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL"

    val VIP_30D_KEY: String by lazy {
        String(Base64.decode(VIP_30D_B64, Base64.NO_WRAP))
    }
    val VIP_3D_KEY: String by lazy {
        String(Base64.decode(VIP_3D_B64, Base64.NO_WRAP))
    }

    /** Plain key (đã decode) → số ngày. Dùng để validate input từ user. */
    private val KEY_TO_DAYS: Map<String, Int> by lazy {
        mapOf(
            VIP_30D_KEY to 30,
            VIP_3D_KEY to 3,
        )
    }

    /** Trả số ngày nếu key hợp lệ, hoặc null. Auto trim trước khi lookup (case-sensitive). */
    fun lookupDays(rawInput: String): Int? = KEY_TO_DAYS[rawInput.trim()]
}
