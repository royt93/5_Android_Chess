package com.saigonphantomlabs;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import com.google.android.gms.ads.AdView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.saigonphantomlabs.chess.BuildConfig;
import com.saigonphantomlabs.chess.Chess;
import com.saigonphantomlabs.chess.Chessman;
import com.saigonphantomlabs.chess.R;
import com.saigonphantomlabs.chess.Storage;
import com.saigonphantomlabs.sdkadbmob.AdMobManager;
import com.saigonphantomlabs.sdkadbmob.UIUtils;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.AdError;

import kotlin.Unit;

public class ChessBoardActivity extends AppCompatActivity implements AdMobManager.InterstitialAdListener {
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

    private AdView adView;

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
        UIUtils.INSTANCE.setupEdgeToEdge2(backgroundLayout, true, true);

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
            if (chess != null && chess.canUndo() && !chess.isAiThinking) {
                chess.undoLastMove();
            }
        });

        // Setup restart button
        // Setup restart button
        btnRestart.setOnClickListener(v -> {
            if (chess != null && !chess.isAiThinking) {
                showRestartConfirmDialog();
            }
        });

        // Board size is now handled by ConstraintLayout with ratio
        // No need to set fixed dimensions

        // Setup modern back navigation
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });

        AdMobManager.INSTANCE.setCurrentActivity(this);
        AdMobManager.INSTANCE.setInterstitialListener(this);

        // AdMob Banner
        adView = AdMobManager.INSTANCE.loadBanner(this,
                BuildConfig.ADMOB_BANNER_ID,
                findViewById(R.id.banner_container),
                findViewById(R.id.tvLabelAd),
                com.google.android.gms.ads.AdSize.BANNER);

        // AdMob Interstitial Load
        AdMobManager.INSTANCE.loadInterstitial(this, BuildConfig.ADMOB_INTERSTITIAL_ID);

        // Wait for board to be laid out to get exact dimensions
        boardLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        boardLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (isFinishing())
                            return;

                        int boardSize = boardLayout.getWidth();
                        if (boardSize <= 0)
                            return;

                        if (Storage.chess == null) {
                            Storage.chess = chess = new Chess(ChessBoardActivity.this, boardSize, boardLayout);

                            // Configure AI Mode from Intent
                            boolean isVsAi = getIntent().getBooleanExtra("IS_VS_AI", false);
                            chess.isVsComputer = isVsAi;
                            if (isVsAi) {
                                String difficultyStr = getIntent().getStringExtra("AI_DIFFICULTY");
                                if (difficultyStr != null) {
                                    try {
                                        chess.difficultyLevel = com.saigonphantomlabs.chess.AIEngine.Difficulty
                                                .valueOf(difficultyStr);
                                    } catch (IllegalArgumentException e) {
                                        chess.difficultyLevel = com.saigonphantomlabs.chess.AIEngine.Difficulty.EASY;
                                    }
                                }
                            }
                        } else {
                            chess = Storage.chess;
                            chess.changeLayout(ChessBoardActivity.this, boardSize, boardLayout);
                        }

                        findViewById(R.id.boardImage).setOnTouchListener(ChessBoardActivity.this::onTouch);
                    }
                });
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return false;
        int t = v.getWidth() / 8;
        if (t <= 0)
            return false;
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
        DialogUtils.showQuitDialog(this, () -> {
            Storage.chess = null;
            AdMobManager.INSTANCE.showInterstitial(ChessBoardActivity.this, result -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return Unit.INSTANCE;
            });
        });
    }

    private void showRestartConfirmDialog() {
        DialogUtils.showRestartDialog(this, () -> {
            if (chess != null) {
                chess.resetGame();
            }
        });
    }

    public void animateTurnChange(Chessman.PlayerColor turn) {
        // Update turn indicators
        updateTurnIndicators(turn);

        // Update status bar and navigation bar tint
        updateSystemBarsTint();
    }

    private void updateTurnIndicators(Chessman.PlayerColor turn) {
        if (turn == Chessman.PlayerColor.Black) {
            blackTurnIndicator.setVisibility(View.VISIBLE);
            whiteTurnIndicator.setVisibility(View.INVISIBLE);
            startBlinkingAnimation(blackTurnIndicator);
        } else {
            whiteTurnIndicator.setVisibility(View.VISIBLE);
            blackTurnIndicator.setVisibility(View.INVISIBLE);
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
    /**
     * Update undo button visibility (Delegates to generic method)
     */
    public void updateUndoButton(boolean visible) {
        updateGameButtons(visible, visible);
    }

    /**
     * Update game buttons visibility independently
     */
    public void updateGameButtons(boolean showUndo, boolean showRestart) {
        // Update Undo Button
        if (btnUndo != null) {
            if (showUndo && btnUndo.getVisibility() != View.VISIBLE) {
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
            } else if (!showUndo && btnUndo.getVisibility() == View.VISIBLE) {
                btnUndo.animate()
                        .alpha(0f)
                        .scaleX(0.5f)
                        .scaleY(0.5f)
                        .setDuration(150)
                        .withEndAction(() -> btnUndo.setVisibility(View.INVISIBLE))
                        .start();
            }
        }

        // Update Restart Button
        if (btnRestart != null) {
            if (showRestart && btnRestart.getVisibility() != View.VISIBLE) {
                btnRestart.setVisibility(View.VISIBLE);
                btnRestart.setAlpha(0f);
                btnRestart.setScaleX(0.5f);
                btnRestart.setScaleY(0.5f);
                btnRestart.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            } else if (!showRestart && btnRestart.getVisibility() == View.VISIBLE) {
                btnRestart.animate()
                        .alpha(0f)
                        .scaleX(0.5f)
                        .scaleY(0.5f)
                        .setDuration(150)
                        .withEndAction(() -> btnRestart.setVisibility(View.INVISIBLE))
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
        View btnReview = dialogView.findViewById(R.id.btn_review);
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
        btnReview.setOnClickListener(v -> {
            dialog.dismiss();
            // Hide undo button when reviewing board, but keep Play Again visible
            updateGameButtons(false, true);
        });

        btnPlayAgain.setOnClickListener(v -> {
            dialog.dismiss();
            chess.resetGame();
        });

        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            Storage.chess = null;
            AdMobManager.INSTANCE.showInterstitial(ChessBoardActivity.this, result -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return Unit.INSTANCE;
            });
        });

        dialog.show();
    }

    /**
     * Build game statistics string
     */
    private String buildGameStats() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.stats_divider));
        sb.append(getString(R.string.stats_title)).append("\n\n");
        sb.append(getString(R.string.stats_duration_icon))
                .append(getString(R.string.stats_duration)).append(": ").append(chess.getFormattedDuration())
                .append("\n");
        sb.append(getString(R.string.stats_moves_icon))
                .append(getString(R.string.stats_moves)).append(": ").append(chess.getMoveCount()).append("\n");
        sb.append(getString(R.string.stats_white_icon))
                .append(getString(R.string.stats_white_captured)).append(": ")
                .append(chess.getCapturedWhiteCount()).append("\n");
        sb.append(getString(R.string.stats_black_icon))
                .append(getString(R.string.stats_black_captured)).append(": ")
                .append(chess.getCapturedBlackCount());

        if (chess.isVsComputer) {
            sb.append("\n\n").append(getString(R.string.stats_divider));
            sb.append(getString(R.string.ai_performance_format, chess.difficultyLevel.toString()));
            // Fetch stats from manager
            com.saigonphantomlabs.chess.GameStatsManager sm = new com.saigonphantomlabs.chess.GameStatsManager(this);
            sb.append(sm.getStatsSummary());
        }

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

    private void updateSystemBarsTint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();

            // Always use dark mode (light text, dark background) for the Game UI
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }

            decorView.setSystemUiVisibility(flags);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // [BUG-02] Cancel the infinite blink animator to prevent View/Context leak
        if (currentBlinkAnimator != null) {
            currentBlinkAnimator.cancel();
            currentBlinkAnimator = null;
        }
        // [ML-04] Stop AI handler callbacks before destroying
        if (chess != null) {
            chess.cancelAiHandler();
        }
        // [BUG-02] Clear interstitial listener so AdMobManager doesn't hold Activity reference
        AdMobManager.INSTANCE.setInterstitialListener(null);
        // [ML-02] Safety: null out static reference to break Activity leak chain
        if (Storage.chess == chess) {
            Storage.chess = null;
        }
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    // AdMob InterstitialAdListener Implementation
    @Override
    public void onAdLoaded() {
    }

    @Override
    public void onAdFailedToLoad(LoadAdError error) {
    }

    @Override
    public void onAdShowed() {
    }

    @Override
    public void onAdDismissed() {
        // Reload for next time
        AdMobManager.INSTANCE.loadInterstitial(this, BuildConfig.ADMOB_INTERSTITIAL_ID);
    }

    @Override
    public void onAdClicked() {
    }

    @Override
    public void onAdFailedToShow(AdError error) {
    }

    @Override
    public void onAdNotAvailable() {
    }
}