package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration test mức model (không Android/UI): dựng thế cờ → {@code captureSaveState} →
 * serialize → deserialize → {@code loadSaveState} vào ván mới → khẳng định khôi phục đúng
 * (vị trí quân, lượt, en passant, đồng hồ 50-nước, cờ hasMoved/firstMove, quân bị ăn).
 */
public class GameSaveRoundTripTest {

    private Chess fresh() {
        Chess c = new Chess();
        c.setBoardViewForTest(new NoOpChessBoardView());
        // làm trống bàn (no-arg ctor không tự dựng quân, nhưng để chắc chắn)
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++) c.chessmen[x][y] = null;
        return c;
    }

    private Chess sourcePosition() {
        Chess c = fresh();
        King wk = new King(new Point(4, 7), Chessman.PlayerColor.White, 8, c);
        c.chessmen[4][7] = wk; c.whiteKing = wk; // vua trắng chưa nhập thành
        King bk = new King(new Point(4, 0), Chessman.PlayerColor.Black, 8, c);
        bk.hasMoved = true;
        c.chessmen[4][0] = bk; c.blackKing = bk;
        Rook wr = new Rook(new Point(0, 7), Chessman.PlayerColor.White, 8, c);
        wr.hasMoved = true;                       // xe đã đi → mất quyền nhập thành
        c.chessmen[0][7] = wr;
        Pawn bp = new Pawn(new Point(4, 4), Chessman.PlayerColor.Black, 8, c);
        bp.firstMove = false;                     // tốt đã đi
        c.chessmen[4][4] = bp;
        c.whichPlayerTurn = Chessman.PlayerColor.Black;
        c.enPassantTarget = new Point(3, 2);
        c.halfMoveClock = 9;
        // 1 quân bị ăn
        c.deadMen.add(new Queen(new Point(0, 0), Chessman.PlayerColor.White, 8, c));
        return c;
    }

    @Test public void capture_serialize_deserialize_load_restoresPosition() {
        Chess src = sourcePosition();
        String blob = GameSaveManager.serialize(src.captureSaveState());
        GameSaveManager.SavedGame g = GameSaveManager.deserialize(blob);

        Chess dst = fresh();
        dst.loadSaveState(g);

        // Vua trắng e1 chưa đi
        assertTrue(dst.chessmen[4][7] instanceof King);
        assertEquals(Chessman.PlayerColor.White, dst.chessmen[4][7].color);
        assertFalse(((King) dst.chessmen[4][7]).hasMoved);
        assertSame(dst.chessmen[4][7], dst.whiteKing);
        // Vua đen e8 đã đi
        assertTrue(((King) dst.chessmen[4][0]).hasMoved);
        assertSame(dst.chessmen[4][0], dst.blackKing);
        // Xe trắng a1 hasMoved=true
        assertTrue(dst.chessmen[0][7] instanceof Rook);
        assertTrue(((Rook) dst.chessmen[0][7]).hasMoved);
        // Tốt đen đã đi (firstMove=false)
        assertTrue(dst.chessmen[4][4] instanceof Pawn);
        assertFalse(((Pawn) dst.chessmen[4][4]).firstMove);
        // Meta
        assertEquals(Chessman.PlayerColor.Black, dst.whichPlayerTurn);
        assertEquals(3, dst.enPassantTarget.x);
        assertEquals(2, dst.enPassantTarget.y);
        assertEquals(9, dst.halfMoveClock);
        // Quân bị ăn khôi phục (1)
        assertEquals(1, dst.deadMen.size());
        assertEquals(Chessman.ChessmanType.Queen, dst.deadMen.get(0).type);
        // Undo-stack rỗng sau resume
        assertFalse(dst.hasMovesMade());
        // Ô không có quân vẫn trống
        assertNull(dst.chessmen[0][0]);
    }

    @Test public void loadedPosition_isPlayable_pawnStillMoves() {
        Chess src = sourcePosition();
        GameSaveManager.SavedGame g = GameSaveManager.deserialize(
                GameSaveManager.serialize(src.captureSaveState()));
        Chess dst = fresh();
        dst.loadSaveState(g);

        // Tốt đen ở e4(4,4) sinh nước đi tiến tới e3(4,5) — bàn cờ dùng được sau resume
        Pawn bp = (Pawn) dst.chessmen[4][4];
        bp.generateMoves();
        boolean canPush = false;
        for (Point p : bp.getMoves()) if (p.x == 4 && p.y == 5) canPush = true;
        assertTrue("tốt đen e4 phải đi được xuống e3 sau resume", canPush);
    }
}
