package com.saigonphantomlabs.chess;

public class Rook extends Chessman {
    public Rook(Point p, PlayerColor color, int minDimension, Chess parent) {
        this.parent = parent;
        setPoint(p);
        type = ChessmanType.Rook;
        this.color = color;
        this.minDimension = minDimension;
    }

    @Override
    public void createButton() {
        int resId = (color == PlayerColor.Black) ? R.drawable.ic_rookb : R.drawable.ic_rookw;
        createButton(
            parent.ctx.getResources().getDrawable(resId, parent.ctx.getTheme()),
            resId, color == PlayerColor.White,
            minDimension, parent.ctx);
    }

    @Override
    public void generateMoves() {
        moves.clear();
        addVerticalMovePoints();
        addHorizontalMovePoints();
    }
}
