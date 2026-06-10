package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test cho move-ordering MVV-LVA của {@link AIEngine} (Wave 1).
 * Quân được tạo với parent=null vì test chỉ đọc type/màu (không chạm Android).
 */
public class AIEngineOrderingTest {

    private final AIEngine engine = new AIEngine();

    private static Chessman piece(Chessman.ChessmanType type, Chessman.PlayerColor color) {
        switch (type) {
            case Queen:  return new Queen(new Point(0, 0), color, 8, null);
            case Rook:   return new Rook(new Point(0, 0), color, 8, null);
            case Bishop: return new Bishop(new Point(0, 0), color, 8, null);
            case Knight: return new Knight(new Point(0, 0), color, 8, null);
            case Pawn:   return new Pawn(new Point(0, 0), color, 8, null);
            default:     return new King(new Point(0, 0), color, 8, null);
        }
    }

    private static MoveRecord move(Chessman mover, Chessman captured) {
        return new MoveRecord(0, 0, 1, 1, mover, captured, null, false);
    }

    @Test
    public void mvvLva_captureHighValueWithLowValue_scoresHighest() {
        Chessman pawn = piece(Chessman.ChessmanType.Pawn, Chessman.PlayerColor.White);
        Chessman queen = piece(Chessman.ChessmanType.Queen, Chessman.PlayerColor.Black);
        // Tốt ăn Hậu (PxQ) phải có điểm cao hơn Hậu ăn Tốt (QxP)
        int pawnTakesQueen = engine.mvvLvaScore(move(pawn, queen));
        int queenTakesPawn = engine.mvvLvaScore(move(queen, pawn));
        assertTrue("PxQ phải > QxP", pawnTakesQueen > queenTakesPawn);
    }

    @Test
    public void mvvLva_nonCapture_isZero() {
        Chessman knight = piece(Chessman.ChessmanType.Knight, Chessman.PlayerColor.White);
        assertEquals(0, engine.mvvLvaScore(move(knight, null)));
    }

    @Test
    public void orderMoves_capturesBeforeQuietMoves_bestCaptureFirst() {
        Chessman pawn = piece(Chessman.ChessmanType.Pawn, Chessman.PlayerColor.White);
        Chessman knight = piece(Chessman.ChessmanType.Knight, Chessman.PlayerColor.White);
        Chessman queen = piece(Chessman.ChessmanType.Queen, Chessman.PlayerColor.White);
        Chessman victimQueen = piece(Chessman.ChessmanType.Queen, Chessman.PlayerColor.Black);
        Chessman victimPawn = piece(Chessman.ChessmanType.Pawn, Chessman.PlayerColor.Black);

        MoveRecord quiet = move(knight, null);              // không ăn
        MoveRecord pawnTakesQueen = move(pawn, victimQueen); // điểm cao nhất
        MoveRecord queenTakesPawn = move(queen, victimPawn); // điểm thấp

        List<MoveRecord> moves = new ArrayList<>(Arrays.asList(quiet, queenTakesPawn, pawnTakesQueen));
        engine.orderMoves(moves);

        assertSame("Nước ăn quân giá trị cao nhất phải đứng đầu", pawnTakesQueen, moves.get(0));
        assertSame(queenTakesPawn, moves.get(1));
        assertSame("Nước không ăn quân xếp cuối", quiet, moves.get(2));
    }
}
