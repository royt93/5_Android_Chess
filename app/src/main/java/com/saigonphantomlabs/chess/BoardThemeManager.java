package com.saigonphantomlabs.chess;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

/**
 * BoardThemeManager — 12 curated board themes.
 *
 * Config is persisted via SharedPreferences (survives app kill/restart automatically).
 *
 * Themes are organized in two rows of 4 (dark/neon) and one row of 4 (light/natural):
 *   Dark:    CYBERPUNK, MIDNIGHT, FIRE, ROSE
 *   Natural: CLASSIC, WALNUT, TURQUOISE, EMERALD
 *   Light:   IVORY, ARCTIC, LAVENDER, SAND
 *
 * OPT: RGB_565 bitmap (50% memory), Matrix gradient reuse, single-pass drawing.
 */
public class BoardThemeManager {

    public enum Theme {
        // ── Dark / Neon ──────────────────────────────────────
        CYBERPUNK,   // cyan glow on deep navy
        MIDNIGHT,    // electric blue on near-black
        FIRE,        // amber on charcoal (hot coals feel)
        ROSE,        // neon pink on dark mauve

        // ── Natural / Warm ───────────────────────────────────
        CLASSIC,     // cream/walnut — timeless tournament look
        WALNUT,      // rich brown tones — premium wood grain feel
        TURQUOISE,   // teal on olive — coastal vibes
        EMERALD,     // bright green on forest green

        // ── Light / Bright ───────────────────────────────────
        IVORY,       // warm off-white / pearl — very bright
        ARCTIC,      // clean light blue / white — frozen lake
        LAVENDER,    // soft purple / lilac — pastel bright
        SAND,        // warm amber cream / tan — sandy bright
    }

    private static final String PREFS = "chess_board_prefs";
    private static final String KEY   = "board_theme";

    /**
     * Theme color table: { lightSquare, darkSquare, borderColor, coordColor }
     * All colors are fully opaque (no alpha variation needed — board uses RGB_565).
     */
    public static final int[][] THEMES = {
        //                lighter square   darker square    border/glow      coord
        // ── Dark / Neon ───────────────────────────────────────────────────────
        { 0xFF0A2040, 0xFF060A14, 0xFF00FFFF, 0xFF00CCFF },   // CYBERPUNK
        { 0xFF1A2B4A, 0xFF080E20, 0xFF3355FF, 0xFF5577FF },   // MIDNIGHT
        { 0xFF3D1800, 0xFF160800, 0xFFFF7700, 0xFFFFAA44 },   // FIRE
        { 0xFF3A0820, 0xFF150408, 0xFFFF44CC, 0xFFFF88DD },   // ROSE

        // ── Natural / Warm ────────────────────────────────────────────────────
        { 0xFFF0D9B5, 0xFFB58863, 0xFF6B3A1E, 0xFF4A2800 },   // CLASSIC
        { 0xFFD4A96A, 0xFF8B5A2B, 0xFF5C3317, 0xFF3A1F00 },   // WALNUT
        { 0xFF87C5A4, 0xFF3D7559, 0xFF00AA77, 0xFF004433 },   // TURQUOISE
        { 0xFF4DB87A, 0xFF1A6640, 0xFF00CC55, 0xFF003322 },   // EMERALD

        // ── Light / Bright ────────────────────────────────────────────────────
        { 0xFFFFFAF0, 0xFFDDC090, 0xFFA07040, 0xFF7A5020 },   // IVORY
        { 0xFFE8F4FF, 0xFF90C0E0, 0xFF4488BB, 0xFF226699 },   // ARCTIC
        { 0xFFF0E8FF, 0xFFC0A0DC, 0xFF8855BB, 0xFF6633AA },   // LAVENDER
        { 0xFFFFF5D6, 0xFFDDB870, 0xFFAA8840, 0xFF886620 },   // SAND
    };

    public static final String[] THEME_NAMES = {
        "CYBERPUNK", "MIDNIGHT", "FIRE", "ROSE",
        "CLASSIC", "WALNUT", "TURQUOISE", "EMERALD",
        "IVORY", "ARCTIC", "LAVENDER", "SAND",
    };

    public static final String[] THEME_EMOJIS = {
        "⚡", "🌙", "🔥", "🌸",
        "♟", "🪵", "🌊", "🌿",
        "🤍", "❄", "💜", "🏖",
    };

    /** Load saved theme (persisted via SharedPreferences — survives app kill/restart) */
    public static Theme load(Context ctx) {
        int idx = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, 0);
        Theme[] values = Theme.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : Theme.CYBERPUNK;
    }

    /** Save chosen theme — stored to local SharedPreferences immediately (apply = async write) */
    public static void save(Context ctx, Theme theme) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putInt(KEY, theme.ordinal()).apply();
    }

    /**
     * Draw the 8x8 board into a Bitmap.
     *
     * OPT perf notes:
     *  • RGB_565 (2 bytes/px) — 50% memory vs ARGB_8888 (board has no transparency)
     *  • 2 LinearGradient reused across 64 squares via Matrix.setTranslate()
     *  • RectF, Paint instances allocated once per call
     *  • blendColors pre-computed once (not per-square)
     */
    public static Bitmap drawBoard(int[] colors, int sizePx) {
        int lightColor  = colors[0];
        int darkColor   = colors[1];
        int borderColor = colors[2];
        int coordColor  = colors[3];

        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmp);

        int sq = sizePx / 8;

        Paint paintLight  = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        Paint paintDark   = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        Paint paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint paintCoord  = new Paint(Paint.ANTI_ALIAS_FLAG);

        paintBorder.setColor(borderColor);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(1f);
        paintBorder.setAlpha(50);

        paintCoord.setColor(coordColor);
        paintCoord.setTextSize(sq * 0.18f);
        paintCoord.setAlpha(130);
        paintCoord.setFakeBoldText(true);

        // Pre-compute highlights once
        int lightHighlight = blendColors(lightColor, Color.WHITE, 0.10f);
        int darkHighlight  = blendColors(darkColor,  Color.WHITE, 0.08f);

        // Two gradients reused via Matrix translation
        LinearGradient gradLight = new LinearGradient(0, 0, sq, sq,
                lightHighlight, lightColor, Shader.TileMode.CLAMP);
        LinearGradient gradDark  = new LinearGradient(0, 0, sq, sq,
                darkHighlight, darkColor, Shader.TileMode.CLAMP);

        Matrix gradMatrix = new Matrix();
        RectF rect = new RectF();

        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                boolean isLight = (col + row) % 2 == 0;
                float left   = col * sq;
                float top    = row * sq;
                float right  = left + sq;
                float bottom = top  + sq;

                gradMatrix.reset();
                gradMatrix.setTranslate(left, top);
                if (isLight) {
                    gradLight.setLocalMatrix(gradMatrix);
                    paintLight.setShader(gradLight);
                    rect.set(left, top, right, bottom);
                    canvas.drawRect(rect, paintLight);
                } else {
                    gradDark.setLocalMatrix(gradMatrix);
                    paintDark.setShader(gradDark);
                    rect.set(left, top, right, bottom);
                    canvas.drawRect(rect, paintDark);
                }

                // Grid lines
                if (col < 7) canvas.drawLine(right, top, right, bottom, paintBorder);
                if (row < 7) canvas.drawLine(left, bottom, right, bottom, paintBorder);

                // Coordinates
                if (row == 7)
                    canvas.drawText(String.valueOf((char)('a' + col)),
                            left + sq * 0.08f, bottom - sq * 0.07f, paintCoord);
                if (col == 0)
                    canvas.drawText(String.valueOf(8 - row),
                            left + sq * 0.05f, top + sq * 0.22f, paintCoord);
            }
        }

        // Outer border ring
        paintBorder.setAlpha(150);
        paintBorder.setStrokeWidth(3f);
        canvas.drawRect(new RectF(1, 1, sizePx - 1, sizePx - 1), paintBorder);

        return bmp;
    }

    /** Apply board theme to the boardImage ImageView */
    public static void applyTheme(android.widget.ImageView boardImage, Theme theme, int sizePx) {
        if (boardImage == null || sizePx <= 0) return;
        Bitmap bmp = drawBoard(THEMES[theme.ordinal()], sizePx);
        boardImage.setImageDrawable(new BitmapDrawable(boardImage.getResources(), bmp));
    }

    private static int blendColors(int c1, int c2, float ratio) {
        int r = (int)(Color.red(c1)   * (1 - ratio) + Color.red(c2)   * ratio);
        int g = (int)(Color.green(c1) * (1 - ratio) + Color.green(c2) * ratio);
        int b = (int)(Color.blue(c1)  * (1 - ratio) + Color.blue(c2)  * ratio);
        int a = (int)(Color.alpha(c1) * (1 - ratio) + Color.alpha(c2) * ratio);
        return Color.argb(a, r, g, b);
    }
}
