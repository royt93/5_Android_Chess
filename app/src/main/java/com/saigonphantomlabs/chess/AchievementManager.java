package com.saigonphantomlabs.chess;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Thành tích / huy hiệu (Achievements) — lưu trạng thái mở khoá + counter tích luỹ trong
 * SharedPreferences ("chess_achievements"). Nhẹ APK (không asset; emoji làm icon).
 *
 * <p>Logic xét điều kiện tách thành {@link #qualifying} thuần (static, không Android) ⇒ unit-test
 * trực tiếp; phần persistence (đếm ván/thắng/giờ + mở khoá) test qua Robolectric.
 *
 * <p>Counter của riêng achievement (đếm CẢ PvP + PvE) — độc lập {@link GameStatsManager} (chỉ PvE).
 * "Người chơi" quy ước là bên Trắng (dưới) cho mọi mode.
 */
public class AchievementManager {

    private static final String PREF = "chess_achievements";
    private static final String K_GAMES = "ach_total_games";
    private static final String K_WINS = "ach_total_wins";
    private static final String K_TIME = "ach_total_time";

    /** Mốc thời gian tích luỹ cho Marathon (1 giờ). */
    private static final long MARATHON_MS = 3_600_000L;
    /** Ngưỡng số nửa-nước cho "thắng nhanh". */
    private static final int QUICK_WIN_PLIES = 20;
    /** Mốc số ván / số thắng cho các huy hiệu cột mốc. */
    private static final int MILESTONE_GAMES = 10;
    private static final int MILESTONE_WINS = 10;

    /** Danh sách huy hiệu (emoji + tên + mô tả). Thứ tự = thứ tự hiển thị. */
    public enum Achievement {
        FIRST_WIN("🏆", R.string.ach_first_win_title, R.string.ach_first_win_desc),
        WINS_10("👑", R.string.ach_wins10_title, R.string.ach_wins10_desc),
        GAMES_10("♟️", R.string.ach_games10_title, R.string.ach_games10_desc),
        BEAT_HARD("🔥", R.string.ach_hard_title, R.string.ach_hard_desc),
        BEAT_UNBEATABLE("💎", R.string.ach_unbeatable_title, R.string.ach_unbeatable_desc),
        CHECKMATE("⚔️", R.string.ach_checkmate_title, R.string.ach_checkmate_desc),
        CASTLE("🏰", R.string.ach_castle_title, R.string.ach_castle_desc),
        PROMOTE("⬆️", R.string.ach_promote_title, R.string.ach_promote_desc),
        FLAWLESS("🛡️", R.string.ach_flawless_title, R.string.ach_flawless_desc),
        QUICK_WIN("⚡", R.string.ach_quick_title, R.string.ach_quick_desc),
        MARATHON("⏳", R.string.ach_marathon_title, R.string.ach_marathon_desc);

        public final String emoji;
        public final int titleRes;
        public final int descRes;

        Achievement(String emoji, int titleRes, int descRes) {
            this.emoji = emoji;
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    /** Dữ kiện 1 ván vừa kết thúc (góc nhìn người chơi = Trắng). */
    public static final class GameResult {
        public boolean vsAi;
        public AIEngine.Difficulty difficulty;
        public boolean humanWon;       // Trắng thắng (quyết định, không hoà)
        public boolean draw;
        public boolean byCheckmate;    // kết thúc bằng chiếu hết (không phải hết giờ/hoà)
        public int moveCount;          // số nửa-nước
        public int humanLostPieces;    // số quân Trắng bị bắt
        public boolean whiteCastled;   // Trắng có nhập thành trong ván
        public boolean whitePromoted;  // Trắng có phong cấp trong ván
        public long durationMs;
    }

    private final SharedPreferences prefs;

    public AchievementManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /**
     * THUẦN: tập huy hiệu ĐỦ ĐIỀU KIỆN từ kết quả ván + counter tích luỹ (đã gồm ván này).
     * Chưa xét "đã mở khoá hay chưa" — chỉ thuần điều kiện ⇒ unit-test dễ.
     */
    public static EnumSet<Achievement> qualifying(GameResult r, int totalGames, int totalWins, long totalTimeMs) {
        EnumSet<Achievement> s = EnumSet.noneOf(Achievement.class);
        if (r.humanWon) s.add(Achievement.FIRST_WIN);
        if (totalWins >= MILESTONE_WINS) s.add(Achievement.WINS_10);
        if (totalGames >= MILESTONE_GAMES) s.add(Achievement.GAMES_10);
        if (r.vsAi && r.humanWon && r.difficulty == AIEngine.Difficulty.HARD) s.add(Achievement.BEAT_HARD);
        if (r.vsAi && r.humanWon && r.difficulty == AIEngine.Difficulty.UNBEATABLE) s.add(Achievement.BEAT_UNBEATABLE);
        if (r.humanWon && r.byCheckmate) s.add(Achievement.CHECKMATE);
        if (r.whiteCastled) s.add(Achievement.CASTLE);
        if (r.whitePromoted) s.add(Achievement.PROMOTE);
        if (r.humanWon && r.humanLostPieces == 0) s.add(Achievement.FLAWLESS);
        if (r.humanWon && r.moveCount > 0 && r.moveCount <= QUICK_WIN_PLIES) s.add(Achievement.QUICK_WIN);
        if (totalTimeMs >= MARATHON_MS) s.add(Achievement.MARATHON);
        return s;
    }

    /**
     * Ghi nhận 1 ván: cập nhật counter (ván/thắng/giờ) + mở khoá huy hiệu mới đạt.
     * Trả danh sách huy hiệu VỪA mở (để hiển thị thông báo). An toàn gọi 1 lần / ván.
     */
    public List<Achievement> recordGameEnd(GameResult r) {
        int games = prefs.getInt(K_GAMES, 0) + 1;
        int wins = prefs.getInt(K_WINS, 0) + (r.humanWon ? 1 : 0);
        long time = prefs.getLong(K_TIME, 0) + Math.max(0L, r.durationMs);

        SharedPreferences.Editor e = prefs.edit();
        e.putInt(K_GAMES, games).putInt(K_WINS, wins).putLong(K_TIME, time);

        List<Achievement> newly = new ArrayList<>();
        for (Achievement a : qualifying(r, games, wins, time)) {
            if (!prefs.getBoolean(key(a), false)) {
                e.putBoolean(key(a), true);
                newly.add(a);
            }
        }
        e.apply();
        return newly;
    }

    public boolean isUnlocked(Achievement a) {
        return prefs.getBoolean(key(a), false);
    }

    public int unlockedCount() {
        int n = 0;
        for (Achievement a : Achievement.values()) if (isUnlocked(a)) n++;
        return n;
    }

    public static int total() {
        return Achievement.values().length;
    }

    private static String key(Achievement a) {
        return "ach_" + a.name();
    }
}
