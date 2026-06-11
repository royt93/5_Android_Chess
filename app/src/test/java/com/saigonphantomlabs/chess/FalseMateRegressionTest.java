package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Regression từ ván chơi thật trên thiết bị (log 20:14:36): nghi ngờ "chiếu hết giả" khi
 * Vua Đen h2 bị Hậu Trắng h8 chiếu dọc cột h — engine báo CHECKMATE nhưng vua có thể thoát
 * sang cột g (g1/g2/g3) nếu không quân nào khống chế.
 *
 * Toạ độ (x,y): h2=(7,6), h8=(7,0), g1=(6,7), g2=(6,6), g3=(6,5).
 */
public class FalseMateRegressionTest {

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
     * Chỉ Hậu h8 chiếu Vua h2, cột g HOÀN TOÀN trống → PHẢI là Check (vua thoát g2),
     * KHÔNG được là chiếu hết. Nếu fail → bug false-mate ở isPointSafe/validateKing.
     */
    @Test
    public void queenOnHFile_kingCanFleeToGFile_isNotMate() {
        king(7, 6, B);            // Vua Đen h2
        king(0, 7, W);            // Vua Trắng a1 (xa)
        board.chessmen[7][0] = new Queen(new Point(7, 0), W, 8, board); // Hậu Trắng h8

        // Xác nhận trực tiếp: g2 (6,6) PHẢI an toàn cho vua đen (không quân nào khống chế)
        // → đẩy Hậu lên h8 bằng doMove để chạy đúng đường checkOpponentKingStatus.
        board.chessmen[7][0] = null;
        board.chessmen[7][1] = new Queen(new Point(7, 1), W, 8, board); // Hậu h7
        board.doMove(new Point(7, 1), new Point(7, 0)); // Qh7-h8+

        assertTrue("Phải chiếu (game chưa kết thúc bằng chiếu hết)",
                !view.gameEndShown || !view.lastWhiteWins == false);
        // Khẳng định mạnh: KHÔNG được kết thúc ván (vì vua thoát được sang g-file)
        assertFalse("Vua thoát sang cột g được → KHÔNG phải chiếu hết", view.gameEndShown);
    }

    /**
     * Có thêm Tượng Trắng d2 + tốt b4 (như cuối ván thật) nhưng chúng KHÔNG khống chế g1/g2/g3
     * → vẫn KHÔNG phải chiếu hết. Tái hiện sát thế cờ bị nghi ngờ.
     */
    @Test
    public void reconstructedEndgame_isNotMate() {
        king(7, 6, B);                                   // Vua Đen h2
        king(4, 7, W);                                   // Vua Trắng e1
        Bishop b = new Bishop(new Point(3, 6), W, 8, board); // Tượng d2 (ô tối)
        board.chessmen[3][6] = b;
        Pawn p = new Pawn(new Point(1, 4), W, 8, board);     // Tốt b4
        p.firstMove = false;
        board.chessmen[1][4] = p;
        board.chessmen[7][1] = new Queen(new Point(7, 1), W, 8, board); // Hậu h7

        board.doMove(new Point(7, 1), new Point(7, 0));  // Qh8+

        assertFalse("g1/g2/g3 trống → vua thoát → KHÔNG phải chiếu hết", view.gameEndShown);
    }
}
