package com.saigonphantomlabs.chess;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;

/**
 * Gom animation highlight quân được chọn (pulse) + flash khi bị chiếu — tách khỏi
 * {@link Chess} để giảm god-class. Tự quản lý animator + quân/vua đang highlight.
 * No-arg, không chạm Android lúc khởi tạo → an toàn cho cả test seam.
 */
final class ChessAnimator {
    private ObjectAnimator selectionPulse;
    private Chessman selectedPiece;
    private ObjectAnimator checkFlash;
    private King kingInCheck;

    /** Bắt đầu pulse alpha trên quân được chọn (chỉ alpha — không scale để không tràn ô). */
    void startSelection(Context ctx, Chessman man) {
        if (man == null || man.button == null || ctx == null) return;
        man.button.setBackground(ctx.getResources().getDrawable(R.drawable.bg_piece_selected, ctx.getTheme()));
        selectedPiece = man;
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.45f, 1.0f);
        selectionPulse = ObjectAnimator.ofPropertyValuesHolder(man.button, alpha);
        selectionPulse.setDuration(500);
        selectionPulse.setRepeatCount(ValueAnimator.INFINITE);
        selectionPulse.start();
    }

    void clearSelection() {
        if (selectionPulse != null) {
            selectionPulse.cancel();
            selectionPulse = null;
        }
        if (selectedPiece != null && selectedPiece.button != null) {
            selectedPiece.button.setScaleX(1.0f);
            selectedPiece.button.setScaleY(1.0f);
            selectedPiece.button.setBackground(null);
        }
        selectedPiece = null;
    }

    /** Flash đỏ + rung khi vua bị chiếu. */
    void showCheck(Context ctx, King king) {
        if (king == null || king.button == null || ctx == null) return;
        kingInCheck = king;
        king.button.setBackground(ctx.getResources().getDrawable(R.drawable.bg_piece_in_check, ctx.getTheme()));
        PropertyValuesHolder pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.15f, 1f);
        checkFlash = ObjectAnimator.ofPropertyValuesHolder(king.button, pvhAlpha);
        checkFlash.setDuration(400);
        checkFlash.setRepeatCount(ValueAnimator.INFINITE);
        checkFlash.start();
        ChessHaptics.check(ctx);
    }

    void clearCheck() {
        if (checkFlash != null) {
            checkFlash.cancel();
            checkFlash = null;
        }
        if (kingInCheck != null && kingInCheck.button != null) {
            kingInCheck.button.setAlpha(1f);
            kingInCheck.button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
        kingInCheck = null;
    }
}
