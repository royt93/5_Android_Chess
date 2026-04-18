package com.saigonphantomlabs;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.saigonphantomlabs.chess.BuildConfig;
import com.saigonphantomlabs.chess.R;
import com.roy.sdkadbmob.UIUtils;

@SuppressLint("CustomSplashScreen")
public class MainActivity extends AppCompatActivity {

    private LinearLayout btnPlayPvP;
    private LinearLayout btnPlayPvE;
    private LinearLayout btnStats;
    private LinearLayout btnRateApp;
    private LinearLayout btnMoreApps;
    private LinearLayout btnShareApp;
    private LinearLayout btnRules;
    private TextView tvVersion;

    // Infinite animators — all cancelled in onDestroy
    private AnimatorSet heartbeatAnim;
    private ObjectAnimator flickerAnim;
    private AnimatorSet shimmerPvPAnim;
    private AnimatorSet shimmerPvEAnim;
    private ObjectAnimator logoRing1Anim;
    private ObjectAnimator logoRing2Anim;
    private ObjectAnimator cornerTLAnim;
    private ObjectAnimator cornerBRAnim;
    private ObjectAnimator cornerTRAnim;
    private ObjectAnimator bottomBtnPulse1;
    private ObjectAnimator bottomBtnPulse2;
    private ObjectAnimator bottomBtnPulse3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_main);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.rootLayout), true, true);
        setupViews();
        animateEntryViews();
    }

    private void setupViews() {
        if (this.getSupportActionBar() != null) {
            this.getSupportActionBar().hide();
        }

        btnPlayPvP = findViewById(R.id.btnPlayPvP);
        btnPlayPvE = findViewById(R.id.btnPlayPvE);
        btnStats = findViewById(R.id.btnStats);
        btnRateApp = findViewById(R.id.btnRateApp);
        btnMoreApps = findViewById(R.id.btnMoreApps);
        btnShareApp = findViewById(R.id.btnShareApp);
        btnRules = findViewById(R.id.btnRules);
        ImageView ivBkg = findViewById(R.id.ivBkg);
        tvVersion = findViewById(R.id.tvVersionTop);

        String versionName = BuildConfig.VERSION_NAME;
        tvVersion.setText(getString(R.string.version_format, versionName));

        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_bkg_1)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivBkg);

        btnPlayPvP.setOnClickListener(new SafeClickListener() {
            @Override public void onSafeClick(View view) { startGame(false, null); }
        });
        btnPlayPvE.setOnClickListener(new SafeClickListener() {
            @Override public void onSafeClick(View view) { showDifficultySelectionDialog(); }
        });
        btnStats.setOnClickListener(new SafeClickListener() {
            @Override public void onSafeClick(View view) { showStatsDialog(); }
        });
        btnRateApp.setOnClickListener(new SafeClickListener() {
            @Override public void onSafeClick(View view) { rateApp(); }
        });
        btnMoreApps.setOnClickListener(new SafeClickListener() {
            @Override public void onSafeClick(View view) { openMoreApps(); }
        });
        btnShareApp.setOnClickListener(new SafeClickListener() {
            @Override public void onSafeClick(View view) { shareApp(); }
        });
        btnRules.setOnClickListener(new SafeClickListener() {
            @Override public void onSafeClick(View view) { DialogUtils.showRulesDialog(MainActivity.this); }
        });
    }

    private void animateEntryViews() {
        ImageView ivMainLogo = findViewById(R.id.ivMainLogo);
        TextView tvMainTitle = findViewById(R.id.tvMainTitle);
        View shimmerPvP = findViewById(R.id.shimmerPvP);
        View shimmerPvE = findViewById(R.id.shimmerPvE);
        View ring1 = findViewById(R.id.logoRing1);
        View ring2 = findViewById(R.id.logoRing2);
        View cornerTL = findViewById(R.id.cornerGlowTL);
        View cornerBR = findViewById(R.id.cornerGlowBR);
        View cornerTR = findViewById(R.id.cornerGlowTR);

        // Initial state for entry animation
        View[] topViews = {tvVersion};
        View[] mainViews = {btnPlayPvP, btnPlayPvE, btnStats, btnRules};
        View[] bottomViews = {btnRateApp, btnMoreApps, btnShareApp};

        for (View v : topViews) { v.setAlpha(0f); v.setTranslationY(-50f); }
        for (View v : mainViews) { v.setAlpha(0f); v.setScaleX(0.1f); v.setScaleY(0.1f); v.setTranslationX(200f); }
        for (View v : bottomViews) { v.setAlpha(0f); v.setTranslationY(150f); }
        if (ivMainLogo != null) { ivMainLogo.setAlpha(0f); ivMainLogo.setScaleX(0f); ivMainLogo.setScaleY(0f); }
        if (tvMainTitle != null) { tvMainTitle.setAlpha(0f); tvMainTitle.setTranslationY(30f); }

        // --- Entry animations (one-shot, ViewPropertyAnimator auto-managed) ---

        // Logo bouncy pop
        if (ivMainLogo != null) {
            ivMainLogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setStartDelay(100).setDuration(600)
                    .setInterpolator(new OvershootInterpolator(2f)).start();
        }
        // Title slide up
        if (tvMainTitle != null) {
            tvMainTitle.animate().alpha(1f).translationY(0f)
                    .setStartDelay(300).setDuration(450)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
        // Top info
        for (int i = 0; i < topViews.length; i++) {
            topViews[i].animate().alpha(1f).translationY(0f)
                    .setStartDelay(200 + i * 100L).setDuration(400)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
        // Main buttons
        for (int i = 0; i < mainViews.length; i++) {
            mainViews[i].animate().alpha(1f).scaleX(1f).scaleY(1f).translationX(0f)
                    .setStartDelay(150 + i * 120L).setDuration(600)
                    .setInterpolator(new OvershootInterpolator(2.5f)).start();
        }
        // Bottom buttons
        for (int i = 0; i < bottomViews.length; i++) {
            bottomViews[i].animate().alpha(1f).translationY(0f)
                    .setStartDelay(650 + i * 80L).setDuration(400)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }

        // --- Continuous animations (stored references, cancelled in onDestroy) ---

        // Logo breathing: scale DOWN slightly (never above 1.0 → no clipping)
        if (ivMainLogo != null) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivMainLogo, "scaleX", 1f, 0.92f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivMainLogo, "scaleY", 1f, 0.92f);
            ObjectAnimator rot = ObjectAnimator.ofFloat(ivMainLogo, "rotation", -3f, 3f);
            for (ObjectAnimator a : new ObjectAnimator[]{scaleX, scaleY, rot}) {
                a.setDuration(1600); a.setRepeatCount(ValueAnimator.INFINITE); a.setRepeatMode(ValueAnimator.REVERSE);
            }
            heartbeatAnim = new AnimatorSet();
            heartbeatAnim.playTogether(scaleX, scaleY, rot);
            heartbeatAnim.setStartDelay(700);
            heartbeatAnim.start();
        }

        // Title neon flicker
        if (tvMainTitle != null) {
            flickerAnim = ObjectAnimator.ofFloat(tvMainTitle, "alpha", 1f, 0.75f, 1f, 0.9f, 1f);
            flickerAnim.setDuration(4500);
            flickerAnim.setRepeatCount(ValueAnimator.INFINITE);
            flickerAnim.setStartDelay(500);
            flickerAnim.start();
        }

        // Shimmer sweep on PvP button
        // Shimmer: measure actual button width after layout, then sweep full width
        View shimmerPvPFinal = shimmerPvP;
        View shimmerPvEFinal = shimmerPvE;
        View btnPvP = findViewById(R.id.btnPlayPvP);
        if (btnPvP != null) {
            btnPvP.getViewTreeObserver().addOnGlobalLayoutListener(
                    new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override public void onGlobalLayout() {
                            btnPvP.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            float w = btnPvP.getWidth();
                            if (w > 0) {
                                shimmerPvPAnim = buildShimmerAnim(shimmerPvPFinal, w, 2200, 0L);
                                shimmerPvEAnim = buildShimmerAnim(shimmerPvEFinal, w, 2200, 800L);
                                if (shimmerPvPAnim != null) shimmerPvPAnim.start();
                                if (shimmerPvEAnim != null) shimmerPvEAnim.start();
                            }
                        }
                    });
        } else {
            // Fallback if view not found
            shimmerPvPAnim = buildShimmerAnim(shimmerPvP, 500f, 2200, 0L);
            shimmerPvEAnim = buildShimmerAnim(shimmerPvE, 500f, 2200, 800L);
            if (shimmerPvPAnim != null) shimmerPvPAnim.start();
            if (shimmerPvEAnim != null) shimmerPvEAnim.start();
        }

        // Logo rings breathing
        logoRing1Anim = buildRingBreath(ring1, 1.1f, 1800, 0L);
        logoRing2Anim = buildRingBreath(ring2, 1.08f, 2400, 300L);
        if (logoRing1Anim != null) logoRing1Anim.start();
        if (logoRing2Anim != null) logoRing2Anim.start();

        // Corner glows pulse
        cornerTLAnim = buildAlphaPulse(cornerTL, 0.4f, 0.9f, 2800, 0L);
        cornerBRAnim = buildAlphaPulse(cornerBR, 0.3f, 0.8f, 3200, 500L);
        cornerTRAnim = buildAlphaPulse(cornerTR, 0.2f, 0.6f, 2500, 900L);
        if (cornerTLAnim != null) cornerTLAnim.start();
        if (cornerBRAnim != null) cornerBRAnim.start();
        if (cornerTRAnim != null) cornerTRAnim.start();

        // Bottom buttons alpha pulse (NOT scale — avoids clipping)
        bottomBtnPulse1 = buildScalePulse(btnRateApp,  1.0f, 0.92f, 1800, 0L);
        bottomBtnPulse2 = buildScalePulse(btnMoreApps, 1.0f, 0.92f, 1800, 300L);
        bottomBtnPulse3 = buildScalePulse(btnShareApp, 1.0f, 0.92f, 1800, 600L);
        if (bottomBtnPulse1 != null) bottomBtnPulse1.start();
        if (bottomBtnPulse2 != null) bottomBtnPulse2.start();
        if (bottomBtnPulse3 != null) bottomBtnPulse3.start();
    }

    // Shimmer sweep: translate from -width to +width for full button sweep
    private AnimatorSet buildShimmerAnim(View shimmerView, float width, long duration, long delay) {
        if (shimmerView == null) return null;
        ObjectAnimator translateX = ObjectAnimator.ofFloat(shimmerView, "translationX", -width, width);
        translateX.setDuration(duration);
        translateX.setRepeatCount(ValueAnimator.INFINITE);
        translateX.setRepeatMode(ValueAnimator.RESTART);
        translateX.setStartDelay(delay);
        translateX.setInterpolator(new DecelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.play(translateX);
        return set;
    }

    // Alpha pulse for a view (e.g. rings, corner glows)
    private ObjectAnimator buildAlphaPulse(View view, float from, float to, long duration, long delay) {
        if (view == null) return null;
        ObjectAnimator a = ObjectAnimator.ofFloat(view, "alpha", from, to);
        a.setDuration(duration);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setStartDelay(delay);
        return a;
    }

    // Scale pulse (breathe) for small icon buttons
    private ObjectAnimator buildScalePulse(View view, float from, float to, long duration, long delay) {
        if (view == null) return null;
        android.animation.PropertyValuesHolder sx = android.animation.PropertyValuesHolder.ofFloat("scaleX", from, to);
        android.animation.PropertyValuesHolder sy = android.animation.PropertyValuesHolder.ofFloat("scaleY", from, to);
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(view, sx, sy);
        a.setDuration(duration);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setStartDelay(delay);
        return a;
    }

    // Ring alpha breath
    private ObjectAnimator buildRingBreath(View view, float to, long duration, long delay) {
        if (view == null) return null;
        ObjectAnimator a = ObjectAnimator.ofFloat(view, "alpha", 0.5f, to);
        a.setDuration(duration);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setStartDelay(delay);
        return a;
    }

    private void rateApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.market_details_url, getPackageName())));
            intent.setPackage("com.android.vending");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.play_store_details_url, getPackageName())));
            startActivity(intent);
        }
    }

    private void openMoreApps() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.market_dev_url)));
            intent.setPackage("com.android.vending");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.play_store_dev_url)));
            startActivity(intent);
        }
    }

    private void startGame(boolean isVsAi, String difficulty) {
        Intent intent = new Intent(MainActivity.this, ChessBoardActivity.class);
        intent.putExtra("IS_VS_AI", isVsAi);
        if (difficulty != null) intent.putExtra("AI_DIFFICULTY", difficulty);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showDifficultySelectionDialog() {
        DialogUtils.showDifficultyDialog(this, difficulty -> startGame(true, difficulty));
    }

    private void showStatsDialog() {
        com.saigonphantomlabs.chess.GameStatsManager statsManager =
                new com.saigonphantomlabs.chess.GameStatsManager(this);
        DialogUtils.showStatsDialog(this, statsManager.getStatsSummary());
    }

    private void shareApp() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
            String shareBody = getString(R.string.share_message) + "\n" +
                    getString(R.string.play_store_details_url, getPackageName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        // Cancel ALL infinite animators — no memory leaks
        cancelAnimator(heartbeatAnim);
        cancelAnimator(flickerAnim);
        cancelAnimator(shimmerPvPAnim);
        cancelAnimator(shimmerPvEAnim);
        cancelAnimator(logoRing1Anim);
        cancelAnimator(logoRing2Anim);
        cancelAnimator(cornerTLAnim);
        cancelAnimator(cornerBRAnim);
        cancelAnimator(cornerTRAnim);
        cancelAnimator(bottomBtnPulse1);
        cancelAnimator(bottomBtnPulse2);
        cancelAnimator(bottomBtnPulse3);
        heartbeatAnim = null; flickerAnim = null;
        shimmerPvPAnim = null; shimmerPvEAnim = null;
        logoRing1Anim = null; logoRing2Anim = null;
        cornerTLAnim = null; cornerBRAnim = null; cornerTRAnim = null;
        bottomBtnPulse1 = null; bottomBtnPulse2 = null; bottomBtnPulse3 = null;
        super.onDestroy();
    }

    private void cancelAnimator(Object animator) {
        if (animator instanceof AnimatorSet) ((AnimatorSet) animator).cancel();
        else if (animator instanceof ObjectAnimator) ((ObjectAnimator) animator).cancel();
    }
}