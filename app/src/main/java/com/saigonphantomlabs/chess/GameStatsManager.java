package com.saigonphantomlabs.chess;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class GameStatsManager {
    private static final String PREF_NAME = "chess_stats";

    // Keys
    private static final String KEY_TOTAL_GAMES = "total_games";
    private static final String KEY_TOTAL_TIME = "total_time_ms";

    // Prefix for difficulty based stats
    private static final String PREFIX_WINS = "wins_";
    private static final String PREFIX_LOSSES = "losses_";
    private static final String PREFIX_DRAWS = "draws_";

    private final Context context;
    private final SharedPreferences prefs;

    public GameStatsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveGameResult(AIEngine.Difficulty difficulty, int result, long durationMs) {
        // result: 1=Win (Human), -1=Loss (AI Win), 0=Draw
        SharedPreferences.Editor editor = prefs.edit();

        // Update general stats
        editor.putInt(KEY_TOTAL_GAMES, prefs.getInt(KEY_TOTAL_GAMES, 0) + 1);
        editor.putLong(KEY_TOTAL_TIME, prefs.getLong(KEY_TOTAL_TIME, 0) + durationMs);

        // Update difficulty specific stats
        String diffKey = difficulty.name().toLowerCase();

        if (result > 0) {
            String key = PREFIX_WINS + diffKey;
            editor.putInt(key, prefs.getInt(key, 0) + 1);
        } else if (result < 0) {
            String key = PREFIX_LOSSES + diffKey;
            editor.putInt(key, prefs.getInt(key, 0) + 1);
        } else {
            String key = PREFIX_DRAWS + diffKey;
            editor.putInt(key, prefs.getInt(key, 0) + 1);
        }

        editor.apply();
    }

    public String getStatsSummary() {
        StringBuilder sb = new StringBuilder();
        long totalTime = prefs.getLong(KEY_TOTAL_TIME, 0);
        int totalGames = prefs.getInt(KEY_TOTAL_GAMES, 0);

        sb.append(context.getString(R.string.stats_total_games, totalGames));
        sb.append(context.getString(R.string.stats_total_time, formatDuration(totalTime)));

        sb.append(context.getString(R.string.stats_performance_ai));
        for (AIEngine.Difficulty diff : AIEngine.Difficulty.values()) {
            String diffKey = diff.name().toLowerCase();
            int wins = prefs.getInt(PREFIX_WINS + diffKey, 0);
            int losses = prefs.getInt(PREFIX_LOSSES + diffKey, 0);
            int draws = prefs.getInt(PREFIX_DRAWS + diffKey, 0);

            if (wins + losses + draws > 0) {
                sb.append(String.format(Locale.getDefault(), "- %s: %dW - %dL - %dD\n",
                        diff.name(), wins, losses, draws));
            }
        }

        return sb.toString();
    }

    private String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0) {
            return context.getString(R.string.duration_h_m_s, hours, minutes, seconds);
        } else {
            return context.getString(R.string.duration_m_s, minutes, seconds);
        }
    }
}
