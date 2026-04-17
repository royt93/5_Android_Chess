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
 * BoardThemePickerDialog — bottom-sheet with 20 light board themes.
 * Layout: 5 named groups × 4 tiles each.
 * Default theme: CLASSIC (ordinal 0).
 */
public class BoardThemePickerDialog {

    public interface OnThemeSelected {
        void onSelected(BoardThemeManager.Theme theme);
    }

    public static void show(Context context, BoardThemeManager.Theme currentTheme,
                            OnThemeSelected callback) {
        float dp = context.getResources().getDisplayMetrics().density;

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(buildPanelBg(dp));
        root.setPadding((int)(16 * dp), 0, (int)(16 * dp), (int)(24 * dp));
        scroll.addView(root);

        addDragHandle(root, dp);
        addTitle(root, dp, context);

        AlertDialog[] dialogRef = { null };
        BoardThemeManager.Theme[] all = BoardThemeManager.Theme.values();
        String[] groups  = BoardThemeManager.GROUP_LABELS;
        int perGroup     = BoardThemeManager.THEMES_PER_GROUP; // 4

        for (int g = 0; g < groups.length; g++) {
            addGroupLabel(root, dp, context, groups[g]);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(perGroup);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = (int)(14 * dp);
            row.setLayoutParams(rowLp);

            for (int col = 0; col < perGroup; col++) {
                int idx = g * perGroup + col;
                if (idx >= all.length) break;

                FrameLayout tile = buildTile(context, dp, all[idx], idx,
                        all[idx] == currentTheme, dialogRef, callback);
                LinearLayout.LayoutParams tileLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                int hm = (int)(3 * dp);
                tileLp.setMarginStart(col == 0 ? 0 : hm);
                tileLp.setMarginEnd(col == perGroup - 1 ? 0 : hm);
                tile.setLayoutParams(tileLp);
                row.addView(tile);
            }
            root.addView(row);
        }

        AlertDialog dialog = new AlertDialog.Builder(context, 0)
                .setView(scroll).setCancelable(true).create();
        dialogRef[0] = dialog;

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ─── Tile builder ──────────────────────────────────────────────────────

    private static FrameLayout buildTile(Context ctx, float dp,
                                          BoardThemeManager.Theme theme, int idx,
                                          boolean isSelected,
                                          AlertDialog[] dialogRef, OnThemeSelected callback) {
        int[] colors = BoardThemeManager.THEMES[idx];

        FrameLayout tile = new FrameLayout(ctx);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setPadding((int)(5 * dp), (int)(8 * dp), (int)(5 * dp), (int)(8 * dp));

        // Tile background — light warm white panel
        GradientDrawable tileBg = new GradientDrawable();
        tileBg.setShape(GradientDrawable.RECTANGLE);
        tileBg.setCornerRadius(10 * dp);
        tileBg.setColor(isSelected ? 0xFFFFFFFF : 0xFFF7F5F2);
        tileBg.setStroke(isSelected ? (int)(2.5f * dp) : (int)(1 * dp),
                isSelected ? colors[2] : 0xFFDDCCBB);
        tile.setBackground(tileBg);

        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);

        // Mini 4×4 board preview
        android.widget.ImageView mini = new android.widget.ImageView(ctx);
        int previewPx = (int)(52 * dp);
        LinearLayout.LayoutParams miniLp = new LinearLayout.LayoutParams(previewPx, previewPx);
        miniLp.gravity = Gravity.CENTER_HORIZONTAL;
        mini.setLayoutParams(miniLp);
        mini.setImageBitmap(drawMini(colors, previewPx));

        // Rounded border around preview
        GradientDrawable miniShape = new GradientDrawable();
        miniShape.setShape(GradientDrawable.RECTANGLE);
        miniShape.setCornerRadius(5 * dp);
        miniShape.setStroke((int)(1.5f * dp), colors[2]);
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
        emoji.setTextSize(12f);
        emoji.setGravity(Gravity.CENTER);
        content.addView(emoji);

        // Name
        TextView name = new TextView(ctx);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.topMargin = (int)(2 * dp);
        name.setLayoutParams(nameLp);
        name.setText(BoardThemeManager.THEME_NAMES[idx]);
        name.setTextSize(8f);
        name.setTextColor(isSelected ? colors[2] : 0xFF888877);
        name.setGravity(Gravity.CENTER);
        name.setLetterSpacing(0.04f);
        content.addView(name);

        // Checkmark for selected
        if (isSelected) {
            TextView check = new TextView(ctx);
            check.setText("✓");
            check.setTextSize(10f);
            check.setTextColor(colors[2]);
            check.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams chkLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chkLp.topMargin = (int)(1 * dp);
            check.setLayoutParams(chkLp);
            content.addView(check);
        }

        tile.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

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

    // ─── Mini board (4×4 preview) ──────────────────────────────────────────

    private static android.graphics.Bitmap drawMini(int[] colors, int sizePx) {
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(
                sizePx, sizePx, android.graphics.Bitmap.Config.RGB_565);
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

    // ─── Layout helpers ────────────────────────────────────────────────────

    private static GradientDrawable buildPanelBg(float dp) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadii(new float[]{ 22*dp, 22*dp, 22*dp, 22*dp, 0, 0, 0, 0 });
        // Warm off-white panel — matches the light themes palette
        bg.setColor(0xFFF8F5F0);
        bg.setStroke((int)(1 * dp), 0xFFDDCCBB);
        return bg;
    }

    private static void addDragHandle(LinearLayout root, float dp) {
        android.view.View handle = new android.view.View(root.getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                (int)(36 * dp), (int)(4 * dp));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin    = (int)(10 * dp);
        lp.bottomMargin = (int)(14 * dp);
        handle.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(4 * dp);
        bg.setColor(0xFFCCBBAA);
        handle.setBackground(bg);
        root.addView(handle);
    }

    private static void addTitle(LinearLayout root, float dp, Context ctx) {
        TextView title = new TextView(ctx);
        title.setText("CHOOSE BOARD THEME");
        title.setTextSize(12f);
        title.setTextColor(0xFF5A3E28);
        title.setLetterSpacing(0.14f);
        title.setGravity(Gravity.CENTER);
        try {
            title.setTypeface(androidx.core.content.res.ResourcesCompat
                    .getFont(ctx, R.font.cinzel_bold));
        } catch (Exception ignored) {}
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int)(14 * dp);
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

        android.view.View lineL = new android.view.View(ctx);
        lineL.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        lineL.setBackgroundColor(0xFFCCBBAA);
        row.addView(lineL);

        TextView tv = new TextView(ctx);
        tv.setText("  " + label + "  ");
        tv.setTextSize(9.5f);
        tv.setTextColor(0xFF8B6A4A);
        tv.setLetterSpacing(0.10f);
        row.addView(tv);

        android.view.View lineR = new android.view.View(ctx);
        lineR.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        lineR.setBackgroundColor(0xFFCCBBAA);
        row.addView(lineR);

        root.addView(row);
    }
}
