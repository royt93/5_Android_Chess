package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Test cho {@link GameAnalyzer} — phân loại, accuracy, fidelity của applyMove (replay) và
 * phát hiện blunder end-to-end. Thuần JVM, không cần Android.
 */
public class GameAnalyzerTest {

    private final AIEngine engine = new AIEngine();

    // ───────────────────────── classify / accuracy (thuần) ─────────────────────────

    @Test
    public void classify_thresholds() {
        assertEquals(GameAnalyzer.Quality.BEST, GameAnalyzer.classify(0));
        assertEquals(GameAnalyzer.Quality.BEST, GameAnalyzer.classify(15));
        assertEquals(GameAnalyzer.Quality.GOOD, GameAnalyzer.classify(16));
        assertEquals(GameAnalyzer.Quality.GOOD, GameAnalyzer.classify(74));
        assertEquals(GameAnalyzer.Quality.INACCURACY, GameAnalyzer.classify(75));
        assertEquals(GameAnalyzer.Quality.INACCURACY, GameAnalyzer.classify(149));
        assertEquals(GameAnalyzer.Quality.MISTAKE, GameAnalyzer.classify(150));
        assertEquals(GameAnalyzer.Quality.MISTAKE, GameAnalyzer.classify(299));
        assertEquals(GameAnalyzer.Quality.BLUNDER, GameAnalyzer.classify(300));
        assertEquals(GameAnalyzer.Quality.BLUNDER, GameAnalyzer.classify(5000));
    }

    @Test
    public void accuracy_perfectIs100_andMonotonicDown() {
        assertEquals(100, GameAnalyzer.accuracy(0, 5));
        assertEquals(100, GameAnalyzer.accuracy(0, 0)); // không nước → mặc định 100
        int low = GameAnalyzer.accuracy(0, 1);
        int mid = GameAnalyzer.accuracy(100, 1);
        int high = GameAnalyzer.accuracy(900, 1);
        assertTrue(low > mid);
        assertTrue(mid > high);
        assertTrue(high >= 0 && low <= 100);
    }

    // ───────────────────────── applyMove fidelity ─────────────────────────

    @Test
    public void applyMove_normalMoveAndCapture() {
        Chess b = GameAnalyzer.freshStartBoard();
        // 1. e4 : tốt trắng e2(4,6) → e4(4,4)
        Chessman wPawn = b.chessmen[4][6];
        GameAnalyzer.applyMove(b, rec(b, 4, 6, 4, 4));
        assertSame(wPawn, b.chessmen[4][4]);
        assertNull(b.chessmen[4][6]);
        assertFalse(((Pawn) wPawn).firstMove);
        assertNotNull(b.enPassantTarget); // đẩy 2 ô → set target
        assertEquals(4, b.enPassantTarget.x);
        assertEquals(5, b.enPassantTarget.y);

        // 1... d5 : tốt đen d7(3,1) → d5(3,3)
        GameAnalyzer.applyMove(b, rec(b, 3, 1, 3, 3));
        // 2. exd5 : trắng ăn tốt đen
        Chessman target = b.chessmen[3][3];
        assertNotNull(target);
        GameAnalyzer.applyMove(b, rec(b, 4, 4, 3, 3));
        assertSame(wPawn, b.chessmen[3][3]); // tốt trắng giờ ở d5
        assertNull(b.chessmen[4][4]);
        assertNull(b.enPassantTarget); // không phải đẩy 2 ô
    }

    @Test
    public void applyMove_promotesToQueen() {
        Chess b = GameAnalyzer.freshStartBoard();
        // Dọn để tốt trắng tiến phong cấp ở a8(0,0)
        b.chessmen[0][0] = null;       // bỏ xe đen góc
        b.chessmen[0][1] = null;       // bỏ tốt đen
        Chessman wp = new Pawn(new Point(0, 1), Chessman.PlayerColor.White, 0, b);
        b.chessmen[0][1] = wp;
        b.chessmen[0][6] = null;       // dời tốt trắng gốc đi cho gọn

        GameAnalyzer.applyMove(b, rec(b, 0, 1, 0, 0)); // a7-a8
        Chessman promoted = b.chessmen[0][0];
        assertNotNull(promoted);
        assertEquals(Chessman.ChessmanType.Queen, promoted.type);
        assertEquals(Chessman.PlayerColor.White, promoted.color);
    }

    @Test
    public void applyMove_castleMovesRook() {
        Chess b = GameAnalyzer.freshStartBoard();
        // Dọn ô giữa vua-xe cánh vua (f1,g1)
        b.chessmen[5][7] = null; // bishop f1
        b.chessmen[6][7] = null; // knight g1
        King wk = b.whiteKing;
        Chessman rook = b.chessmen[7][7];

        MoveRecord r = new MoveRecord(4, 7, 6, 7, wk, null, null, false);
        r.isCastle = true;
        r.rookFromX = 7; r.rookFromY = 7; r.rookToX = 5; r.rookToY = 7;
        GameAnalyzer.applyMove(b, r);

        assertSame(wk, b.chessmen[6][7]);   // vua g1
        assertSame(rook, b.chessmen[5][7]); // xe f1
        assertNull(b.chessmen[4][7]);
        assertNull(b.chessmen[7][7]);
        assertTrue(wk.hasMoved);
        assertTrue(((Rook) rook).hasMoved);
    }

    @Test
    public void applyMove_enPassantRemovesVictim() {
        Chess b = GameAnalyzer.freshStartBoard();
        // Dựng: tốt trắng e5(4,3), tốt đen vừa đẩy d7-d5 → d5(3,3); target d6(3,2)
        b.chessmen[4][6] = null;
        Chessman wp = new Pawn(new Point(4, 3), Chessman.PlayerColor.White, 0, b);
        b.chessmen[4][3] = wp;
        b.chessmen[3][1] = null;
        Chessman victim = new Pawn(new Point(3, 3), Chessman.PlayerColor.Black, 0, b);
        b.chessmen[3][3] = victim;

        MoveRecord r = new MoveRecord(4, 3, 3, 2, wp, victim, null, false);
        r.isEnPassant = true;
        r.epVictimX = 3; r.epVictimY = 3;
        GameAnalyzer.applyMove(b, r);

        assertSame(wp, b.chessmen[3][2]);  // tốt trắng tới d6
        assertNull(b.chessmen[4][3]);
        assertNull(b.chessmen[3][3]);      // con tốt bị bắt qua đường biến mất
    }

    // ───────────────────────── analyze end-to-end ─────────────────────────

    @Test
    public void analyze_detectsHangedQueenBlunder() {
        // 1.e4 a5 2.Qh5 a4 3.Qxh7?? — hậu trắng bị Rxh7 ăn không công ⇒ BLUNDER ở ply 4.
        Chess gen = GameAnalyzer.freshStartBoard();
        List<MoveRecord> history = new ArrayList<>();
        history.add(rec(gen, 4, 6, 4, 4)); // e4
        history.add(rec(gen, 0, 1, 0, 3)); // a5
        history.add(rec(gen, 3, 7, 7, 3)); // Qh5
        history.add(rec(gen, 0, 3, 0, 4)); // a4
        history.add(rec(gen, 7, 3, 7, 1)); // Qxh7 (ăn tốt h7) — sẽ mất hậu cho Rxh7

        GameAnalyzer.Result res = GameAnalyzer.analyze(history, engine);
        assertNotNull(res);
        assertEquals(5, res.plies.size());

        GameAnalyzer.MovePly qxh7 = res.plies.get(4);
        assertTrue(qxh7.whiteMove);
        assertEquals("Qxh7", qxh7.san);
        assertEquals(GameAnalyzer.Quality.BLUNDER, qxh7.quality);
        assertTrue("loss phải lớn (mất hậu)", qxh7.lossCp >= 500);
        assertEquals(1, res.whiteBlunders);
    }

    @Test
    public void analyze_returnsNullWhenFirstMoveNotWhite() {
        // History bắt đầu bằng nước Đen (mô phỏng ván resume bị cắt) → không phân tích được.
        Chessman blackPawn = new Pawn(new Point(4, 1), Chessman.PlayerColor.Black, 0, null);
        List<MoveRecord> history = new ArrayList<>();
        history.add(new MoveRecord(4, 1, 4, 3, blackPawn, null, null, false));
        assertNull(GameAnalyzer.analyze(history, engine));
    }

    @Test
    public void analyze_emptyOrNullReturnsNull() {
        assertNull(GameAnalyzer.analyze(null, engine));
        assertNull(GameAnalyzer.analyze(new ArrayList<>(), engine));
    }

    // ───────────────────────── helper ─────────────────────────

    /** Tạo 1 MoveRecord nước thường từ bàn {@code b} RỒI áp lên {@code b} (để build chuỗi liên tiếp). */
    private MoveRecord rec(Chess b, int fx, int fy, int tx, int ty) {
        Chessman piece = b.chessmen[fx][fy];
        Chessman captured = b.chessmen[tx][ty];
        MoveRecord r = new MoveRecord(fx, fy, tx, ty, piece, captured, null, false);
        GameAnalyzer.applyMove(b, r);
        return r;
    }
}
