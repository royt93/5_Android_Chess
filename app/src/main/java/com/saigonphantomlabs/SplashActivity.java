package com.saigonphantomlabs;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.saigonphantomlabs.chess.R;
import com.roy.sdkadbmob.AdManager;
import com.roy.sdkadbmob.UIUtils;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DURATION = 1000L;
    private long splashStartTime;
    private boolean isNavigating = false;

    // Handler for minimum duration delay
    private android.os.Handler splashHandler;

    // Infinite animator references — all cancelled in onDestroy
    private AnimatorSet logoAnim;
    private ObjectAnimator flickerAnim;
    private AnimatorSet ring1Anim;
    private AnimatorSet ring2Anim;
    private AnimatorSet ring3Anim;
    private ObjectAnimator cornerGlowTLAnim;
    private ObjectAnimator cornerGlowBRAnim;
    private ObjectAnimator progressPulseAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        splashStartTime = System.currentTimeMillis();
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_splash);

        View rootLayout = findViewById(R.id.rootLayout);
        UIUtils.INSTANCE.setupEdgeToEdge2(rootLayout, true, true);

        // Load background GIF
        ImageView ivBkg = findViewById(R.id.ivBkg);
        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_bkg_1)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivBkg);

        ImageView ivLogo = findViewById(R.id.ivLogo);
        android.widget.TextView tvTitle = findViewById(R.id.tvTitle);
        View ring1 = findViewById(R.id.ring1);
        View ring2 = findViewById(R.id.ring2);
        View ring3 = findViewById(R.id.ring3);
        View cornerTL = findViewById(R.id.cornerGlowTL);
        View cornerBR = findViewById(R.id.cornerGlowBR);
        View progressBar = findViewById(R.id.progressBar);

        startLogoAnimation(ivLogo);
        startTitleFlicker(tvTitle);
        startRingPulse(ring1, ring2, ring3);
        startCornerGlowPulse(cornerTL, cornerBR);
        startProgressPulse(progressBar);

        AdManager.INSTANCE.initSplashScreen(this, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                runOnUiThread(SplashActivity.this::navigateToMainActivity);
                return Unit.INSTANCE;
            }
        });
    }

    // Logo breathing: scale DOWN only (never above 1.0 — no clipping)
    private void startLogoAnimation(ImageView ivLogo) {
        if (ivLogo == null) return;
        ObjectAnimator scaleX  = ObjectAnimator.ofFloat(ivLogo, "scaleX", 1.0f, 0.90f);
        ObjectAnimator scaleY  = ObjectAnimator.ofFloat(ivLogo, "scaleY", 1.0f, 0.90f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(ivLogo, "rotation", -4f, 4f);
        for (ObjectAnimator a : new ObjectAnimator[]{scaleX, scaleY, rotation}) {
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.setDuration(1400);
        }
        logoAnim = new AnimatorSet();
        logoAnim.playTogether(scaleX, scaleY, rotation);
        logoAnim.start();
    }

    // Neon flicker for title
    private void startTitleFlicker(android.widget.TextView tvTitle) {
        if (tvTitle == null) return;
        flickerAnim = ObjectAnimator.ofFloat(tvTitle, "alpha", 1.0f, 0.65f, 1.0f, 0.88f, 1.0f);
        flickerAnim.setDuration(3500);
        flickerAnim.setRepeatCount(ValueAnimator.INFINITE);
        flickerAnim.start();
    }

    // Concentric pulsing rings (halo ripple effect)
    private void startRingPulse(View ring1, View ring2, View ring3) {
        ring1Anim = buildRingAnimator(ring1, 0L);
        ring2Anim = buildRingAnimator(ring2, 400L);
        ring3Anim = buildRingAnimator(ring3, 800L);
        if (ring1Anim != null) ring1Anim.start();
        if (ring2Anim != null) ring2Anim.start();
        if (ring3Anim != null) ring3Anim.start();
    }

    /**
     * Ring ripple effect: translationY UP + alpha fade (like sonar rings rising).
     * NO scale — rings never overflow their layout bounds.
     */
    private AnimatorSet buildRingAnimator(View ring, long startDelay) {
        if (ring == null) return null;
        // Rise upward 40dp while fading out
        ObjectAnimator transY = ObjectAnimator.ofFloat(ring, "translationY", 0f, -40f);
        ObjectAnimator alpha  = ObjectAnimator.ofFloat(ring, "alpha", 0.8f, 0f);
        // Scale gently from 0.85 to 1.0 (never above 1.0)
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 0.85f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 0.85f, 1.0f);
        for (ObjectAnimator a : new ObjectAnimator[]{transY, alpha, scaleX, scaleY}) {
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.RESTART);
            a.setDuration(2000);
            a.setStartDelay(startDelay);
            a.setInterpolator(new android.view.animation.DecelerateInterpolator());
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(transY, alpha, scaleX, scaleY);
        return set;
    }

    // Corner glows gentle alpha pulse
    private void startCornerGlowPulse(View cornerTL, View cornerBR) {
        if (cornerTL != null) {
            cornerGlowTLAnim = ObjectAnimator.ofFloat(cornerTL, "alpha", 0.4f, 0.8f);
            cornerGlowTLAnim.setDuration(2500);
            cornerGlowTLAnim.setRepeatCount(ValueAnimator.INFINITE);
            cornerGlowTLAnim.setRepeatMode(ValueAnimator.REVERSE);
            cornerGlowTLAnim.start();
        }
        if (cornerBR != null) {
            cornerGlowBRAnim = ObjectAnimator.ofFloat(cornerBR, "alpha", 0.3f, 0.7f);
            cornerGlowBRAnim.setDuration(3000);
            cornerGlowBRAnim.setRepeatCount(ValueAnimator.INFINITE);
            cornerGlowBRAnim.setRepeatMode(ValueAnimator.REVERSE);
            cornerGlowBRAnim.start();
        }
    }

    // ProgressBar pulse alpha
    private void startProgressPulse(View progressBar) {
        if (progressBar == null) return;
        progressPulseAnim = ObjectAnimator.ofFloat(progressBar, "alpha", 0.5f, 1.0f);
        progressPulseAnim.setDuration(800);
        progressPulseAnim.setRepeatCount(ValueAnimator.INFINITE);
        progressPulseAnim.setRepeatMode(ValueAnimator.REVERSE);
        progressPulseAnim.start();
    }

    private void navigateToMainActivity() {
        if (isNavigating || isFinishing()) return;
        isNavigating = true;
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // Prevent back during splash
    }

    @Override
    protected void onDestroy() {
        // Cancel all infinite animators to prevent context/view leaks
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
            splashHandler = null;
        }
        cancelAll(logoAnim, flickerAnim, ring1Anim, ring2Anim, ring3Anim,
                  cornerGlowTLAnim, cornerGlowBRAnim, progressPulseAnim);
        logoAnim = null; flickerAnim = null;
        ring1Anim = null; ring2Anim = null; ring3Anim = null;
        cornerGlowTLAnim = null; cornerGlowBRAnim = null;
        progressPulseAnim = null;

        super.onDestroy();
    }

    private void cancelAll(Object... animators) {
        for (Object a : animators) {
            if (a instanceof AnimatorSet) ((AnimatorSet) a).cancel();
            else if (a instanceof ObjectAnimator) ((ObjectAnimator) a).cancel();
        }
    }
}
