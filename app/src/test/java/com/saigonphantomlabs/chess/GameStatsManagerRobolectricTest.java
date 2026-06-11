package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Widget/component test (Robolectric) cho {@link GameStatsManager}: ghi kết quả ván vào
 * SharedPreferences (thắng/thua/hoà theo độ khó + tổng số ván + tổng thời gian).
 * Đọc thẳng prefs để khỏi phụ thuộc chuỗi tài nguyên định dạng.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GameStatsManagerRobolectricTest {

    private Context ctx() {
        return ApplicationProvider.getApplicationContext();
    }

    private SharedPreferences prefs() {
        return ctx().getSharedPreferences("chess_stats", Context.MODE_PRIVATE);
    }

    @Test
    public void saveWin_incrementsTotalsAndWinCount() {
        GameStatsManager mgr = new GameStatsManager(ctx());
        mgr.saveGameResult(AIEngine.Difficulty.MEDIUM, 1, 60_000L); // thắng, 1 phút

        assertEquals("tổng số ván = 1", 1, prefs().getInt("total_games", 0));
        assertEquals("thời gian luỹ kế", 60_000L, prefs().getLong("total_time_ms", 0));
        assertEquals("1 thắng ở MEDIUM", 1, prefs().getInt("wins_medium", 0));
        assertEquals("không thua", 0, prefs().getInt("losses_medium", 0));
    }

    @Test
    public void saveMixedResults_accumulatePerDifficulty() {
        GameStatsManager mgr = new GameStatsManager(ctx());
        mgr.saveGameResult(AIEngine.Difficulty.HARD, 1, 1000L);   // win
        mgr.saveGameResult(AIEngine.Difficulty.HARD, -1, 1000L);  // loss
        mgr.saveGameResult(AIEngine.Difficulty.HARD, 0, 1000L);   // draw
        mgr.saveGameResult(AIEngine.Difficulty.EASY, 1, 1000L);   // win EASY

        assertEquals(4, prefs().getInt("total_games", 0));
        assertEquals(1, prefs().getInt("wins_hard", 0));
        assertEquals(1, prefs().getInt("losses_hard", 0));
        assertEquals(1, prefs().getInt("draws_hard", 0));
        assertEquals(1, prefs().getInt("wins_easy", 0));
    }
}
