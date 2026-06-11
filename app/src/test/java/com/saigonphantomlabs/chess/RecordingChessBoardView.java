package com.saigonphantomlabs.chess;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake {@link ChessBoardView} cho test execution/integration: ghi nhận các callback quan
 * trọng (game-end dialog, captured pieces, undo-button) để test quan sát hành vi mà không
 * cần Android/UI. Mở rộng từ {@link NoOpChessBoardView} bằng cách ghi lại trạng thái.
 */
class RecordingChessBoardView implements ChessBoardView {

    // Game-end dialog
    boolean gameEndShown = false;
    boolean lastWhiteWins = false;
    boolean lastIsStalemate = false;
    int gameEndCount = 0;

    // Captured pieces
    final List<Chessman> captured = new ArrayList<>();

    // Undo button
    boolean undoVisible = false;
    boolean promotionRequested = false;
    Chessman.PlayerColor lastTurnChange = null;

    @Override public void updateUndoButton(boolean visible) { undoVisible = visible; }

    @Override public void addCapturedPiece(Chessman piece) { captured.add(piece); }

    @Override public void removeCapturedPiece(Chessman piece) { captured.remove(piece); }

    @Override public void clearCapturedPieces() { captured.clear(); }

    @Override
    public void showCustomGameEndDialog(boolean whiteWins, boolean isStalemate) {
        gameEndShown = true;
        gameEndCount++;
        lastWhiteWins = whiteWins;
        lastIsStalemate = isStalemate;
    }

    @Override public void animateTurnChange(Chessman.PlayerColor turn) { lastTurnChange = turn; }

    @Override public void showPromotionActivity() { promotionRequested = true; }
}
