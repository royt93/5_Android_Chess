package com.saigonphantomlabs.chess;

public class Bishop extends Chessman {
    public Bishop(Point p, PlayerColor color, int minDimension, Chess parent) {
        this.parent = parent;
        setPoint(p);
        type = ChessmanType.Bishop;
        this.color = color;
        this.minDimension = minDimension;
    }

    @Override
    public void createButton() {
        int resId = (color == PlayerColor.Black) ? R.drawable.ic_bishopb : R.drawable.ic_bishopw;
        super.createButton(
            parent.ctx.getResources().getDrawable(resId, parent.ctx.getTheme()),
            resId, color == PlayerColor.White,
            minDimension, parent.ctx);
    }

    @Override
    public void generateMoves() {
        moves.clear();
        addObliqueNEtoSWMovePoints();
        addObliqueNWtoSEMovePoints();
    }
}
