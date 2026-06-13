package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Widget/component test (Robolectric) cho {@link PuzzleProgress}: persist trạng thái đã-giải +
 * đếm câu đã giải qua SharedPreferences. Không cần thiết bị.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PuzzleProgressRobolectricTest {

    private Context ctx() {
        return ApplicationProvider.getApplicationContext();
    }

    @Test
    public void markSolved_persistsAndCounts() {
        assertFalse(PuzzleProgress.isSolved(ctx(), "p1"));
        assertEquals(0, PuzzleProgress.solvedCount(ctx()));

        PuzzleProgress.markSolved(ctx(), "p1");
        PuzzleProgress.markSolved(ctx(), "p3");

        assertTrue(PuzzleProgress.isSolved(ctx(), "p1"));
        assertTrue(PuzzleProgress.isSolved(ctx(), "p3"));
        assertFalse(PuzzleProgress.isSolved(ctx(), "p2"));
        assertEquals(2, PuzzleProgress.solvedCount(ctx()));

        // Đọc lại (instance prefs mới) → vẫn persist
        assertTrue(PuzzleProgress.isSolved(ctx(), "p1"));
    }

    @Test
    public void markSolved_idempotent_andNullSafe() {
        PuzzleProgress.markSolved(ctx(), "p2");
        PuzzleProgress.markSolved(ctx(), "p2"); // lặp không tăng đếm
        assertEquals(1, PuzzleProgress.solvedCount(ctx()));
        PuzzleProgress.markSolved(ctx(), null);  // null an toàn
        assertFalse(PuzzleProgress.isSolved(ctx(), null));
    }

    @Test
    public void solvedCount_onlyCountsKnownPuzzles() {
        PuzzleProgress.markSolved(ctx(), "does_not_exist");
        // id không thuộc bộ câu đố → không tính vào solvedCount
        assertEquals(0, PuzzleProgress.solvedCount(ctx()));
    }
}
