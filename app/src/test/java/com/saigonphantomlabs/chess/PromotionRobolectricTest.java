package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Robolectric test PHONG CẤP (promotion) + undo — cần Android thật (createButton, addView,
 * animator) nên không chạy được ở test seam thuần JVM. Dùng nhánh tự-phong-cấp của AI
 * ({@code isVsComputer && Black}) để kích hoạt {@code promotionResault} qua {@code doMove}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PromotionRobolectricTest {

    private Chess board;
    private RecordingChessBoardView view;

    private static final Chessman.PlayerColor W = Chessman.PlayerColor.White;
    private static final Chessman.PlayerColor B = Chessman.PlayerColor.Black;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        FrameLayout layout = new FrameLayout(ctx);

        board = new Chess();                  // test seam (bàn trống)
        view = new RecordingChessBoardView();
        board.setBoardViewForTest(view);

        // 2 vua + 1 tốt Đen sắp phong cấp (a7 = (0,6) → đi xuống a8 = (0,7))
        King wk = new King(new Point(4, 5), W, 8, board);
        board.chessmen[4][5] = wk; board.whiteKing = wk;
        King bk = new King(new Point(4, 0), B, 8, board);
        board.chessmen[4][0] = bk; board.blackKing = bk;
        Pawn bp = new Pawn(new Point(0, 6), B, 8, board);
        bp.firstMove = false;
        board.chessmen[0][6] = bp;

        // Inject ctx + boardLayout (tạo button cho các quân hiện có). isVsComputer vẫn false
        // tại đây nên changeTurn() bên trong không kích hoạt AI handler (aiHandler null ở seam).
        board.changeLayout(ctx, 800, layout);
        board.whichPlayerTurn = B;            // Đen tới lượt
        board.isVsComputer = true;            // bật nhánh auto-promote cho Đen
    }

    @Test
    public void blackPawnReachesLastRank_autoPromotesToQueen() {
        board.doMove(new Point(0, 6), new Point(0, 7)); // a7-a8 → tự phong Hậu

        Chessman promoted = board.chessmen[0][7];
        assertNotNull("phải có quân ở ô phong cấp", promoted);
        assertEquals("tốt phải biến thành Hậu", Chessman.ChessmanType.Queen, promoted.type);
        assertEquals(1, board.getMoveCount());
    }

    @Test
    public void undoPromotion_restoresPawn() {
        board.doMove(new Point(0, 6), new Point(0, 7));
        assertEquals(Chessman.ChessmanType.Queen, board.chessmen[0][7].type);

        board.undoLastMove();

        assertNull("ô phong cấp trống lại", board.chessmen[0][7]);
        Chessman back = board.chessmen[0][6];
        assertNotNull("tốt phải trở về a7", back);
        assertEquals("phải là Tốt trở lại (không còn Hậu)", Chessman.ChessmanType.Pawn, back.type);
        assertEquals(0, board.getMoveCount());
    }
}
