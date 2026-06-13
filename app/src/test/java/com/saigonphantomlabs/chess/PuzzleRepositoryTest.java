package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Integration test: dựng từng câu đố từ FEN qua {@link FenParser} + {@link Chess#loadSaveState}
 * (model-only), replay TRỌN line (nước Trắng + Đen xen kẽ), xác nhận engine báo CHIẾU HẾT sau nước
 * Trắng cuối (qua {@link RecordingChessBoardView}). Bảo đảm MỌI câu (mate-in-1 & 2) đúng lời giải.
 */
public class PuzzleRepositoryTest {

    @Test
    public void everyPuzzle_lineDeliversCheckmate() {
        assertTrue("phải có câu đố", PuzzleRepository.count() > 0);
        for (Puzzle p : PuzzleRepository.all()) {
            RecordingChessBoardView rec = new RecordingChessBoardView();
            Chess board = new Chess();
            board.setBoardViewForTest(rec);

            GameSaveManager.SavedGame sg = FenParser.toSavedGame(p.fen);
            assertNotNull("FEN parse được: " + p.id, sg);
            board.loadSaveState(sg);

            // Số nước Trắng = mateIn; tổng nước = 2*mateIn - 1
            assertTrue(p.id + ": line khớp mateIn", p.line.length == 2 * p.mateIn - 1);

            for (int[] m : p.line) {
                board.doMove(new Point(m[0], m[1]), new Point(m[2], m[3]));
            }

            assertTrue("câu " + p.id + ": nước Trắng cuối phải kết thúc ván", rec.gameEndShown);
            assertTrue("câu " + p.id + ": Trắng thắng (chiếu hết)", rec.lastWhiteWins);
            assertFalse("câu " + p.id + ": KHÔNG phải hoà/hết cờ", rec.lastIsStalemate);
        }
    }

    @Test
    public void mateInTwo_firstMoveIsNotYetCheckmate() {
        // Câu mate-in-2: sau nước Trắng ĐẦU, chưa được kết thúc ván (mới là nước ép, chưa mate).
        Puzzle p = PuzzleRepository.byId("p9");
        assertNotNull(p);
        assertTrue(p.mateIn == 2);
        RecordingChessBoardView rec = new RecordingChessBoardView();
        Chess board = new Chess();
        board.setBoardViewForTest(rec);
        board.loadSaveState(FenParser.toSavedGame(p.fen));

        int[] w1 = p.line[0];
        board.doMove(new Point(w1[0], w1[1]), new Point(w1[2], w1[3]));
        assertFalse("nước ép đầu không phải chiếu hết", rec.gameEndShown);
    }

    @Test
    public void wrongMove_doesNotSolve() {
        Puzzle p = PuzzleRepository.byId("p1");
        assertNotNull(p);
        RecordingChessBoardView rec = new RecordingChessBoardView();
        Chess board = new Chess();
        board.setBoardViewForTest(rec);
        board.loadSaveState(FenParser.toSavedGame(p.fen));

        board.doMove(new Point(6, 7), new Point(5, 7)); // Kg1-f1 (không chiếu)
        assertFalse("nước sai không được tính chiếu hết", rec.gameEndShown);
    }

    @Test
    public void puzzleLookup_byIdAndIndex() {
        assertNotNull(PuzzleRepository.get(0));
        assertNotNull(PuzzleRepository.byId("p6"));
        assertNull(PuzzleRepository.get(-1));
        assertNull(PuzzleRepository.get(999));
        assertNull(PuzzleRepository.byId("nope"));
    }
}
