package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test THỰC THI + UNDO bắt tốt qua đường ở mức model (chessmen[][]).
 */
public class EnPassantExecutionTest {

    private Chess board;
    private Pawn whitePawn;
    private Pawn blackVictim;

    @Before
    public void setUp() {
        board = new Chess();
        board.setBoardViewForTest(new NoOpChessBoardView());

        // Tốt Trắng e5 (4,3); tốt Đen vừa đẩy d7-d5 nằm d5 (3,3)
        whitePawn = new Pawn(new Point(4, 3), Chessman.PlayerColor.White, 8, board);
        whitePawn.firstMove = false;
        board.chessmen[4][3] = whitePawn;
        blackVictim = new Pawn(new Point(3, 3), Chessman.PlayerColor.Black, 8, board);
        blackVictim.firstMove = false;
        board.chessmen[3][3] = blackVictim;

        King whiteKing = new King(new Point(4, 7), Chessman.PlayerColor.White, 8, board);
        board.chessmen[4][7] = whiteKing;
        board.whiteKing = whiteKing;
        King blackKing = new King(new Point(0, 0), Chessman.PlayerColor.Black, 8, board);
        board.chessmen[0][0] = blackKing;
        board.blackKing = blackKing;

        // Đối phương vừa tạo cơ hội bắt qua đường: ô bỏ qua d6 = (3,2)
        board.enPassantTarget = new Point(3, 2);
        board.whichPlayerTurn = Chessman.PlayerColor.White;
    }

    @Test
    public void enPassant_capturesVictimOffTargetSquare_thenUndoRestores() {
        board.doMove(new Point(4, 3), new Point(3, 2)); // exd6 e.p.

        // Tốt Trắng tới d6(3,2); ô gốc e5 trống; tốt Đen d5 BỊ BẮT (ô trống)
        assertSame(whitePawn, board.chessmen[3][2]);
        assertNull(board.chessmen[4][3]);
        assertNull("Con tốt bị bắt qua đường phải bị gỡ khỏi d5", board.chessmen[3][3]);
        assertTrue(blackVictim.isDead);

        board.undoLastMove();

        // Tốt Trắng về e5; tốt Đen sống lại đúng ô d5; d6 trống
        assertSame(whitePawn, board.chessmen[4][3]);
        assertSame("Tốt bị bắt phải được khôi phục về d5", blackVictim, board.chessmen[3][3]);
        assertNull(board.chessmen[3][2]);
        assertFalse(blackVictim.isDead);
    }

    /** Đẩy tốt 2 ô phải mở cơ hội en passant (đặt enPassantTarget = ô bị bỏ qua). */
    @Test
    public void doublePush_setsTarget() {
        Pawn p = new Pawn(new Point(4, 6), Chessman.PlayerColor.White, 8, board); // e2
        p.firstMove = true;
        board.chessmen[4][6] = p;
        board.enPassantTarget = null;

        board.doMove(new Point(4, 6), new Point(4, 4)); // e2-e4

        assertEquals("Phải đặt en passant target tại e3 (4,5)", new Point(4, 5), board.enPassantTarget);
    }

    /** Nước KHÔNG phải đẩy 2 ô phải đóng cơ hội en passant (xoá target). */
    @Test
    public void nonDoublePush_clearsTarget() {
        // setUp đã đặt enPassantTarget=(3,2). Đẩy tốt Trắng e5→e6 (1 ô) phải xoá target.
        board.doMove(new Point(4, 3), new Point(4, 2));

        assertNull("Nước 1 ô phải đóng cơ hội en passant", board.enPassantTarget);
    }
}
