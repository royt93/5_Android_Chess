package com.saigonphantomlabs;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.saigonphantomlabs.BaseActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.saigonphantomlabs.chess.R;
import com.roy.sdkadbmob.AdManager;
import com.roy.sdkadbmob.UIUtils;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

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
        // Downsample GIF nền (RGB_565 + override kích thước gốc) → giảm RAM decode ~5x
        // (GIF nền ở alpha thấp, không cần full-res; chống áp lực heap/OOM).
        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_bkg_1)
                .apply(new com.bumptech.glide.request.RequestOptions()
                        .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
                        .override(360, 556))
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

        // UMP consent (GDPR/EEA) PHẢI resolved trước khi load App Open/Banner/Interstitial.
        // Lib tự lo personalized vs non-personalized theo IAB TCF; Non-EEA → skip dialog.
        // Sau khi consent xong (accept hoặc deny) mới chạy splash warmup → navigate.
        //
        // ⚠️ MEMORY LEAK FIX: dùng static-nested callback + WeakReference thay anonymous inner class.
        // Lib giữ callback `initSplashScreen` trong static `splashTimeoutRunnable` và KHÔNG clear →
        // anonymous inner class (ngầm giữ SplashActivity.this) làm leak cả Activity (~918KB/lần mở app).
        // Static nested + WeakReference cắt chuỗi: lib giữ static → chỉ còn WeakRef → Activity GC được.
        AdManager.INSTANCE.requestConsentInfoUpdate(this, false, new ConsentCallback(this));
    }

    private void runSplashAfterConsent() {
        if (isFinishing() || isNavigating) return;
        AdManager.INSTANCE.initSplashScreen(this, new SplashNavCallback(this));
    }

    /** Callback consent — static + WeakReference để không leak SplashActivity qua static của lib. */
    private static final class ConsentCallback implements Function1<Boolean, Unit> {
        private final java.lang.ref.WeakReference<SplashActivity> ref;
        ConsentCallback(SplashActivity a) { this.ref = new java.lang.ref.WeakReference<>(a); }
        @Override public Unit invoke(Boolean canRequestAds) {
            SplashActivity a = ref.get();
            if (a != null && !a.isFinishing()) a.runSplashAfterConsent();
            return Unit.INSTANCE;
        }
    }

    /** Callback splash→navigate — static + WeakReference (lib giữ qua splashTimeoutRunnable static). */
    private static final class SplashNavCallback implements Function0<Unit> {
        private final java.lang.ref.WeakReference<SplashActivity> ref;
        SplashNavCallback(SplashActivity a) { this.ref = new java.lang.ref.WeakReference<>(a); }
        @Override public Unit invoke() {
            SplashActivity a = ref.get();
            if (a != null && !a.isFinishing() && !a.isDestroyed()) {
                a.runOnUiThread(a::navigateToMainActivity);
            }
            return Unit.INSTANCE;
        }
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
