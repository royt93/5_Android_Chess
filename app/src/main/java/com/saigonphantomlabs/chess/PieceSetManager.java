package com.saigonphantomlabs.chess;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lưu & áp bộ quân cờ đang chọn (SharedPreferences "chess_pieceset"). Khi đổi: nạp tint vào
 * {@link PieceRenderer} + xoá cache để render lại quân với màu mới.
 */
public final class PieceSetManager {

    private static final String PREF = "chess_pieceset";
    private static final String KEY = "piece_set";

    private PieceSetManager() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static PieceSet getCurrent(Context c) {
        return PieceSet.fromName(prefs(c).getString(KEY, PieceSet.CLASSIC.name()));
    }

    /** Đổi bộ quân: lưu lựa chọn + nạp tint vào renderer + xoá cache (render lại). */
    public static void setCurrent(Context c, PieceSet set) {
        prefs(c).edit().putString(KEY, set.name()).apply();
        apply(set);
    }

    /** Nạp tint của bộ quân vào {@link PieceRenderer} + xoá cache. Gọi lúc khởi tạo board. */
    public static void apply(PieceSet set) {
        PieceRenderer.setTints(set.whiteTint, set.blackTint);
    }

    /** Tiện ích: nạp bộ quân đã lưu vào renderer (gọi sớm, trước khi dựng quân). */
    public static void applySaved(Context c) {
        apply(getCurrent(c));
    }
}
