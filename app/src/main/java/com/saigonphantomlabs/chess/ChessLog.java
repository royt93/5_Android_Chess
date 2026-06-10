package com.saigonphantomlabs.chess;

import android.util.Log;

/**
 * Logging gate cho engine cờ: chỉ log khi {@code BuildConfig.DEBUG}.
 * Release build sẽ không phát log "roy93~" (giảm noise + chi phí chuỗi/IO).
 */
final class ChessLog {
    private static final String TAG = "roy93~";

    private ChessLog() {
    }

    static void d(String msg) {
        if (BuildConfig.DEBUG) Log.d(TAG, msg);
    }

    static void e(String msg) {
        if (BuildConfig.DEBUG) Log.e(TAG, msg);
    }
}
