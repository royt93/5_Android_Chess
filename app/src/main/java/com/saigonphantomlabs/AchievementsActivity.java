package com.saigonphantomlabs;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.roy.sdkadbmob.UIUtils;
import com.saigonphantomlabs.chess.AchievementManager;
import com.saigonphantomlabs.chess.R;

/**
 * Màn "Thành tích" — list huy hiệu (emoji + tên + mô tả), mở khoá hiển thị ✓ vàng, khoá thì mờ + 🔒.
 * Header hiện tiến độ X/N. ScrollView + LinearLayout (không thêm dep RecyclerView — giữ APK nhẹ).
 */
public class AchievementsActivity extends BaseActivity {

    private ObjectAnimator glowAnim1, glowAnim2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_achievements);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.contentLayout), true, true);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        startGlow();
        populate();
    }

    private void populate() {
        AchievementManager mgr = new AchievementManager(this);
        LinearLayout list = findViewById(R.id.achList);
        list.removeAllViews();

        ((TextView) findViewById(R.id.tvProgress)).setText(
                getString(R.string.achievements_progress, mgr.unlockedCount(), AchievementManager.total()));

        for (AchievementManager.Achievement a : AchievementManager.Achievement.values()) {
            boolean unlocked = mgr.isUnlocked(a);
            View row = getLayoutInflater().inflate(R.layout.item_achievement, list, false);
            ((TextView) row.findViewById(R.id.achEmoji)).setText(a.emoji);
            ((TextView) row.findViewById(R.id.achTitle)).setText(getString(a.titleRes));
            ((TextView) row.findViewById(R.id.achDesc)).setText(getString(a.descRes));

            TextView status = row.findViewById(R.id.achStatus);
            status.setText(unlocked ? "✅" : "🔒");
            // Khoá → làm mờ cả hàng để phân biệt nhanh
            row.setAlpha(unlocked ? 1f : 0.45f);
            list.addView(row);
        }
    }

    private void startGlow() {
        glowAnim1 = pulse(findViewById(R.id.glowTL), 0.55f, 0.85f, 3200, 0);
        glowAnim2 = pulse(findViewById(R.id.glowBR), 0.45f, 0.75f, 3800, 600);
    }

    private ObjectAnimator pulse(View v, float from, float to, long dur, long delay) {
        if (v == null) return null;
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, "alpha", from, to);
        anim.setDuration(dur);
        anim.setStartDelay(delay);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
        return anim;
    }

    @Override
    protected void onDestroy() {
        if (glowAnim1 != null) { glowAnim1.cancel(); glowAnim1 = null; }
        if (glowAnim2 != null) { glowAnim2.cancel(); glowAnim2 = null; }
        super.onDestroy();
    }
}
