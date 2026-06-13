package com.saigonphantomlabs.chess;

/**
 * Parser FEN → {@link GameSaveManager.SavedGame} (thuần, testable JVM) để tái dùng
 * {@link Chess#loadSaveState} dựng thế cờ cho câu đố. Cũng có helper toạ độ "e4" ↔ {@link Point}.
 *
 * <p>Quy ước bàn cờ engine: y=0 là hàng TRÊN (rank 8), y=7 là hàng DƯỚI (rank 1); x=0..7 = cột a..h.
 * FEN liệt kê rank từ 8→1 nên rank đầu tiên = y=0 (khớp trực tiếp).
 */
public final class FenParser {

    private FenParser() { }

    /** Parse FEN đầy đủ thành SavedGame (board + lượt + en passant + 50-nước). Trả null nếu FEN sai. */
    public static GameSaveManager.SavedGame toSavedGame(String fen) {
        if (fen == null) return null;
        String[] f = fen.trim().split("\\s+");
        if (f.length < 1 || f[0].isEmpty()) return null;

        String[] ranks = f[0].split("/");
        if (ranks.length != 8) return null;

        String side = f.length > 1 ? f[1] : "w";
        String castling = f.length > 2 ? f[2] : "-";
        String ep = f.length > 3 ? f[3] : "-";
        int half = 0;
        if (f.length > 4) {
            try { half = Integer.parseInt(f[4]); } catch (NumberFormatException ignored) { }
        }

        boolean wK = castling.indexOf('K') >= 0, wQ = castling.indexOf('Q') >= 0;
        boolean bK = castling.indexOf('k') >= 0, bQ = castling.indexOf('q') >= 0;

        GameSaveManager.SavedGame g = new GameSaveManager.SavedGame();
        boolean sawWhiteKing = false, sawBlackKing = false;

        for (int y = 0; y < 8; y++) {
            String row = ranks[y];
            int x = 0;
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c >= '1' && c <= '8') { x += c - '0'; continue; }
                if (x > 7) return null; // tràn cột
                Chessman.ChessmanType type = typeOf(Character.toLowerCase(c));
                if (type == null) return null; // ký tự lạ
                Chessman.PlayerColor color = Character.isUpperCase(c)
                        ? Chessman.PlayerColor.White : Chessman.PlayerColor.Black;
                if (type == Chessman.ChessmanType.King) {
                    if (color == Chessman.PlayerColor.White) sawWhiteKing = true; else sawBlackKing = true;
                }
                boolean moved = computeMoved(type, color, x, y, wK, wQ, bK, bQ);
                g.pieces.add(new GameSaveManager.PieceData(x, y, type, color, moved));
                x++;
            }
            if (x != 8) return null; // mỗi rank phải đủ 8 ô
        }
        if (!sawWhiteKing || !sawBlackKing) return null; // thế cờ phải có đủ 2 vua

        g.turn = side.startsWith("b") ? Chessman.PlayerColor.Black : Chessman.PlayerColor.White;
        g.enPassant = parseSquare(ep);
        g.halfMoveClock = Math.max(0, half);
        g.isVsAi = false;
        g.difficulty = null;
        g.hasClock = false;
        return g;
    }

    /** "e4" → Point(x,y). Trả null nếu sai định dạng / ngoài bàn / "-". */
    public static Point parseSquare(String sq) {
        if (sq == null || sq.length() != 2) return null;
        int x = sq.charAt(0) - 'a';
        int rank = sq.charAt(1) - '0';
        if (x < 0 || x > 7 || rank < 1 || rank > 8) return null;
        int y = 8 - rank;
        return new Point(x, y);
    }

    private static Chessman.ChessmanType typeOf(char lower) {
        switch (lower) {
            case 'k': return Chessman.ChessmanType.King;
            case 'q': return Chessman.ChessmanType.Queen;
            case 'r': return Chessman.ChessmanType.Rook;
            case 'b': return Chessman.ChessmanType.Bishop;
            case 'n': return Chessman.ChessmanType.Knight;
            case 'p': return Chessman.ChessmanType.Pawn;
            default:  return null;
        }
    }

    /**
     * Cờ "đã đi" để {@link Chess#loadSaveState} khôi phục quyền nhập thành / đẩy 2 ô:
     * Vua/Xe suy từ quyền castling FEN (mất quyền ⇒ coi như đã đi); Tốt suy từ việc còn ở hàng xuất phát.
     */
    private static boolean computeMoved(Chessman.ChessmanType type, Chessman.PlayerColor color,
            int x, int y, boolean wK, boolean wQ, boolean bK, boolean bQ) {
        switch (type) {
            case King:
                return color == Chessman.PlayerColor.White ? !(wK || wQ) : !(bK || bQ);
            case Rook:
                if (color == Chessman.PlayerColor.White && y == 7) {
                    if (x == 0) return !wQ; // a1
                    if (x == 7) return !wK; // h1
                } else if (color == Chessman.PlayerColor.Black && y == 0) {
                    if (x == 0) return !bQ; // a8
                    if (x == 7) return !bK; // h8
                }
                return true; // xe không ở ô gốc ⇒ coi như đã đi (không có quyền nhập thành)
            case Pawn:
                // Tốt còn ở hàng xuất phát ⇒ chưa đi (cho phép đẩy 2 ô). Trắng rank2=y6, Đen rank7=y1.
                return color == Chessman.PlayerColor.White ? (y != 6) : (y != 1);
            default:
                return false; // Q/B/N không dùng cờ moved
        }
    }
}
