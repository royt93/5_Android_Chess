package com.saigonphantomlabs.chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bộ câu đố nhúng (mate-in-1 & mate-in-2, Trắng đi). Nhẹ APK (chỉ chuỗi FEN). Mọi câu được
 * {@code PuzzleRepositoryTest} kiểm chứng: replay trọn line ⇒ chiếu hết thật (oracle = engine).
 *
 * <p>Line: nước Trắng (người chơi) ở index chẵn, nước Đen (auto đáp) ở index lẻ; nước Trắng cuối = mate.
 */
public final class PuzzleRepository {

    private PuzzleRepository() { }

    private static final List<Puzzle> PUZZLES = Collections.unmodifiableList(build());

    private static List<Puzzle> build() {
        List<Puzzle> l = new ArrayList<>();
        // ── Mate-in-1 ──
        l.add(new Puzzle("p1", "6k1/5ppp/8/8/8/8/8/3R2K1 w - - 0 1", 1, "d1d8"));      // Xe back-rank
        l.add(new Puzzle("p2", "6k1/5ppp/8/8/8/8/5PPP/3Q2K1 w - - 0 1", 1, "d1d8"));   // Hậu back-rank
        l.add(new Puzzle("p3", "7k/8/6KQ/8/8/8/8/8 w - - 0 1", 1, "h6g7"));            // K+Q áp sát
        l.add(new Puzzle("p4", "6rk/6pp/7N/8/8/8/8/7K w - - 0 1", 1, "h6f7"));         // Smothered Mã
        l.add(new Puzzle("p5", "7k/R7/8/8/8/8/8/1R5K w - - 0 1", 1, "b1b8"));          // Bậc thang 2 Xe
        l.add(new Puzzle("p6", "1k6/ppp5/8/8/8/8/8/3R3K w - - 0 1", 1, "d1d8"));       // Xe back-rank (b8)
        l.add(new Puzzle("p7", "k7/ppp5/8/8/8/8/8/3R3K w - - 0 1", 1, "d1d8"));        // Xe back-rank (a8 góc)
        l.add(new Puzzle("p8", "k7/8/1K1Q4/8/8/8/8/8 w - - 0 1", 1, "d6d8"));          // Hậu + Vua hỗ trợ
        // ── Mate-in-2 ── (Trắng ép, Đen buộc đi, Trắng chiếu hết)
        // Vua đen a8 góc, Vua trắng c6 hỗ trợ. Qg7! (zugzwang) Kb8 (nước duy nhất) Qb7# (Hậu được Vc6 đỡ).
        l.add(new Puzzle("p9", "k7/8/2K5/8/8/8/8/6Q1 w - - 0 1", 2, "g1g7", "a8b8", "g7b7"));
        return l;
    }

    public static List<Puzzle> all() { return PUZZLES; }

    public static int count() { return PUZZLES.size(); }

    public static Puzzle get(int index) {
        return (index >= 0 && index < PUZZLES.size()) ? PUZZLES.get(index) : null;
    }

    public static Puzzle byId(String id) {
        for (Puzzle p : PUZZLES) if (p.id.equals(id)) return p;
        return null;
    }
}
