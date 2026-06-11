package com.saigonphantomlabs.feature.vip

import android.content.Context

/**
 * Helper SharedPreferences riêng cho VIP screen.
 *
 * Lý do tách: lib chỉ persist `vipByKeyUntil` (expiry), KHÔNG persist `grantedAt`.
 * Để vẽ progress bar elapsed-semantic cần cả 2 mốc. Đồng thời lưu cờ `userRedeemedOnce`
 * để phân biệt grace entry (auto-trial của SDK) vs user tự redeem key.
 *
 * Khi lib bổ sung `getVipByKeyGrantedAt()` → xoá class này, đọc trực tiếp lib.
 */
class VipPrefs(context: Context) {

    private val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveGrantedAtMs(ms: Long) = sp.edit().putLong(KEY_GRANTED_AT, ms).apply()
    fun getGrantedAtMs(): Long = sp.getLong(KEY_GRANTED_AT, 0L)
    fun clearGrantedAtMs() = sp.edit().remove(KEY_GRANTED_AT).apply()

    fun markUserRedeemed() = sp.edit().putBoolean(KEY_USER_REDEEMED, true).apply()
    fun userRedeemedAtLeastOnce(): Boolean = sp.getBoolean(KEY_USER_REDEEMED, false)

    companion object {
        private const val PREFS_NAME = "vip_screen_prefs"
        private const val KEY_GRANTED_AT = "granted_at_ms"
        private const val KEY_USER_REDEEMED = "user_redeemed_once"
    }
}
