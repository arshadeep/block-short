package com.still.app.accessibility

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.still.app.domain.InterventionMessage

/** Keep exit actions on screen; long copy and large-font actions scroll independently. */
internal class InterventionOverlayLayout(context: Context, message: InterventionMessage) : LinearLayout(context) {
    val content = LinearLayout(context).apply { orientation = VERTICAL }
    val actions = LinearLayout(context).apply { orientation = VERTICAL }
    private val actionScroll = ScrollView(context).apply {
        addView(actions)
        isFillViewport = false
    }

    init {
        orientation = VERTICAL
        setPadding(dp(24), dp(20), dp(24), dp(20))
        setBackgroundColor(PAPER)
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(dp(24) + bars.left, dp(20) + bars.top, dp(24) + bars.right, dp(20) + bars.bottom)
            insets
        }
        content.setPadding(0, 0, 0, dp(20))
        content.addView(TextView(context).apply {
            text = "BLOCK SHORT  ·  REELS BLOCKED"
            textSize = 12f
            setTextColor(MOSS)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.12f
        }, matchWidthParams())
        content.addView(TextView(context).apply {
            text = message.eyebrow
            textSize = 13f
            setTextColor(MOSS)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.1f
        }, matchWidthParams(top = 20))
        content.addView(TextView(context).apply {
            text = message.title
            textSize = 28f
            setTextColor(INK)
            typeface = Typeface.create("sans-serif-serif", Typeface.BOLD)
            setLineSpacing(0f, 0.95f)
        }, matchWidthParams(top = 14))
        content.addView(TextView(context).apply {
            text = message.body
            textSize = 16f
            setTextColor(SOFT_INK)
            setLineSpacing(dp(5).toFloat(), 1f)
        }, matchWidthParams(top = 22))
        addView(ScrollView(context).apply {
            addView(content)
            isFillViewport = true
        }, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(actionScroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun addAction(label: String, filled: Boolean, onClick: () -> Unit): Button =
        Button(context).apply {
            text = label
            textSize = 16f
            isAllCaps = false
            setTypeface(typeface, Typeface.BOLD)
            minHeight = dp(56)
            setTextColor(if (filled) PAPER else INK)
            background = roundedDrawable(if (filled) INK else SUNRISE, 18)
            stateListAnimator = null
            setOnClickListener { onClick() }
            actions.addView(this, matchWidthParams(top = if (actions.childCount == 0) 0 else 12))
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(0)
        val height = (MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom).coerceAtLeast(0)
        actions.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        // Leave room to read even in landscape or at the largest accessibility font size.
        actionScroll.layoutParams.height = actions.measuredHeight.coerceAtMost(height / 2)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun matchWidthParams(top: Int = 0) = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        .apply { topMargin = dp(top) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun roundedDrawable(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private companion object {
        val INK = Color.rgb(17, 22, 16)
        val SOFT_INK = Color.rgb(57, 66, 54)
        val PAPER = Color.rgb(242, 240, 232)
        val MOSS = Color.rgb(64, 91, 63)
        val SUNRISE = Color.rgb(255, 180, 94)
    }
}
