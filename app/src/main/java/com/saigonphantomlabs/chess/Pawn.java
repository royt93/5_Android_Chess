package com.saigonphantomlabs.chess;

public class Pawn extends Chessman {
    public boolean firstMove = true;
    public boolean promoted = false;

    public Pawn(Point p, PlayerColor color, int minDimension, Chess parent) {
        this.parent = parent;
        setPoint(p);
        type = ChessmanType.Pawn;
        this.color = color;
        this.minDimension = minDimension;
    }

    @Override
    public void createButton() {
        int resId = (color == PlayerColor.Black) ? R.drawable.ic_pawnb : R.drawable.ic_pawnw;
        createButton(
            color == PlayerColor.Black
                ? parent.ctx.getResources().getDrawable(R.drawable.ic_pawnb, parent.ctx.getTheme())
                : parent.ctx.getResources().getDrawable(R.drawable.ic_pawnw, parent.ctx.getTheme()),
            resId, color == PlayerColor.White,
            minDimension, parent.ctx);
    }

    @Override
    public void setPoint(Point p) {
        super.setPoint(p);
    }

    @Override
    public void generateMoves() {
        moves.clear();

        // Direction: Black moves down (y+1), White moves up (y-1)
        int step = (color == PlayerColor.Black) ? 1 : -1;
        Point current = getPoint();

        // 1. Move Forward 1 step
        Point front = new Point(current.x, current.y + step);
        if (front.isValid() && parent.chessmen[front.x][front.y] == null) {
            moves.add(front);

            // 2. Move Forward 2 steps (only if first move and front is clear)
            if (firstMove) {
                Point front2 = new Point(current.x, current.y + step * 2);
                if (front2.isValid() && parent.chessmen[front2.x][front2.y] == null) {
                    moves.add(front2);
                }
            }
        }

        // 3. Capture Diagonally (Left)
        Point left = new Point(current.x - 1, current.y + step);
        if (left.isValid()) {
            Chessman target = parent.chessmen[left.x][left.y];
            if (target != null && target.color != color && !target.isDead) {
                moves.add(left);
            }
        }

        // 4. Capture Diagonally (Right)
        Point right = new Point(current.x + 1, current.y + step);
        if (right.isValid()) {
            Chessman target = parent.chessmen[right.x][right.y];
            if (target != null && target.color != color && !target.isDead) {
                moves.add(right);
            }
        }
    }
}
