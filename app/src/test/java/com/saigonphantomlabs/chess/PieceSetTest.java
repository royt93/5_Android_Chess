package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Unit test thuần cho {@link PieceSet} — định nghĩa bộ quân + parse an toàn. */
public class PieceSetTest {

    @Test
    public void classic_hasNoTint() {
        assertEquals(0, PieceSet.CLASSIC.whiteTint);
        assertEquals(0, PieceSet.CLASSIC.blackTint);
        assertEquals(0, PieceSet.CLASSIC.tintFor(true));
        assertEquals(0, PieceSet.CLASSIC.tintFor(false));
    }

    @Test
    public void nonClassic_hasTintAndNameRes() {
        for (PieceSet s : PieceSet.values()) {
            assertTrue("nameRes hợp lệ: " + s, s.nameRes != 0);
            assertTrue("emoji không rỗng: " + s, s.emoji != null && !s.emoji.isEmpty());
            if (s != PieceSet.CLASSIC) {
                assertNotEquals("set màu phải có whiteTint: " + s, 0, s.whiteTint);
            }
        }
    }

    @Test
    public void tintFor_returnsWhiteOrBlack() {
        assertEquals(PieceSet.GOLD.whiteTint, PieceSet.GOLD.tintFor(true));
        assertEquals(PieceSet.GOLD.blackTint, PieceSet.GOLD.tintFor(false));
    }

    @Test
    public void fromName_safeParse() {
        assertEquals(PieceSet.GOLD, PieceSet.fromName("GOLD"));
        assertEquals(PieceSet.NEON, PieceSet.fromName("NEON"));
        assertEquals(PieceSet.CLASSIC, PieceSet.fromName(null));
        assertEquals(PieceSet.CLASSIC, PieceSet.fromName("DOES_NOT_EXIST"));
    }
}
