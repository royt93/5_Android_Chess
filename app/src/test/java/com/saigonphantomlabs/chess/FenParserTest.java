package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Unit test thuần cho {@link FenParser} — parse FEN + helper toạ độ. */
public class FenParserTest {

    @Test
    public void parseSquare_mapsAlgebraicToBoardCoords() {
        assertEquals(new Point(4, 4), FenParser.parseSquare("e4"));
        assertEquals(new Point(0, 7), FenParser.parseSquare("a1"));
        assertEquals(new Point(7, 0), FenParser.parseSquare("h8"));
        assertEquals(new Point(3, 0), FenParser.parseSquare("d8"));
        assertNull(FenParser.parseSquare("-"));
        assertNull(FenParser.parseSquare("z9"));
        assertNull(FenParser.parseSquare("e"));
        assertNull(FenParser.parseSquare(null));
    }

    @Test
    public void startPosition_has32Pieces_whiteToMove() {
        GameSaveManager.SavedGame g = FenParser.toSavedGame(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        assertNotNull(g);
        assertEquals(32, g.pieces.size());
        assertEquals(Chessman.PlayerColor.White, g.turn);
        assertNull(g.enPassant);
        assertEquals(0, g.halfMoveClock);
        assertTrue(!g.isVsAi && !g.hasClock);
    }

    @Test
    public void piecePlacement_correctSquareAndColor() {
        // Vua đen h8, Vua trắng e1, Xe trắng a1; Trắng đi
        GameSaveManager.SavedGame g = FenParser.toSavedGame("7k/8/8/8/8/8/8/R3K3 w Q - 0 1");
        assertNotNull(g);
        assertEquals(3, g.pieces.size());
        assertTrue(hasPiece(g, 7, 0, Chessman.ChessmanType.King, Chessman.PlayerColor.Black));
        assertTrue(hasPiece(g, 4, 7, Chessman.ChessmanType.King, Chessman.PlayerColor.White));
        assertTrue(hasPiece(g, 0, 7, Chessman.ChessmanType.Rook, Chessman.PlayerColor.White));
    }

    @Test
    public void castlingRights_setMovedFlags() {
        // "Q" → chỉ Trắng cánh hậu còn quyền: xe a1 chưa đi, vua chưa đi (vẫn còn 1 quyền)
        GameSaveManager.SavedGame g = FenParser.toSavedGame("7k/8/8/8/8/8/8/R3K2R w Q - 0 1");
        assertNotNull(g);
        assertEquals(false, movedOf(g, 0, 7)); // xe a1 chưa đi (còn quyền Q)
        assertEquals(true, movedOf(g, 7, 7));  // xe h1 đã đi (mất quyền K)
        assertEquals(false, movedOf(g, 4, 7)); // vua e1 chưa đi (vẫn còn quyền Q)
    }

    @Test
    public void enPassantAndTurn_parsed() {
        GameSaveManager.SavedGame g = FenParser.toSavedGame(
                "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3");
        assertNotNull(g);
        assertEquals(Chessman.PlayerColor.White, g.turn);
        assertEquals(new Point(3, 2), g.enPassant); // d6 = (3,2)
    }

    @Test
    public void invalidFen_returnsNull() {
        assertNull(FenParser.toSavedGame(null));
        assertNull(FenParser.toSavedGame(""));
        assertNull(FenParser.toSavedGame("8/8/8/8/8/8/8 w - - 0 1"));      // chỉ 7 rank
        assertNull(FenParser.toSavedGame("9/8/8/8/8/8/8/8 w - - 0 1"));    // rank > 8 ô (9)
        assertNull(FenParser.toSavedGame("8/8/8/8/8/8/8/8 w - - 0 1"));    // không có vua
        assertNull(FenParser.toSavedGame("xxxxxxxx/8/8/8/8/8/8/7k w - - 0 1")); // ký tự lạ
    }

    private boolean hasPiece(GameSaveManager.SavedGame g, int x, int y,
            Chessman.ChessmanType t, Chessman.PlayerColor c) {
        for (GameSaveManager.PieceData p : g.pieces)
            if (p.x == x && p.y == y && p.type == t && p.color == c) return true;
        return false;
    }

    private boolean movedOf(GameSaveManager.SavedGame g, int x, int y) {
        for (GameSaveManager.PieceData p : g.pieces)
            if (p.x == x && p.y == y) return p.moved;
        throw new AssertionError("no piece at " + x + "," + y);
    }
}
