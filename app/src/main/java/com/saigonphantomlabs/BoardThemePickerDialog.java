package com.saigonphantomlabs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.saigonphantomlabs.chess.BoardThemeManager;
import com.saigonphantomlabs.chess.R;

/**
 * BoardThemePickerDialog — bottom-sheet with 12 themes in 3 groups of 4.
 * Config persisted to SharedPreferences (survives app kill/restart).
 */
public class BoardThemePickerDialog {

    public interface OnThemeSelected {
        void onSelected(BoardThemeManager.Theme theme);
    }

    // Group labels
    private static final String[] GROUP_LABELS   = { "DARK & NEON", "NATURAL", "LIGHT & BRIGHT" };
    private static final int      THEMES_PER_ROW = 4;

    public static void show(Context context, BoardThemeManager.Theme currentTheme,
                            OnThemeSelected callback) {
        float dp = context.getResources().getDisplayMetrics().density;

        // Root scroll container
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(buildPanelBg(dp));
        root.setPadding((int)(16 * dp), 0, (int)(16 * dp), (int)(20 * dp));
        scroll.addView(root);

        // Drag handle
        addDragHandle(root, dp);

        // Title
        addTitle(root, dp, context);

        // Full divider
        addDivider(root, dp, 0x25FFFFFF, 16);

        AlertDialog[] dialogRef = new AlertDialog[1];
        BoardThemeManager.Theme[] all = BoardThemeManager.Theme.values();

        // 3 groups × 4 themes each
        for (int group = 0; group < 3; group++) {
            // Group header label
            addGroupLabel(root, dp, context, GROUP_LABELS[group]);

            // Row of 4 tiles
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = (int)(12 * dp);
            row.setLayoutParams(rowLp);
            row.setWeightSum(THEMES_PER_ROW);

            for (int col = 0; col < THEMES_PER_ROW; col++) {
                int idx = group * THEMES_PER_ROW + col;
                if (idx >= all.length) break;
                BoardThemeManager.Theme theme = all[idx];
                int[] colors = BoardThemeManager.THEMES[idx];
                boolean isSelected = (theme == currentTheme);

                FrameLayout tile = buildTile(context, dp, theme, idx, colors, isSelected, dialogRef, callback);
                LinearLayout.LayoutParams tileLp = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                int hMargin = (int)(3 * dp);
                tileLp.setMarginStart(col == 0 ? 0 : hMargin);
                tileLp.setMarginEnd(col == THEMES_PER_ROW - 1 ? 0 : hMargin);
                tile.setLayoutParams(tileLp);
                row.addView(tile);
            }
            root.addView(row);
        }

        // Build dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, 0);
        builder.setView(scroll);
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialogRef[0] = dialog;

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private static FrameLayout buildTile(Context ctx, float dp, BoardThemeManager.Theme theme,
                                          int idx, int[] colors, boolean isSelected,
                                          AlertDialog[] dialogRef, OnThemeSelected callback) {
        FrameLayout tile = new FrameLayout(ctx);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setPadding((int)(6 * dp), (int)(8 * dp), (int)(6 * dp), (int)(8 * dp));

        GradientDrawable tileBg = new GradientDrawable();
        tileBg.setShape(GradientDrawable.RECTANGLE);
        tileBg.setCornerRadius(10 * dp);
        // Light themes: slightly lighter panel background
        boolean isLight = (idx >= 8);
        tileBg.setColor(isLight ? 0xFF1E2A3A : 0xFF0D1624);
        tileBg.setStroke(isSelected ? (int)(2 * dp) : (int)(1 * dp),
                isSelected ? colors[2] : 0x22FFFFFF);
        tile.setBackground(tileBg);

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);

        // Mini 4x4 board preview
        android.widget.ImageView mini = new android.widget.ImageView(ctx);
        int previewPx = (int)(52 * dp);
        LinearLayout.LayoutParams miniLp = new LinearLayout.LayoutParams(previewPx, previewPx);
        miniLp.gravity = Gravity.CENTER_HORIZONTAL;
        mini.setLayoutParams(miniLp);
        mini.setImageBitmap(drawMini(colors, previewPx));

        GradientDrawable miniShape = new GradientDrawable();
        miniShape.setShape(GradientDrawable.RECTANGLE);
        miniShape.setCornerRadius(5 * dp);
        miniShape.setStroke(1, colors[2]);
        mini.setBackground(miniShape);
        mini.setClipToOutline(true);
        content.addView(mini);

        // Emoji
        TextView emoji = new TextView(ctx);
        LinearLayout.LayoutParams emojiLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        emojiLp.gravity = Gravity.CENTER_HORIZONTAL;
        emojiLp.topMargin = (int)(4 * dp);
        emoji.setLayoutParams(emojiLp);
        emoji.setText(BoardThemeManager.THEME_EMOJIS[idx]);
        emoji.setTextSize(13f);
        emoji.setGravity(Gravity.CENTER);
        content.addView(emoji);

        // Name
        TextView name = new TextView(ctx);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.topMargin = (int)(2 * dp);
        name.setLayoutParams(nameLp);
        name.setText(BoardThemeManager.THEME_NAMES[idx]);
        name.setTextSize(8.5f);
        name.setTextColor(isSelected ? colors[2] : 0x99FFFFFF);
        name.setGravity(Gravity.CENTER);
        name.setLetterSpacing(0.05f);
        content.addView(name);

        if (isSelected) {
            TextView check = new TextView(ctx);
            check.setText("✓");
            check.setTextSize(9f);
            check.setTextColor(colors[2]);
            check.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams chkLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            check.setLayoutParams(chkLp);
            content.addView(check);
        }

        FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        content.setLayoutParams(contentLp);
        tile.addView(content);

        tile.setOnClickListener(v -> {
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
            BoardThemeManager.save(ctx, theme);
            if (callback != null) callback.onSelected(theme);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
        });

        return tile;
    }

    private static android.graphics.Bitmap drawMini(int[] colors, int sizePx) {
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx,
                android.graphics.Bitmap.Config.RGB_565);
        android.graphics.Canvas c = new android.graphics.Canvas(bmp);
        android.graphics.Paint p = new android.graphics.Paint();
        int sq = sizePx / 4;
        for (int col = 0; col < 4; col++)
            for (int row = 0; row < 4; row++) {
                p.setColor((col + row) % 2 == 0 ? colors[0] : colors[1]);
                c.drawRect(col * sq, row * sq, col * sq + sq, row * sq + sq, p);
            }
        return bmp;
    }

    private static void addDragHandle(LinearLayout root, float dp) {
        android.view.View handle = new android.view.View(root.getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int)(36 * dp), (int)(4 * dp));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = (int)(10 * dp);
        lp.bottomMargin = (int)(12 * dp);
        handle.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(4 * dp);
        bg.setColor(0x33FFFFFF);
        handle.setBackground(bg);
        root.addView(handle);
    }

    private static void addTitle(LinearLayout root, float dp, Context ctx) {
        TextView title = new TextView(ctx);
        title.setText("BOARD THEME");
        title.setTextSize(13f);
        title.setTextColor(0xFF00CCFF);
        title.setLetterSpacing(0.18f);
        title.setGravity(Gravity.CENTER);
        try {
            title.setTypeface(androidx.core.content.res.ResourcesCompat
                    .getFont(ctx, R.font.cinzel_bold));
        } catch (Exception ignored) {}
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int)(10 * dp);
        title.setLayoutParams(lp);
        root.addView(title);
    }

    private static void addGroupLabel(LinearLayout root, float dp, Context ctx, String label) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = (int)(8 * dp);
        row.setLayoutParams(rowLp);

        // Decorative line
        android.view.View line = new android.view.View(ctx);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(0, 1, 1f);
        line.setLayoutParams(lineLp);
        line.setBackgroundColor(0x20FFFFFF);
        row.addView(line);

        // Label text
        TextView tv = new TextView(ctx);
        tv.setText("  " + label + "  ");
        tv.setTextSize(9f);
        tv.setTextColor(0x88FFFFFF);
        tv.setLetterSpacing(0.12f);
        row.addView(tv);

        // Right decorative line
        android.view.View line2 = new android.view.View(ctx);
        LinearLayout.LayoutParams line2Lp = new LinearLayout.LayoutParams(0, 1, 1f);
        line2.setLayoutParams(line2Lp);
        line2.setBackgroundColor(0x20FFFFFF);
        row.addView(line2);

        root.addView(row);
    }

    private static void addDivider(LinearLayout root, float dp, int color, int bottomMarginDp) {
        android.view.View div = new android.view.View(root.getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.bottomMargin = (int)(bottomMarginDp * dp);
        div.setLayoutParams(lp);
        div.setBackgroundColor(color);
        root.addView(div);
    }

    private static GradientDrawable buildPanelBg(float dp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadii(new float[]{
                20 * dp, 20 * dp, 20 * dp, 20 * dp, 0, 0, 0, 0
        });
        bg.setColor(0xF2060812);
        bg.setStroke((int)(1 * dp), 0x2000CCFF);
        return bg;
    }
}
