package com.saigonphantomlabs.chess;

/**
 * Đánh giá an toàn vua: phân loại trạng thái (Safe/Check/CheckMate/Stalemate) + các phép mô
 * phỏng nước đi để kiểm hợp lệ. Tách khỏi {@link Chess} để giảm god-class — thao tác trực
 * tiếp trên {@code board.chessmen} qua tham chiếu Chess (không sở hữu state riêng).
 *
 * Mô phỏng theo kiểu make/undo trên chính mảng bàn cờ nên KHÔNG thread-safe (chỉ gọi trên
 * main thread, đúng như engine hiện tại).
 */
final class KingSafetyEvaluator {

    private final Chess board;

    KingSafetyEvaluator(Chess board) {
        this.board = board;
    }

    /** Phân loại trạng thái của vua {@code k}: Safe / Check / CheckMate / Stalemate. */
    King.KingRiskType validate(King k) {
        ChessLog.d(
                "validateKing: checking " + k.color + " king at (" + k.getPoint().x + "," + k.getPoint().y + ")");
        k.generateMoves();
        ChessLog.d("validateKing: king has " + k.moves.size() + " potential moves");

        boolean inCheck = !k.isPointSafe();
        ChessLog.d("validateKing: inCheck = " + inCheck);

        // Check if king can move to any safe square (with simulation)
        boolean kingCanMove = false;
        for (Point p : k.moves) {
            boolean moveSafe = isKingMoveSafe(k, p);
            ChessLog.d("validateKing: king move to (" + p.x + "," + p.y + ") safe = " + moveSafe);
            if (moveSafe) {
                kingCanMove = true;
                break;
            }
        }
        ChessLog.d("validateKing: kingCanMove = " + kingCanMove);

        // Check if any other piece has legal moves
        boolean otherPiecesCanMove = hasAnyLegalMove(k.color);
        ChessLog.d("validateKing: otherPiecesCanMove = " + otherPiecesCanMove);

        if (inCheck) {
            // King is in check
            ChessLog.d("validateKing: King IS in check");
            if (kingCanMove) {
                ChessLog.d("validateKing: returning CHECK (king can escape)");
                return King.KingRiskType.Check; // King can escape
            }
            boolean canSave = canAnyPieceSaveKing(k);
            ChessLog.d("validateKing: canAnyPieceSaveKing = " + canSave);
            if (canSave) {
                ChessLog.d("validateKing: returning CHECK (piece can save)");
                return King.KingRiskType.Check; // Another piece can save
            }
            ChessLog.d("validateKing: returning CHECKMATE");
            return King.KingRiskType.CheckMate;
        } else {
            // King is NOT in check
            ChessLog.d("validateKing: King is NOT in check");
            if (!kingCanMove && !otherPiecesCanMove) {
                ChessLog.d("validateKing: returning STALEMATE");
                return King.KingRiskType.Stalemate;
            }
            ChessLog.d("validateKing: returning SAFE");
            return King.KingRiskType.Safe;
        }
    }

    /** Check if King moving to target point would be safe (simulates the move). */
    private boolean isKingMoveSafe(King k, Point target) {
        Point originalPos = k.getPoint();
        Chessman capturedPiece = board.chessmen[target.x][target.y];

        // Simulate King's move
        board.chessmen[target.x][target.y] = k;
        board.chessmen[originalPos.x][originalPos.y] = null;
        k.setPoint(target);

        // Check if King is safe at new position
        boolean isSafe = k.isPointSafe();
        ChessLog.d(
                "isKingMoveSafe: " + k.color + " King moving to (" + target.x + "," + target.y + ") safe=" + isSafe);

        // Undo simulation
        board.chessmen[originalPos.x][originalPos.y] = k;
        board.chessmen[target.x][target.y] = capturedPiece;
        k.setPoint(originalPos);

        return isSafe;
    }

    /** Check if any piece of the given color has any legal moves. */
    private boolean hasAnyLegalMove(Chessman.PlayerColor color) {
        King k = (color == Chessman.PlayerColor.White) ? board.whiteKing : board.blackKing;
        ChessLog.d("hasAnyLegalMove: checking for " + color);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Chessman piece = board.chessmen[x][y];
                if (piece != null && !piece.isDead && piece.color == color
                        && piece.type != Chessman.ChessmanType.King) {
                    piece.generateMoves();

                    // Check if any move is legal (doesn't put own king in check)
                    for (Point target : piece.moves) {
                        if (isMoveLegal(piece, target, k)) {
                            ChessLog.d("hasAnyLegalMove: FOUND legal move! Piece " + piece.type + " at (" + x + ","
                                    + y + ") to (" + target.x + "," + target.y + ")");
                            return true;
                        }
                    }
                }
            }
        }
        ChessLog.d("hasAnyLegalMove: NO legal moves found for any piece!");
        return false;
    }

    /** Check if a move is legal (doesn't put own king in check). */
    private boolean isMoveLegal(Chessman piece, Point target, King k) {
        Point originalPos = piece.getPoint();
        Chessman capturedPiece = board.chessmen[target.x][target.y];

        // En passant: con tốt bị bắt nằm ở (target.x, originalPos.y), phải gỡ khi mô phỏng
        boolean ep = isEnPassantPseudoMove(piece, originalPos, target, capturedPiece);
        Chessman epVic = null;
        if (ep) {
            epVic = board.chessmen[target.x][originalPos.y];
            board.chessmen[target.x][originalPos.y] = null;
        }

        // Simulate the move
        board.chessmen[target.x][target.y] = piece;
        board.chessmen[originalPos.x][originalPos.y] = null;
        piece.setPoint(target);

        // Check if our king is in check after this move
        boolean kingInCheck = !k.isPointSafe();

        // Undo the simulation
        board.chessmen[originalPos.x][originalPos.y] = piece;
        board.chessmen[target.x][target.y] = capturedPiece;
        piece.setPoint(originalPos);
        if (ep) board.chessmen[target.x][originalPos.y] = epVic;

        return !kingInCheck;
    }

    /**
     * True nếu (piece, target) là một nước bắt tốt qua đường (en passant) trên board thật:
     * tốt đi chéo sang ô trống trùng enPassantTarget.
     */
    private boolean isEnPassantPseudoMove(Chessman piece, Point from, Point target, Chessman capturedAtTarget) {
        return (piece instanceof Pawn) && target.x != from.x && capturedAtTarget == null
                && board.enPassantTarget != null && target.equals(board.enPassantTarget);
    }

    /** Check if any piece of the same color can make a move that removes the check. */
    private boolean canAnyPieceSaveKing(King k) {
        Chessman.PlayerColor kingColor = k.color;
        ChessLog.d("canAnyPieceSaveKing: checking for " + kingColor);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Chessman piece = board.chessmen[x][y];
                if (piece != null && !piece.isDead && piece.color == kingColor
                        && piece.type != Chessman.ChessmanType.King) {
                    piece.generateMoves();

                    for (Point targetMove : piece.moves) {
                        if (wouldMoveSaveKing(piece, targetMove, k)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Simulate a move and check if it would save the king from check. */
    private boolean wouldMoveSaveKing(Chessman piece, Point target, King k) {
        Point originalPos = piece.getPoint();
        Chessman capturedPiece = board.chessmen[target.x][target.y];

        // En passant: gỡ con tốt bị bắt qua đường khi mô phỏng (vd bắt qua đường để giải chiếu)
        boolean ep = isEnPassantPseudoMove(piece, originalPos, target, capturedPiece);
        Chessman epVic = null;
        if (ep) {
            epVic = board.chessmen[target.x][originalPos.y];
            board.chessmen[target.x][originalPos.y] = null;
        }

        // Simulate the move
        board.chessmen[target.x][target.y] = piece;
        board.chessmen[originalPos.x][originalPos.y] = null;
        piece.setPoint(target);

        // Check if king is still in check
        boolean stillInCheck = !k.isPointSafe();

        // Undo the simulation
        board.chessmen[originalPos.x][originalPos.y] = piece;
        board.chessmen[target.x][target.y] = capturedPiece;
        piece.setPoint(originalPos);
        if (ep) board.chessmen[target.x][originalPos.y] = epVic;

        return !stillInCheck;
    }
}
