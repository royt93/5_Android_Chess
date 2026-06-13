package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.junit.Test;

/**
 * Unit test THUẦN cho {@link AchievementManager#qualifying} — logic xét điều kiện huy hiệu,
 * không cần Android/SharedPreferences.
 */
public class AchievementManagerTest {

    private AchievementManager.GameResult win() {
        AchievementManager.GameResult r = new AchievementManager.GameResult();
        r.humanWon = true;
        r.byCheckmate = true;
        r.moveCount = 30;
        r.humanLostPieces = 2;
        r.durationMs = 60_000L;
        return r;
    }

    private EnumSet<AchievementManager.Achievement> q(AchievementManager.GameResult r,
            int games, int wins, long time) {
        return AchievementManager.qualifying(r, games, wins, time);
    }

    @Test
    public void firstWin_onlyWhenHumanWon() {
        assertTrue(q(win(), 1, 1, 60_000L).contains(AchievementManager.Achievement.FIRST_WIN));
        AchievementManager.GameResult loss = new AchievementManager.GameResult();
        loss.humanWon = false;
        assertFalse(q(loss, 1, 0, 0).contains(AchievementManager.Achievement.FIRST_WIN));
    }

    @Test
    public void milestones_gamesAndWins() {
        AchievementManager.GameResult r = win();
        EnumSet<AchievementManager.Achievement> s = q(r, 10, 10, 0);
        assertTrue(s.contains(AchievementManager.Achievement.GAMES_10));
        assertTrue(s.contains(AchievementManager.Achievement.WINS_10));
        EnumSet<AchievementManager.Achievement> s9 = q(r, 9, 9, 0);
        assertFalse(s9.contains(AchievementManager.Achievement.GAMES_10));
        assertFalse(s9.contains(AchievementManager.Achievement.WINS_10));
    }

    @Test
    public void beatAi_difficultyGated() {
        AchievementManager.GameResult r = win();
        r.vsAi = true;
        r.difficulty = AIEngine.Difficulty.HARD;
        EnumSet<AchievementManager.Achievement> s = q(r, 1, 1, 0);
        assertTrue(s.contains(AchievementManager.Achievement.BEAT_HARD));
        assertFalse(s.contains(AchievementManager.Achievement.BEAT_UNBEATABLE));

        r.difficulty = AIEngine.Difficulty.UNBEATABLE;
        EnumSet<AchievementManager.Achievement> s2 = q(r, 1, 1, 0);
        assertTrue(s2.contains(AchievementManager.Achievement.BEAT_UNBEATABLE));
        assertFalse(s2.contains(AchievementManager.Achievement.BEAT_HARD));

        // Thắng người (không vsAi) → không có huy hiệu hạ AI
        r.vsAi = false;
        assertFalse(q(r, 1, 1, 0).contains(AchievementManager.Achievement.BEAT_UNBEATABLE));
    }

    @Test
    public void checkmate_notWhenTimeoutWin() {
        AchievementManager.GameResult r = win();
        assertTrue(q(r, 1, 1, 0).contains(AchievementManager.Achievement.CHECKMATE));
        r.byCheckmate = false; // thắng do hết giờ
        assertFalse(q(r, 1, 1, 0).contains(AchievementManager.Achievement.CHECKMATE));
    }

    @Test
    public void castleAndPromote_independentOfWin() {
        AchievementManager.GameResult r = new AchievementManager.GameResult();
        r.humanWon = false; // thua nhưng vẫn nhập thành/phong cấp
        r.whiteCastled = true;
        r.whitePromoted = true;
        EnumSet<AchievementManager.Achievement> s = q(r, 1, 0, 0);
        assertTrue(s.contains(AchievementManager.Achievement.CASTLE));
        assertTrue(s.contains(AchievementManager.Achievement.PROMOTE));
    }

    @Test
    public void flawless_requiresWinAndZeroLost() {
        AchievementManager.GameResult r = win();
        r.humanLostPieces = 0;
        assertTrue(q(r, 1, 1, 0).contains(AchievementManager.Achievement.FLAWLESS));
        r.humanLostPieces = 1;
        assertFalse(q(r, 1, 1, 0).contains(AchievementManager.Achievement.FLAWLESS));
        // Mất 0 quân nhưng thua → không hoàn hảo
        AchievementManager.GameResult loss = new AchievementManager.GameResult();
        loss.humanWon = false; loss.humanLostPieces = 0;
        assertFalse(q(loss, 1, 0, 0).contains(AchievementManager.Achievement.FLAWLESS));
    }

    @Test
    public void quickWin_threshold() {
        AchievementManager.GameResult r = win();
        r.moveCount = 20;
        assertTrue(q(r, 1, 1, 0).contains(AchievementManager.Achievement.QUICK_WIN));
        r.moveCount = 21;
        assertFalse(q(r, 1, 1, 0).contains(AchievementManager.Achievement.QUICK_WIN));
        r.moveCount = 0; // dữ liệu bất thường → không tính
        assertFalse(q(r, 1, 1, 0).contains(AchievementManager.Achievement.QUICK_WIN));
    }

    @Test
    public void marathon_byCumulativeTime() {
        AchievementManager.GameResult r = new AchievementManager.GameResult();
        assertFalse(q(r, 1, 0, 3_599_000L).contains(AchievementManager.Achievement.MARATHON));
        assertTrue(q(r, 1, 0, 3_600_000L).contains(AchievementManager.Achievement.MARATHON));
    }

    @Test
    public void draw_grantsNoWinAchievements() {
        AchievementManager.GameResult r = new AchievementManager.GameResult();
        r.draw = true;
        r.humanWon = false;
        EnumSet<AchievementManager.Achievement> s = q(r, 5, 0, 0);
        assertFalse(s.contains(AchievementManager.Achievement.FIRST_WIN));
        assertFalse(s.contains(AchievementManager.Achievement.CHECKMATE));
        assertFalse(s.contains(AchievementManager.Achievement.FLAWLESS));
    }
}
