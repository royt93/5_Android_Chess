package com.saigonphantomlabs.chess;

public class King extends Chessman {

    public enum KingRiskType {
        Check,
        CheckMate,
        Stalemate,
        Safe
    }

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
            parent.ctx.getResources().getDrawable(resId, parent.ctx.getTheme()),
            resId, color == PlayerColor.White,
            minDimension, parent.ctx);
    }

    @Override
    public void generateMoves() {
        moves.clear();
        addAroundMovePoints();
    }
}
