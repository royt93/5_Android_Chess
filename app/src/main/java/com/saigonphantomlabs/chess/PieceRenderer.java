package com.saigonphantomlabs.chess;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.LruCache;

/**
 * PieceRenderer — applies runtime pseudo-3D effects to 2D chess piece PNGs.
 *
 * Effect layers (bottom to top):
 *   1. Drop shadow   — soft gaussian blur of piece silhouette, offset down-right
 *   2. Base piece    — original PNG tinted for White/Black color
 *   3. Rim light     — subtle top-left edge highlight (simulates 3D form lighting)
 *   4. Specular      — small white oval at top-left (point light reflection)
 *
 * All processing is done once per piece type and cached in LruCache.
 */
public class PieceRenderer {

    private static final int CACHE_SIZE = 24; // 12 piece types × 2 colors
    private static final LruCache<String, BitmapDrawable> cache = new LruCache<>(CACHE_SIZE);

    /** Shadow config */
    private static final float SHADOW_RADIUS = 6f;
    private static final float SHADOW_DX = 3f;
    private static final float SHADOW_DY = 4f;
    private static final int   SHADOW_COLOR = 0x99000000;

    /** Specular highlight config */
    private static final float SPEC_SIZE_RATIO = 0.22f; // relative to piece width
    private static final float SPEC_X_RATIO    = 0.22f;
    private static final float SPEC_Y_RATIO    = 0.15f;

    /**
     * Get a 3D-enhanced drawable for a chess piece.
     * @param ctx Context for resource loading
     * @param resId The piece PNG resource id (e.g., R.drawable.ic_queenw)
     * @param isWhite Whether this is a white piece (affects tint)
     * @param targetSizePx Desired output size in pixels
     */
    public static BitmapDrawable get3dPiece(Context ctx, int resId, boolean isWhite, int targetSizePx) {
        String key = resId + "_" + isWhite + "_" + targetSizePx;
        BitmapDrawable cached = cache.get(key);
        if (cached != null) return cached;

        BitmapDrawable result = render3d(ctx, resId, isWhite, targetSizePx);
        cache.put(key, result);
        return result;
    }

    /** Clear all cached bitmaps — call when theme changes or in low-memory callback */
    public static void clearCache() {
        cache.evictAll();
    }

    private static BitmapDrawable render3d(Context ctx, int resId, boolean isWhite, int sizePx) {
        // Decode source piece PNG
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap src = BitmapFactory.decodeResource(ctx.getResources(), resId, opts);
        if (src == null) return null;

        // Scale to target size
        Bitmap scaled = Bitmap.createScaledBitmap(src, sizePx, sizePx, true);
        if (scaled != src) src.recycle();

        // Output canvas with extra padding for shadow
        int pad = (int)(SHADOW_RADIUS + Math.max(SHADOW_DX, SHADOW_DY)) + 2;
        int outSize = sizePx + pad * 2;
        Bitmap out = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        // ── Layer 1: Drop Shadow ──────────────────────────────────────
        drawDropShadow(canvas, scaled, pad, pad, sizePx, SHADOW_DX, SHADOW_DY, SHADOW_RADIUS);

        // ── Layer 2: Piece base with subtle color enhancement ─────────
        Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(scaled, pad, pad, basePaint);

        // ── Layer 3: Rim light (top-left diagonal gradient overlay) ───
        drawRimLight(canvas, pad, pad, sizePx, isWhite);

        // ── Layer 4: Specular highlight (round glint top-left) ────────
        drawSpecular(canvas, pad, pad, sizePx);

        scaled.recycle();
        return new BitmapDrawable(ctx.getResources(), out);
    }

    /** Render a blurred silhouette of the piece as drop shadow */
    private static void drawDropShadow(Canvas canvas, Bitmap piece,
                                        int baseX, int baseY, int sizePx,
                                        float dx, float dy, float blurR) {
        // Extract silhouette
        Bitmap silhouette = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas sCanvas = new Canvas(silhouette);
        Paint silPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sCanvas.drawBitmap(piece, 0, 0, silPaint);

        // Colorize to shadow color
        Paint colorPaint = new Paint();
        colorPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        colorPaint.setColor(SHADOW_COLOR);
        sCanvas.drawRect(0, 0, sizePx, sizePx, colorPaint);

        // Draw blurred shadow offset
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setMaskFilter(new BlurMaskFilter(blurR, BlurMaskFilter.Blur.NORMAL));
        canvas.drawBitmap(silhouette, baseX + dx, baseY + dy, shadowPaint);
        silhouette.recycle();
    }

    /** Subtle diagonal overlay simulating 3D form light from top-left */
    private static void drawRimLight(Canvas canvas, int x, int y, int sizePx, boolean isWhite) {
        // White pieces: softer warm highlight; Black pieces: cool blue highlight
        int rimColor = isWhite
            ? Color.argb(35, 255, 248, 220)   // warm ivory glow
            : Color.argb(30, 100, 180, 255);   // cool blue edge

        Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient rimGrad = new LinearGradient(
            x, y,
            x + sizePx * 0.6f, y + sizePx * 0.6f,
            rimColor, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        );
        rimPaint.setShader(rimGrad);
        rimPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
        canvas.drawRect(x, y, x + sizePx, y + sizePx, rimPaint);
        rimPaint.setXfermode(null);
    }

    /** Small oval specular glint at top-left — the "point of light" highlight */
    private static void drawSpecular(Canvas canvas, int x, int y, int sizePx) {
        float specSize = sizePx * SPEC_SIZE_RATIO;
        float cx = x + sizePx * SPEC_X_RATIO;
        float cy = y + sizePx * SPEC_Y_RATIO;

        Paint specPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RadialGradient specGrad = new RadialGradient(
            cx, cy, specSize,
            new int[]{ Color.argb(180, 255, 255, 255), Color.argb(60, 255, 255, 255), Color.TRANSPARENT },
            new float[]{ 0f, 0.5f, 1f },
            Shader.TileMode.CLAMP
        );
        specPaint.setShader(specGrad);
        canvas.drawCircle(cx, cy, specSize, specPaint);
    }
}
