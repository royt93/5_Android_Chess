package com.saigonphantomlabs;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.saigonphantomlabs.chess.Chessman;
import com.saigonphantomlabs.chess.R;
import com.saigonphantomlabs.chess.Storage;

public class PawnPromotionActivity extends AppCompatActivity {

    // Card views (2x2 grid)
    private MaterialCardView cardQueen, cardRook, cardBishop, cardKnight;

    // Breathing handlers — cancelled in onDestroy
    private Handler cardBreathingHandler;
    private Handler selectionAnimHandler;

    // Breathing animators for each card — cancelled in onDestroy
    private ObjectAnimator breathQueenAnim;
    private ObjectAnimator breathRookAnim;
    private ObjectAnimator breathBishopAnim;
    private ObjectAnimator breathKnightAnim;

    // Corner glow animators
    private ObjectAnimator cornerTLAnim, cornerBRAnim, cornerTRAnim, cornerBLAnim;

    // Title flicker
    private ObjectAnimator titleFlickerAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.saigonphantomlabs.sdkadbmob.UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_pawn_promotion);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Load GIF background
        ImageView ivBkg = findViewById(R.id.ivBkg);
        if (ivBkg != null) {
            Glide.with(this).asGif().load(R.drawable.ic_bkg_1).into(ivBkg);
        }

        // Get piece color from intent (default WHITE)
        boolean isWhite = getIntent().getBooleanExtra("IS_WHITE", true);

        // Find card views
        cardQueen  = findViewById(R.id.cardQueen);
        cardRook   = findViewById(R.id.cardRook);
        cardBishop = findViewById(R.id.cardBishop);
        cardKnight = findViewById(R.id.cardKnight);

        // Set piece images
        setPieceImages(isWhite);

        // Setup click listeners
        setupCardClick(cardQueen,  Chessman.ChessmanType.Queen);
        setupCardClick(cardRook,   Chessman.ChessmanType.Rook);
        setupCardClick(cardBishop, Chessman.ChessmanType.Bishop);
        setupCardClick(cardKnight, Chessman.ChessmanType.Knight);

        // Corner glows
        startCornerGlowPulse(
            findViewById(R.id.cornerGlowTL),
            findViewById(R.id.cornerGlowBR),
            findViewById(R.id.cornerGlowTR),
            findViewById(R.id.cornerGlowBL)
        );

        // Title flicker
        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            titleFlickerAnim = ObjectAnimator.ofFloat(tvTitle, "alpha", 1f, 0.7f, 1f, 0.85f, 1f);
            titleFlickerAnim.setDuration(4000);
            titleFlickerAnim.setRepeatCount(ValueAnimator.INFINITE);
            titleFlickerAnim.start();
        }

        // Entry animation: cards slide up from below, staggered
        animateEntry();
    }

    private void setPieceImages(boolean isWhite) {
        ImageView ivQ = findViewById(R.id.ivQueen);
        ImageView ivR = findViewById(R.id.ivRook);
        ImageView ivB = findViewById(R.id.ivBishop);
        ImageView ivN = findViewById(R.id.ivKnight);

        if (ivQ != null) ivQ.setImageResource(isWhite ? R.drawable.ic_queenw  : R.drawable.ic_queenb);
        if (ivR != null) ivR.setImageResource(isWhite ? R.drawable.ic_rookw   : R.drawable.ic_rookb);
        if (ivB != null) ivB.setImageResource(isWhite ? R.drawable.ic_bishopw : R.drawable.ic_bishopb);
        if (ivN != null) ivN.setImageResource(isWhite ? R.drawable.ic_knightw : R.drawable.ic_knightb);
    }

    private void setupCardClick(MaterialCardView card, Chessman.ChessmanType type) {
        if (card == null) return;
        card.setOnClickListener(v -> selectWithAnimation(card, type));
    }

    /** Entry: cards fly up from bottom, staggered */
    private void animateEntry() {
        MaterialCardView[] cards = {cardQueen, cardRook, cardBishop, cardKnight};
        long[] delays = {0L, 60L, 120L, 180L};

        for (int i = 0; i < cards.length; i++) {
            MaterialCardView card = cards[i];
            if (card == null) continue;
            card.setAlpha(0f);
            card.setTranslationY(80f);

            final long delay = delays[i];
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .start();
        }

        // Start subtle breathing after entry completes
        cardBreathingHandler = new Handler(Looper.getMainLooper());
        cardBreathingHandler.postDelayed(this::startCardBreathing, 700);
    }

    /** Breathing: alpha pulse only — NO scale (no clipping) */
    private void startCardBreathing() {
        if (isFinishing()) return;
        long[] delays = {0L, 300L, 600L, 900L};
        MaterialCardView[] cards = {cardQueen, cardRook, cardBishop, cardKnight};
        ObjectAnimator[] anims = {null, null, null, null};

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) continue;
            ObjectAnimator a = ObjectAnimator.ofFloat(cards[i], "alpha", 1.0f, 0.75f);
            a.setDuration(1200);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.setStartDelay(delays[i]);
            a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            a.start();
            anims[i] = a;
        }
        breathQueenAnim  = anims[0];
        breathRookAnim   = anims[1];
        breathBishopAnim = anims[2];
        breathKnightAnim = anims[3];
    }

    private void stopCardBreathing() {
        cancelAnim(breathQueenAnim);  breathQueenAnim  = null;
        cancelAnim(breathRookAnim);   breathRookAnim   = null;
        cancelAnim(breathBishopAnim); breathBishopAnim = null;
        cancelAnim(breathKnightAnim); breathKnightAnim = null;

        // Reset alpha
        for (MaterialCardView c : new MaterialCardView[]{cardQueen, cardRook, cardBishop, cardKnight}) {
            if (c != null) c.setAlpha(1f);
        }
    }

    private void startCornerGlowPulse(View tl, View br, View tr, View bl) {
        cornerTLAnim = buildAlphaPulse(tl, 0.15f, 0.45f, 2400, 0L);
        cornerBRAnim = buildAlphaPulse(br, 0.12f, 0.38f, 2800, 400L);
        cornerTRAnim = buildAlphaPulse(tr, 0.10f, 0.30f, 2200, 700L);
        cornerBLAnim = buildAlphaPulse(bl, 0.10f, 0.28f, 3000, 900L);
        if (cornerTLAnim != null) cornerTLAnim.start();
        if (cornerBRAnim != null) cornerBRAnim.start();
        if (cornerTRAnim != null) cornerTRAnim.start();
        if (cornerBLAnim != null) cornerBLAnim.start();
    }

    private ObjectAnimator buildAlphaPulse(View v, float from, float to, long duration, long delay) {
        if (v == null) return null;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "alpha", from, to);
        a.setDuration(duration);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setStartDelay(delay);
        return a;
    }

    /**
     * Selection: squeeze (scale down slightly) + flash + zoom out.
     * Scale never goes above 1.0 — no clipping.
     */
    private void selectWithAnimation(MaterialCardView card, Chessman.ChessmanType type) {
        stopCardBreathing();

        // Highlight selected card, dim others
        for (MaterialCardView c : new MaterialCardView[]{cardQueen, cardRook, cardBishop, cardKnight}) {
            if (c == null) continue;
            if (c == card) {
                c.animate().alpha(1f).setDuration(100).start();
            } else {
                c.animate().alpha(0.3f).scaleX(0.92f).scaleY(0.92f).setDuration(200).start();
            }
        }

        // Squeeze + flash on selected card
        card.animate().scaleX(0.90f).scaleY(0.90f).setDuration(80)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    // Flash
                    ObjectAnimator flash = ObjectAnimator.ofFloat(card, "alpha", 1f, 0.15f, 1f);
                    flash.setDuration(200);
                    flash.start();
                    // Spring back
                    card.animate().scaleX(1f).scaleY(1f).setDuration(150)
                            .setInterpolator(new OvershootInterpolator(1.5f)).start();
                }).start();

        // Exit after animation completes
        selectionAnimHandler = new Handler(Looper.getMainLooper());
        selectionAnimHandler.postDelayed(() -> {
            card.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(280)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> select(type)).start();
        }, 420);
    }

    private void select(Chessman.ChessmanType newType) {
        Storage.result = newType;
        setResult(Activity.RESULT_OK, new Intent());
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    private void cancelAnim(ObjectAnimator a) { if (a != null) a.cancel(); }

    @Override
    protected void onDestroy() {
        // Cancel all infinite animators — prevent memory leaks
        cancelAnim(breathQueenAnim);  breathQueenAnim  = null;
        cancelAnim(breathRookAnim);   breathRookAnim   = null;
        cancelAnim(breathBishopAnim); breathBishopAnim = null;
        cancelAnim(breathKnightAnim); breathKnightAnim = null;
        cancelAnim(titleFlickerAnim); titleFlickerAnim = null;
        cancelAnim(cornerTLAnim);     cornerTLAnim = null;
        cancelAnim(cornerBRAnim);     cornerBRAnim = null;
        cancelAnim(cornerTRAnim);     cornerTRAnim = null;
        cancelAnim(cornerBLAnim);     cornerBLAnim = null;

        if (cardBreathingHandler != null) {
            cardBreathingHandler.removeCallbacksAndMessages(null);
            cardBreathingHandler = null;
        }
        if (selectionAnimHandler != null) {
            selectionAnimHandler.removeCallbacksAndMessages(null);
            selectionAnimHandler = null;
        }

        // Clear card references
        cardQueen = null; cardRook = null; cardBishop = null; cardKnight = null;

        super.onDestroy();
    }
}
