package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Unit test thuần JVM cho {@link Point} — chứng minh equals/hashCode đúng contract
 * (Wave 1: bổ sung hashCode).
 */
public class PointTest {

    @Test
    public void equals_samCoords_true() {
        assertEquals(new Point(3, 5), new Point(3, 5));
    }

    @Test
    public void equals_differentCoords_false() {
        assertNotEquals(new Point(3, 5), new Point(5, 3));
        assertNotEquals(new Point(0, 0), new Point(0, 1));
    }

    @Test
    public void hashCode_consistentWithEquals() {
        // Contract: a.equals(b) ⇒ a.hashCode() == b.hashCode()
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Point a = new Point(x, y);
                Point b = new Point(x, y);
                assertEquals(a, b);
                assertEquals("hashCode phải bằng nhau khi equals", a.hashCode(), b.hashCode());
            }
        }
    }

    @Test
    public void hashCode_uniqueForAllBoardSquares() {
        // 64 ô bàn cờ phải cho 64 mã băm khác nhau (không đụng độ)
        HashSet<Integer> hashes = new HashSet<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                hashes.add(new Point(x, y).hashCode());
            }
        }
        assertEquals(64, hashes.size());
    }

    @Test
    public void worksInHashSet_andArrayListContains() {
        HashSet<Point> set = new HashSet<>();
        set.add(new Point(2, 6));
        assertTrue(set.contains(new Point(2, 6)));
        assertFalse(set.contains(new Point(6, 2)));

        ArrayList<Point> list = new ArrayList<>();
        list.add(new Point(4, 4));
        assertTrue(list.contains(new Point(4, 4)));
    }
}
