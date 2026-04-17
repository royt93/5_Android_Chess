package com.saigonphantomlabs.chess;

public class Knight extends Chessman {
    public Knight(Point p, PlayerColor color, int minDimension, Chess parent) {
        this.parent = parent;
        setPoint(p);
        type = ChessmanType.Knight;
        this.color = color;
        this.minDimension = minDimension;
    }

    @Override
    public void createButton() {
        int resId = (color == PlayerColor.Black) ? R.drawable.ic_knightb : R.drawable.ic_knightw;
        createButton(
            parent.ctx.getResources().getDrawable(resId, parent.ctx.getTheme()),
            resId, color == PlayerColor.White,
            minDimension, parent.ctx);
    }

    @Override
    public void generateMoves() {
        moves.clear();
        addLMovePoints();
    }
}
