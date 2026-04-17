package com.saigonphantomlabs.chess;

public class Queen extends Chessman {

    public Queen(Point p, PlayerColor color, int minDimension, Chess parent) {
        this.parent = parent;
        setPoint(p);
        type = ChessmanType.Queen;
        this.color = color;
        this.minDimension = minDimension;
    }

    @Override
    public void createButton() {
        int resId = (color == PlayerColor.Black) ? R.drawable.ic_queenb : R.drawable.ic_queenw;
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
        addObliqueNEtoSWMovePoints();
        addObliqueNWtoSEMovePoints();
    }
}
