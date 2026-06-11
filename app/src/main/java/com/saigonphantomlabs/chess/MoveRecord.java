package com.saigonphantomlabs.chess;

import android.content.Context;

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

    // --- Special moves (set bởi Chess.doMove cho ván thật; AIEngine không dùng) ---
    // Nhập thành (castling)
    public boolean isCastle = false;
    public int rookFromX, rookFromY, rookToX, rookToY;
    public Chessman castledRook = null;
    // Bắt tốt qua đường (en passant)
    public boolean isEnPassant = false;
    public int epVictimX, epVictimY; // ô con tốt bị bắt qua đường (khác toX/toY)
    // Khôi phục cờ "chưa di chuyển" của vua/xe khi undo
    public boolean movedPieceWasUnmoved = false;
    // Trạng thái en passant TRƯỚC nước đi này (để undo phục hồi)
    public Point prevEnPassantTarget = null;
    // Luật hoà: đồng hồ 50-nước TRƯỚC nước này + khoá thế CỦA nước này (để undo phục hồi/đếm)
    public int prevHalfMoveClock = 0;
    public String resultingPositionKey = null;

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
    public String getNotation(Context context) {
        String pieceSymbol = getPieceSymbol(context, movedPiece.type);
        String from = getSquareName(fromX, fromY);
        String to = getSquareName(toX, toY);
        String capture = capturedPiece != null ? context.getString(R.string.notation_capture)
                : context.getString(R.string.notation_move);
        String promotion = promotedTo != null
                ? context.getString(R.string.notation_promotion) + getPieceSymbol(context, promotedTo)
                : "";
        return pieceSymbol + from + capture + to + promotion;
    }

    private String getPieceSymbol(Context context, Chessman.ChessmanType type) {
        switch (type) {
            case King:
                return context.getString(R.string.symbol_king);
            case Queen:
                return context.getString(R.string.symbol_queen);
            case Rook:
                return context.getString(R.string.symbol_rook);
            case Bishop:
                return context.getString(R.string.symbol_bishop);
            case Knight:
                return context.getString(R.string.symbol_knight);
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
