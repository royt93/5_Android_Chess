package com.saigonphantomlabs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ContextThemeWrapper
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saigonphantomlabs.chess.R
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test (instrumented, cần device/emulator: `./gradlew connectedDebugAndroidTest`).
 *
 * Khoá hợp đồng layout dialog CHUNG `dialog_glass_choice.xml`: inflate trên runtime thật
 * (cần Material theme cho MaterialButton) → xác nhận đủ view id mà DialogUtils.showChoiceDialog dùng.
 */
@RunWith(AndroidJUnit4::class)
class DialogGlassChoiceInstrumentedTest {

    @Test
    fun glassChoiceLayout_hasRequiredViews_onDevice() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val themed = ContextThemeWrapper(ctx, R.style.Theme_Chess)
        val view = LayoutInflater.from(themed).inflate(R.layout.dialog_glass_choice, null)

        assertNotNull("choiceContainer", view.findViewById<LinearLayout>(R.id.choiceContainer))
        assertNotNull("dialog_title", view.findViewById<TextView>(R.id.dialog_title))
        assertNotNull("dialog_icon", view.findViewById<ImageView>(R.id.dialog_icon))
        assertNotNull("btnCancel", view.findViewById<View>(R.id.btnCancel))
    }
}
