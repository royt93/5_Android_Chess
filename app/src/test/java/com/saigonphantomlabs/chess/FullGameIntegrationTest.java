package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration test "ván cờ thu nhỏ": chơi nhiều nửa-nước luân phiên qua {@code doMove()},
 * kiểm tra luân phiên lượt + tích luỹ lịch sử + phát hiện chiếu hết cuối ván + undo.
 */
public class FullGameIntegrationTest {

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

    /**
     * Chiếu hết K+R: Vua Trắng b6(1,2) áp sát Vua Đen a8(0,0); Xe Trắng h-file.
     * 1.Rh7 Kb8 2.Rh8# — kết thúc, Trắng thắng. Mọi nửa-nước luân phiên lượt đúng.
     */
    @Test
    public void miniGame_kingAndRookMate() {
        king(1, 2, W);                                  // Vua Trắng b6
        king(0, 0, B);                                  // Vua Đen a8
        Rook rook = new Rook(new Point(7, 7), W, 8, board);
        board.chessmen[7][7] = rook;

        // Nửa-nước 1 (Trắng): Rh1-h7
        board.doMove(new Point(7, 7), new Point(7, 1));
        assertSame("sau nước Trắng → tới lượt Đen", B, board.whichPlayerTurn);
        assertEquals(1, board.getMoveCount());
        assertFalse("chưa kết thúc", view.gameEndShown);

        // Nửa-nước 2 (Đen): Ka8-b8 (nước duy nhất hợp lệ)
        board.doMove(new Point(0, 0), new Point(1, 0));
        assertSame("sau nước Đen → tới lượt Trắng", W, board.whichPlayerTurn);
        assertEquals(2, board.getMoveCount());
        assertFalse(view.gameEndShown);

        // Nửa-nước 3 (Trắng): Rh7-h8 chiếu hết
        board.doMove(new Point(7, 1), new Point(7, 0));
        assertTrue("phải chiếu hết → hiện dialog", view.gameEndShown);
        assertTrue("Trắng thắng", view.lastWhiteWins);
        assertFalse("không phải hoà", view.lastIsStalemate);
        assertEquals(3, board.getMoveCount());
    }

    /** Undo nước chiếu hết: lịch sử giảm, vẫn undo tiếp được. */
    @Test
    public void undoAfterMate_restoresPlayablePosition() {
        king(1, 2, W);
        king(0, 0, B);
        Rook rook = new Rook(new Point(7, 7), W, 8, board);
        board.chessmen[7][7] = rook;

        board.doMove(new Point(7, 7), new Point(7, 1));
        board.doMove(new Point(0, 0), new Point(1, 0));
        board.doMove(new Point(7, 1), new Point(7, 0)); // mate
        assertEquals(3, board.getMoveCount());

        board.undoLastMove();                            // hoàn tác nước chiếu hết

        assertEquals("lịch sử còn 2 nước", 2, board.getMoveCount());
        assertSame("xe về h7", rook, board.chessmen[7][1]);
        assertTrue("vẫn undo tiếp được", board.canUndo());
    }
}
