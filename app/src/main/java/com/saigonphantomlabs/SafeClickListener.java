package com.saigonphantomlabs;

import android.view.View;
import android.view.animation.AnimationUtils;

public abstract class SafeClickListener implements View.OnClickListener {
    private static final int DEFAULT_CLICK_INTERVAL = 1000; // 1 second
    private long lastClickTime = 0;
    private final long clickInterval;

    public SafeClickListener() {
        this.clickInterval = DEFAULT_CLICK_INTERVAL;
    }

    public SafeClickListener(long clickInterval) {
        this.clickInterval = clickInterval;
    }

    @Override
    public void onClick(View view) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime >= clickInterval) {
            lastClickTime = currentTime;

            // Add click animation
            view.startAnimation(AnimationUtils.loadAnimation(view.getContext(),
                com.saigonphantomlabs.chess.R.anim.button_click_scale));

            onSafeClick(view);
        }
    }

    public abstract void onSafeClick(View view);
}