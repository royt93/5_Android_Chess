package com.saigonphantomlabs.chess;

import java.util.ArrayList;
import java.util.List;

/**
 * Phân tích sau ván (blunder detection) — THUẦN logic, testable JVM, KHÔNG đụng Android.
 *
 * <p>Cách làm: dựng lại bàn cờ model-only từ thế bắt đầu, replay từng {@link MoveRecord}. Với mỗi
 * nước, so điểm tốt nhất bên đi có thể đạt ({@code bestCp}) với điểm thực tế sau nước đã đánh
 * ({@code playedCp}) — cùng độ sâu search qua {@link AIEngine#searchBestScore}. Chênh lệch (centipawn
 * loss) ⇒ gắn nhãn chất lượng. Tái dùng 100% engine, không thêm asset → nhẹ APK.
 *
 * <p><b>Giới hạn v1:</b> chỉ phân tích ván chơi TỪ ĐẦU (nước đầu là Trắng); ván resume bị cắt
 * history nên không hỗ trợ ({@link #analyze} trả {@code null}). Phong cấp giả định Hậu (Queen) vì
 * {@link MoveRecord} không lưu lựa chọn under-promotion. Search nông (depth 2) ⇒ có thể chấm "nhầm"
 * nước thí quân tốt thành sai lầm — chấp nhận cho app casual.
 */
public final class GameAnalyzer {

    private GameAnalyzer() { }

    /**
     * Độ sâu search cho phân tích (negamax). 3 ⇒ eval nước đã đánh dùng depth 2 cho đối thủ ⇒ phát
     * hiện được cả "để đối thủ CHIẾU HẾT sau đó" (vd Fool's mate), không chỉ treo quân tức thì.
     */
    private static final int ANALYSIS_DEPTH = 3;

    // Ngưỡng centipawn loss để phân loại (bảo thủ — tránh báo nhầm blunder).
    private static final int BLUNDER_CP = 300;
    private static final int MISTAKE_CP = 150;
    private static final int INACCURACY_CP = 75;
    private static final int BEST_CP = 15;

    /** Nhãn chất lượng 1 nước đi, từ tốt → tệ. */
    public enum Quality {
        BEST, GOOD, INACCURACY, MISTAKE, BLUNDER
    }

    /** Kết quả phân tích 1 nửa-nước. */
    public static final class MovePly {
        public final int plyIndex;     // 0-based theo thứ tự đánh
        public final boolean whiteMove;
        public final String san;       // ký hiệu nước (vd "Nf3", "exd5", "O-O")
        public final int playedCp;     // điểm cho bên đi SAU nước thực đánh
        public final int bestCp;       // điểm tốt nhất bên đi có thể đạt
        public final int lossCp;       // bestCp - playedCp (>= 0)
        public final Quality quality;

        MovePly(int plyIndex, boolean whiteMove, String san,
                int playedCp, int bestCp, int lossCp, Quality quality) {
            this.plyIndex = plyIndex;
            this.whiteMove = whiteMove;
            this.san = san;
            this.playedCp = playedCp;
            this.bestCp = bestCp;
            this.lossCp = lossCp;
            this.quality = quality;
        }
    }

    /** Tổng hợp toàn ván: danh sách nước + đếm lỗi & độ chính xác 2 bên. */
    public static final class Result {
        public final List<MovePly> plies;
        public final int whiteBlunders, whiteMistakes, whiteInaccuracies;
        public final int blackBlunders, blackMistakes, blackInaccuracies;
        public final int whiteAccuracy, blackAccuracy; // 0..100

        Result(List<MovePly> plies,
               int wB, int wM, int wI, int bB, int bM, int bI,
               int wAcc, int bAcc) {
            this.plies = plies;
            this.whiteBlunders = wB; this.whiteMistakes = wM; this.whiteInaccuracies = wI;
            this.blackBlunders = bB; this.blackMistakes = bM; this.blackInaccuracies = bI;
            this.whiteAccuracy = wAcc; this.blackAccuracy = bAcc;
        }
    }

    /**
     * Phân tích toàn ván. Trả {@code null} nếu không phân tích được (rỗng, hoặc nước đầu không phải
     * Trắng ⇒ ván resume/cắt history). An toàn gọi trên background thread (không đụng UI/Android).
     */
    public static Result analyze(List<MoveRecord> history, AIEngine engine) {
        if (history == null || history.isEmpty() || engine == null) return null;
        // Nước đầu phải là Trắng — nếu không, history bị cắt (ván resume) → không replay từ thế đầu được.
        if (history.get(0).movedPiece == null
                || history.get(0).movedPiece.color != Chessman.PlayerColor.White) {
            return null;
        }

        Chess board = freshStartBoard();
        List<MovePly> plies = new ArrayList<>(history.size());

        int wB = 0, wM = 0, wI = 0, bB = 0, bM = 0, bI = 0;
        long wLossSum = 0, bLossSum = 0;
        int wMoves = 0, bMoves = 0;

        for (int i = 0; i < history.size(); i++) {
            MoveRecord rec = history.get(i);
            if (rec.movedPiece == null) break; // history hỏng giữa chừng → dừng an toàn (không NPE)
            Chessman.PlayerColor mover = rec.movedPiece.color;
            Chessman.PlayerColor opp = (mover == Chessman.PlayerColor.White)
                    ? Chessman.PlayerColor.Black : Chessman.PlayerColor.White;

            int bestCp = engine.searchBestScore(board, mover, ANALYSIS_DEPTH);
            applyMove(board, rec);
            int playedCp = -engine.searchBestScore(board, opp, ANALYSIS_DEPTH - 1);

            int loss = Math.max(0, bestCp - playedCp);
            Quality q = classify(loss);
            boolean white = mover == Chessman.PlayerColor.White;
            plies.add(new MovePly(i, white, PgnExporter.toSan(rec), playedCp, bestCp, loss, q));

            if (white) {
                wMoves++; wLossSum += loss;
                if (q == Quality.BLUNDER) wB++;
                else if (q == Quality.MISTAKE) wM++;
                else if (q == Quality.INACCURACY) wI++;
            } else {
                bMoves++; bLossSum += loss;
                if (q == Quality.BLUNDER) bB++;
                else if (q == Quality.MISTAKE) bM++;
                else if (q == Quality.INACCURACY) bI++;
            }
        }

        int wAcc = accuracy(wLossSum, wMoves);
        int bAcc = accuracy(bLossSum, bMoves);
        return new Result(plies, wB, wM, wI, bB, bM, bI, wAcc, bAcc);
    }

    /** Phân loại theo centipawn loss. */
    static Quality classify(int loss) {
        if (loss >= BLUNDER_CP) return Quality.BLUNDER;
        if (loss >= MISTAKE_CP) return Quality.MISTAKE;
        if (loss >= INACCURACY_CP) return Quality.INACCURACY;
        if (loss <= BEST_CP) return Quality.BEST;
        return Quality.GOOD;
    }

    /**
     * Độ chính xác 0..100 từ centipawn loss trung bình (ACPL). Hàm mũ giảm dần: ACPL 0→100%,
     * ~100→~72%, ~300→~37%. Chỉ để hiển thị engagement, không phải con số chuẩn engine lớn.
     */
    static int accuracy(long lossSum, int moves) {
        if (moves <= 0) return 100;
        double acpl = (double) lossSum / moves;
        double acc = 100.0 * Math.exp(-acpl / 300.0);
        return (int) Math.round(Math.max(0, Math.min(100, acc)));
    }

    /**
     * Áp 1 nước đã ghi vào bàn model (chỉ mutate chessmen/king/cờ — KHÔNG UI/sound). Tự suy castle /
     * en passant / phong cấp từ flag của {@link MoveRecord}. Hoạt động trên quân CỦA bàn replay
     * (định vị theo toạ độ), không đụng {@code rec.movedPiece} của ván gốc.
     */
    static void applyMove(Chess b, MoveRecord rec) {
        int fx = rec.fromX, fy = rec.fromY, tx = rec.toX, ty = rec.toY;
        Chessman piece = b.chessmen[fx][fy];
        if (piece == null) return; // an toàn (không nên xảy ra với history hợp lệ)

        // En passant: gỡ con tốt bị bắt (nằm lệch ô đích)
        if (rec.isEnPassant) {
            b.chessmen[rec.epVictimX][rec.epVictimY] = null;
        }

        // Di chuyển quân (capture thường: quân ở ô đích bị ghi đè ⇒ biến mất khỏi bàn)
        b.chessmen[tx][ty] = piece;
        b.chessmen[fx][fy] = null;
        piece.setPoint(new Point(tx, ty));

        if (piece instanceof King) ((King) piece).hasMoved = true;
        if (piece instanceof Rook) ((Rook) piece).hasMoved = true;
        if (piece instanceof Pawn) ((Pawn) piece).firstMove = false;

        // Nhập thành: di chuyển xe theo
        if (rec.isCastle) {
            Chessman rook = b.chessmen[rec.rookFromX][rec.rookFromY];
            if (rook != null) {
                b.chessmen[rec.rookToX][rec.rookToY] = rook;
                b.chessmen[rec.rookFromX][rec.rookFromY] = null;
                rook.setPoint(new Point(rec.rookToX, rec.rookToY));
                if (rook instanceof Rook) ((Rook) rook).hasMoved = true;
            }
        }

        // Phong cấp → Hậu (v1 giả định Queen vì MoveRecord không lưu lựa chọn)
        boolean promo = (piece instanceof Pawn)
                && ((piece.color == Chessman.PlayerColor.White && ty == 0)
                || (piece.color == Chessman.PlayerColor.Black && ty == 7));
        if (promo) {
            Queen q = new Queen(new Point(tx, ty), piece.color, b.minDimension, b);
            b.chessmen[tx][ty] = q;
        }

        // En passant target cho nước kế tiếp (tốt vừa đi 2 ô)
        if (piece.type == Chessman.ChessmanType.Pawn && Math.abs(ty - fy) == 2) {
            b.enPassantTarget = new Point(fx, (fy + ty) / 2);
        } else {
            b.enPassantTarget = null;
        }
    }

    /** Bàn cờ model-only ở thế bắt đầu chuẩn (Trắng đi trước). Không tạo button (không Android). */
    static Chess freshStartBoard() {
        Chess b = new Chess(); // test-seam: bàn rỗng, không khởi tạo Android
        Chessman.PlayerColor BL = Chessman.PlayerColor.Black;
        Chessman.PlayerColor WH = Chessman.PlayerColor.White;

        b.chessmen[0][0] = new Rook(new Point(0, 0), BL, 0, b);
        b.chessmen[1][0] = new Knight(new Point(1, 0), BL, 0, b);
        b.chessmen[2][0] = new Bishop(new Point(2, 0), BL, 0, b);
        b.chessmen[3][0] = new Queen(new Point(3, 0), BL, 0, b);
        b.chessmen[4][0] = b.blackKing = new King(new Point(4, 0), BL, 0, b);
        b.chessmen[5][0] = new Bishop(new Point(5, 0), BL, 0, b);
        b.chessmen[6][0] = new Knight(new Point(6, 0), BL, 0, b);
        b.chessmen[7][0] = new Rook(new Point(7, 0), BL, 0, b);
        for (int i = 0; i < 8; i++) b.chessmen[i][1] = new Pawn(new Point(i, 1), BL, 0, b);

        for (int i = 0; i < 8; i++) b.chessmen[i][6] = new Pawn(new Point(i, 6), WH, 0, b);
        b.chessmen[0][7] = new Rook(new Point(0, 7), WH, 0, b);
        b.chessmen[1][7] = new Knight(new Point(1, 7), WH, 0, b);
        b.chessmen[2][7] = new Bishop(new Point(2, 7), WH, 0, b);
        b.chessmen[3][7] = new Queen(new Point(3, 7), WH, 0, b);
        b.chessmen[4][7] = b.whiteKing = new King(new Point(4, 7), WH, 0, b);
        b.chessmen[5][7] = new Bishop(new Point(5, 7), WH, 0, b);
        b.chessmen[6][7] = new Knight(new Point(6, 7), WH, 0, b);
        b.chessmen[7][7] = new Rook(new Point(7, 7), WH, 0, b);

        b.whichPlayerTurn = WH; // Trắng đi trước (người chơi)
        return b;
    }
}
