package com.saigonphantomlabs.sdkadbmob

import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object UIUtils {
    fun setupEdgeToEdge1(window: Window) {
        // Edge-to-edge cho Android 10+ (API 29+)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    fun setupEdgeToEdge2(rootLayout: View) {
        // Nếu cần inset padding cho layout chính
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                /* left = */ systemBars.left,
                /* top = */ systemBars.top,
                /* right = */ systemBars.right,
                /* bottom = */ systemBars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }
}
