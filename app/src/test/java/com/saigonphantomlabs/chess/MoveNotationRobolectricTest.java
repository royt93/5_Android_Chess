package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Robolectric test cho {@link MoveRecord#getNotation(Context)} — cần Context để đọc chuỗi
 * ký hiệu quân/ăn quân từ resources. Tên ô (a1..h8) tính bằng code nên kiểm tra ổn định.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MoveNotationRobolectricTest {

    private Context ctx() {
        return ApplicationProvider.getApplicationContext();
    }

    private static final Chessman.PlayerColor W = Chessman.PlayerColor.White;

    @Test
    public void pawnPush_containsBothSquares() {
        Pawn p = new Pawn(new Point(4, 6), W, 8, null); // e2
        MoveRecord m = new MoveRecord(4, 6, 4, 4, p, null, null, true); // e2-e4
        String n = m.getNotation(ctx());
        assertTrue("chứa ô xuất phát e2: " + n, n.contains("e2"));
        assertTrue("chứa ô đích e4: " + n, n.contains("e4"));
    }

    @Test
    public void rookCapture_differsFromQuietMove() {
        Rook r = new Rook(new Point(0, 7), W, 8, null); // a1
        Rook victim = new Rook(new Point(0, 4), Chessman.PlayerColor.Black, 8, null);

        String capture = new MoveRecord(0, 7, 0, 4, r, victim, null, false).getNotation(ctx());
        String quiet   = new MoveRecord(0, 7, 0, 4, r, null, null, false).getNotation(ctx());

        assertTrue(capture.contains("a1"));
        assertTrue(capture.contains("a4"));
        assertNotEquals("ký hiệu ăn quân phải khác nước đi thường", capture, quiet);
    }

    @Test
    public void promotionNotation_notEmpty() {
        Pawn p = new Pawn(new Point(0, 1), W, 8, null);
        MoveRecord m = new MoveRecord(0, 1, 0, 0, p, null, Chessman.ChessmanType.Queen, false);
        String n = m.getNotation(ctx());
        assertFalse("ký hiệu phong cấp không rỗng", n.isEmpty());
    }
}
