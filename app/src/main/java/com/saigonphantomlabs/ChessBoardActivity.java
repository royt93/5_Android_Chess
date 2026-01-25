package com.saigonphantomlabs;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.saigonphantomlabs.chess.Chess;
import com.saigonphantomlabs.chess.Chessman;
import com.saigonphantomlabs.chess.R;
import com.saigonphantomlabs.chess.Storage;
import com.saigonphantomlabs.sdkadbmob.UIUtils;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.view.animation.DecelerateInterpolator;
import android.graphics.drawable.ColorDrawable;

public class ChessBoardActivity extends AppCompatActivity {
    public ConstraintLayout backgroundLayout;
    public FrameLayout boardLayout;
    public View blackTurnIndicator;
    public View whiteTurnIndicator;

    // Captured pieces containers
    private LinearLayout capturedBlackPiecesContainer;
    private LinearLayout capturedWhitePiecesContainer;

    // Buttons
    private MaterialButton btnUndo;
    private MaterialButton btnRestart;

    public Chess chess = null;

    public int displayWidth;
    public int displayHeight;
    public int displayMinDimensions;

    public int blackColor;
    public int whiteColor;

    // Animation references for cleanup
    private ObjectAnimator currentBlinkAnimator;

    // Modern Activity Result API for pawn promotion
    private final ActivityResultLauncher<Intent> promotionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Handle promotion result
                chess.promotionResault(Storage.result);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_chess_board);

        // Initialize status bar for dynamic tint changes
        initializeStatusBar();

        // hiding actionbar
        if (this.getSupportActionBar() != null) {
            this.getSupportActionBar().hide();
        }

        // change background
        backgroundLayout = findViewById(R.id.backgroundLayout);

        // initiate black and white colors
        blackColor = ContextCompat.getColor(this, R.color.white);
        whiteColor = ContextCompat.getColor(this, R.color.black);

        // Get display dimensions using modern API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Rect bounds = getWindowManager().getCurrentWindowMetrics().getBounds();
            displayWidth = bounds.width();
            displayHeight = bounds.height();
        } else {
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            displayHeight = displayMetrics.heightPixels;
            displayWidth = displayMetrics.widthPixels;
        }
        displayMinDimensions = Math.min(displayWidth, displayHeight);

        // Initialize views
        boardLayout = findViewById(R.id.boardLayout);
        blackTurnIndicator = findViewById(R.id.blackTurnIndicator);
        whiteTurnIndicator = findViewById(R.id.whiteTurnIndicator);
        capturedBlackPiecesContainer = findViewById(R.id.capturedBlackPiecesContainer);
        capturedWhitePiecesContainer = findViewById(R.id.capturedWhitePiecesContainer);
        btnUndo = findViewById(R.id.btnUndo);
        btnRestart = findViewById(R.id.btnRestart);

        // Setup undo button
        btnUndo.setOnClickListener(v -> {
            if (chess != null && chess.canUndo()) {
                chess.undoLastMove();
            }
        });

        // Setup restart button
        btnRestart.setOnClickListener(v -> showRestartConfirmDialog());

        // Board size is now handled by ConstraintLayout with ratio
        // No need to set fixed dimensions

        if (Storage.chess == null) {
            Storage.chess = chess = new Chess(this, displayMinDimensions, boardLayout);
        } else {
            chess = Storage.chess;
            chess.changeLayout(this, displayMinDimensions, boardLayout);
        }

        findViewById(R.id.boardImage).setOnTouchListener(this::onTouch);

        // Setup modern back navigation
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return false;
        int t = displayMinDimensions / 8;
        chess.onBoardClick(((int) event.getX()) / t, ((int) event.getY()) / t);
        return true;
    }

    public void showPromotionActivity() {
        Intent intent = new Intent(this, PawnPromotionActivity.class);
        promotionLauncher.launch(intent);
        // Slide up animation for promotion dialog style
        overridePendingTransition(R.anim.slide_up, R.anim.slide_out_left);
    }

    private void handleBackPress() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setTitle(getResources().getString(R.string.warning))
                .setMessage(getResources().getString(R.string.saveBoardPrompt))
                .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                })
                .setNegativeButton(getResources().getString(R.string.no), (dialog, i) -> {
                    dialog.dismiss();
                    Storage.chess = null;
                })
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Add custom button colors and styling
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }
    }

    private void showRestartConfirmDialog() {
        new MaterialAlertDialogBuilder(this, R.style.CustomDialogTheme)
                .setTitle(getString(R.string.play_again))
                .setMessage(getString(R.string.restart_confirm))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    dialog.dismiss();
                    if (chess != null) {
                        chess.resetGame();
                    }
                })
                .setNegativeButton(getString(R.string.no), (dialog, i) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    public void animateTurnChange(Chessman.PlayerColor turn) {
        // Update turn indicators
        updateTurnIndicators(turn);

        // Update status bar and navigation bar tint
        updateSystemBarsTint(turn);

        // Background color animation
        ValueAnimator colorAnimation;
        if (turn == Chessman.PlayerColor.White)
            colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), whiteColor, blackColor);
        else
            colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), blackColor, whiteColor);

        colorAnimation.setDuration(300);
        colorAnimation
                .addUpdateListener(animator -> backgroundLayout.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }

    private void updateTurnIndicators(Chessman.PlayerColor turn) {
        if (turn == Chessman.PlayerColor.Black) {
            blackTurnIndicator.setVisibility(View.VISIBLE);
            whiteTurnIndicator.setVisibility(View.GONE);
            startBlinkingAnimation(blackTurnIndicator);
        } else {
            whiteTurnIndicator.setVisibility(View.VISIBLE);
            blackTurnIndicator.setVisibility(View.GONE);
            startBlinkingAnimation(whiteTurnIndicator);
        }
    }

    private void startBlinkingAnimation(View indicator) {
        if (currentBlinkAnimator != null) {
            currentBlinkAnimator.cancel();
        }
        indicator.clearAnimation();

        currentBlinkAnimator = ObjectAnimator.ofFloat(indicator, "alpha", 1f, 0.3f);
        currentBlinkAnimator.setDuration(400);
        currentBlinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        currentBlinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        currentBlinkAnimator.start();
    }

    /**
     * Update undo button visibility
     */
    public void updateUndoButton(boolean visible) {
        if (btnUndo != null) {
            if (visible && btnUndo.getVisibility() != View.VISIBLE) {
                btnUndo.setVisibility(View.VISIBLE);
                btnUndo.setAlpha(0f);
                btnUndo.setScaleX(0.5f);
                btnUndo.setScaleY(0.5f);
                btnUndo.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            } else if (!visible) {
                btnUndo.animate()
                        .alpha(0f)
                        .scaleX(0.5f)
                        .scaleY(0.5f)
                        .setDuration(150)
                        .withEndAction(() -> btnUndo.setVisibility(View.GONE))
                        .start();
            }
        }
    }

    /**
     * Add captured piece to display
     */
    public void addCapturedPiece(Chessman piece) {
        if (piece == null)
            return;

        LinearLayout container = (piece.color == Chessman.PlayerColor.White)
                ? capturedBlackPiecesContainer // White pieces captured by black
                : capturedWhitePiecesContainer; // Black pieces captured by white

        if (container == null)
            return;

        ImageView pieceImage = new ImageView(this);
        int size = (int) (32 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(2, 0, 2, 0);
        pieceImage.setLayoutParams(params);
        pieceImage.setImageResource(getPieceDrawableId(piece));
        pieceImage.setTag(piece); // Store reference for removal

        // Animate in
        pieceImage.setScaleX(0f);
        pieceImage.setScaleY(0f);
        pieceImage.setAlpha(0f);
        container.addView(pieceImage);

        pieceImage.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    /**
     * Remove captured piece from display (for undo)
     */
    public void removeCapturedPiece(Chessman piece) {
        if (piece == null)
            return;

        LinearLayout container = (piece.color == Chessman.PlayerColor.White)
                ? capturedBlackPiecesContainer
                : capturedWhitePiecesContainer;

        if (container == null)
            return;

        for (int i = container.getChildCount() - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            if (child.getTag() == piece) {
                final View viewToRemove = child;
                child.animate()
                        .scaleX(0f)
                        .scaleY(0f)
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> container.removeView(viewToRemove))
                        .start();
                break;
            }
        }
    }

    /**
     * Clear all captured pieces (for game reset)
     */
    public void clearCapturedPieces() {
        if (capturedBlackPiecesContainer != null) {
            capturedBlackPiecesContainer.removeAllViews();
        }
        if (capturedWhitePiecesContainer != null) {
            capturedWhitePiecesContainer.removeAllViews();
        }
    }

    /**
     * Get drawable resource ID for piece
     */
    private int getPieceDrawableId(Chessman piece) {
        boolean isWhite = piece.color == Chessman.PlayerColor.White;
        switch (piece.type) {
            case King:
                return isWhite ? R.drawable.ic_kingw : R.drawable.ic_kingb;
            case Queen:
                return isWhite ? R.drawable.ic_queenw : R.drawable.ic_queenb;
            case Rook:
                return isWhite ? R.drawable.ic_rookw : R.drawable.ic_rookb;
            case Bishop:
                return isWhite ? R.drawable.ic_bishopw : R.drawable.ic_bishopb;
            case Knight:
                return isWhite ? R.drawable.ic_knightw : R.drawable.ic_knightb;
            case Pawn:
                return isWhite ? R.drawable.ic_pawnw : R.drawable.ic_pawnb;
            default:
                return R.drawable.ic_pawnw;
        }
    }

    /**
     * Show custom game end dialog with premium UI and animations
     */
    public void showCustomGameEndDialog(boolean whiteWins, boolean isStalemate) {
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_game_end, null);

        // Find views
        ImageView iconView = dialogView.findViewById(R.id.dialog_icon);
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);

        TextView durationValue = dialogView.findViewById(R.id.stats_duration_value);
        TextView movesValue = dialogView.findViewById(R.id.stats_moves_value);
        TextView whiteCapturedValue = dialogView.findViewById(R.id.stats_white_captured_value);
        TextView blackCapturedValue = dialogView.findViewById(R.id.stats_black_captured_value);

        View statsContainer = dialogView.findViewById(R.id.stats_container);
        View btnExit = dialogView.findViewById(R.id.btn_exit);
        View btnPlayAgain = dialogView.findViewById(R.id.btn_play_again);

        // Set data
        if (isStalemate) {
            titleView.setText(R.string.stalemate);
            messageView.setText(R.string.stalemate_message);
            // Dynamic drawable loading (API 21+)
            iconView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_draw));
        } else {
            titleView.setText(whiteWins ? R.string.white_wins : R.string.black_wins);
            messageView.setText(whiteWins ? R.string.black_loses : R.string.white_loses);
            iconView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_trophy));
        }

        // Set stats
        durationValue.setText(chess.getFormattedDuration());
        movesValue.setText(String.valueOf(chess.getMoveCount()));
        whiteCapturedValue.setText(String.valueOf(chess.getCapturedWhiteCount()));
        blackCapturedValue.setText(String.valueOf(chess.getCapturedBlackCount()));

        // Create Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();

        // Set transparent background to let custom shape show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Play enter animation on content
        Animation enterAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_enter_anim);
        dialogView.startAnimation(enterAnim);

        // Add stagger animation for stats
        statsContainer.setTranslationY(100f);
        statsContainer.setAlpha(0f);
        statsContainer.animate()
                .translationY(0f)
                .alpha(1f)
                .setStartDelay(200)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Setup actions
        btnPlayAgain.setOnClickListener(v -> {
            dialog.dismiss();
            chess.resetGame();
        });

        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            Storage.chess = null;
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        dialog.show();
    }

    /**
     * Build game statistics string
     */
    private String buildGameStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(getString(R.string.stats_title)).append("\n\n");
        sb.append("⏱ ").append(getString(R.string.stats_duration)).append(": ").append(chess.getFormattedDuration())
                .append("\n");
        sb.append("🎯 ").append(getString(R.string.stats_moves)).append(": ").append(chess.getMoveCount()).append("\n");
        sb.append("⚪ ").append(getString(R.string.stats_white_captured)).append(": ")
                .append(chess.getCapturedWhiteCount()).append("\n");
        sb.append("⚫ ").append(getString(R.string.stats_black_captured)).append(": ")
                .append(chess.getCapturedBlackCount());
        return sb.toString();
    }

    private void initializeStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setNavigationBarColor(Color.TRANSPARENT);
            }

            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }

            decorView.setSystemUiVisibility(flags);
        }
    }

    private void updateSystemBarsTint(Chessman.PlayerColor turn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();

            if (turn == Chessman.PlayerColor.White) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }

            decorView.setSystemUiVisibility(flags);
        }
    }
}