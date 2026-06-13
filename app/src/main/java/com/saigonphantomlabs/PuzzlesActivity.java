package com.saigonphantomlabs;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.roy.sdkadbmob.UIUtils;
import com.saigonphantomlabs.chess.PuzzleRepository;
import com.saigonphantomlabs.chess.R;

/**
 * Màn "Câu đố" — list các câu mate-in-1 nhúng. Tap 1 câu → mở ChessBoardActivity ở puzzle mode
 * (truyền PUZZLE_INDEX). ScrollView + LinearLayout (không thêm dep RecyclerView — giữ APK nhẹ).
 */
public class PuzzlesActivity extends BaseActivity {

    private ObjectAnimator glowAnim1, glowAnim2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_puzzles);
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.contentLayout), true, true);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        startGlow();
        populate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populate(); // refresh trạng thái đã-giải khi back từ màn giải
    }

    private void populate() {
        LinearLayout list = findViewById(R.id.puzzleList);
        list.removeAllViews();
        ((TextView) findViewById(R.id.tvProgress)).setText(getString(R.string.achievements_progress,
                com.saigonphantomlabs.chess.PuzzleProgress.solvedCount(this), PuzzleRepository.count()));

        for (int i = 0; i < PuzzleRepository.count(); i++) {
            final int index = i;
            com.saigonphantomlabs.chess.Puzzle p = PuzzleRepository.get(index);
            boolean solved = p != null
                    && com.saigonphantomlabs.chess.PuzzleProgress.isSolved(this, p.id);
            View row = getLayoutInflater().inflate(R.layout.item_puzzle, list, false);
            ((TextView) row.findViewById(R.id.puzzleTitle))
                    .setText(getString(R.string.puzzle_item, i + 1));
            // mate-in-1 / mate-in-2 ở subtitle
            ((TextView) row.findViewById(R.id.puzzleSubtitle)).setText(
                    p != null && p.mateIn == 2 ? getString(R.string.puzzle_mate_in_2)
                            : getString(R.string.puzzle_mate_in_1));
            // Câu đã giải → ✅ thay mũi tên
            TextView status = row.findViewById(R.id.puzzleStatus);
            if (solved) { status.setText("✅"); status.setTextSize(18f); }
            row.setOnClickListener(new SafeClickListener() {
                @Override public void onSafeClick(View v) { openPuzzle(index); }
            });
            list.addView(row);
        }
    }

    private void openPuzzle(int index) {
        Intent i = new Intent(this, ChessBoardActivity.class);
        i.putExtra("PUZZLE_INDEX", index);
        startActivity(i);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
