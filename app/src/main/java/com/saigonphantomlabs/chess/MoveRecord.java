package com.saigonphantomlabs.chess;

/**
 * Represents a single move in the game for undo/redo and move history
 */
public class MoveRecord {
    public final int fromX;
    public final int fromY;
    public final int toX;
    public final int toY;
    public final Chessman movedPiece;
    public final Chessman capturedPiece; // null if no capture
    public final Chessman.ChessmanType promotedTo; // null if not a promotion
    public final boolean wasFirstMove; // for pawn's first move tracking

    public MoveRecord(int fromX, int fromY, int toX, int toY,
            Chessman movedPiece, Chessman capturedPiece,
            Chessman.ChessmanType promotedTo, boolean wasFirstMove) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.promotedTo = promotedTo;
        this.wasFirstMove = wasFirstMove;
    }

    /**
     * Get algebraic notation for the move (e.g., "e2-e4", "Nf3xg5")
     */
    public String getNotation() {
        String pieceSymbol = getPieceSymbol(movedPiece.type);
        String from = getSquareName(fromX, fromY);
        String to = getSquareName(toX, toY);
        String capture = capturedPiece != null ? "x" : "-";
        String promotion = promotedTo != null ? "=" + getPieceSymbol(promotedTo) : "";
        return pieceSymbol + from + capture + to + promotion;
    }

    private String getPieceSymbol(Chessman.ChessmanType type) {
        switch (type) {
            case King:
                return "K";
            case Queen:
                return "Q";
            case Rook:
                return "R";
            case Bishop:
                return "B";
            case Knight:
                return "N";
            case Pawn:
                return "";
            default:
                return "";
        }
    }

    private String getSquareName(int x, int y) {
        char file = (char) ('a' + x);
        int rank = 8 - y;
        return "" + file + rank;
    }
}
