package com.saigonphantomlabs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.saigonphantomlabs.chess.Chessman;
import com.saigonphantomlabs.chess.GameSaveManager;

/**
 * Bàn cờ thu nhỏ (thumbnail) cho danh sách ván đã lưu. Vẽ trực tiếp trong onDraw từ
 * {@link GameSaveManager.SavedGame} — 8x8 ô + glyph unicode quân (trắng/đen). KHÔNG cache bitmap
 * → nhẹ RAM, chỉ row đang hiển thị mới vẽ (RecyclerView tái dùng).
 */
public class MiniBoardView extends View {

    private static final int LIGHT = 0xFFE7E9F4;
    private static final int DARK = 0xFF8089B6;

    // Glyph unicode đặc (solid) cho mọi quân — tô màu trắng/đen theo bên.
    private static final String[] GLYPH = new String[6]; // index theo ChessmanType.ordinal()

    private final Paint sq = new Paint();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private GameSaveManager.SavedGame game;

    public MiniBoardView(Context c) { super(c); init(); }
    public MiniBoardView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        sq.setStyle(Paint.Style.FILL);
        fill.setStyle(Paint.Style.FILL);
        outline.setStyle(Paint.Style.STROKE);
        // Map type → glyph (King..Pawn)
        GLYPH[Chessman.ChessmanType.King.ordinal()] = "♚";
        GLYPH[Chessman.ChessmanType.Queen.ordinal()] = "♛";
        GLYPH[Chessman.ChessmanType.Rook.ordinal()] = "♜";
        GLYPH[Chessman.ChessmanType.Bishop.ordinal()] = "♝";
        GLYPH[Chessman.ChessmanType.Knight.ordinal()] = "♞";
        GLYPH[Chessman.ChessmanType.Pawn.ordinal()] = "♟";
    }

    public void setPosition(GameSaveManager.SavedGame g) {
        this.game = g;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int s = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(s, s); // ép vuông
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = Math.min(getWidth(), getHeight());
        if (n <= 0) return;
        float cell = n / 8f;

        // 8x8 ô
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                sq.setColor(((x + y) % 2 == 0) ? LIGHT : DARK);
                canvas.drawRect(x * cell, y * cell, (x + 1) * cell, (y + 1) * cell, sq);
            }
        }

        if (game == null) return;

        float textSize = cell * 0.86f;
        fill.setTextSize(textSize);
        outline.setTextSize(textSize);
        outline.setStrokeWidth(Math.max(1f, cell * 0.05f));
        Paint.FontMetrics fm = fill.getFontMetrics();
        float glyphH = fm.descent - fm.ascent;

        for (GameSaveManager.PieceData p : game.pieces) {
            String g = GLYPH[p.type.ordinal()];
            if (g == null) continue;
            float w = fill.measureText(g);
            float cx = p.x * cell + cell / 2f;
            float cy = p.y * cell + cell / 2f;
            float tx = cx - w / 2f;
            float ty = cy + glyphH / 2f - fm.descent;
            boolean white = p.color == Chessman.PlayerColor.White;
            // Viền tương phản + tô màu quân
            outline.setColor(white ? Color.BLACK : 0xFFEEEEEE);
            fill.setColor(white ? Color.WHITE : 0xFF20232B);
            canvas.drawText(g, tx, ty, outline);
            canvas.drawText(g, tx, ty, fill);
        }
    }
}
