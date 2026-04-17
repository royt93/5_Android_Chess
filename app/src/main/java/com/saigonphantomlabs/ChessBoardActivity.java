package com.saigonphantomlabs;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.animation.OvershootInterpolator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import com.google.android.gms.ads.AdView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import androidx.appcompat.widget.AppCompatButton;
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

    // Core views
    public FrameLayout boardLayout;
    public View backgroundLayout;
    public View blackTurnIndicator;
    public View whiteTurnIndicator;
    public View whiteTurnGlow;
    public View blackTurnGlow;

    private LinearLayout capturedBlackPiecesContainer;
    private LinearLayout capturedWhitePiecesContainer;
    private AppCompatButton btnUndo;
    private AppCompatButton btnRestart;

    public Chess chess = null;
    public int displayWidth;
    public int displayHeight;
    public int displayMinDimensions;
    public int blackColor;
    public int whiteColor;

    // Infinite animator references — ALL cancelled in onDestroy
    private ObjectAnimator currentBlinkAnimator;
    private ObjectAnimator boardPulseAnim;
    private ObjectAnimator cornerTRAnim;
    private ObjectAnimator cornerBLAnim;
    // Turn glow ValueAnimator (for custom intensity control)
    private ValueAnimator whiteGlowAnim;
    private ValueAnimator blackGlowAnim;

    private AdView adView;

    private final ActivityResultLauncher<Intent> promotionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (chess != null) chess.promotionResault(Storage.result);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_chess_board);
        initializeStatusBar();
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        backgroundLayout = findViewById(R.id.backgroundLayout);
        UIUtils.INSTANCE.setupEdgeToEdge2(backgroundLayout, true, true);

        blackColor = ContextCompat.getColor(this, R.color.white);
        whiteColor = ContextCompat.getColor(this, R.color.black);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Rect bounds = getWindowManager().getCurrentWindowMetrics().getBounds();
            displayWidth = bounds.width();
            displayHeight = bounds.height();
        } else {
            android.util.DisplayMetrics m = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(m);
            displayHeight = m.heightPixels;
            displayWidth = m.widthPixels;
        }
        displayMinDimensions = Math.min(displayWidth, displayHeight);

        boardLayout = findViewById(R.id.boardLayout);
        blackTurnIndicator = findViewById(R.id.blackTurnIndicator);
        whiteTurnIndicator = findViewById(R.id.whiteTurnIndicator);
        whiteTurnGlow = findViewById(R.id.whiteTurnGlow);
        blackTurnGlow = findViewById(R.id.blackTurnGlow);
        capturedBlackPiecesContainer = findViewById(R.id.capturedBlackPiecesContainer);
        capturedWhitePiecesContainer = findViewById(R.id.capturedWhitePiecesContainer);
        btnUndo = findViewById(R.id.btnUndo);
        btnRestart = findViewById(R.id.btnRestart);

        btnUndo.setOnClickListener(v -> {
            if (chess != null && chess.canUndo() && !chess.isAiThinking) chess.undoLastMove();
        });
        btnRestart.setOnClickListener(v -> {
            if (chess != null && !chess.isAiThinking) showRestartConfirmDialog();
        });

        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() { handleBackPress(); }
                });

        startAmbientAnimations();

        AdMobManager.INSTANCE.setCurrentActivity(this);
        AdMobManager.INSTANCE.setInterstitialListener(this);
        adView = AdMobManager.INSTANCE.loadBanner(this,
                BuildConfig.ADMOB_BANNER_ID,
                findViewById(R.id.banner_container),
                findViewById(R.id.tvLabelAd),
                com.google.android.gms.ads.AdSize.BANNER);
        AdMobManager.INSTANCE.loadInterstitial(this, BuildConfig.ADMOB_INTERSTITIAL_ID);

        // Board is made square by ConstraintLayout dimensionRatio="1:1" in XML.
        // Wait for layout to complete, then init Chess with the actual square board size.
        boardLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        boardLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (isFinishing()) return;

                        int boardSize = boardLayout.getWidth(); // == height (square guaranteed by XML)
                        if (boardSize <= 0) return;

                        if (Storage.chess == null) {
                            Storage.chess = chess = new Chess(ChessBoardActivity.this,
                                    boardSize, boardLayout);
                            boolean isVsAi = getIntent().getBooleanExtra("IS_VS_AI", false);
                            chess.isVsComputer = isVsAi;
                            if (isVsAi) {
                                String diffStr = getIntent().getStringExtra("AI_DIFFICULTY");
                                if (diffStr != null) {
                                    try {
                                        chess.difficultyLevel = com.saigonphantomlabs.chess.AIEngine.Difficulty.valueOf(diffStr);
                                    } catch (IllegalArgumentException ex) {
                                        chess.difficultyLevel = com.saigonphantomlabs.chess.AIEngine.Difficulty.EASY;
                                    }
                                }
                            }
                        } else {
                            chess = Storage.chess;
                            chess.changeLayout(ChessBoardActivity.this, boardSize, boardLayout);
                        }
                        // Register touch handler
                        findViewById(R.id.boardImage).setOnTouchListener(ChessBoardActivity.this::onTouch);

                        // Hero: board flies in
                        boardLayout.setAlpha(0f);
                        boardLayout.setScaleX(0.75f);
                        boardLayout.setScaleY(0.75f);
                        boardLayout.animate().alpha(1f).scaleX(1f).scaleY(1f)
                                .setDuration(650).setInterpolator(new OvershootInterpolator(1.2f)).start();
                    }
                });
    }

    private void startAmbientAnimations() {
        // Board alive feel: subtle alpha breathe (NO scale — board is exact square, must not overflow)
        if (boardLayout != null) {
            boardPulseAnim = ObjectAnimator.ofFloat(boardLayout, "alpha", 1.0f, 0.96f);
            boardPulseAnim.setDuration(3500);
            boardPulseAnim.setRepeatCount(ValueAnimator.INFINITE);
            boardPulseAnim.setRepeatMode(ValueAnimator.REVERSE);
            boardPulseAnim.setInterpolator(new DecelerateInterpolator());
            boardPulseAnim.start();
        }

        View cornerTR = findViewById(R.id.cornerGlowTR);
        if (cornerTR != null) {
            cornerTRAnim = ObjectAnimator.ofFloat(cornerTR, "alpha", 0.2f, 0.5f);
            cornerTRAnim.setDuration(2800);
            cornerTRAnim.setRepeatCount(ValueAnimator.INFINITE);
            cornerTRAnim.setRepeatMode(ValueAnimator.REVERSE);
            cornerTRAnim.start();
        }
        View cornerBL = findViewById(R.id.cornerGlowBL);
        if (cornerBL != null) {
            cornerBLAnim = ObjectAnimator.ofFloat(cornerBL, "alpha", 0.15f, 0.45f);
            cornerBLAnim.setDuration(3400);
            cornerBLAnim.setRepeatCount(ValueAnimator.INFINITE);
            cornerBLAnim.setRepeatMode(ValueAnimator.REVERSE);
            cornerBLAnim.start();
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
        int t = v.getWidth() / 8;
        if (t <= 0) return false;
        chess.onBoardClick(((int) event.getX()) / t, ((int) event.getY()) / t);
        return true;
    }

    public void showPromotionActivity() {
        Intent intent = new Intent(this, PawnPromotionActivity.class);
        promotionLauncher.launch(intent);
        overridePendingTransition(R.anim.slide_up, R.anim.slide_out_left);
    }

    private void handleBackPress() {
        DialogUtils.showQuitDialog(this, () -> {
            Storage.chess = null;
            AdMobManager.INSTANCE.showInterstitial(this, result -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return Unit.INSTANCE;
            });
        });
    }

    private void showRestartConfirmDialog() {
        DialogUtils.showRestartDialog(this, () -> { if (chess != null) chess.resetGame(); });
    }

    /**
     * Enhanced turn change:
     * - Bright, high-alpha glow overlay (0.0 → 0.92) with slow oscillation
     * - Turn indicators pop in with overshoot
     * - System bars tinted
     */
    public void animateTurnChange(Chessman.PlayerColor turn) {
        updateTurnIndicators(turn);

        // Cancel previous glow animators before starting new ones
        if (whiteGlowAnim != null) { whiteGlowAnim.cancel(); whiteGlowAnim = null; }
        if (blackGlowAnim != null) { blackGlowAnim.cancel(); blackGlowAnim = null; }

        if (turn == Chessman.PlayerColor.White) {
            // White gets strong gold glow that breathes
            whiteTurnGlow.animate().alpha(0.90f).setDuration(600).start();
            blackTurnGlow.animate().alpha(0f).setDuration(400).start();
            // Breathing oscillation: 0.90 → 0.60 → 0.90 …
            whiteGlowAnim = ValueAnimator.ofFloat(0.90f, 0.55f);
            whiteGlowAnim.setDuration(2000);
            whiteGlowAnim.setRepeatCount(ValueAnimator.INFINITE);
            whiteGlowAnim.setRepeatMode(ValueAnimator.REVERSE);
            whiteGlowAnim.addUpdateListener(a -> {
                if (whiteTurnGlow != null) whiteTurnGlow.setAlpha((float) a.getAnimatedValue());
            });
            whiteGlowAnim.setStartDelay(650);
            whiteGlowAnim.start();
        } else {
            // Black gets strong cyan glow that breathes
            blackTurnGlow.animate().alpha(0.90f).setDuration(600).start();
            whiteTurnGlow.animate().alpha(0f).setDuration(400).start();
            blackGlowAnim = ValueAnimator.ofFloat(0.90f, 0.55f);
            blackGlowAnim.setDuration(2000);
            blackGlowAnim.setRepeatCount(ValueAnimator.INFINITE);
            blackGlowAnim.setRepeatMode(ValueAnimator.REVERSE);
            blackGlowAnim.addUpdateListener(a -> {
                if (blackTurnGlow != null) blackTurnGlow.setAlpha((float) a.getAnimatedValue());
            });
            blackGlowAnim.setStartDelay(650);
            blackGlowAnim.start();
        }

        updateSystemBarsTint();
    }

    private void updateTurnIndicators(Chessman.PlayerColor turn) {
        // Cancel dot blink before reassigning
        if (currentBlinkAnimator != null) {
            currentBlinkAnimator.cancel();
            currentBlinkAnimator = null;
        }
        // Reset dot states
        resetDot(findViewById(R.id.whiteTurnDot));
        resetDot(findViewById(R.id.blackTurnDot));

        if (turn == Chessman.PlayerColor.Black) {
            animatePopIn(blackTurnIndicator);
            if (whiteTurnIndicator != null) whiteTurnIndicator.setVisibility(View.INVISIBLE);
            startBreathingDot(findViewById(R.id.blackTurnDot));
        } else {
            animatePopIn(whiteTurnIndicator);
            if (blackTurnIndicator != null) blackTurnIndicator.setVisibility(View.INVISIBLE);
            startBreathingDot(findViewById(R.id.whiteTurnDot));
        }
    }

    private void resetDot(View dot) {
        if (dot == null) return;
        dot.setScaleX(1f); dot.setScaleY(1f); dot.setAlpha(1f);
    }

    private void animatePopIn(View indicator) {
        if (indicator == null) return;
        indicator.setVisibility(View.VISIBLE);
        indicator.setAlpha(0f);
        indicator.setScaleX(0.4f);
        indicator.setScaleY(0.4f);
        indicator.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(450).setInterpolator(new OvershootInterpolator(1.8f)).start();
    }

    private void startBreathingDot(View dotView) {
        if (dotView == null) return;
        // Use alpha pulse ONLY — no scale, so dot never overflows its own bounds
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0.15f);
        currentBlinkAnimator = ObjectAnimator.ofPropertyValuesHolder(dotView, alpha);
        currentBlinkAnimator.setDuration(550);
        currentBlinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        currentBlinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        currentBlinkAnimator.start();
    }

    public void updateUndoButton(boolean visible) {
        updateGameButtons(visible, visible);
    }

    public void updateGameButtons(boolean showUndo, boolean showRestart) {
        animateButton(btnUndo, showUndo);
        animateButton(btnRestart, showRestart);
    }

    private void animateButton(AppCompatButton btn, boolean show) {
        if (btn == null) return;
        if (show && btn.getVisibility() != View.VISIBLE) {
            btn.setVisibility(View.VISIBLE);
            btn.setAlpha(0f); btn.setScaleX(0.5f); btn.setScaleY(0.5f);
            btn.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300)
                    .setInterpolator(new OvershootInterpolator()).start();
        } else if (!show && btn.getVisibility() == View.VISIBLE) {
            btn.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(150)
                    .withEndAction(() -> btn.setVisibility(View.INVISIBLE)).start();
        }
    }

    public void addCapturedPiece(Chessman piece) {
        if (piece == null) return;
        LinearLayout container = (piece.color == Chessman.PlayerColor.White)
                ? capturedBlackPiecesContainer : capturedWhitePiecesContainer;
        if (container == null) return;

        ImageView img = new ImageView(this);
        int sz = (int) (30 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
        lp.setMargins(2, 0, 2, 0);
        img.setLayoutParams(lp);
        img.setImageResource(getPieceDrawableId(piece));
        img.setTag(piece);
        img.setScaleX(0f); img.setScaleY(0f); img.setAlpha(0f);
        container.addView(img);
        img.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300)
                .setInterpolator(new OvershootInterpolator()).start();
    }

    public void removeCapturedPiece(Chessman piece) {
        if (piece == null) return;
        LinearLayout container = (piece.color == Chessman.PlayerColor.White)
                ? capturedBlackPiecesContainer : capturedWhitePiecesContainer;
        if (container == null) return;
        for (int i = container.getChildCount() - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            if (child.getTag() == piece) {
                View v = child;
                child.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200)
                        .withEndAction(() -> container.removeView(v)).start();
                break;
            }
        }
    }

    public void clearCapturedPieces() {
        if (capturedBlackPiecesContainer != null) capturedBlackPiecesContainer.removeAllViews();
        if (capturedWhitePiecesContainer != null) capturedWhitePiecesContainer.removeAllViews();
    }

    private int getPieceDrawableId(Chessman piece) {
        boolean w = piece.color == Chessman.PlayerColor.White;
        switch (piece.type) {
            case King:   return w ? R.drawable.ic_kingw   : R.drawable.ic_kingb;
            case Queen:  return w ? R.drawable.ic_queenw  : R.drawable.ic_queenb;
            case Rook:   return w ? R.drawable.ic_rookw   : R.drawable.ic_rookb;
            case Bishop: return w ? R.drawable.ic_bishopw : R.drawable.ic_bishopb;
            case Knight: return w ? R.drawable.ic_knightw : R.drawable.ic_knightb;
            case Pawn:   return w ? R.drawable.ic_pawnw   : R.drawable.ic_pawnb;
            default:     return R.drawable.ic_pawnw;
        }
    }

    public void showCustomGameEndDialog(boolean whiteWins, boolean isStalemate) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_game_end, null);

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

        if (isStalemate) {
            titleView.setText(R.string.stalemate);
            messageView.setText(R.string.stalemate_message);
            iconView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_draw));
        } else {
            titleView.setText(whiteWins ? R.string.white_wins : R.string.black_wins);
            messageView.setText(whiteWins ? R.string.black_loses : R.string.white_loses);
            iconView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_trophy));
        }

        if (chess != null) {
            if (durationValue != null) durationValue.setText(chess.getFormattedDuration());
            if (movesValue != null) movesValue.setText(String.valueOf(chess.getMoveCount()));
            if (whiteCapturedValue != null) whiteCapturedValue.setText(String.valueOf(chess.getCapturedWhiteCount()));
            if (blackCapturedValue != null) blackCapturedValue.setText(String.valueOf(chess.getCapturedBlackCount()));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Disable clipping so dialog entry scale animation isn't cut at edges
            View decorView = dialog.getWindow().getDecorView();
            if (decorView instanceof ViewGroup) {
                ((ViewGroup) decorView).setClipChildren(false);
                ((ViewGroup) decorView).setClipToPadding(false);
            }
        }

        // Entry animation + staggered stats
        Animation enterAnim = AnimationUtils.loadAnimation(this, R.anim.dialog_enter_anim);
        dialogView.startAnimation(enterAnim);

        if (statsContainer != null) {
            statsContainer.setTranslationY(80f);
            statsContainer.setAlpha(0f);
            statsContainer.animate().translationY(0f).alpha(1f)
                    .setStartDelay(220).setDuration(450)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }

        if (btnReview != null) btnReview.setOnClickListener(v -> {
            dialog.dismiss();
            updateGameButtons(false, true);
        });
        if (btnPlayAgain != null) btnPlayAgain.setOnClickListener(v -> {
            dialog.dismiss();
            chess.resetGame();
        });
        if (btnExit != null) btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            Storage.chess = null;
            AdMobManager.INSTANCE.showInterstitial(this, result -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return Unit.INSTANCE;
            });
        });
        dialog.show();
    }

    private void initializeStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                w.setNavigationBarColor(Color.TRANSPARENT);
            View dv = w.getDecorView();
            int flags = dv.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            dv.setSystemUiVisibility(flags);
        }
    }

    private void updateSystemBarsTint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View dv = getWindow().getDecorView();
            int flags = dv.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            dv.setSystemUiVisibility(flags);
        }
    }

    @Override protected void onResume() { super.onResume(); if (adView != null) adView.resume(); }
    @Override protected void onPause() { if (adView != null) adView.pause(); super.onPause(); }

    @Override
    protected void onDestroy() {
        // Cancel all infinite animators — no leaks
        cancelAnim(currentBlinkAnimator); currentBlinkAnimator = null;
        cancelAnim(boardPulseAnim);       boardPulseAnim = null;
        cancelAnim(cornerTRAnim);         cornerTRAnim = null;
        cancelAnim(cornerBLAnim);         cornerBLAnim = null;
        if (whiteGlowAnim != null) { whiteGlowAnim.cancel(); whiteGlowAnim = null; }
        if (blackGlowAnim != null) { blackGlowAnim.cancel(); blackGlowAnim = null; }

        if (chess != null) chess.cancelAiHandler();
        AdMobManager.INSTANCE.setInterstitialListener(null);
        if (Storage.chess == chess) Storage.chess = null;
        if (adView != null) adView.destroy();
        super.onDestroy();
    }

    private void cancelAnim(ObjectAnimator a) { if (a != null) a.cancel(); }

    @Override public void onAdLoaded() {}
    @Override public void onAdFailedToLoad(LoadAdError e) {}
    @Override public void onAdShowed() {}
    @Override public void onAdDismissed() {
        AdMobManager.INSTANCE.loadInterstitial(this, BuildConfig.ADMOB_INTERSTITIAL_ID);
    }
    @Override public void onAdClicked() {}
    @Override public void onAdFailedToShow(AdError e) {}
    @Override public void onAdNotAvailable() {}
}