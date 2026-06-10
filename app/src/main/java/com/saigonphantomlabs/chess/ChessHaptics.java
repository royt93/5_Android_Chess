package com.saigonphantomlabs.chess;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Gom toàn bộ phản hồi rung/haptic của ván cờ (tách khỏi {@link Chess}).
 * Tất cả tĩnh, không trạng thái; an toàn no-op khi ctx/view null.
 */
final class ChessHaptics {
    private ChessHaptics() {
    }

    /** Haptic nhẹ khi chọn quân. */
    static void selection(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    /** Rung mạnh khi ăn quân. */
    static void capture(Context ctx) {
        Vibrator v = vibrator(ctx);
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] timings = {0, 20, 30, 50};
            int[] amplitudes = {0, 100, 0, 255};
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
        } else {
            v.vibrate(80);
        }
    }

    /** Rung 2 nhịp khi chiếu. */
    static void check(Context ctx) {
        Vibrator v = vibrator(ctx);
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] timings = {0, 50, 50, 50, 50, 200};
            int[] amplitudes = {0, 255, 0, 150, 0, 255};
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
        } else {
            long[] pattern = {0, 50, 50, 50, 50, 200};
            v.vibrate(pattern, -1);
        }
    }

    private static Vibrator vibrator(Context ctx) {
        if (ctx == null) return null;
        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        return (v != null && v.hasVibrator()) ? v : null;
    }
}
