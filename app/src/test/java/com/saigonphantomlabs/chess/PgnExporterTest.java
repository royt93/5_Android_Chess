package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** Unit test thuần JVM cho {@link PgnExporter} — SAN từng nước + movetext + PGN đầy đủ. */
public class PgnExporterTest {

    private Chess board;
    private static final Chessman.PlayerColor W = Chessman.PlayerColor.White;
    private static final Chessman.PlayerColor B = Chessman.PlayerColor.Black;

    @Before public void setUp() { board = new Chess(); }

    private Chessman piece(Chessman.ChessmanType t, int x, int y, Chessman.PlayerColor c) {
        switch (t) {
            case Pawn: return new Pawn(new Point(x, y), c, 8, board);
            case Knight: return new Knight(new Point(x, y), c, 8, board);
            case Bishop: return new Bishop(new Point(x, y), c, 8, board);
            case Rook: return new Rook(new Point(x, y), c, 8, board);
            case Queen: return new Queen(new Point(x, y), c, 8, board);
            default: return new King(new Point(x, y), c, 8, board);
        }
    }

    private MoveRecord move(Chessman.ChessmanType t, int fx, int fy, int tx, int ty, boolean capture) {
        Chessman moved = piece(t, fx, fy, W);
        Chessman cap = capture ? piece(Chessman.ChessmanType.Pawn, tx, ty, B) : null;
        return new MoveRecord(fx, fy, tx, ty, moved, cap, null, true);
    }

    @Test public void san_pawnPush_e4() {
        assertEquals("e4", PgnExporter.toSan(move(Chessman.ChessmanType.Pawn, 4, 6, 4, 4, false)));
    }

    @Test public void san_pawnCapture_exd5() {
        assertEquals("exd5", PgnExporter.toSan(move(Chessman.ChessmanType.Pawn, 4, 4, 3, 3, true)));
    }

    @Test public void san_knight_Nf3() {
        assertEquals("Nf3", PgnExporter.toSan(move(Chessman.ChessmanType.Knight, 6, 7, 5, 5, false)));
    }

    @Test public void san_pieceCapture_Nxd5() {
        assertEquals("Nxd5", PgnExporter.toSan(move(Chessman.ChessmanType.Knight, 6, 4, 3, 3, true)));
    }

    @Test public void san_castleKingside_OO() {
        MoveRecord m = move(Chessman.ChessmanType.King, 4, 7, 6, 7, false);
        m.isCastle = true;
        assertEquals("O-O", PgnExporter.toSan(m));
    }

    @Test public void san_castleQueenside_OOO() {
        MoveRecord m = move(Chessman.ChessmanType.King, 4, 7, 2, 7, false);
        m.isCastle = true;
        assertEquals("O-O-O", PgnExporter.toSan(m));
    }

    @Test public void san_promotion_a8Q() {
        Chessman pawn = piece(Chessman.ChessmanType.Pawn, 0, 1, W);
        MoveRecord m = new MoveRecord(0, 1, 0, 0, pawn, null, Chessman.ChessmanType.Queen, false);
        assertEquals("a8=Q", PgnExporter.toSan(m));
    }

    @Test public void san_enPassant_isCapture() {
        Chessman pawn = piece(Chessman.ChessmanType.Pawn, 4, 3, W);
        MoveRecord m = new MoveRecord(4, 3, 3, 2, pawn, null, null, false);
        m.isEnPassant = true;
        assertEquals("exd6", PgnExporter.toSan(m));
    }

    @Test public void moveText_numbersPairs() {
        List<MoveRecord> moves = new ArrayList<>();
        moves.add(move(Chessman.ChessmanType.Pawn, 4, 6, 4, 4, false)); // e4
        moves.add(move(Chessman.ChessmanType.Pawn, 4, 1, 4, 3, false)); // e5
        moves.add(move(Chessman.ChessmanType.Knight, 6, 7, 5, 5, false)); // Nf3
        assertEquals("1. e4 e5 2. Nf3", PgnExporter.buildMoveText(moves));
    }

    @Test public void buildPgn_hasTagsAndResult() {
        List<MoveRecord> moves = new ArrayList<>();
        moves.add(move(Chessman.ChessmanType.Pawn, 4, 6, 4, 4, false));
        String pgn = PgnExporter.buildPgn(moves, "Player", "AI", "2026.06.12", "1-0");
        assertTrue(pgn.contains("[White \"Player\"]"));
        assertTrue(pgn.contains("[Black \"AI\"]"));
        assertTrue(pgn.contains("[Result \"1-0\"]"));
        assertTrue(pgn.contains("1. e4"));
        assertTrue(pgn.trim().endsWith("1-0"));
    }

    @Test public void buildMoveTextMultiline_pairsPerLine() {
        List<MoveRecord> moves = new ArrayList<>();
        moves.add(move(Chessman.ChessmanType.Pawn, 4, 6, 4, 4, false)); // e4
        moves.add(move(Chessman.ChessmanType.Pawn, 4, 1, 4, 3, false)); // e5
        moves.add(move(Chessman.ChessmanType.Knight, 6, 7, 5, 5, false)); // Nf3
        assertEquals("1. e4   e5\n2. Nf3", PgnExporter.buildMoveTextMultiline(moves));
    }

    @Test public void buildPgn_emptyGame_resultStar() {
        String pgn = PgnExporter.buildPgn(new ArrayList<>(), "W", "B", "2026.06.12", null);
        assertTrue(pgn.trim().endsWith("*"));
    }
}
