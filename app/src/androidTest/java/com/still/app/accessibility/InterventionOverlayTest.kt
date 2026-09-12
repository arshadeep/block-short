package com.still.app.accessibility

import android.content.res.Configuration
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.still.app.MainActivity
import com.still.app.domain.InterventionMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterventionOverlayTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun compactScreenKeepsBothExitsVisibleWithLargeText() {
        instrumentation.runOnMainSync {
            for (scale in listOf(1f, 1.5f, 2f)) {
                val root = createLayout(scale, 320, 568)
                val buttons = addStandardActions(root)
                measure(root, 320, 568)
                buttons.forEach { assertFullyInside(root, it) }
            }
        }
    }

    @Test
    fun landscapeCanReachBothExitsWithLargeText() {
        instrumentation.runOnMainSync {
            val root = createLayout(2f, 568, 320)
            val options = addStandardActions(root)
            measure(root, 568, 320)
            val scroller = root.actions.parent as ScrollView
            options.forEach { button ->
                scroller.scrollTo(0, button.top)
                assertFullyInside(root, button)
            }
        }
    }

    @Test
    fun eachActionRespondsToOneInjectedTap() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var taps = 0
            lateinit var buttons: List<Button>
            scenario.onActivity { activity ->
                val root = InterventionOverlayLayout(activity, InterventionMessages.hard)
                buttons = listOf("Instagram home", "Phone home").mapIndexed { i, label ->
                    root.addAction(label, i == 0) { taps++ }
                }
                activity.setContentView(root)
            }
            instrumentation.waitForIdleSync()
            buttons.forEachIndexed { index, button ->
                val bounds = Rect()
                scenario.onActivity { assertTrue(button.getGlobalVisibleRect(bounds)) }
                val down = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                        bounds.exactCenterX(), bounds.exactCenterY(), 0)
                    instrumentation.sendPointerSync(event)
                    event.recycle()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { assertEquals(index + 1, taps) }
            }
        }
    }

    private fun createLayout(scale: Float, width: Int, height: Int): InterventionOverlayLayout {
        val base = instrumentation.targetContext
        val config = Configuration(base.resources.configuration).apply {
            fontScale = scale
            screenWidthDp = width
            screenHeightDp = height
        }
        return InterventionOverlayLayout(base.createConfigurationContext(config), InterventionMessages.hard)
    }

    private fun addStandardActions(root: InterventionOverlayLayout): List<Button> =
        listOf("Instagram home", "Phone home").mapIndexed { i, label ->
            root.addAction(label, i == 0) {}
        }

    private fun measure(root: View, widthDp: Int, heightDp: Int) {
        val density = root.resources.displayMetrics.density
        val width = (widthDp * density).toInt()
        val height = (heightDp * density).toInt()
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
    }

    private fun assertFullyInside(root: InterventionOverlayLayout, button: Button) {
        val bounds = Rect(0, 0, button.width, button.height)
        root.offsetDescendantRectToMyCoords(button, bounds)
        val scroller = button.parent.parent as View
        // offsetDescendantRectToMyCoords subtracts this view's own scroll offset too.
        val viewport = Rect(scroller.scrollX, scroller.scrollY,
            scroller.scrollX + scroller.width, scroller.scrollY + scroller.height)
        root.offsetDescendantRectToMyCoords(scroller, viewport)
        assertTrue("${button.text} clipped: $bounds outside $viewport", viewport.contains(bounds))
        assertTrue(bounds.top >= root.paddingTop && bounds.bottom <= root.height - root.paddingBottom)
    }
}
