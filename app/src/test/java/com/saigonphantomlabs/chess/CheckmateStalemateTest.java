package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration test PHÁT HIỆN KẾT THÚC VÁN (checkmate / stalemate / check) — chạy qua
 * {@code doMove()} → {@code checkOpponentKingStatus()} → quan sát callback
 * {@link RecordingChessBoardView#showCustomGameEndDialog}. Thuần JVM, không Android.
 *
 * Lưu ý: {@code move()} không kiểm tra luật di chuyển quân, chỉ kiểm an toàn vua —
 * nên ta dựng thế cờ tối giản rồi thực thi nước chiếu hết / ghim cờ trực tiếp.
 */
public class CheckmateStalemateTest {

    private Chess board;
    private RecordingChessBoardView view;

    private static final Chessman.PlayerColor W = Chessman.PlayerColor.White;
    private static final Chessman.PlayerColor B = Chessman.PlayerColor.Black;

    @Before
    public void setUp() {
        board = new Chess();
        view = new RecordingChessBoardView();
        board.setBoardViewForTest(view);
        board.whichPlayerTurn = W; // Trắng tới lượt → kiểm tra vua Đen sau nước đi
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

    private Queen queen(int x, int y, Chessman.PlayerColor c) {
        Queen q = new Queen(new Point(x, y), c, 8, board);
        board.chessmen[x][y] = q;
        return q;
    }

    /**
     * Chiếu hết kiểu hàng ngang (two-rook ladder): Vua Đen d8(3,0). Xe Trắng a7(0,1)
     * khoá hàng y=1; Xe Trắng h1(7,7) đi xuống h8(7,0) chiếu dọc hàng y=0 → hết cờ.
     */
    @Test
    public void checkmate_twoRookLadder_endsGameWhiteWins() {
        king(3, 0, B);
        king(4, 7, W);
        rook(0, 1, W);          // khoá hàng y=1
        rook(7, 7, W);          // xe sẽ chiếu hết

        board.doMove(new Point(7, 7), new Point(7, 0)); // Rh8#

        assertTrue("Phải hiện dialog kết thúc ván", view.gameEndShown);
        assertTrue("Trắng thắng", view.lastWhiteWins);
        assertFalse("Không phải hoà (stalemate)", view.lastIsStalemate);
    }

    /**
     * Chiếu (không hết): Xe Trắng chiếu dọc hàng y=0 nhưng Vua Đen còn thoát xuống y=1.
     * Game KHÔNG kết thúc.
     */
    @Test
    public void check_notMate_gameContinues() {
        king(4, 0, B);
        king(4, 7, W);
        rook(0, 7, W);

        board.doMove(new Point(0, 7), new Point(0, 0)); // Ra8+ (vua thoát xuống hàng 7)

        assertFalse("Chiếu thường không kết thúc ván", view.gameEndShown);
    }

    /**
     * Hết nước đi nhưng KHÔNG bị chiếu (stalemate → hoà): Vua Đen h8(7,0), Hậu Trắng đi
     * tới g6(6,2) khống chế mọi ô vua có thể tới nhưng không chiếu trực tiếp.
     */
    @Test
    public void stalemate_endsGameAsDraw() {
        king(7, 0, B);
        king(0, 7, W);
        queen(6, 7, W);

        board.doMove(new Point(6, 7), new Point(6, 2)); // Qg6 — stalemate

        assertTrue("Phải hiện dialog kết thúc ván", view.gameEndShown);
        assertTrue("Stalemate là hoà", view.lastIsStalemate);
    }

    /** Nước thường không chiếu → trạng thái SAFE, không kết thúc, không dialog. */
    @Test
    public void safeMove_noDialog() {
        king(4, 0, B);
        king(4, 7, W);
        rook(0, 6, W);

        board.doMove(new Point(0, 6), new Point(1, 6)); // nước vu vơ

        assertFalse(view.gameEndShown);
    }
}
