package com.saigonphantomlabs.chess;

/** Fake UI callback cho unit test execution/undo — mọi callback đều no-op. */
class NoOpChessBoardView implements ChessBoardView {
    @Override public void updateUndoButton(boolean visible) { }
    @Override public void addCapturedPiece(Chessman piece) { }
    @Override public void removeCapturedPiece(Chessman piece) { }
    @Override public void clearCapturedPieces() { }
    @Override public void showCustomGameEndDialog(boolean whiteWins, boolean isStalemate) { }
    @Override public void animateTurnChange(Chessman.PlayerColor turn) { }
    @Override public void showPromotionActivity() { }
}
