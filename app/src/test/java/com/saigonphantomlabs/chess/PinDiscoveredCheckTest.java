package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test các tình huống phát sinh từ kiểm tra an toàn vua (không phải luật riêng): GHIM (pin)
 * và MỞ CHIẾU (discovered check). Cả hai đều "miễn phí" nhờ {@code move()} mô phỏng + kiểm
 * {@code isPointSafe()}; chứng minh engine xử lý đúng mà không cần code đặc biệt.
 */
public class PinDiscoveredCheckTest {

    private Chess board;

    private static final Chessman.PlayerColor W = Chessman.PlayerColor.White;
    private static final Chessman.PlayerColor B = Chessman.PlayerColor.Black;

    @Before
    public void setUp() {
        board = new Chess();
        board.setBoardViewForTest(new NoOpChessBoardView());
    }

    private King king(int x, int y, Chessman.PlayerColor c) {
        King k = new King(new Point(x, y), c, 8, board);
        board.chessmen[x][y] = k;
        if (c == W) board.whiteKing = k; else board.blackKing = k;
        return k;
    }

    // ---------------- PIN ----------------

    /** Quân bị ghim (Mã chắn giữa Vua và Xe địch) KHÔNG được rời tuyến ghim. */
    @Test
    public void pinnedKnight_cannotLeavePinLine() {
        king(4, 7, W);                                   // Vua Trắng e1
        Knight knight = new Knight(new Point(4, 6), W, 8, board); // Mã e2 (ghim)
        board.chessmen[4][6] = knight;
        board.chessmen[4][0] = new Rook(new Point(4, 0), B, 8, board); // Xe Đen e8 ghim dọc cột e

        boolean ok = board.move(4, 6, 3, 4); // Mã e2-d4 (rời cột e)

        assertFalse("Mã bị ghim không được rời tuyến → để hở Vua", ok);
        assertSame("Mã phải ở nguyên e2", knight, board.chessmen[4][6]);
        assertNull(board.chessmen[3][4]);
    }

    /** Quân bị ghim VẪN được đi DỌC theo tuyến ghim (Xe trượt trên cột e vẫn chắn Vua). */
    @Test
    public void pinnedRook_mayMoveAlongPinLine() {
        king(4, 7, W);                                   // Vua Trắng e1
        Rook rook = new Rook(new Point(4, 6), W, 8, board); // Xe e2 (ghim)
        board.chessmen[4][6] = rook;
        board.chessmen[4][0] = new Rook(new Point(4, 0), B, 8, board); // Xe Đen e8

        boolean along = board.move(4, 6, 4, 5);          // e2-e3 dọc cột → vẫn chắn

        assertTrue("Xe bị ghim vẫn đi dọc tuyến ghim được", along);
        assertSame(rook, board.chessmen[4][5]);
    }

    // ---------------- DISCOVERED CHECK ----------------

    /** Mở chiếu: di chuyển quân chắn để lộ đường chiếu của Xe sau lưng vào Vua địch. */
    @Test
    public void movingBlocker_revealsDiscoveredCheck() {
        king(0, 7, W);                                   // Vua Trắng a1 (xa, không bị ảnh hưởng)
        king(4, 0, B);                                   // Vua Đen e8
        board.chessmen[4][7] = new Rook(new Point(4, 7), W, 8, board); // Xe Trắng e1 trên cột e
        Knight blocker = new Knight(new Point(4, 3), W, 8, board);     // Mã Trắng e4 chắn cột e
        board.chessmen[4][3] = blocker;

        assertTrue("Trước khi đi: Vua Đen an toàn (Mã chắn)", board.blackKing.isPointSafe());

        boolean ok = board.move(4, 3, 2, 2);             // Mã e4-c5 mở cột e

        assertTrue("Nước mở chiếu hợp lệ (Vua Trắng vẫn an toàn)", ok);
        assertFalse("Mở chiếu: Vua Đen giờ bị Xe e1 chiếu", board.blackKing.isPointSafe());
    }
}
