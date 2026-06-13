package com.saigonphantomlabs;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.core.app.ActivityOptionsCompat;

import com.saigonphantomlabs.chess.R;

/**
 * Hero animation (shared element transition) cho điều hướng giữa các activity.
 *
 * <p>Một view ở màn nguồn "bay & biến hình" thành view tương ứng ở màn đích thay vì cả màn fade.
 * View nguồn được gán {@code transitionName} ngay lúc click (cần cho item trong list — id trùng
 * nhau), view đích khai báo cùng {@code transitionName} (XML hoặc code). Hai chuỗi PHẢI khớp.
 *
 * <p>Lưu ý: khi đã dùng scene transition thì KHÔNG gọi {@code overridePendingTransition} cho cùng
 * lần navigate (chúng xung đột). Nếu không có hero ({@code hero == null}) → fallback fade_zoom như cũ.
 */
public final class NavAnim {

    private NavAnim() {}

    // Tên shared element — 2 đầu (view nguồn + view đích) phải khớp chuỗi này.
    public static final String HERO_LOGO = "hero_logo";
    public static final String HERO_BOARD = "hero_board";
    public static final String HERO_SAVED = "hero_saved";
    public static final String HERO_ACHIEVEMENTS = "hero_achievements";
    public static final String HERO_PUZZLES = "hero_puzzles";
    public static final String HERO_VIP = "hero_vip";

    /**
     * Cờ Intent báo activity đích "đã vào bằng hero" → đích nên BỎ entry animation thủ công của
     * riêng hero view (tránh giật đôi), để shared element lo phần chuyển động chính.
     */
    public static final String EXTRA_HERO = "nav_hero";

    /**
     * Khởi chạy activity với hero transition.
     *
     * @param from            activity nguồn
     * @param intent          intent đích
     * @param hero            view nguồn dùng làm shared element (null → fallback fade_zoom)
     * @param transitionName  tên shared element, phải khớp view đích
     */
    public static void startWithHero(Activity from, Intent intent, View hero, String transitionName) {
        if (hero != null && transitionName != null) {
            hero.setTransitionName(transitionName);
            intent.putExtra(EXTRA_HERO, true);
            ActivityOptionsCompat opts =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(from, hero, transitionName);
            from.startActivity(intent, opts.toBundle());
            // KHÔNG overridePendingTransition — xung đột với scene transition.
        } else {
            from.startActivity(intent);
            from.overridePendingTransition(R.anim.fade_zoom_in, R.anim.fade_zoom_out);
        }
    }

    /** Activity đích vào bằng hero? → dùng để bỏ entry animation trùng của hero view. */
    public static boolean enteredViaHero(Activity activity) {
        return activity.getIntent() != null
                && activity.getIntent().getBooleanExtra(EXTRA_HERO, false);
    }
}
