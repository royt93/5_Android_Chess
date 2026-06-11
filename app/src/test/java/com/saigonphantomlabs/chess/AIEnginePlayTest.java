package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

/**
 * Test AIEngine ở tầng "chơi thật": chọn nước qua minimax. {@code getThinkDelay} thuần
 * hằng số. {@code getBestMove} không được làm thay đổi bàn cờ (simulate + undo).
 *
 * AI luôn cầm quân Đen trong app. Dùng MEDIUM (minimax depth 2, bỏ qua opening book)
 * để hành vi xác định mà vẫn thấy được nước ăn quân.
 */
public class AIEnginePlayTest {

    private final AIEngine engine = new AIEngine();
    private Chess board;

    private static final Chessman.PlayerColor W = Chessman.PlayerColor.White;
    private static final Chessman.PlayerColor B = Chessman.PlayerColor.Black;

    @Before
    public void setUp() {
        board = new Chess();
        board.setBoardViewForTest(new NoOpChessBoardView());
        King wk = new King(new Point(7, 7), W, 8, board);
        board.chessmen[7][7] = wk; board.whiteKing = wk;
        King bk = new King(new Point(0, 0), B, 8, board);
        board.chessmen[0][0] = bk; board.blackKing = bk;
    }

    @Test
    public void thinkDelay_increasesWithDifficulty() {
        assertEquals(500, engine.getThinkDelay(AIEngine.Difficulty.EASY));
        assertEquals(800, engine.getThinkDelay(AIEngine.Difficulty.MEDIUM));
        assertEquals(1200, engine.getThinkDelay(AIEngine.Difficulty.HARD));
        assertEquals(1500, engine.getThinkDelay(AIEngine.Difficulty.UNBEATABLE));
    }

    @Test
    public void getBestMove_grabsHangingQueen() {
        // Hậu Trắng treo ở d5 (3,3); Xe Đen d7 (3,1) ăn dọc cột d được tự do.
        Queen hangingQueen = new Queen(new Point(3, 3), W, 8, board);
        board.chessmen[3][3] = hangingQueen;
        Rook blackRook = new Rook(new Point(3, 1), B, 8, board);
        board.chessmen[3][1] = blackRook;

        MoveRecord best = engine.getBestMove(board, AIEngine.Difficulty.MEDIUM, B);

        assertNotNull("AI phải tìm được nước", best);
        assertEquals("AI phải ăn Hậu treo (đi tới d5.x)", 3, best.toX);
        assertEquals("AI phải ăn Hậu treo (đi tới d5.y)", 3, best.toY);
    }

    @Test
    public void getBestMove_doesNotMutateBoard() {
        Queen q = new Queen(new Point(3, 3), W, 8, board);
        board.chessmen[3][3] = q;
        Rook r = new Rook(new Point(3, 1), B, 8, board);
        board.chessmen[3][1] = r;

        engine.getBestMove(board, AIEngine.Difficulty.MEDIUM, B);

        assertSame("Hậu vẫn ở d5 sau khi AI suy nghĩ", q, board.chessmen[3][3]);
        assertSame("Xe đen vẫn ở d7", r, board.chessmen[3][1]);
        assertEquals("inAiSimulation phải được reset về false", false, board.inAiSimulation);
    }

    @Test
    public void getBestMove_unbeatableAlphaBeta_grabsHangingQueen() {
        // UNBEATABLE = minimax depth 4 + alpha-beta. Thế giữa ván (không khớp opening book)
        // nên book trả null → chạy alpha-beta thật. Vẫn phải ăn Hậu treo.
        Queen hangingQueen = new Queen(new Point(3, 3), W, 8, board);
        board.chessmen[3][3] = hangingQueen;
        Rook blackRook = new Rook(new Point(3, 1), B, 8, board);
        board.chessmen[3][1] = blackRook;

        MoveRecord best = engine.getBestMove(board, AIEngine.Difficulty.UNBEATABLE, B);

        assertNotNull(best);
        assertEquals("alpha-beta cũng ăn Hậu treo (x)", 3, best.toX);
        assertEquals("alpha-beta cũng ăn Hậu treo (y)", 3, best.toY);
        assertEquals("inAiSimulation reset", false, board.inAiSimulation);
    }

    @Test
    public void getBestMove_returnsNullWhenNoLegalMoves() {
        // Đặt lại Vua Đen sang d8 (3,0) cho thế chiếu hết kiểu ladder hai xe.
        board.chessmen[0][0] = null;
        King bk = new King(new Point(3, 0), B, 8, board);
        board.chessmen[3][0] = bk; board.blackKing = bk;

        Rook rA = new Rook(new Point(0, 1), W, 8, board); // khoá hàng y=1
        board.chessmen[0][1] = rA;
        Rook rB = new Rook(new Point(7, 0), W, 8, board); // chiếu hàng y=0 (cách xa, vua không ăn được)
        board.chessmen[7][0] = rB;

        MoveRecord best = engine.getBestMove(board, AIEngine.Difficulty.MEDIUM, B);

        assertNull("Hết nước hợp lệ (chiếu hết) → trả null", best);
    }
}
