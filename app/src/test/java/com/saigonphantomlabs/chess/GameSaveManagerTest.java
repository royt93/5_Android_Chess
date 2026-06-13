package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test thuần (JVM) cho mã hoá/giải mã snapshot {@link GameSaveManager}.
 * Phủ: round-trip đầy đủ field, en passant có/không, đồng hồ có/không, dữ liệu hỏng → null.
 */
public class GameSaveManagerTest {

    private GameSaveManager.SavedGame sample() {
        GameSaveManager.SavedGame g = new GameSaveManager.SavedGame();
        g.pieces.add(new GameSaveManager.PieceData(4, 7, Chessman.ChessmanType.King, Chessman.PlayerColor.White, false));
        g.pieces.add(new GameSaveManager.PieceData(0, 7, Chessman.ChessmanType.Rook, Chessman.PlayerColor.White, true));
        g.pieces.add(new GameSaveManager.PieceData(4, 4, Chessman.ChessmanType.Pawn, Chessman.PlayerColor.Black, true));
        g.captured.add(new GameSaveManager.PieceData(0, 0, Chessman.ChessmanType.Queen, Chessman.PlayerColor.Black, false));
        g.turn = Chessman.PlayerColor.Black;
        g.halfMoveClock = 7;
        g.isVsAi = true;
        g.difficulty = "HARD";
        g.enPassant = new Point(3, 2);
        g.hasClock = true;
        g.whiteMs = 295000;
        g.blackMs = 301000;
        g.incrementMs = 2000;
        g.whiteActive = false;
        return g;
    }

    @Test public void roundTrip_preservesEverything() {
        GameSaveManager.SavedGame g = sample();
        GameSaveManager.SavedGame r = GameSaveManager.deserialize(GameSaveManager.serialize(g));
        assertEquals(3, r.pieces.size());
        assertEquals(1, r.captured.size());
        assertEquals(Chessman.PlayerColor.Black, r.turn);
        assertEquals(7, r.halfMoveClock);
        assertTrue(r.isVsAi);
        assertEquals("HARD", r.difficulty);
        assertEquals(3, r.enPassant.x);
        assertEquals(2, r.enPassant.y);
        assertTrue(r.hasClock);
        assertEquals(295000, r.whiteMs);
        assertEquals(301000, r.blackMs);
        assertEquals(2000, r.incrementMs);
        assertFalse(r.whiteActive);
        // quân đầu: vua trắng e1 chưa đi
        GameSaveManager.PieceData p0 = r.pieces.get(0);
        assertEquals(Chessman.ChessmanType.King, p0.type);
        assertEquals(Chessman.PlayerColor.White, p0.color);
        assertFalse(p0.moved);
        // xe đã đi
        assertTrue(r.pieces.get(1).moved);
        // quân bị ăn giữ đúng loại/màu
        assertEquals(Chessman.ChessmanType.Queen, r.captured.get(0).type);
        assertEquals(Chessman.PlayerColor.Black, r.captured.get(0).color);
    }

    @Test public void enPassantAbsent_roundTrips_asNull() {
        GameSaveManager.SavedGame g = sample();
        g.enPassant = null;
        GameSaveManager.SavedGame r = GameSaveManager.deserialize(GameSaveManager.serialize(g));
        assertNull(r.enPassant);
    }

    @Test public void noClock_roundTrips() {
        GameSaveManager.SavedGame g = sample();
        g.hasClock = false;
        GameSaveManager.SavedGame r = GameSaveManager.deserialize(GameSaveManager.serialize(g));
        assertFalse(r.hasClock);
    }

    @Test public void pvp_difficultyNull_roundTrips() {
        GameSaveManager.SavedGame g = sample();
        g.isVsAi = false;
        g.difficulty = null;
        GameSaveManager.SavedGame r = GameSaveManager.deserialize(GameSaveManager.serialize(g));
        assertFalse(r.isVsAi);
        assertNull(r.difficulty);
    }

    @Test public void garbage_returnsNull() {
        assertNull(GameSaveManager.deserialize("không phải save"));
        assertNull(GameSaveManager.deserialize(""));
        assertNull(GameSaveManager.deserialize(null));
        assertNull(GameSaveManager.deserialize("CHESSSAVE1\nM|X")); // M thiếu field → parse lỗi → null
    }

    @Test public void wrongHeader_returnsNull() {
        assertNull(GameSaveManager.deserialize("CHESSSAVE9\nM|W|0|0|-|-"));
    }
}
