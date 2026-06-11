package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test SINH NƯỚC ĐI (pseudo-legal) cho cả 6 loại quân — thuần JVM qua test seam.
 *
 * Toạ độ (x,y): x=0 cột a … x=7 cột h; y=7 hàng dưới (Trắng), y=0 hàng trên (Đen).
 * Trắng đi "lên" (y giảm). generateMoves() chỉ sinh nước theo luật di chuyển quân,
 * KHÔNG lọc an toàn vua (việc đó ở tầng thực thi move()).
 */
public class MoveGenerationTest {

    private Chess board;

    @Before
    public void setUp() {
        board = new Chess();
        board.setBoardViewForTest(new NoOpChessBoardView());
    }

    private <T extends Chessman> T put(T man) {
        board.chessmen[man.getPoint().x][man.getPoint().y] = man;
        return man;
    }

    private Rook rook(int x, int y, Chessman.PlayerColor c) {
        return put(new Rook(new Point(x, y), c, 8, board));
    }
    private Bishop bishop(int x, int y, Chessman.PlayerColor c) {
        return put(new Bishop(new Point(x, y), c, 8, board));
    }
    private Knight knight(int x, int y, Chessman.PlayerColor c) {
        return put(new Knight(new Point(x, y), c, 8, board));
    }
    private Queen queen(int x, int y, Chessman.PlayerColor c) {
        return put(new Queen(new Point(x, y), c, 8, board));
    }

    private static final Chessman.PlayerColor W = Chessman.PlayerColor.White;
    private static final Chessman.PlayerColor B = Chessman.PlayerColor.Black;

    // ---------------- ROOK ----------------

    @Test
    public void rook_openBoard_has14Moves() {
        Rook r = rook(3, 4, W);
        r.generateMoves();
        assertEquals("Xe giữa bàn trống đi được 14 ô", 14, r.moves.size());
        assertTrue(r.moves.contains(new Point(3, 0)));
        assertTrue(r.moves.contains(new Point(7, 4)));
        assertTrue(r.moves.contains(new Point(0, 4)));
    }

    @Test
    public void rook_blockedByFriendly_stopsBeforeIt() {
        Rook r = rook(3, 4, W);
        rook(3, 2, W); // quân nhà chắn ở (3,2)
        r.generateMoves();
        assertTrue("đi được tới ô ngay trước quân nhà", r.moves.contains(new Point(3, 3)));
        assertFalse("không được vào ô quân nhà", r.moves.contains(new Point(3, 2)));
        assertFalse("không nhảy qua quân nhà", r.moves.contains(new Point(3, 1)));
    }

    @Test
    public void rook_capturesEnemy_stopsAfterIt() {
        Rook r = rook(3, 4, W);
        rook(3, 2, B); // quân địch ở (3,2)
        r.generateMoves();
        assertTrue("được ăn quân địch", r.moves.contains(new Point(3, 2)));
        assertFalse("không đi xuyên qua quân địch", r.moves.contains(new Point(3, 1)));
    }

    // ---------------- BISHOP ----------------

    @Test
    public void bishop_centerOpenBoard_has13Moves() {
        Bishop b = bishop(3, 4, W);
        b.generateMoves();
        assertEquals("Tượng ở (3,4) trên bàn trống đi 13 ô", 13, b.moves.size());
        assertTrue(b.moves.contains(new Point(0, 1)));
        assertTrue(b.moves.contains(new Point(7, 0)));
    }

    @Test
    public void bishop_blockedByFriendly() {
        Bishop b = bishop(3, 4, W);
        bishop(5, 2, W); // chắn đường chéo NE
        b.generateMoves();
        assertTrue(b.moves.contains(new Point(4, 3)));
        assertFalse(b.moves.contains(new Point(5, 2)));
        assertFalse(b.moves.contains(new Point(6, 1)));
    }

    // ---------------- KNIGHT ----------------

    @Test
    public void knight_center_has8Moves() {
        Knight n = knight(3, 4, W);
        n.generateMoves();
        assertEquals("Mã giữa bàn có 8 nước", 8, n.moves.size());
        assertTrue(n.moves.contains(new Point(1, 3)));
        assertTrue(n.moves.contains(new Point(5, 5)));
    }

    @Test
    public void knight_corner_has2Moves() {
        Knight n = knight(0, 0, W);
        n.generateMoves();
        assertEquals("Mã ở góc chỉ có 2 nước", 2, n.moves.size());
        assertTrue(n.moves.contains(new Point(1, 2)));
        assertTrue(n.moves.contains(new Point(2, 1)));
    }

    @Test
    public void knight_jumpsOverPieces_butNotOntoFriendly() {
        Knight n = knight(3, 4, W);
        // bao quanh bằng quân nhà — mã vẫn nhảy qua được
        rook(3, 3, W); rook(3, 5, W); rook(2, 4, W); rook(4, 4, W);
        knight(1, 3, W); // đích (1,3) bị quân nhà chiếm
        n.generateMoves();
        assertFalse("không nhảy lên ô quân nhà", n.moves.contains(new Point(1, 3)));
        assertTrue("vẫn nhảy qua hàng rào tới ô trống", n.moves.contains(new Point(5, 5)));
    }

    // ---------------- QUEEN ----------------

    @Test
    public void queen_centerOpenBoard_has27Moves() {
        Queen q = queen(3, 4, W);
        q.generateMoves();
        // Hậu = Xe (14) + Tượng (13) = 27 trên bàn trống
        assertEquals("Hậu giữa bàn trống đi 27 ô", 27, q.moves.size());
    }

    // ---------------- PAWN ----------------

    @Test
    public void pawn_firstMove_canPushOneOrTwo() {
        Pawn p = put(new Pawn(new Point(4, 6), W, 8, board)); // e2
        p.firstMove = true;
        p.generateMoves();
        assertTrue("đẩy 1 ô", p.moves.contains(new Point(4, 5)));
        assertTrue("đẩy 2 ô lần đầu", p.moves.contains(new Point(4, 4)));
    }

    @Test
    public void pawn_notFirstMove_onlyOneStep() {
        Pawn p = put(new Pawn(new Point(4, 5), W, 8, board));
        p.firstMove = false;
        p.generateMoves();
        assertTrue(p.moves.contains(new Point(4, 4)));
        assertFalse("đã đi rồi thì không đẩy 2 ô", p.moves.contains(new Point(4, 3)));
    }

    @Test
    public void pawn_blockedAhead_cannotPush() {
        Pawn p = put(new Pawn(new Point(4, 6), W, 8, board));
        p.firstMove = true;
        rook(4, 5, B); // chắn ngay phía trước
        p.generateMoves();
        assertFalse("bị chắn thì không đẩy 1 ô", p.moves.contains(new Point(4, 5)));
        assertFalse("bị chắn thì không đẩy 2 ô", p.moves.contains(new Point(4, 4)));
    }

    @Test
    public void pawn_capturesDiagonally_notForward() {
        Pawn p = put(new Pawn(new Point(4, 5), W, 8, board));
        p.firstMove = false;
        rook(3, 4, B); // địch chéo trái
        rook(5, 4, B); // địch chéo phải
        p.generateMoves();
        assertTrue("ăn chéo trái", p.moves.contains(new Point(3, 4)));
        assertTrue("ăn chéo phải", p.moves.contains(new Point(5, 4)));
    }

    @Test
    public void pawn_noDiagonalOntoEmpty() {
        Pawn p = put(new Pawn(new Point(4, 5), W, 8, board));
        p.firstMove = false;
        p.generateMoves();
        assertFalse("không đi chéo vào ô trống", p.moves.contains(new Point(3, 4)));
        assertFalse("không ăn chéo ô trống", p.moves.contains(new Point(5, 4)));
    }

    @Test
    public void pawn_doesNotCaptureFriendlyDiagonally() {
        Pawn p = put(new Pawn(new Point(4, 5), W, 8, board));
        p.firstMove = false;
        rook(3, 4, W); // quân nhà chéo trái
        p.generateMoves();
        assertFalse("không ăn quân nhà", p.moves.contains(new Point(3, 4)));
    }
}
