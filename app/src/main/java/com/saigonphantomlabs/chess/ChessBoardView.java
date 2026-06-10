package com.saigonphantomlabs.chess;

/**
 * Tách engine cờ ({@link Chess}) khỏi Android UI cụ thể.
 *
 * Trước đây {@code Chess} cast thẳng {@code (ChessBoardActivity) ctx} để gọi callback UI,
 * khiến không thể unit-test logic thực thi/undo nếu không có Activity thật.
 * Nay {@code Chess} chỉ phụ thuộc interface này; production dùng {@code ChessBoardActivity},
 * test dùng một fake/mock.
 */
public interface ChessBoardView {
    /** Hiện/ẩn nút Undo (và Restart). */
    void updateUndoButton(boolean visible);

    /** Thêm một quân vừa bị bắt vào dải hiển thị. */
    void addCapturedPiece(Chessman piece);

    /** Gỡ một quân khỏi dải bị bắt (khi undo). */
    void removeCapturedPiece(Chessman piece);

    /** Xoá toàn bộ dải quân bị bắt (khi reset). */
    void clearCapturedPieces();

    /** Hiện dialog kết thúc ván (thắng/thua/hoà). */
    void showCustomGameEndDialog(boolean whiteWins, boolean isStalemate);

    /** Animation đổi lượt + cập nhật chỉ báo lượt đi. */
    void animateTurnChange(Chessman.PlayerColor turn);

    /** Mở màn hình chọn quân phong cấp. */
    void showPromotionActivity();
}
