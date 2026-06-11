package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test HOÀN TÁC (undo) cho các loại nước cơ bản (thường / bắt quân / đẩy 2 ô) ở tầng model.
 * Undo nhập thành & en passant đã có ở Castling/EnPassantExecutionTest; undo phong cấp cần
 * Android (createButton) nên test ở tầng Robolectric.
 */
public class UndoTest {

    private Chess board;
    private RecordingChessBoardView view;

    private static final Chessman.PlayerColor W = Chessman.PlayerColor.White;
    private static final Chessman.PlayerColor B = Chessman.PlayerColor.Black;

    @Before
    public void setUp() {
        board = new Chess();
        view = new RecordingChessBoardView();
        board.setBoardViewForTest(view);
        board.whichPlayerTurn = W;
        // 2 vua tối thiểu để move()/validateKing hoạt động
        King wk = new King(new Point(4, 7), W, 8, board);
        board.chessmen[4][7] = wk; board.whiteKing = wk;
        King bk = new King(new Point(4, 0), B, 8, board);
        board.chessmen[4][0] = bk; board.blackKing = bk;
    }

    @Test
    public void undoNormalMove_restoresPositionHistoryAndTurn() {
        Rook r = new Rook(new Point(0, 7), W, 8, board);
        board.chessmen[0][7] = r;

        board.doMove(new Point(0, 7), new Point(0, 5));
        assertSame(Chessman.PlayerColor.Black, board.whichPlayerTurn);
        assertEquals(1, board.getMoveCount());

        board.undoLastMove();

        assertSame("xe về ô gốc", r, board.chessmen[0][7]);
        assertNull("ô đích trống lại", board.chessmen[0][5]);
        assertEquals("lịch sử rỗng", 0, board.getMoveCount());
        assertFalse(board.canUndo());
        assertSame("lượt trả về Trắng", W, board.whichPlayerTurn);
        assertFalse("undo button ẩn khi hết lịch sử", view.undoVisible);
    }

    @Test
    public void undoCapture_restoresCapturedPiece() {
        Rook attacker = new Rook(new Point(7, 0), W, 8, board);
        board.chessmen[7][0] = attacker;
        Rook victim = new Rook(new Point(7, 7), B, 8, board);
        board.chessmen[7][7] = victim;

        board.doMove(new Point(7, 0), new Point(7, 7)); // Rxh1
        assertTrue(victim.isDead);
        assertTrue(view.captured.contains(victim));

        board.undoLastMove();

        assertSame("quân bị bắt sống lại đúng ô", victim, board.chessmen[7][7]);
        assertFalse("không còn chết", victim.isDead);
        assertSame("quân ăn về ô gốc", attacker, board.chessmen[7][0]);
        assertFalse("đã gỡ khỏi danh sách captured", view.captured.contains(victim));
    }

    @Test
    public void undoDoublePush_restoresFirstMoveAndClearsEnPassant() {
        Pawn p = new Pawn(new Point(0, 6), W, 8, board);
        p.firstMove = true;
        board.chessmen[0][6] = p;

        board.doMove(new Point(0, 6), new Point(0, 4)); // a2-a4
        assertFalse("firstMove tắt sau khi đi", p.firstMove);
        assertEquals("đặt en passant target a3", new Point(0, 5), board.enPassantTarget);

        board.undoLastMove();

        assertSame("tốt về a2", p, board.chessmen[0][6]);
        assertTrue("firstMove khôi phục", p.firstMove);
        assertNull("en passant target xoá về trạng thái trước", board.enPassantTarget);
    }
}
