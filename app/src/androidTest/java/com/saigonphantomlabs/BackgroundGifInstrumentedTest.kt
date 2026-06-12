package com.saigonphantomlabs

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saigonphantomlabs.chess.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test (instrumented, cần device/emulator: `./gradlew connectedDebugAndroidTest`).
 *
 * Khoá hợp đồng tối ưu memory: GIF nền `ic_bkg_1` đã được resize xuống ≤360px (từ 559×863)
 * → giảm RAM decode (~27MB → ~5.6MB). Nếu ai thay lại GIF full-res → test fail, cảnh báo OOM.
 */
@RunWith(AndroidJUnit4::class)
class BackgroundGifInstrumentedTest {

    @Test
    fun backgroundGif_isDownsized() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(ctx.resources, R.drawable.ic_bkg_1, opts)
        // Asset đã resize 559×863 → 360×556; cho biên độ tới 400 phòng đổi nhẹ.
        assertTrue(
            "GIF nền phải đã downsize (≤400px) — hiện ${opts.outWidth}x${opts.outHeight}",
            opts.outWidth in 1..400,
        )
    }
}
