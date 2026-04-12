package com.saigonphantomlabs;

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
import com.saigonphantomlabs.sdkadbmob.UIUtils;

//TODO roy93~ admob
//TODO roy93~ ad applovin
//TODO roy93~ review in app
//TODO roy93~ font scale
//TODO roy93~ 120hz
//TODO roy93~ rate, more app, share app
//TODO roy93~ github
//TODO roy93~ license

//done
//rename app
//leak canary
//keystore
//sdk 35 edge to edge

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
    
    // UI Animators
    private android.animation.AnimatorSet heartbeatAnim;
    private android.animation.ObjectAnimator flickerAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_main);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.rootLayout),
                true,
                true);
        setupViews();
        animateEntryViews();
    }

    private void setupViews() {
        // hiding actionbar
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

        // Play PvP button click
        btnPlayPvP.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View view) {
                startGame(false, null);
            }
        });

        // Play PvE button click
        btnPlayPvE.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View view) {
                showDifficultySelectionDialog();
            }
        });

        // Stats button click
        btnStats.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View view) {
                showStatsDialog();
            }
        });

        // Rate App button click with safe click and animation
        btnRateApp.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View view) {
                rateApp();
            }
        });

        // More Apps button click with safe click and animation
        btnMoreApps.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View view) {
                openMoreApps();
            }
        });

        // Share App button click with safe click and animation
        btnShareApp.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View view) {
                shareApp();
            }
        });

        // Rules button click
        btnRules.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View view) {
                DialogUtils.showRulesDialog(MainActivity.this);
            }
        });
    }

    private void rateApp() {
        try {
            // Try to open Play Store app
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.market_details_url, getPackageName())));
            intent.setPackage("com.android.vending");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // If Play Store app is not available, open in browser
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.play_store_details_url, getPackageName())));
            startActivity(intent);
        }
    }

    private void openMoreApps() {
        try {
            // Try to open Play Store app with developer page
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.market_dev_url)));
            intent.setPackage("com.android.vending");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // If Play Store app is not available, open in browser
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getString(R.string.play_store_dev_url)));
            startActivity(intent);
        }
    }

    private void startGame(boolean isVsAi, String difficulty) {
        Intent intent = new Intent(MainActivity.this, ChessBoardActivity.class);
        intent.putExtra("IS_VS_AI", isVsAi);
        if (difficulty != null) {
            intent.putExtra("AI_DIFFICULTY", difficulty);
        }
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void showDifficultySelectionDialog() {
        DialogUtils.showDifficultyDialog(this, difficulty -> {
            startGame(true, difficulty);
        });
    }

    private void showStatsDialog() {
        com.saigonphantomlabs.chess.GameStatsManager statsManager = new com.saigonphantomlabs.chess.GameStatsManager(
                this);
        String stats = statsManager.getStatsSummary();
        DialogUtils.showStatsDialog(this, stats);
    }

    private void shareApp() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));

            String appPackageName = getPackageName();
            String shareBody = getString(R.string.share_message) + "\n" +
                    getString(R.string.play_store_details_url, appPackageName);

            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Animate entry of all interactive views with cascading effect
     */
    private void animateEntryViews() {
        // App Logo
        ImageView ivMainLogo = findViewById(R.id.ivMainLogo);
        TextView tvMainTitle = findViewById(R.id.tvMainTitle);

        // Array of views to animate with their delays
        View[] topViews = { tvVersion };

        View[] mainViews = { btnPlayPvP, btnPlayPvE, btnStats, btnRules };
        View[] bottomViews = { btnRateApp, btnMoreApps, btnShareApp };

        // Initially hide all views
        for (View v : topViews) {
            v.setAlpha(0f);
            v.setTranslationY(-50f);
        }
        for (View v : mainViews) {
            v.setAlpha(0f);
            v.setScaleX(0.1f);
            v.setScaleY(0.1f);
            v.setTranslationX(200f);
        }
        for (View v : bottomViews) {
            v.setAlpha(0f);
            v.setTranslationY(150f);
        }

        // Animate top views - fade in from top
        for (int i = 0; i < topViews.length; i++) {
            View v = topViews[i];
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(200 + i * 100L)
                    .setDuration(400)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Extremely dramatic bounce slide for main buttons
        for (int i = 0; i < mainViews.length; i++) {
            View v = mainViews[i];
            v.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationX(0f)
                    .setStartDelay(100 + i * 120L)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator(2.5f)) // very bouncy
                    .start();
        }

        // Animate bottom buttons - cascading slide up
        for (int i = 0; i < bottomViews.length; i++) {
            View v = bottomViews[i];
            v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(600 + i * 80L)
                    .setDuration(400)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Add continuous heartbeat glow to main logo
        if (ivMainLogo != null) {
            android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(ivMainLogo, "scaleX", 1f, 1.15f);
            android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(ivMainLogo, "scaleY", 1f, 1.15f);
            scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleX.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            scaleX.setDuration(1500);
            scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleY.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            scaleY.setDuration(1500);
            heartbeatAnim = new android.animation.AnimatorSet();
            heartbeatAnim.playTogether(scaleX, scaleY);
            heartbeatAnim.start();
        }

        // Add subtle neon flicker to title
        if (tvMainTitle != null) {
            flickerAnim = android.animation.ObjectAnimator.ofFloat(tvMainTitle, "alpha", 1f, 0.8f, 1f, 0.95f, 1f);
            flickerAnim.setDuration(4000);
            flickerAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            flickerAnim.start();
        }
    }

    @Override
    protected void onDestroy() {
        if (heartbeatAnim != null) {
            heartbeatAnim.cancel();
            heartbeatAnim = null;
        }
        if (flickerAnim != null) {
            flickerAnim.cancel();
            flickerAnim = null;
        }
        super.onDestroy();
    }
}