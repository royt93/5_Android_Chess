package com.saigonphantomlabs;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.card.MaterialCardView;
import com.saigonphantomlabs.chess.Chessman;
import com.saigonphantomlabs.chess.R;
import com.saigonphantomlabs.chess.Storage;
import com.saigonphantomlabs.sdkadbmob.UIUtils;

public class PawnPromotionActivity extends AppCompatActivity {

    private MaterialCardView cardQueen, cardRook, cardBishop, cardKnight;
    private TextView tvTitle;
    private boolean isSelecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_pawn_promotion);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.rootLayout),
                false,
                true);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        ImageView ivBkg = findViewById(R.id.ivBkg);
        tvTitle = findViewById(R.id.tvTitle);
        cardQueen = findViewById(R.id.cardQueen);
        cardRook = findViewById(R.id.cardRook);
        cardBishop = findViewById(R.id.cardBishop);
        cardKnight = findViewById(R.id.cardKnight);

        // Load background
        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_bkg_1)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivBkg);

        // Set click listeners directly on cards
        setupCardClick(cardQueen, Chessman.ChessmanType.Queen);
        setupCardClick(cardRook, Chessman.ChessmanType.Rook);
        setupCardClick(cardBishop, Chessman.ChessmanType.Bishop);
        setupCardClick(cardKnight, Chessman.ChessmanType.Knight);

        // Animate entry
        animateEntry();

        // Disable back navigation - user must select a piece
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing - disable back button
            }
        });
    }

    private void setupCardClick(MaterialCardView card, Chessman.ChessmanType type) {
        card.setOnClickListener(v -> {
            if (!isSelecting) {
                isSelecting = true;
                selectWithAnimation(card, type);
            }
        });
    }

    private void animateEntry() {
        // Hide views initially
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(-50f);

        MaterialCardView[] cards = { cardQueen, cardRook, cardBishop, cardKnight };
        for (MaterialCardView card : cards) {
            card.setAlpha(0f);
            card.setScaleX(0f);
            card.setScaleY(0f);
        }

        // Animate title
        tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(100)
                .setDuration(400)
                .start();

        // Animate cards with stagger
        for (int i = 0; i < cards.length; i++) {
            MaterialCardView card = cards[i];
            card.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(200 + i * 80L)
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }

    private void selectWithAnimation(View view, Chessman.ChessmanType type) {
        // Animate selected card
        view.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(120)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(0.9f)
                            .scaleY(0.9f)
                            .alpha(0.5f)
                            .setDuration(100)
                            .withEndAction(() -> select(type))
                            .start();
                })
                .start();
    }

    // Keep for XML onClick fallback (not used anymore)
    public void selectNewType(View view) {
        // Not used - click handled by card listeners
    }

    private void select(Chessman.ChessmanType newType) {
        Storage.result = newType;
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
        // Slide down animation when dismissing
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_down);
    }
}
