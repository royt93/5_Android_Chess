package com.saigonphantomlabs;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.roy.sdkadbmob.UIUtils;
import com.saigonphantomlabs.chess.ChessClock;
import com.saigonphantomlabs.chess.Chessman;
import com.saigonphantomlabs.chess.GameSaveManager;
import com.saigonphantomlabs.chess.R;

import java.util.List;

/**
 * Thư viện "Ván đã lưu" — list mọi ván dở (PvP + PvE), sort mới→cũ, mini-board thumbnail.
 * Tap để tiếp tục; nút xoá có dialog xác nhận. Dùng ScrollView+LinearLayout (≤100 item, không
 * thêm dependency RecyclerView — giữ APK nhẹ).
 */
public class SavedGamesActivity extends BaseActivity {

    private LinearLayout savedList;
    private ScrollView scrollList;
    private View emptyState;
    private ObjectAnimator glowAnim1, glowAnim2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.INSTANCE.setupEdgeToEdge1(getWindow());
        setContentView(R.layout.a_saved_games);
        // Pad nội dung theo system bars; glow nằm ở rootLayout nên vẫn tràn full màn.
        UIUtils.INSTANCE.setupEdgeToEdge2(findViewById(R.id.contentLayout), true, true);
        savedList = findViewById(R.id.savedList);
        scrollList = findViewById(R.id.scrollList);
        emptyState = findViewById(R.id.emptyState);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        startGlow();
    }

    /** Nhịp sáng nhẹ cho 2 corner glow → nền lung linh. */
    private void startGlow() {
        glowAnim1 = pulse(findViewById(R.id.glowTL), 0.55f, 0.85f, 3200, 0);
        glowAnim2 = pulse(findViewById(R.id.glowBR), 0.45f, 0.75f, 3800, 600);
    }

    private ObjectAnimator pulse(View v, float from, float to, long dur, long delay) {
        if (v == null) return null;
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "alpha", from, to);
        a.setDuration(dur);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.start();
        return a;
    }

    @Override
    protected void onDestroy() {
        if (glowAnim1 != null) { glowAnim1.cancel(); glowAnim1 = null; }
        if (glowAnim2 != null) { glowAnim2.cancel(); glowAnim2 = null; }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populate(); // refresh khi back từ ván vừa resume
    }

    private void populate() {
        savedList.removeAllViews();
        List<GameSaveManager.SavedGame> slots = GameSaveManager.listSlots(this);
        boolean empty = slots.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        scrollList.setVisibility(empty ? View.GONE : View.VISIBLE);

        LayoutInflater inf = getLayoutInflater();
        for (GameSaveManager.SavedGame g : slots) {
            View row = inf.inflate(R.layout.item_saved_game, savedList, false);
            bindRow(row, g);
            savedList.addView(row);
        }
    }

    private void bindRow(View row, GameSaveManager.SavedGame g) {
        ((MiniBoardView) row.findViewById(R.id.miniBoard)).setPosition(g);

        String mode = g.isVsAi
                ? getString(R.string.saved_mode_pve) + (g.difficulty != null ? " · " + g.difficulty : "")
                : getString(R.string.saved_mode_pvp);
        ((TextView) row.findViewById(R.id.tvMode)).setText(mode);

        String turn = g.turn == Chessman.PlayerColor.White
                ? getString(R.string.white_turn) : getString(R.string.black_turn);
        ((TextView) row.findViewById(R.id.tvMeta))
                .setText(getString(R.string.saved_moves, g.moveCount) + " · " + turn);

        TextView tvClock = row.findViewById(R.id.tvClock);
        if (g.hasClock) {
            tvClock.setText("⏱ " + ChessClock.format(g.whiteMs) + " / " + ChessClock.format(g.blackMs));
        } else {
            tvClock.setText(getString(R.string.time_off));
        }

        ((TextView) row.findViewById(R.id.tvTime)).setText(
                DateUtils.getRelativeTimeSpanString(g.savedAtMs, System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS));

        row.setOnClickListener(new SafeClickListener() {
            @Override public void onSafeClick(View v) { resume(g.sessionId); }
        });
        ((ImageButton) row.findViewById(R.id.btnDelete)).setOnClickListener(new SafeClickListener() {
            @Override public void onSafeClick(View v) { confirmDelete(g); }
        });
    }

    private void resume(String sessionId) {
        Intent intent = new Intent(this, ChessBoardActivity.class);
        intent.putExtra("RESUME_SESSION", sessionId);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void confirmDelete(GameSaveManager.SavedGame g) {
        DialogUtils.showBasicDialog(this,
                getString(R.string.saved_delete_title), getString(R.string.saved_delete_message),
                getString(R.string.saved_delete), getString(R.string.cancel),
                R.drawable.ic_delete,
                () -> {
                    GameSaveManager.deleteSlot(this, g.sessionId);
                    populate();
                },
                null);
    }
}
