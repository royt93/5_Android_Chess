package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test luật HOÀ mới: 50 nước (halfMoveClock) + LẶP THẾ 3 lần (threefold) + undo phục hồi.
 * Quan sát hoà qua callback {@code showCustomGameEndDialog(false, true)}.
 */
public class DrawRulesTest {

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
    }

    private King king(int x, int y, Chessman.PlayerColor c) {
        King k = new King(new Point(x, y), c, 8, board);
        board.chessmen[x][y] = k;
        if (c == W) board.whiteKing = k; else board.blackKing = k;
        return k;
    }

    // ---------------- 50-MOVE CLOCK ----------------

    @Test
    public void quietMove_incrementsClock() {
        king(0, 7, W); king(7, 0, B);
        Rook r = new Rook(new Point(3, 4), W, 8, board);
        board.chessmen[3][4] = r;

        board.doMove(new Point(3, 4), new Point(3, 3)); // xe đi quân (không ăn)

        assertEquals("nước im lặng tăng đồng hồ", 1, board.halfMoveClock);
    }

    @Test
    public void pawnMove_resetsClock() {
        king(0, 7, W); king(7, 0, B);
        board.halfMoveClock = 40;
        Pawn p = new Pawn(new Point(4, 6), W, 8, board);
        p.firstMove = true;
        board.chessmen[4][6] = p;

        board.doMove(new Point(4, 6), new Point(4, 5)); // đẩy tốt

        assertEquals("đẩy tốt reset đồng hồ", 0, board.halfMoveClock);
    }

    @Test
    public void capture_resetsClock() {
        king(0, 7, W); king(7, 0, B);
        board.halfMoveClock = 30;
        Rook r = new Rook(new Point(3, 4), W, 8, board);
        board.chessmen[3][4] = r;
        board.chessmen[3][1] = new Rook(new Point(3, 1), B, 8, board); // mồi

        board.doMove(new Point(3, 4), new Point(3, 1)); // ăn quân

        assertEquals("ăn quân reset đồng hồ", 0, board.halfMoveClock);
    }

    @Test
    public void fiftyMoveRule_drawAt100HalfMoves() {
        king(0, 7, W); king(7, 0, B);
        Rook r = new Rook(new Point(3, 4), W, 8, board);
        board.chessmen[3][4] = r;
        board.halfMoveClock = 99; // nước kế là nửa-nước thứ 100

        board.doMove(new Point(3, 4), new Point(3, 3)); // nước im lặng thứ 100

        assertEquals(100, board.halfMoveClock);
        assertTrue("đạt 50 nước → hoà", view.gameEndShown);
        assertTrue("là hoà", view.lastIsStalemate);
    }

    // ---------------- THREEFOLD ----------------

    @Test
    public void threefold_drawOnThirdRepetition() {
        // Chỉ 2 vua (đặt hasMoved=true để quyền nhập thành không đổi khoá thế).
        King wk = king(0, 7, W); wk.hasMoved = true;
        King bk = king(7, 0, B); bk.hasMoved = true;

        // Chu kỳ 4 nửa-nước đưa về thế cũ; lặp tới khi thế K1 xuất hiện lần 3.
        Point[][] cycle = {
                {new Point(0, 7), new Point(1, 7)}, // W Ka1-b1  → thế K1
                {new Point(7, 0), new Point(6, 0)}, // B Kh8-g8  → K2
                {new Point(1, 7), new Point(0, 7)}, // W Kb1-a1  → K3
                {new Point(6, 0), new Point(7, 0)}, // B Kg8-h8  → K4 (về thế đầu)
        };
        // 8 nửa-nước đầu: K1,K2,K3,K4 mỗi thế xuất hiện 2 lần — CHƯA hoà
        for (int rep = 0; rep < 2; rep++) {
            for (Point[] mv : cycle) {
                board.doMove(mv[0], mv[1]);
            }
        }
        assertFalse("sau 2 vòng (mỗi thế 2 lần) chưa hoà", view.gameEndShown);

        // Nửa-nước thứ 9: W Ka1-b1 → thế K1 lần 3 → hoà
        board.doMove(new Point(0, 7), new Point(1, 7));

        assertTrue("lặp thế lần 3 → hoà", view.gameEndShown);
        assertTrue(view.lastIsStalemate);
    }

    // ---------------- UNDO ----------------

    @Test
    public void undo_restoresHalfMoveClock() {
        king(0, 7, W); king(7, 0, B);
        board.halfMoveClock = 10;
        Rook r = new Rook(new Point(3, 4), W, 8, board);
        board.chessmen[3][4] = r;

        board.doMove(new Point(3, 4), new Point(3, 3));
        assertEquals(11, board.halfMoveClock);

        board.undoLastMove();
        assertEquals("undo trả đồng hồ về trước nước", 10, board.halfMoveClock);
    }

    @Test
    public void undo_afterThreefoldDraw_removesRepetitionCount() {
        King wk = king(0, 7, W); wk.hasMoved = true;
        King bk = king(7, 0, B); bk.hasMoved = true;
        Point[][] cycle = {
                {new Point(0, 7), new Point(1, 7)},
                {new Point(7, 0), new Point(6, 0)},
                {new Point(1, 7), new Point(0, 7)},
                {new Point(6, 0), new Point(7, 0)},
        };
        for (int rep = 0; rep < 2; rep++)
            for (Point[] mv : cycle) board.doMove(mv[0], mv[1]);
        board.doMove(new Point(0, 7), new Point(1, 7)); // lần 3 → hoà
        assertTrue(view.gameEndShown);

        board.undoLastMove(); // gỡ lần xuất hiện thứ 3

        // Đi lại đúng nước đó: lúc này lại là lần 3 → hoà lần nữa (chứng tỏ count đã giảm đúng,
        // không bị kẹt ở 3+). Reset cờ quan sát trước khi đi lại.
        view.gameEndShown = false;
        board.doMove(new Point(0, 7), new Point(1, 7));
        assertTrue("redo đúng nước → lại đạt lặp thế lần 3", view.gameEndShown);
    }
}
