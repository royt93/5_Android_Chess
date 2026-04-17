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
 * BoardThemeManager — 20 light/bright board themes.
 *
 * DEFAULT = CLASSIC (ordinal 0) — persisted via SharedPreferences.
 *
 * Themes are grouped into 5 families of 4 for the picker UI:
 *   🪵 WOOD & CLASSIC   — Classic, Ivory, Sand, Oak
 *   🌊 OCEAN & SKY      — Arctic, Sky, Cerulean, Denim
 *   🌸 BLUSH & FLORAL   — Lavender, Blush, Rose Quartz, Rose Gold
 *   🌿 NATURE & GREEN   — Mint, Sage, Jade, Wheat
 *   ☀️ WARM & BRIGHT    — Peach, Coral, Mineral, Buttercup
 */
public class BoardThemeManager {

    public enum Theme {
        // ── 🪵 Wood & Classic ─────────────────────────────────────────────
        CLASSIC,     // cream / walnut — timeless tournament default
        IVORY,       // warm off-white / golden cream
        SAND,        // bright cream / warm tan
        OAK,         // warm amber / medium oak

        // ── 🌊 Ocean & Sky ────────────────────────────────────────────────
        ARCTIC,      // near-white / soft ice blue
        SKY,         // white / muted steel blue
        CERULEAN,    // pale sky / cerulean blue
        DENIM,       // pearl / soft denim blue

        // ── 🌸 Blush & Floral ─────────────────────────────────────────────
        LAVENDER,    // white-lavender / medium lilac
        BLUSH,       // petal white / dusty rose
        ROSE_QUARTZ, // shell white / rose quartz pink
        ROSE_GOLD,   // cream white / warm rose gold

        // ── 🌿 Nature & Green ─────────────────────────────────────────────
        MINT,        // fresh white / cool mint green
        SAGE,        // pale sage / medium sage green
        JADE,        // white-jade / celadon jade
        WHEAT,       // cream yellow / warm wheat gold

        // ── ☀️ Warm & Bright ──────────────────────────────────────────────
        PEACH,       // warm white / soft peach apricot
        CORAL,       // pearl white / warm coral
        MINERAL,     // light silver / blue-grey platinum
        BUTTERCUP,   // bright cream / warm golden yellow
    }

    private static final String PREFS   = "chess_board_prefs";
    private static final String KEY     = "board_theme";
    private static final int    DEFAULT = 0; // CLASSIC

    /**
     * Theme colors: { lightSquare, darkSquare, borderAccent, coordColor }
     * Must match Theme enum ordinal order exactly.
     */
    public static final int[][] THEMES = {
        //                 light sq.    dark sq.     border       coords
        // ── 🪵 Wood & Classic ──────────────────────────────────────────────
        { 0xFFF0D9B5, 0xFFB58863, 0xFF7A4A1E, 0xFF4A2800 },  // CLASSIC
        { 0xFFFFF9ED, 0xFFD4B483, 0xFF9B7843, 0xFF7A5020 },  // IVORY
        { 0xFFFFF0CC, 0xFFCCAA66, 0xFF9B7733, 0xFF7A5510 },  // SAND
        { 0xFFEAD5A8, 0xFFBB8844, 0xFF8B5A22, 0xFF663300 },  // OAK

        // ── 🌊 Ocean & Sky ─────────────────────────────────────────────────
        { 0xFFEEF6FF, 0xFF99BBDD, 0xFF4488BB, 0xFF226699 },  // ARCTIC
        { 0xFFE4F0FF, 0xFF88AACC, 0xFF3366AA, 0xFF1144AA },  // SKY
        { 0xFFEAF5FF, 0xFF88BCD8, 0xFF2288BB, 0xFF006699 },  // CERULEAN
        { 0xFFF0F3FF, 0xFF8899CC, 0xFF3355AA, 0xFF114488 },  // DENIM

        // ── 🌸 Blush & Floral ──────────────────────────────────────────────
        { 0xFFF5F0FF, 0xFFBBA0DC, 0xFF8855BB, 0xFF6633AA },  // LAVENDER
        { 0xFFFFF3F7, 0xFFEAAFC4, 0xFFCC6688, 0xFFAA4466 },  // BLUSH
        { 0xFFFFF5F8, 0xFFE8B8CC, 0xFFCC7799, 0xFFAA5577 },  // ROSE_QUARTZ
        { 0xFFFFF2EE, 0xFFDDAA99, 0xFFBB7766, 0xFF994444 },  // ROSE_GOLD

        // ── 🌿 Nature & Green ──────────────────────────────────────────────
        { 0xFFEAFFF3, 0xFF88CCB0, 0xFF339977, 0xFF116655 },  // MINT
        { 0xFFF0F8F0, 0xFF99BB99, 0xFF557755, 0xFF335533 },  // SAGE
        { 0xFFEDFFF8, 0xFF88C4A8, 0xFF339966, 0xFF117744 },  // JADE
        { 0xFFFFFAE0, 0xFFE0CC80, 0xFFAA9922, 0xFF887700 },  // WHEAT

        // ── ☀️ Warm & Bright ───────────────────────────────────────────────
        { 0xFFFFF5EE, 0xFFEEB898, 0xFFCC7744, 0xFFAA5522 },  // PEACH
        { 0xFFFFF4F2, 0xFFEEA898, 0xFFCC5544, 0xFFAA3322 },  // CORAL
        { 0xFFF2F4F6, 0xFFBBC4CC, 0xFF778899, 0xFF556677 },  // MINERAL
        { 0xFFFFFCE0, 0xFFE8D060, 0xFFBBAA00, 0xFF887700 },  // BUTTERCUP
    };

    public static final String[] THEME_NAMES = {
        "CLASSIC", "IVORY", "SAND", "OAK",
        "ARCTIC", "SKY", "CERULEAN", "DENIM",
        "LAVENDER", "BLUSH", "ROSE QUARTZ", "ROSE GOLD",
        "MINT", "SAGE", "JADE", "WHEAT",
        "PEACH", "CORAL", "MINERAL", "BUTTERCUP",
    };

    public static final String[] THEME_EMOJIS = {
        "♟", "🤍", "🏖", "🪵",
        "❄", "🌤", "🌊", "🔵",
        "💜", "🌸", "💗", "🌹",
        "🌿", "🍃", "💚", "🌾",
        "🍑", "🪸", "🪨", "🌻",
    };

    // Picker groups: 5 groups × 4 = 20
    public static final String[] GROUP_LABELS = {
        "🪵  WOOD & CLASSIC",
        "🌊  OCEAN & SKY",
        "🌸  BLUSH & FLORAL",
        "🌿  NATURE & GREEN",
        "☀️  WARM & BRIGHT",
    };
    public static final int THEMES_PER_GROUP = 4;

    // ─────────────────────────────────────────────────────────────────────────

    /** Load saved theme — default is CLASSIC (index 0) */
    public static Theme load(Context ctx) {
        int idx = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, DEFAULT);
        Theme[] values = Theme.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : Theme.CLASSIC;
    }

    /** Persist chosen theme immediately (async write) */
    public static void save(Context ctx, Theme theme) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putInt(KEY, theme.ordinal()).apply();
    }

    /**
     * Draw an 8×8 board Bitmap at sizePx.
     *
     * OPT: RGB_565 (2 bytes/px), 2 LinearGradient reused via Matrix, single RectF instance.
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
        paintBorder.setAlpha(55);

        paintCoord.setColor(coordColor);
        paintCoord.setTextSize(sq * 0.18f);
        paintCoord.setAlpha(140);
        paintCoord.setFakeBoldText(true);

        int lightHL = blendColors(lightColor, Color.WHITE, 0.10f);
        int darkHL  = blendColors(darkColor,  Color.WHITE, 0.08f);

        LinearGradient gradLight = new LinearGradient(0, 0, sq, sq,
                lightHL, lightColor, Shader.TileMode.CLAMP);
        LinearGradient gradDark  = new LinearGradient(0, 0, sq, sq,
                darkHL, darkColor, Shader.TileMode.CLAMP);

        Matrix gradMatrix = new Matrix();
        RectF  rect       = new RectF();

        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                boolean isLight = (col + row) % 2 == 0;
                float left   = col * sq,  top    = row * sq;
                float right  = left + sq, bottom = top  + sq;

                gradMatrix.reset();
                gradMatrix.setTranslate(left, top);
                if (isLight) { gradLight.setLocalMatrix(gradMatrix); paintLight.setShader(gradLight); rect.set(left, top, right, bottom); canvas.drawRect(rect, paintLight); }
                else         { gradDark.setLocalMatrix(gradMatrix);  paintDark.setShader(gradDark);   rect.set(left, top, right, bottom); canvas.drawRect(rect, paintDark);  }

                if (col < 7) canvas.drawLine(right, top, right, bottom, paintBorder);
                if (row < 7) canvas.drawLine(left, bottom, right, bottom, paintBorder);

                if (row == 7) canvas.drawText(String.valueOf((char)('a' + col)), left + sq * 0.08f, bottom - sq * 0.07f, paintCoord);
                if (col == 0) canvas.drawText(String.valueOf(8 - row), left + sq * 0.05f, top + sq * 0.22f, paintCoord);
            }
        }

        paintBorder.setAlpha(150);
        paintBorder.setStrokeWidth(3f);
        canvas.drawRect(new RectF(1, 1, sizePx - 1, sizePx - 1), paintBorder);
        return bmp;
    }

    public static void applyTheme(android.widget.ImageView boardImage, Theme theme, int sizePx) {
        if (boardImage == null || sizePx <= 0) return;
        boardImage.setImageDrawable(
            new BitmapDrawable(boardImage.getResources(),
                drawBoard(THEMES[theme.ordinal()], sizePx)));
    }

    private static int blendColors(int c1, int c2, float ratio) {
        return Color.argb(
            (int)(Color.alpha(c1) * (1-ratio) + Color.alpha(c2) * ratio),
            (int)(Color.red(c1)   * (1-ratio) + Color.red(c2)   * ratio),
            (int)(Color.green(c1) * (1-ratio) + Color.green(c2) * ratio),
            (int)(Color.blue(c1)  * (1-ratio) + Color.blue(c2)  * ratio));
    }
}
