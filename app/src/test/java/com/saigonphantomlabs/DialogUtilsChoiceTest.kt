package com.saigonphantomlabs

import android.app.Activity
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.saigonphantomlabs.chess.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * Widget test (Robolectric) cho dialog CHUNG [DialogUtils.showChoiceDialog] (glass game style).
 * Phủ: hiển thị, build đủ N row, set title/icon, chọn đúng index, nút Cancel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class DialogUtilsChoiceTest {

    private lateinit var activity: Activity

    private fun opts() = arrayOf(
        DialogUtils.ChoiceOption("A", null, "🟢", Color.GREEN),
        DialogUtils.ChoiceOption("B", "sub", "🟡", Color.YELLOW),
        DialogUtils.ChoiceOption("C", null, null, Color.RED),
    )

    @Before fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setTheme(R.style.Theme_Chess)
    }

    private fun container(d: android.app.Dialog) =
        d.findViewById<LinearLayout>(R.id.choiceContainer)

    @Test fun showChoiceDialog_displays_withTitleAndRows() {
        DialogUtils.showChoiceDialog(activity, "Pick one", R.drawable.ic_clock, opts()) { }
        val dialog = ShadowDialog.getLatestDialog()
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)
        assertEquals("Pick one", dialog.findViewById<TextView>(R.id.dialog_title).text.toString())
        assertEquals(3, container(dialog).childCount)
    }

    @Test fun iconResZero_hidesIcon() {
        DialogUtils.showChoiceDialog(activity, "T", 0, opts()) { }
        val dialog = ShadowDialog.getLatestDialog()
        assertEquals(View.GONE, dialog.findViewById<View>(R.id.dialog_icon).visibility)
    }

    @Test fun clickRow_firesOnPickWithIndex_andDismisses() {
        var picked = -1
        DialogUtils.showChoiceDialog(activity, "T", R.drawable.ic_clock, opts()) { i -> picked = i }
        val dialog = ShadowDialog.getLatestDialog()
        // click row thứ 2 (index 1)
        container(dialog).getChildAt(1).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, picked)
        assertFalse(dialog.isShowing)
    }

    @Test fun cancel_dismisses_withoutPick() {
        var called = false
        DialogUtils.showChoiceDialog(activity, "T", R.drawable.ic_clock, opts()) { called = true }
        val dialog = ShadowDialog.getLatestDialog()
        dialog.findViewById<View>(R.id.btnCancel).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(dialog.isShowing)
        assertFalse(called)
    }
}
