package com.saigonphantomlabs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.saigonphantomlabs.chess.R;
import com.saigonphantomlabs.sdkadbmob.AdMobManager;
import com.saigonphantomlabs.sdkadbmob.UIUtils;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DURATION = 1000L;
    private long splashStartTime;
    private boolean isNavigating = false;
    // [WARN-06] Store handler reference to cancel pending callbacks on destroy
    private android.os.Handler splashHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        splashStartTime = System.currentTimeMillis();
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_splash);
        View rootLayout = findViewById(R.id.rootLayout);
        ImageView ivBkg = findViewById(R.id.ivBkg);

        UIUtils.INSTANCE.setupEdgeToEdge2(rootLayout,
                true,
                true);

        Glide.with(this)
                .asGif()
                .load(R.drawable.ic_bkg_1)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivBkg);

        AdMobManager.INSTANCE.setCurrentActivity(this);

        // AdMob init
        AdMobManager.INSTANCE.initSplashScreen(this, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - splashStartTime;
                long remainingTime = MIN_SPLASH_DURATION - elapsedTime;

                if (remainingTime > 0) {
                    // [WARN-06] Use stored handler so we can cancel it in onDestroy()
                    splashHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    splashHandler.postDelayed(() -> {
                        runOnUiThread(() -> navigateToMainActivity());
                    }, remainingTime);
                } else {
                    runOnUiThread(() -> navigateToMainActivity());
                }
                return Unit.INSTANCE;
            }
        });
    }

    private void navigateToMainActivity() {
        if (isNavigating || isFinishing()) {
            return;
        }
        isNavigating = true;
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // Prevent back button during splash
    }

    @Override
    protected void onDestroy() {
        // [WARN-06] Cancel pending navigation callback if Activity is destroyed first
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }
        // [BUG-03] Clear stale Activity reference from AdMobManager
        AdMobManager.INSTANCE.clearCurrentActivity();
        super.onDestroy();
    }
}
