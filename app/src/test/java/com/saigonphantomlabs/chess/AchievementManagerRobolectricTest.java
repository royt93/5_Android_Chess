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

import java.util.List;

/**
 * Widget/component test (Robolectric) cho {@link AchievementManager}: persistence mở khoá +
 * counter tích luỹ trong SharedPreferences. Không cần thiết bị.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AchievementManagerRobolectricTest {

    private Context ctx() {
        return ApplicationProvider.getApplicationContext();
    }

    private AchievementManager.GameResult win() {
        AchievementManager.GameResult r = new AchievementManager.GameResult();
        r.humanWon = true;
        r.byCheckmate = true;
        r.moveCount = 30;
        r.humanLostPieces = 1;
        r.durationMs = 60_000L;
        return r;
    }

    @Test
    public void firstWin_unlocksAndPersists() {
        AchievementManager mgr = new AchievementManager(ctx());
        List<AchievementManager.Achievement> newly = mgr.recordGameEnd(win());

        assertTrue("First win nằm trong danh sách vừa mở",
                newly.contains(AchievementManager.Achievement.FIRST_WIN));
        assertTrue(mgr.isUnlocked(AchievementManager.Achievement.FIRST_WIN));
        assertTrue("unlockedCount tăng", mgr.unlockedCount() >= 1);

        // Instance mới đọc lại từ prefs → vẫn mở khoá (persist)
        AchievementManager mgr2 = new AchievementManager(ctx());
        assertTrue(mgr2.isUnlocked(AchievementManager.Achievement.FIRST_WIN));
    }

    @Test
    public void alreadyUnlocked_notReturnedAgain() {
        AchievementManager mgr = new AchievementManager(ctx());
        mgr.recordGameEnd(win());
        // Ván thắng thứ hai: First Win đã mở → không nằm trong "vừa mở" lần này
        List<AchievementManager.Achievement> again = mgr.recordGameEnd(win());
        assertFalse(again.contains(AchievementManager.Achievement.FIRST_WIN));
    }

    @Test
    public void cumulativeWins_unlockChampionAtTen() {
        AchievementManager mgr = new AchievementManager(ctx());
        boolean championSeen = false;
        for (int i = 0; i < 10; i++) {
            List<AchievementManager.Achievement> n = mgr.recordGameEnd(win());
            if (n.contains(AchievementManager.Achievement.WINS_10)) championSeen = true;
        }
        assertTrue("WINS_10 mở đúng ở ván thắng thứ 10", championSeen);
        assertTrue(mgr.isUnlocked(AchievementManager.Achievement.WINS_10));
    }

    @Test
    public void lossesCountTowardGames_butNotWins() {
        AchievementManager mgr = new AchievementManager(ctx());
        AchievementManager.GameResult loss = new AchievementManager.GameResult();
        loss.humanWon = false;
        loss.durationMs = 10_000L;
        for (int i = 0; i < 10; i++) mgr.recordGameEnd(loss);
        // 10 ván → GAMES_10 mở; nhưng không có thắng → không có FIRST_WIN/WINS_10
        assertTrue(mgr.isUnlocked(AchievementManager.Achievement.GAMES_10));
        assertFalse(mgr.isUnlocked(AchievementManager.Achievement.FIRST_WIN));
        assertFalse(mgr.isUnlocked(AchievementManager.Achievement.WINS_10));
    }

    @Test
    public void totalCount_matchesEnum() {
        assertEquals(AchievementManager.Achievement.values().length, AchievementManager.total());
    }
}
