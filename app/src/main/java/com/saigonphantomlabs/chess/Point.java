package com.saigonphantomlabs.chess;

public class Point {
    public int x;
    public int y;

    /*
     *  <- X ->
     *  ........ ˄
     *  ........ |
     *  ........
     *  ........ Y
     *  ........
     *  ........ |
     *  ........ ˅
     *  ........
     * */

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isValid() {
        return isValid(x, y);
    }

    public static boolean isValid(Point p) {
        return isValid(p.x, p.y);
    }

    public static boolean isValid(int x, int y) {
        return (x >= 0 && x < 8) && (y >= 0 && y < 8);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        return ((Point) obj).x == this.x && ((Point) obj).y == this.y;
    }

    // [OPT-03] hashCode() phải đi cùng equals() để giữ đúng contract.
    // x,y luôn nằm trong [0,7] nên (x << 3) | y cho mã băm duy nhất, không đụng độ.
    @Override
    public int hashCode() {
        return (x << 3) | (y & 7);
    }
}
