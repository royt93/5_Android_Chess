package com.saigonphantomlabs.common.consts

import android.util.Base64
import com.saigonphantomlabs.chess.BuildConfig

/**
 * Centralize hằng số nhạy cảm cho ad/VIP.
 *
 * - Ad unit ID + AppLovin SDK key đã đi qua [BuildConfig] (set per buildType ở `app/build.gradle`),
 *   đọc trực tiếp `BuildConfig.APPLOVIN_*` / `BuildConfig.ADMOB_*` — KHÔNG hardcode ở đây.
 * - File này chỉ giữ VIP secret (Base64-obfuscated) + Privacy Policy URL.
 */
object AdKeys {

    /** Privacy Policy — nhúng vào VIP screen footer + consent. Lấy từ BuildConfig. */
    val PRIVACY_POLICY_URL: String = BuildConfig.PRIVACY_POLICY_URL

    /**
     * VIP secret dùng cho [com.roy.sdkadbmob.AdSdkConfig.vipKeySecret] (single-secret design của lib).
     * = key 30 ngày. Plain key KHÔNG hardcode — chỉ lưu Base64, decode runtime.
     * Mức che giấu = Base64 (đã thống nhất: chặn user thường peek decompiled APK).
     */
    val VIP_SECRET: String by lazy {
        String(Base64.decode(VIP_SECRET_B64, Base64.NO_WRAP))
    }

    // Base64(NO_WRAP) của plain key 30-ngày (xem doc/AD.MD §4 — không ghi plain ở source)
    private const val VIP_SECRET_B64 = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA=="
}
