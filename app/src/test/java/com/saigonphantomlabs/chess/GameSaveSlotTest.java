package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.List;

/**
 * Test slot store (file-per-slot) của {@link GameSaveManager}: CRUD, sort, đếm, upsert theo
 * sessionId, và LRU evict khi vượt MAX_SLOTS. Dùng filesDir thật do Robolectric cấp.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, application = Application.class)
public class GameSaveSlotTest {

    private Context ctx() { return ApplicationProvider.getApplicationContext(); }

    private void clearAll() {
        File dir = new File(ctx().getFilesDir(), "saves");
        File[] fs = dir.listFiles();
        if (fs != null) for (File f : fs) f.delete();
    }

    @Before public void setUp() { clearAll(); }
    @After public void tearDown() { clearAll(); }

    private GameSaveManager.SavedGame game(String id, long savedAt, int moveCount) {
        GameSaveManager.SavedGame g = new GameSaveManager.SavedGame();
        g.sessionId = id;
        g.savedAtMs = savedAt;
        g.moveCount = moveCount;
        g.turn = Chessman.PlayerColor.White;
        g.pieces.add(new GameSaveManager.PieceData(4, 7, Chessman.ChessmanType.King, Chessman.PlayerColor.White, false));
        return g;
    }

    @Test public void saveThenLoad_roundTrips() {
        GameSaveManager.saveSlot(ctx(), game("g1", 1000, 5));
        GameSaveManager.SavedGame r = GameSaveManager.loadSlot(ctx(), "g1");
        assertNotNull(r);
        assertEquals("g1", r.sessionId);
        assertEquals(1000, r.savedAtMs);
        assertEquals(5, r.moveCount);
        assertEquals(1, r.pieces.size());
    }

    @Test public void listSlots_sortedNewestFirst() {
        GameSaveManager.saveSlot(ctx(), game("a", 100, 1));
        GameSaveManager.saveSlot(ctx(), game("b", 300, 1));
        GameSaveManager.saveSlot(ctx(), game("c", 200, 1));
        List<GameSaveManager.SavedGame> list = GameSaveManager.listSlots(ctx());
        assertEquals(3, list.size());
        assertEquals("b", list.get(0).sessionId); // 300
        assertEquals("c", list.get(1).sessionId); // 200
        assertEquals("a", list.get(2).sessionId); // 100
    }

    @Test public void sameSessionId_upserts_notDuplicate() {
        GameSaveManager.saveSlot(ctx(), game("g1", 100, 1));
        GameSaveManager.saveSlot(ctx(), game("g1", 200, 9)); // ghi đè
        assertEquals(1, GameSaveManager.slotCount(ctx()));
        assertEquals(9, GameSaveManager.loadSlot(ctx(), "g1").moveCount);
    }

    @Test public void deleteSlot_removes() {
        GameSaveManager.saveSlot(ctx(), game("g1", 100, 1));
        GameSaveManager.deleteSlot(ctx(), "g1");
        assertNull(GameSaveManager.loadSlot(ctx(), "g1"));
        assertEquals(0, GameSaveManager.slotCount(ctx()));
    }

    @Test public void exceedingMax_evictsOldest_LRU() {
        int extra = 2;
        for (int i = 0; i < GameSaveManager.MAX_SLOTS + extra; i++) {
            // savedAt tăng dần → i=0 là cũ nhất
            GameSaveManager.saveSlot(ctx(), game("s" + i, 1000 + i, 1));
        }
        assertEquals(GameSaveManager.MAX_SLOTS, GameSaveManager.slotCount(ctx()));
        // 2 ván cũ nhất (s0, s1) bị evict; ván mới nhất còn
        assertNull(GameSaveManager.loadSlot(ctx(), "s0"));
        assertNull(GameSaveManager.loadSlot(ctx(), "s1"));
        assertNotNull(GameSaveManager.loadSlot(ctx(), "s" + (GameSaveManager.MAX_SLOTS + extra - 1)));
    }

    @Test public void emptyDir_listEmpty() {
        assertTrue(GameSaveManager.listSlots(ctx()).isEmpty());
        assertEquals(0, GameSaveManager.slotCount(ctx()));
        assertNull(GameSaveManager.loadSlot(ctx(), "nope"));
    }

    @Test public void nullSessionId_saveIgnored() {
        GameSaveManager.SavedGame g = game("x", 1, 1);
        g.sessionId = null;
        GameSaveManager.saveSlot(ctx(), g);
        assertEquals(0, GameSaveManager.slotCount(ctx()));
        assertFalse(GameSaveManager.listSlots(ctx()).size() > 0);
    }
}
