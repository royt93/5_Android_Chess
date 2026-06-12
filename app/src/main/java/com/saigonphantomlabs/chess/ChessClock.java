package com.saigonphantomlabs.chess;

import java.util.Locale;

/**
 * Đồng hồ cờ 2 bên (countdown + increment tuỳ chọn). Thuần logic — không đụng Android/timer thật;
 * Activity gọi {@link #tick(long)} định kỳ cho bên đang chạy. Testable trực tiếp JVM.
 */
public final class ChessClock {

    public enum Flag { NONE, WHITE, BLACK }

    private long whiteMs;
    private long blackMs;
    private final long incrementMs;
    private boolean whiteActive = true; // bên đang bị trừ giờ

    public ChessClock(long initialMs, long incrementMs) {
        this.whiteMs = Math.max(0, initialMs);
        this.blackMs = Math.max(0, initialMs);
        this.incrementMs = Math.max(0, incrementMs);
    }

    public long getWhiteMs() { return whiteMs; }
    public long getBlackMs() { return blackMs; }
    public boolean isWhiteActive() { return whiteActive; }

    /** Đặt bên đang chạy đồng hồ (đồng bộ với lượt đi của ván). */
    public void setWhiteActive(boolean white) { this.whiteActive = white; }

    /** Trừ deltaMs khỏi bên đang chạy (clamp ≥ 0). */
    public void tick(long deltaMs) {
        if (deltaMs <= 0) return;
        if (whiteActive) {
            whiteMs = Math.max(0, whiteMs - deltaMs);
        } else {
            blackMs = Math.max(0, blackMs - deltaMs);
        }
    }

    /**
     * Bên vừa đi xong một nước: cộng increment cho bên ĐÓ rồi chuyển đồng hồ sang bên kia.
     * Gọi sau khi nước đi hoàn tất (đồng bộ chuyển lượt).
     */
    public void onMoveCompleted() {
        if (whiteActive) {
            whiteMs += incrementMs;
        } else {
            blackMs += incrementMs;
        }
        whiteActive = !whiteActive;
    }

    /** Bên nào đã hết giờ (cờ rơi), hoặc NONE. */
    public Flag flagged() {
        if (whiteMs <= 0) return Flag.WHITE;
        if (blackMs <= 0) return Flag.BLACK;
        return Flag.NONE;
    }

    /** Format "M:SS"; dưới 10 giây hiện thêm 1 số thập phân "S.s" cho cảm giác gấp gáp. */
    public static String format(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000;
        if (ms < 10_000) {
            long tenths = (ms % 1000) / 100;
            return String.format(Locale.US, "%d.%d", totalSec, tenths);
        }
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format(Locale.US, "%d:%02d", m, s);
    }
}
