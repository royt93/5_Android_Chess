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

    fun setupEdgeToEdge2(
        rootView: View,
        paddingTop: Boolean = true,
        paddingBottom: Boolean = true,
    ) {
        // Nếu cần inset padding cho layout chính
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                /* left = */ systemBars.left,
                /* top = */ if (paddingTop) systemBars.top else 0,
                /* right = */ systemBars.right,
                /* bottom = */ if (paddingBottom) systemBars.bottom else 0,
            )
            WindowInsetsCompat.CONSUMED
        }
    }
}
