package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test THỰC THI + UNDO nhập thành ở mức model (chessmen[][]), không cần Android/UI
 * nhờ refactor {@link ChessBoardView} + chế độ model-only (button null).
 */
public class CastlingExecutionTest {

    private Chess board;
    private King whiteKing;

    @Before
    public void setUp() {
        board = new Chess();
        board.setBoardViewForTest(new NoOpChessBoardView());
        whiteKing = new King(new Point(4, 7), Chessman.PlayerColor.White, 8, board);
        board.chessmen[4][7] = whiteKing;
        board.whiteKing = whiteKing;
        // Vua đen ở xa để nước đi không gây chiếu
        King blackKing = new King(new Point(4, 0), Chessman.PlayerColor.Black, 8, board);
        board.chessmen[4][0] = blackKing;
        board.blackKing = blackKing;
        board.whichPlayerTurn = Chessman.PlayerColor.White; // Trắng đang đi
    }

    @Test
    public void kingSideCastle_movesKingAndRook_thenUndoRestores() {
        Rook rook = new Rook(new Point(7, 7), Chessman.PlayerColor.White, 8, board);
        board.chessmen[7][7] = rook;

        board.doMove(new Point(4, 7), new Point(6, 7)); // O-O

        // Vua tới g1(6,7), xe tới f1(5,7); ô gốc trống
        assertSame(whiteKing, board.chessmen[6][7]);
        assertSame(rook, board.chessmen[5][7]);
        assertNull(board.chessmen[4][7]);
        assertNull(board.chessmen[7][7]);
        assertTrue(whiteKing.hasMoved);
        assertTrue(rook.hasMoved);

        board.undoLastMove();

        // Khôi phục đúng vị trí + cờ hasMoved
        assertSame(whiteKing, board.chessmen[4][7]);
        assertSame(rook, board.chessmen[7][7]);
        assertNull(board.chessmen[5][7]);
        assertNull(board.chessmen[6][7]);
        assertFalse(whiteKing.hasMoved);
        assertFalse(rook.hasMoved);
    }

    @Test
    public void queenSideCastle_movesKingAndRook_thenUndoRestores() {
        Rook rook = new Rook(new Point(0, 7), Chessman.PlayerColor.White, 8, board);
        board.chessmen[0][7] = rook;

        board.doMove(new Point(4, 7), new Point(2, 7)); // O-O-O

        // Vua tới c1(2,7), xe tới d1(3,7)
        assertSame(whiteKing, board.chessmen[2][7]);
        assertSame(rook, board.chessmen[3][7]);
        assertNull(board.chessmen[4][7]);
        assertNull(board.chessmen[0][7]);

        board.undoLastMove();

        assertSame(whiteKing, board.chessmen[4][7]);
        assertSame(rook, board.chessmen[0][7]);
        assertNull(board.chessmen[2][7]);
        assertNull(board.chessmen[3][7]);
        assertFalse(whiteKing.hasMoved);
        assertFalse(rook.hasMoved);
    }

    /**
     * Lưới an toàn ở tầng thực thi: nếu vua nhập thành VÀO ô bị chiếu, move() phải từ chối
     * → bàn cờ không đổi (vua & xe đứng yên). Bảo vệ kể cả khi nước được gọi trực tiếp.
     */
    @Test
    public void castleIntoCheck_notExecuted() {
        Rook rook = new Rook(new Point(7, 7), Chessman.PlayerColor.White, 8, board);
        board.chessmen[7][7] = rook;
        // Xe đen ở g8 (6,0) khống chế cột g → ô đích g1 (6,7) bị tấn công
        board.chessmen[6][0] = new Rook(new Point(6, 0), Chessman.PlayerColor.Black, 8, board);

        board.doMove(new Point(4, 7), new Point(6, 7)); // thử O-O vào ô bị chiếu

        // move() từ chối → mọi thứ giữ nguyên
        assertSame(whiteKing, board.chessmen[4][7]);
        assertSame(rook, board.chessmen[7][7]);
        assertNull(board.chessmen[6][7]);
        assertNull(board.chessmen[5][7]);
        assertFalse(whiteKing.hasMoved);
        assertFalse(rook.hasMoved);
    }
}
