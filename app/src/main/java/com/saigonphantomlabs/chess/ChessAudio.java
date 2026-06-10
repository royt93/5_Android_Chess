package com.saigonphantomlabs.chess;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

/**
 * Đóng gói toàn bộ phát âm thanh của ván cờ qua {@link SoundPool}
 * (tách khỏi {@link Chess} để giảm god-class + gom side-effect âm thanh một chỗ).
 *
 * Hai mẫu âm: id1 (chess_1) dùng cho nước Trắng / chiếu / phong cấp;
 * id2 (chess_2) cho nước Đen / nước sai / biến hình. Giữ nguyên mapping cũ.
 */
final class ChessAudio {
    private SoundPool soundPool;
    private int soundId1 = 0;
    private int soundId2 = 0;
    private boolean ready = false;

    ChessAudio(Context ctx) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) ready = true;
        });
        soundId1 = soundPool.load(ctx, R.raw.chess_1, 1);
        soundId2 = soundPool.load(ctx, R.raw.chess_2, 1);
    }

    private void play(int soundId) {
        if (ready && soundPool != null && soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }

    void playMove(boolean isWhite) { play(isWhite ? soundId1 : soundId2); }
    void playCheck()               { play(soundId1); }
    void playIllegal()             { play(soundId2); }
    void playPromotion()           { play(soundId1); }
    void playTransformation()      { play(soundId2); }

    void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            ready = false;
        }
    }
}
