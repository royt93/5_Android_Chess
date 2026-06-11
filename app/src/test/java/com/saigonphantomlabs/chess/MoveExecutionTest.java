package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test THỰC THI nước đi ở tầng model qua {@code doMove()}/{@code move()}:
 * từ chối nước để hở vua, xử lý bắt quân, cờ firstMove. Thuần JVM.
 */
public class MoveExecutionTest {

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

    private Rook rook(int x, int y, Chessman.PlayerColor c) {
        Rook r = new Rook(new Point(x, y), c, 8, board);
        board.chessmen[x][y] = r;
        return r;
    }

    /** Nước để hở vua (vua đang/ sẽ bị chiếu) phải bị từ chối → bàn cờ giữ nguyên. */
    @Test
    public void moveLeavingKingInCheck_rejected() {
        king(4, 7, W);
        rook(0, 6, W);            // quân nhà không liên quan
        rook(4, 0, B);            // xe đen chiếu dọc cột e vào vua trắng

        // Trắng đi xe (0,6)->(1,6): không hoá giải chiếu → vua vẫn hở → từ chối
        boolean ok = board.move(0, 6, 1, 6);

        assertFalse("move() phải trả false", ok);
        assertSame("xe phải về chỗ cũ", board.chessmen[0][6].type, Chessman.ChessmanType.Rook);
        assertNull("ô đích phải trống", board.chessmen[1][6]);
    }

    /** Nước hoá giải chiếu (chặn) phải được chấp nhận. */
    @Test
    public void moveBlockingCheck_accepted() {
        king(4, 7, W);
        rook(4, 0, B);            // xe đen chiếu cột e
        Rook blocker = rook(0, 4, W);

        // Xe trắng (0,4)->(4,4) chặn giữa xe đen và vua
        boolean ok = board.move(0, 4, 4, 4);

        assertTrue("nước chặn chiếu phải hợp lệ", ok);
        assertSame(blocker, board.chessmen[4][4]);
    }

    /** Bắt quân: quân địch bị đánh dấu chết + báo lên view (captured list). */
    @Test
    public void capture_marksDeadAndNotifiesView() {
        king(4, 7, W);
        king(0, 0, B);
        Rook target = rook(7, 7, B);
        rook(7, 0, W);            // xe trắng cột h

        board.doMove(new Point(7, 0), new Point(7, 7)); // Rxh1

        assertTrue("quân bị bắt phải chết", target.isDead);
        assertTrue("view phải nhận captured piece", view.captured.contains(target));
        assertSame("xe trắng chiếm ô", board.chessmen[7][7].color, W);
    }

    /** Sau khi tốt di chuyển, cờ firstMove phải tắt. */
    @Test
    public void pawnMove_clearsFirstMove() {
        king(4, 7, W);
        king(4, 0, B);
        Pawn p = new Pawn(new Point(4, 6), W, 8, board);
        p.firstMove = true;
        board.chessmen[4][6] = p;

        board.doMove(new Point(4, 6), new Point(4, 4)); // e2-e4

        assertFalse("firstMove phải false sau khi đi", p.firstMove);
        assertSame(p, board.chessmen[4][4]);
    }

    /** doMove cập nhật undo button + lịch sử nước đi. */
    @Test
    public void doMove_pushesHistory_andShowsUndo() {
        king(4, 7, W);
        king(4, 0, B);
        rook(0, 7, W);

        board.doMove(new Point(0, 7), new Point(0, 5));

        assertTrue("undo button phải hiện", view.undoVisible);
        assertEquals("lịch sử có 1 nước", 1, board.getMoveCount());
        assertTrue(board.canUndo());
    }
}
