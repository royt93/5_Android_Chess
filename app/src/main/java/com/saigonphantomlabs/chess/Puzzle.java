package com.saigonphantomlabs.chess;

/**
 * 1 câu đố cờ (mate-in-N). Lời giải là 1 "line" các nước xen kẽ: index CHẴN = nước Trắng
 * (người chơi phải tìm), index LẺ = nước Đen (đáp trả, app tự đi). Nước Trắng cuối = chiếu hết.
 *
 * <p>Toạ độ mỗi nước: {@code int[]{fromX,fromY,toX,toY}}. Khởi tạo từ chuỗi đại số gọn "d1d8".
 * mate-in-1: 1 nước Trắng. mate-in-2: Trắng, Đen, Trắng.
 */
public final class Puzzle {
    public final String id;
    public final String fen;
    public final int mateIn;
    public final int[][] line; // mỗi phần tử {fx,fy,tx,ty}

    public Puzzle(String id, String fen, int mateIn, String... moves) {
        this.id = id;
        this.fen = fen;
        this.mateIn = mateIn;
        this.line = new int[moves.length][];
        for (int i = 0; i < moves.length; i++) {
            this.line[i] = parseMove(moves[i]);
        }
    }

    /** Nước Trắng người chơi cần tìm ở lượt thứ {@code whitePly} (0-based). null nếu vượt phạm vi. */
    public int[] whiteMove(int whitePly) {
        int idx = whitePly * 2;
        return (idx < line.length) ? line[idx] : null;
    }

    /** Nước Đen đáp trả NGAY SAU nước Trắng thứ {@code whitePly}. null nếu không có (nước cuối). */
    public int[] blackReply(int whitePly) {
        int idx = whitePly * 2 + 1;
        return (idx < line.length) ? line[idx] : null;
    }

    /** Nước Trắng đầu (lời giải gợi ý). */
    public Point keyFrom() { return new Point(line[0][0], line[0][1]); }
    public Point keyTo()   { return new Point(line[0][2], line[0][3]); }

    private static int[] parseMove(String m) {
        Point a = FenParser.parseSquare(m.substring(0, 2));
        Point b = FenParser.parseSquare(m.substring(2, 4));
        return new int[]{a.x, a.y, b.x, b.y};
    }
}
