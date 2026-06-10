package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test rule logic NHẬP THÀNH (castling) — chỉ kiểm thử phần SINH nước đi
 * ({@link King#generateMoves()} → addCastlingMovePoints), không chạm Android/UI.
 *
 * Toạ độ: y=7 là hàng cuối (Trắng), x=4 là cột Vua (e), x=7 cột h, x=0 cột a.
 * Vua Trắng đi king-side đến (6,7); queen-side đến (2,7).
 */
public class CastlingRulesTest {

    private Chess board;
    private King whiteKing;

    @Before
    public void setUp() {
        board = new Chess();                 // test seam: bàn cờ 8x8 rỗng
        whiteKing = new King(new Point(4, 7), Chessman.PlayerColor.White, 8, board);
        board.chessmen[4][7] = whiteKing;
        board.whiteKing = whiteKing;
    }

    private Rook putWhiteRook(int x, int y) {
        Rook r = new Rook(new Point(x, y), Chessman.PlayerColor.White, 8, board);
        board.chessmen[x][y] = r;
        return r;
    }

    private boolean canCastleKingSide() {
        whiteKing.generateMoves();
        return whiteKing.moves.contains(new Point(6, 7));
    }

    private boolean canCastleQueenSide() {
        whiteKing.generateMoves();
        return whiteKing.moves.contains(new Point(2, 7));
    }

    @Test
    public void kingSide_allClear_allowed() {
        putWhiteRook(7, 7);
        assertTrue(canCastleKingSide());
    }

    @Test
    public void queenSide_allClear_allowed() {
        putWhiteRook(0, 7);
        assertTrue(canCastleQueenSide());
    }

    @Test
    public void kingSide_squareBlocked_notAllowed() {
        putWhiteRook(7, 7);
        // Đặt một quân chắn ở (5,7) (f1)
        board.chessmen[5][7] = new Bishop(new Point(5, 7), Chessman.PlayerColor.White, 8, board);
        assertFalse(canCastleKingSide());
    }

    @Test
    public void kingHasMoved_notAllowed() {
        putWhiteRook(7, 7);
        whiteKing.hasMoved = true;
        assertFalse(canCastleKingSide());
    }

    @Test
    public void rookHasMoved_notAllowed() {
        Rook r = putWhiteRook(7, 7);
        r.hasMoved = true;
        assertFalse(canCastleKingSide());
    }

    @Test
    public void kingInCheck_notAllowed() {
        putWhiteRook(7, 7);
        // Xe đen ở (4,0) chiếu thẳng cột e vào Vua Trắng (4,7)
        board.chessmen[4][0] = new Rook(new Point(4, 0), Chessman.PlayerColor.Black, 8, board);
        assertFalse(canCastleKingSide());
    }

    @Test
    public void throughCheck_squareAttacked_notAllowed() {
        putWhiteRook(7, 7);
        // Xe đen ở (5,0) khống chế cột f → ô (5,7) Vua đi qua bị tấn công
        board.chessmen[5][0] = new Rook(new Point(5, 0), Chessman.PlayerColor.Black, 8, board);
        assertFalse(canCastleKingSide());
    }

    /**
     * Edge case 1 (đã vá): "xe sau lưng vua". Xe đen ở a1 (0,7), các ô b/c/d trống,
     * Vua Trắng e1 đang tự chắn tia. Sau khi vua rời e1 để nhập thành king-side,
     * cột 1 hở → f1/g1 bị xe a1 tấn công ⇒ nhập thành PHẢI bị cấm.
     * Bản vá tạm gỡ vua khỏi ô gốc khi kiểm tra nên phát hiện đúng.
     */
    @Test
    public void rookBehindKing_kingSide_notAllowed() {
        putWhiteRook(7, 7);                              // xe nhập thành h1
        board.chessmen[0][7] = new Rook(new Point(0, 7), Chessman.PlayerColor.Black, 8, board); // xe đen a1
        // b1,c1,d1 = (1,7)(2,7)(3,7) để trống
        assertFalse("Không được nhập thành khi vua đi qua ô bị xe sau lưng tấn công",
                canCastleKingSide());
    }
}
