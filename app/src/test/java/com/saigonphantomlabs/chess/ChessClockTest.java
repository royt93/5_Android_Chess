package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Unit test thuần JVM cho {@link ChessClock} — tick, switch, increment, flag, format. */
public class ChessClockTest {

    @Test public void tick_decrementsActiveSideOnly() {
        ChessClock c = new ChessClock(60_000, 0);
        c.setWhiteActive(true);
        c.tick(5_000);
        assertEquals(55_000, c.getWhiteMs());
        assertEquals(60_000, c.getBlackMs()); // bên kia không bị trừ
    }

    @Test public void onMoveCompleted_switchesActiveSide() {
        ChessClock c = new ChessClock(60_000, 0);
        c.setWhiteActive(true);
        c.onMoveCompleted();
        assertFalse(c.isWhiteActive());
        c.tick(3_000);
        assertEquals(57_000, c.getBlackMs());
        assertEquals(60_000, c.getWhiteMs());
    }

    @Test public void increment_addedToMoverOnComplete() {
        ChessClock c = new ChessClock(60_000, 2_000); // +2s/nước
        c.setWhiteActive(true);
        c.tick(5_000);          // white còn 55s
        c.onMoveCompleted();    // white +2s = 57s, chuyển sang black
        assertEquals(57_000, c.getWhiteMs());
        assertFalse(c.isWhiteActive());
    }

    @Test public void tick_clampsAtZero() {
        ChessClock c = new ChessClock(3_000, 0);
        c.setWhiteActive(true);
        c.tick(5_000);
        assertEquals(0, c.getWhiteMs());
    }

    @Test public void flagged_detectsTimeout() {
        ChessClock c = new ChessClock(2_000, 0);
        c.setWhiteActive(true);
        assertEquals(ChessClock.Flag.NONE, c.flagged());
        c.tick(2_000);
        assertEquals(ChessClock.Flag.WHITE, c.flagged());
    }

    @Test public void flagged_black() {
        ChessClock c = new ChessClock(1_000, 0);
        c.setWhiteActive(false);
        c.tick(1_000);
        assertEquals(ChessClock.Flag.BLACK, c.flagged());
    }

    @Test public void format_minutesSeconds() {
        assertEquals("5:00", ChessClock.format(300_000));
        assertEquals("1:05", ChessClock.format(65_000));
        assertEquals("0:30", ChessClock.format(30_000));
    }

    @Test public void format_underTenSeconds_showsTenths() {
        assertEquals("9.0", ChessClock.format(9_000));
        assertEquals("5.5", ChessClock.format(5_500));
        assertEquals("0.0", ChessClock.format(0));
    }
}
