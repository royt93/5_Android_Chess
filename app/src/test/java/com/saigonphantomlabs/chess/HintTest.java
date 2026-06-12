package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

/**
 * Test tính năng Hint ở tầng model ({@link Chess#computeBestHint()} — không đụng UI).
 * Board: White ở row 7 (đáy), Black ở row 0 (đỉnh); x=cột a..h (0..7).
 */
public class HintTest {

    private Chess board;
    private static final Chessman.PlayerColor W = Chessman.PlayerColor.White;
    private static final Chessman.PlayerColor B = Chessman.PlayerColor.Black;

    @Before
    public void setUp() {
        board = new Chess();
        board.setBoardViewForTest(new NoOpChessBoardView());
        King wk = new King(new Point(4, 7), W, 8, board);
        board.chessmen[4][7] = wk; board.whiteKing = wk;
        King bk = new King(new Point(4, 0), B, 8, board);
        board.chessmen[4][0] = bk; board.blackKing = bk;
    }

    @Test
    public void computeBestHint_capturesHangingQueen() {
        // Xe trắng a1 (0,7) ăn được hậu đen treo a8 (0,0) dọc cột a thông thoáng.
        Rook wr = new Rook(new Point(0, 7), W, 8, board);
        board.chessmen[0][7] = wr;
        Queen bq = new Queen(new Point(0, 0), B, 8, board);
        board.chessmen[0][0] = bq;
        board.whichPlayerTurn = W;

        MoveRecord hint = board.computeBestHint();
        assertNotNull("Hint phải có nước cho bên đang đi", hint);
        assertSame("Hint phải là nước của White (bên đang đi)", W, hint.movedPiece.color);
        // Nước tốt nhất = ăn hậu treo: Ra1xa8
        assertEquals("Hint nên ăn hậu treo (toX=a)", 0, hint.toX);
        assertEquals("Hint nên ăn hậu treo (toY=8)", 0, hint.toY);
    }

    @Test
    public void computeBestHint_returnsMoveForCurrentMover() {
        // Tốt trắng e2 (4,6) — luôn có nước hợp lệ cho White.
        Pawn wp = new Pawn(new Point(4, 6), W, 8, board);
        board.chessmen[4][6] = wp;
        board.whichPlayerTurn = W;

        MoveRecord hint = board.computeBestHint();
        assertNotNull(hint);
        assertSame(W, hint.movedPiece.color);
    }
}
