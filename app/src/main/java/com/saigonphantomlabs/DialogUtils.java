package com.saigonphantomlabs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.saigonphantomlabs.chess.R;

public class DialogUtils {

    /**
     * Show a basic Glass Dialog with Title, Message, and Two Buttons
     */
    public static void showBasicDialog(Context context,
            String title,
            String message,
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
        Button btnPositive = dialogView.findViewById(R.id.btn_positive);
        Button btnNegative = dialogView.findViewById(R.id.btn_negative);

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
            btnNegative.setText(negativeText);
            btnNegative.setOnClickListener(v -> {
                dialog.dismiss();
                if (onNegative != null)
                    onNegative.run();
            });
        } else {
            btnNegative.setVisibility(View.GONE);
            // If no negative button, make positive button centered or full width?
            // Current layout uses weight 1 for both. If one gone, the other expands.
        }

        // Animation
        Animation enterAnim = AnimationUtils.loadAnimation(context, R.anim.dialog_enter_anim);
        dialogView.startAnimation(enterAnim);

        dialog.show();
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
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_glass_generic, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        titleView.setText(R.string.select_difficulty);

        dialogView.findViewById(R.id.dialog_message).setVisibility(View.GONE);
        dialogView.findViewById(R.id.dialog_icon).setVisibility(View.GONE);
        dialogView.findViewById(R.id.btn_positive).setVisibility(View.GONE); // Hide OK/Cancel
        Button btnMsgNegative = dialogView.findViewById(R.id.btn_negative);
        btnMsgNegative.setText("Cancel");
        btnMsgNegative.setOnClickListener(v -> dialog.dismiss());

        FrameLayout container = dialogView.findViewById(R.id.dialog_content_container);
        container.setVisibility(View.VISIBLE);

        // Create Layout for buttons
        LinearLayout buttonsLayout = new LinearLayout(context);
        buttonsLayout.setOrientation(LinearLayout.VERTICAL);

        String[] difficulties = { "EASY", "MEDIUM", "HARD", "UNBEATABLE" };
        int[] colors = {
                0xFF4CAF50, // Green
                0xFFFF9800, // Orange
                0xFFF44336, // Red
                0xFF9C27B0 // Purple
        };

        for (int i = 0; i < difficulties.length; i++) {
            final String diff = difficulties[i];
            Button btn = new com.google.android.material.button.MaterialButton(context);
            btn.setText(diff);
            btn.setBackgroundColor(colors[i]);
            btn.setTextColor(Color.WHITE);
            // btn.setCornerRadius(20) - MaterialButton specific
            // Let's rely on default style or cast properly if needed but code construction
            // is verbose
            // Simpler: use styles.
            // For now, just functional buttons.

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> {
                dialog.dismiss();
                if (callback != null)
                    callback.onSelected(diff);
            });
            buttonsLayout.addView(btn);
        }

        container.addView(buttonsLayout);

        Animation enterAnim = AnimationUtils.loadAnimation(context, R.anim.dialog_enter_anim);
        dialogView.startAnimation(enterAnim);

        dialog.show();
    }

    public interface DifficultySelectionCallback {
        void onSelected(String difficulty);
    }

    /**
     * Show Stats Dialog
     */
    public static void showStatsDialog(Context context, String stats) {
        showBasicDialog(context,
                "🏆 Game Statistics",
                stats,
                "Close",
                null,
                R.drawable.ic_trophy,
                null,
                null);
    }
}
