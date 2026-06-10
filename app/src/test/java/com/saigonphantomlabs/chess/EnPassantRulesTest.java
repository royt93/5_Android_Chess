package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test rule logic BẮT TỐT QUA ĐƯỜNG (en passant) — phần SINH nước đi
 * ({@link Pawn#generateMoves()}), không chạm Android/UI.
 */
public class EnPassantRulesTest {

    private Chess board;

    @Before
    public void setUp() {
        board = new Chess(); // test seam
    }

    private Pawn putPawn(int x, int y, Chessman.PlayerColor color) {
        Pawn p = new Pawn(new Point(x, y), color, 8, board);
        p.firstMove = false;
        board.chessmen[x][y] = p;
        return p;
    }

    @Test
    public void white_enPassant_generated() {
        // Tốt Trắng e5 (4,3); tốt Đen vừa đẩy d7-d5 nằm d5 (3,3); ô bỏ qua d6 = (3,2)
        Pawn white = putPawn(4, 3, Chessman.PlayerColor.White);
        putPawn(3, 3, Chessman.PlayerColor.Black);
        board.enPassantTarget = new Point(3, 2);

        white.generateMoves();
        assertTrue("Phải sinh nước bắt qua đường tới (3,2)", white.moves.contains(new Point(3, 2)));
    }

    @Test
    public void black_enPassant_generated() {
        // Tốt Đen d4 (3,4); tốt Trắng vừa đẩy e2-e4 nằm e4 (4,4); ô bỏ qua e3 = (4,5)
        Pawn black = putPawn(3, 4, Chessman.PlayerColor.Black);
        putPawn(4, 4, Chessman.PlayerColor.White);
        board.enPassantTarget = new Point(4, 5);

        black.generateMoves();
        assertTrue("Phải sinh nước bắt qua đường tới (4,5)", black.moves.contains(new Point(4, 5)));
    }

    @Test
    public void noEnPassantTarget_notGenerated() {
        Pawn white = putPawn(4, 3, Chessman.PlayerColor.White);
        putPawn(3, 3, Chessman.PlayerColor.Black);
        // enPassantTarget = null (mặc định)

        white.generateMoves();
        assertFalse(white.moves.contains(new Point(3, 2)));
    }

    /**
     * Guard an toàn AI: khi AIEngine đang search (inAiSimulation=true) thì KHÔNG
     * sinh nước en passant (để minimax không bị corrupt).
     */
    @Test
    public void duringAiSimulation_notGenerated() {
        Pawn white = putPawn(4, 3, Chessman.PlayerColor.White);
        putPawn(3, 3, Chessman.PlayerColor.Black);
        board.enPassantTarget = new Point(3, 2);
        board.inAiSimulation = true;

        white.generateMoves();
        assertFalse("AI search không được sinh en passant", white.moves.contains(new Point(3, 2)));
    }
}
