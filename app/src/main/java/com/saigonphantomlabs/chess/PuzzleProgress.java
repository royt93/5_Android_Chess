package com.saigonphantomlabs.chess;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lưu tiến độ giải câu đố (đã giải hay chưa) trong SharedPreferences "chess_puzzles".
 * Static helper, nhẹ — chỉ 1 boolean / câu theo id.
 */
public final class PuzzleProgress {

    private static final String PREF = "chess_puzzles";
    private static final String SOLVED_PREFIX = "solved_";

    private PuzzleProgress() { }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void markSolved(Context c, String id) {
        if (id == null) return;
        prefs(c).edit().putBoolean(SOLVED_PREFIX + id, true).apply();
    }

    public static boolean isSolved(Context c, String id) {
        return id != null && prefs(c).getBoolean(SOLVED_PREFIX + id, false);
    }

    /** Số câu đã giải trên tổng bộ câu đố hiện có. */
    public static int solvedCount(Context c) {
        int n = 0;
        for (Puzzle p : PuzzleRepository.all()) if (isSolved(c, p.id)) n++;
        return n;
    }
}
