package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Widget/component test (Robolectric, JVM — không cần device) cho {@link BoardThemeManager}:
 * persistence qua SharedPreferences + vẽ bitmap bàn cờ.
 *
 * compileSdk=37 vượt mức Robolectric hỗ trợ → ghim sdk=34 cho runtime test.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BoardThemeManagerRobolectricTest {

    private Context ctx() {
        return ApplicationProvider.getApplicationContext();
    }

    @Test
    public void defaultTheme_isClassic() {
        assertEquals("Mặc định khi chưa lưu là CLASSIC",
                BoardThemeManager.Theme.CLASSIC, BoardThemeManager.load(ctx()));
    }

    @Test
    public void saveThenLoad_roundTrips() {
        BoardThemeManager.save(ctx(), BoardThemeManager.Theme.JADE);
        assertEquals(BoardThemeManager.Theme.JADE, BoardThemeManager.load(ctx()));

        BoardThemeManager.save(ctx(), BoardThemeManager.Theme.CORAL);
        assertEquals("ghi đè theme mới", BoardThemeManager.Theme.CORAL, BoardThemeManager.load(ctx()));
    }

    @Test
    public void drawBoard_returnsBitmapOfRequestedSize() {
        int[] colors = BoardThemeManager.THEMES[0];
        Bitmap bmp = BoardThemeManager.drawBoard(colors, 800);
        assertNotNull(bmp);
        assertEquals(800, bmp.getWidth());
        assertEquals(800, bmp.getHeight());
    }

    @Test
    public void themeMetadataArrays_areConsistent() {
        // Mỗi theme phải có đủ tên + emoji + bảng màu tương ứng
        assertTrue(BoardThemeManager.THEME_NAMES.length >= BoardThemeManager.Theme.values().length);
        assertEquals(BoardThemeManager.Theme.values().length, BoardThemeManager.THEMES.length);
    }
}
