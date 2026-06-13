package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Widget/component test (Robolectric) cho {@link PieceSetManager}: mặc định CLASSIC + persist lựa
 * chọn bộ quân qua SharedPreferences.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PieceSetManagerRobolectricTest {

    private Context ctx() {
        return ApplicationProvider.getApplicationContext();
    }

    @Test
    public void default_isClassic() {
        assertEquals(PieceSet.CLASSIC, PieceSetManager.getCurrent(ctx()));
    }

    @Test
    public void setCurrent_persists() {
        PieceSetManager.setCurrent(ctx(), PieceSet.NEON);
        assertEquals(PieceSet.NEON, PieceSetManager.getCurrent(ctx()));

        PieceSetManager.setCurrent(ctx(), PieceSet.GOLD);
        assertEquals(PieceSet.GOLD, PieceSetManager.getCurrent(ctx()));

        // applySaved không lỗi (nạp tint vào renderer)
        PieceSetManager.applySaved(ctx());
    }
}
