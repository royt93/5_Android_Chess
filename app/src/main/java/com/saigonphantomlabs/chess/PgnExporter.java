package com.saigonphantomlabs.chess;

import java.util.List;

/**
 * Xuất ván cờ ra PGN + danh sách nước (SAN gần chuẩn). Thuần logic — không đụng Android,
 * testable trực tiếp JVM.
 *
 * Lưu ý: KHÔNG disambiguation (vd "Nbd7") và KHÔNG ký hiệu chiếu (+/#) — đủ đọc & share cho
 * ván casual; phần lớn viewer PGN vẫn parse được. Nâng cấp SAN đầy đủ = wave sau.
 */
public final class PgnExporter {

    private PgnExporter() { }

    /** SAN cho 1 nước (vd "e4", "exd5", "Nf3", "O-O", "e8=Q"). */
    public static String toSan(MoveRecord m) {
        if (m.isCastle) {
            // toX: 6 = cánh vua (g-file) O-O ; 2 = cánh hậu (c-file) O-O-O
            return (m.toX == 6) ? "O-O" : "O-O-O";
        }
        String dest = square(m.toX, m.toY);
        boolean capture = (m.capturedPiece != null) || m.isEnPassant;
        StringBuilder san = new StringBuilder();

        if (m.movedPiece.type == Chessman.ChessmanType.Pawn) {
            if (capture) san.append(file(m.fromX)).append('x');
            san.append(dest);
            if (m.promotedTo != null) san.append('=').append(pieceLetter(m.promotedTo));
        } else {
            san.append(pieceLetter(m.movedPiece.type));
            if (capture) san.append('x');
            san.append(dest);
        }
        return san.toString();
    }

    /** Movetext đánh số: "1. e4 e5 2. Nf3 Nc6 ...". */
    public static String buildMoveText(List<MoveRecord> moves) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moves.size(); i++) {
            if (i % 2 == 0) {
                if (i > 0) sb.append(' ');
                sb.append(i / 2 + 1).append(". ");
            } else {
                sb.append(' ');
            }
            sb.append(toSan(moves.get(i)));
        }
        return sb.toString();
    }

    /** Movetext nhiều dòng cho dialog: mỗi dòng 1 cặp nước "1. e4  e5". */
    public static String buildMoveTextMultiline(List<MoveRecord> moves) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moves.size(); i += 2) {
            sb.append(i / 2 + 1).append(". ").append(toSan(moves.get(i)));
            if (i + 1 < moves.size()) sb.append("   ").append(toSan(moves.get(i + 1)));
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * PGN đầy đủ (7-tag roster + movetext). [date] dạng "YYYY.MM.DD"; [result] vd "1-0","0-1","1/2-1/2","*".
     */
    public static String buildPgn(List<MoveRecord> moves, String white, String black,
            String date, String result) {
        String r = (result == null || result.isEmpty()) ? "*" : result;
        StringBuilder sb = new StringBuilder();
        sb.append("[Event \"Quick Chess\"]\n");
        sb.append("[Site \"Quick Chess App\"]\n");
        sb.append("[Date \"").append(date == null ? "????.??.??" : date).append("\"]\n");
        sb.append("[Round \"1\"]\n");
        sb.append("[White \"").append(white == null ? "White" : white).append("\"]\n");
        sb.append("[Black \"").append(black == null ? "Black" : black).append("\"]\n");
        sb.append("[Result \"").append(r).append("\"]\n\n");
        String mt = buildMoveText(moves);
        if (!mt.isEmpty()) sb.append(mt).append(' ');
        sb.append(r);
        return sb.toString();
    }

    private static String square(int x, int y) {
        return "" + file(x) + (8 - y);
    }

    private static char file(int x) {
        return (char) ('a' + x);
    }

    private static char pieceLetter(Chessman.ChessmanType type) {
        switch (type) {
            case King: return 'K';
            case Queen: return 'Q';
            case Rook: return 'R';
            case Bishop: return 'B';
            case Knight: return 'N';
            default: return ' '; // Pawn — không có chữ
        }
    }
}
