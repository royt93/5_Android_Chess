package com.saigonphantomlabs.chess;

public class King extends Chessman {

    public enum KingRiskType {
        Check,
        CheckMate,
        Stalemate,
        Safe
    }

    // Dùng cho nhập thành: vua chưa từng di chuyển
    public boolean hasMoved = false;

    public King(Point p, PlayerColor color, int minDimension, Chess parent) {
        this.parent = parent;
        setPoint(p);
        type = ChessmanType.King;
        this.color = color;
        this.minDimension = minDimension;
    }

    @Override
    public void createButton() {
        int resId = (color == PlayerColor.Black) ? R.drawable.ic_kingb : R.drawable.ic_kingw;
        createButton(
            parent.getCtx().getResources().getDrawable(resId, parent.getCtx().getTheme()),
            resId, color == PlayerColor.White,
            minDimension, parent.getCtx());
    }

    @Override
    public void generateMoves() {
        moves.clear();
        addAroundMovePoints();
        addCastlingMovePoints();
    }

    /**
     * Sinh nước nhập thành (castling).
     * Điều kiện: vua & xe chưa đi, các ô giữa trống, vua không đang bị chiếu,
     * vua không đi qua / không đến ô đang bị tấn công.
     * Tắt khi AI đang search (parent.inAiSimulation) để không làm hỏng minimax.
     */
    private void addCastlingMovePoints() {
        if (parent.inAiSimulation) return;
        if (hasMoved) return;

        Point cur = getPoint();
        int row = cur.y;
        // Vua phải ở vị trí xuất phát (cột 4)
        if (cur.x != 4) return;
        // Không được nhập thành khi đang bị chiếu (vua còn trên bàn → tự nó không chắn được nước chiếu vào chính mình)
        if (!isPointSafe()) return;

        Chessman[][] men = parent.chessmen;

        // Tạm gỡ vua khỏi ô gốc để khi kiểm tra ô vua "đi qua"/ô đích, các tia tấn công
        // của quân địch không bị chính thân vua chặn (edge case "xe sau lưng vua").
        men[cur.x][row] = null;
        try {
            // King-side (nhập thành gần): xe ở (7,row), ô (5,row)(6,row) trống
            Chessman ksRook = men[7][row];
            if (ksRook instanceof Rook && ksRook.color == color && !((Rook) ksRook).hasMoved
                    && men[5][row] == null && men[6][row] == null
                    && isPointSafe(new Point(5, row)) && isPointSafe(new Point(6, row))) {
                moves.add(new Point(6, row)); // vua đến cột 6
            }

            // Queen-side (nhập thành xa): xe ở (0,row), ô (1,row)(2,row)(3,row) trống
            Chessman qsRook = men[0][row];
            if (qsRook instanceof Rook && qsRook.color == color && !((Rook) qsRook).hasMoved
                    && men[1][row] == null && men[2][row] == null && men[3][row] == null
                    && isPointSafe(new Point(3, row)) && isPointSafe(new Point(2, row))) {
                moves.add(new Point(2, row)); // vua đến cột 2
            }
        } finally {
            men[cur.x][row] = this; // luôn khôi phục vua về ô gốc
        }
    }
}
