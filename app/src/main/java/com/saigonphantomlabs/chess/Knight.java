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
        createButton(color == PlayerColor.Black ? parent.ctx.getResources().getDrawable(R.drawable.ic_knightb, parent.ctx.getTheme()) : parent.ctx.getResources().getDrawable(R.drawable.ic_knightw, parent.ctx.getTheme()), minDimension, parent.ctx);
    }

    @Override
    public void generateMoves() {
        moves.clear();
        addLMovePoints();
    }
}
