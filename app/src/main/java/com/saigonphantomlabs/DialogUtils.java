package com.saigonphantomlabs;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.saigonphantomlabs.chess.R;

public class DialogUtils {

    /**
     * Show a basic Glass Dialog with Title, Message, and Two Buttons
     */
    public static void showBasicDialog(Context context,
            String title,
            CharSequence message,
            String positiveText,
            String negativeText,
            int iconResId,
            Runnable onPositive,
            Runnable onNegative) {

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_glass_generic, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Setup Views
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        ImageView iconView = dialogView.findViewById(R.id.dialog_icon);
        MaterialButton btnPositive = dialogView.findViewById(R.id.btn_positive);

        titleView.setText(title);
        messageView.setText(message);

        if (iconResId != 0) {
            iconView.setImageResource(iconResId);
        } else {
            iconView.setVisibility(View.GONE);
        }

        btnPositive.setText(positiveText);
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            if (onPositive != null)
                onPositive.run();
        });

        if (negativeText != null) {
            MaterialButton btnMsgNegative = dialogView.findViewById(R.id.btn_negative);
            btnMsgNegative.setText(negativeText);
            btnMsgNegative.setOnClickListener(v -> {
                dialog.dismiss();
                if (onNegative != null)
                    onNegative.run();
            });
        } else {
            dialogView.findViewById(R.id.btn_negative).setVisibility(View.GONE);
        }

        // Animation
        Animation enterAnim = AnimationUtils.loadAnimation(context, R.anim.dialog_enter_anim);
        dialogView.startAnimation(enterAnim);

        dialog.show();
        // Allow scale animation to overflow dialog window edges (no clipping)
        disableDialogClipping(dialog);
    }

    /**
     * Show Rules Dialog
     */
    public static void showRulesDialog(Context context) {
        CharSequence rules;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            rules = android.text.Html.fromHtml(context.getString(R.string.game_rules),
                    android.text.Html.FROM_HTML_MODE_LEGACY);
        } else {
            rules = android.text.Html.fromHtml(context.getString(R.string.game_rules));
        }

        showBasicDialog(context,
                context.getString(R.string.game_rules_title),
                rules,
                context.getString(R.string.got_it),
                null,
                R.drawable.ic_rules_blue,
                null,
                null);
    }

    /**
     * Show Quit Game Confirmation
     */
    public static void showQuitDialog(Context context, Runnable onQuit) {
        showBasicDialog(context,
                context.getString(R.string.warning),
                context.getString(R.string.saveBoardPrompt), // "Are you sure you want to quit?" logic
                context.getString(R.string.yes),
                context.getString(R.string.no),
                R.drawable.ic_warning_ember, // Ensure this exists or use generic
                onQuit,
                null);
    }

    // Fallback if ic_warning_ember doesn't exist, use 0 or standard
    // Actually we can pass 0 and hide it, or use a standard one.
    // For now let's assume we want a warning icon. check if ic_star exists? yes.

    /**
     * Show Restart Game Confirmation
     */
    public static void showRestartDialog(Context context, Runnable onRestart) {
        showBasicDialog(context,
                context.getString(R.string.play_again),
                context.getString(R.string.restart_confirm),
                context.getString(R.string.yes),
                context.getString(R.string.no),
                R.drawable.ic_restart_green,
                onRestart,
                null);
    }

    /**
     * Show Difficulty Selection Dialog
     */
    public static void showDifficultyDialog(Context context, DifficultySelectionCallback callback) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_difficulty, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        LinearLayout container = dialogView.findViewById(R.id.difficultyContainer);

        int[] difficulties = { R.string.difficulty_easy, R.string.difficulty_medium, R.string.difficulty_hard,
                R.string.difficulty_unbeatable };
        String[] diffNames = { "EASY", "MEDIUM", "HARD", "UNBEATABLE" };
        String[] diffDesc = { "Novice", "Knight", "Grandmaster", "Magnus" };
        int[] diffColors = {
                ContextCompat.getColor(context, R.color.game_green_action),
                ContextCompat.getColor(context, R.color.game_gold_primary),
                ContextCompat.getColor(context, R.color.game_red_danger),
                ContextCompat.getColor(context, R.color.game_neon_cyan)
        };

        // Rank emojis for visual distinction
        String[] rankEmojis = { "🟢", "🟡", "🔴", "⚡" };
        float display = context.getResources().getDisplayMetrics().density;

        for (int i = 0; i < difficulties.length; i++) {
            final String diffName = diffNames[i];
            final int color = diffColors[i];

            // --- Row container ---
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setClickable(true);
            row.setFocusable(true);
            int rowH = (int) (72 * display);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, rowH);
            lp.setMargins(0, 0, 0, (int)(12 * display));
            row.setLayoutParams(lp);
            int pH = (int)(20 * display); int pV = (int)(12 * display);
            row.setPadding(pH, pV, pH, pV);

            // Layered background: dark body + left glow strip via GradientDrawable
            GradientDrawable body = new GradientDrawable();
            body.setShape(GradientDrawable.RECTANGLE);
            body.setCornerRadius(16 * display);
            body.setColor(Color.parseColor("#22" + String.format("%06X", color & 0xFFFFFF)));
            body.setStroke((int)(1 * display), color & 0x99FFFFFF);
            row.setBackground(body);

            // Left color accent bar
            View accentBar = new View(context);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                    (int)(4 * display), ViewGroup.LayoutParams.MATCH_PARENT);
            barLp.setMargins(0, 0, (int)(16 * display), 0);
            accentBar.setLayoutParams(barLp);
            GradientDrawable barBg = new GradientDrawable();
            barBg.setShape(GradientDrawable.RECTANGLE);
            barBg.setCornerRadius(4 * display);
            barBg.setColor(color);
            accentBar.setBackground(barBg);
            row.addView(accentBar);

            // Rank emoji
            TextView emoji = new TextView(context);
            emoji.setText(rankEmojis[i]);
            emoji.setTextSize(20f);
            LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            eLp.setMargins(0, 0, (int)(14 * display), 0);
            emoji.setLayoutParams(eLp);
            row.addView(emoji);

            // Text column
            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // Title
            TextView title = new TextView(context);
            title.setText(difficulties[i]);
            title.setTextColor(color);
            title.setTextSize(15f);
            title.setShadowLayer(16f, 0, 0, color & 0x80FFFFFF);
            try { title.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.cinzel_bold)); }
            catch (Exception e) { title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); }
            textCol.addView(title);

            // Subtitle
            TextView desc = new TextView(context);
            desc.setText(diffDesc[i]);
            desc.setTextColor(ContextCompat.getColor(context, R.color.game_text_muted));
            desc.setTextSize(12f);
            try { desc.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.exo2_regular)); }
            catch (Exception e) { desc.setTypeface(android.graphics.Typeface.DEFAULT); }
            textCol.addView(desc);

            row.addView(textCol);

            // Arrow indicator →
            TextView arrow = new TextView(context);
            arrow.setText("▶");
            arrow.setTextColor(color & 0x80FFFFFF);
            arrow.setTextSize(14f);
            row.addView(arrow);

            // Touch feedback with scale
            row.setOnClickListener(v -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
                        .withEndAction(() -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                            dialog.dismiss();
                            if (callback != null) callback.onSelected(diffName);
                        }).start();
            });

            // Start hidden for stagger
            row.setAlpha(0f);
            row.setTranslationX(120f);
            container.addView(row);
        } // end for-loop

        // Stagger row entries — handlers stored so they can be cancelled on dismiss
        final java.util.List<Handler> staggerHandlers = new java.util.ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            final View rowView = container.getChildAt(i);
            final int idx = i;
            Handler h = new Handler(Looper.getMainLooper());
            staggerHandlers.add(h);
            h.postDelayed(() ->
                    rowView.animate().alpha(1f).translationX(0f)
                            .setDuration(350).setInterpolator(new OvershootInterpolator(1.2f)).start(),
                    80 + idx * 80L);
        }

        Animation enterAnim = AnimationUtils.loadAnimation(context, R.anim.dialog_enter_anim);
        dialogView.startAnimation(enterAnim);

        dialog.show();
        disableDialogClipping(dialog);

        // Cancel stagger handlers if dialog dismissed before they fire
        dialog.setOnDismissListener(d -> {
            for (Handler h : staggerHandlers) h.removeCallbacksAndMessages(null);
        });
    }

    public interface DifficultySelectionCallback {
        void onSelected(String difficulty);
    }

    /**
     * Show Stats Dialog
     */
    public static void showStatsDialog(Context context, String stats) {
        showBasicDialog(context,
                context.getString(R.string.stats_game_title),
                stats,
                context.getString(R.string.close),
                null,
                R.drawable.ic_trophy,
                null,
                null);
    }

    /**
     * Disables clipping on the dialog's decor view hierarchy so that
     * entry scale/bounce animations are not cut off at the window boundary.
     */
    private static void disableDialogClipping(AlertDialog dialog) {
        if (dialog.getWindow() == null) return;
        View decor = dialog.getWindow().getDecorView();
        setNoClip(decor);
        if (decor instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) decor;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                setNoClip(child);
                if (child instanceof ViewGroup) {
                    ViewGroup vg2 = (ViewGroup) child;
                    for (int j = 0; j < vg2.getChildCount(); j++) setNoClip(vg2.getChildAt(j));
                }
            }
        }
    }

    private static void setNoClip(View v) {
        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setClipChildren(false);
            ((ViewGroup) v).setClipToPadding(false);
        }
    }
}
