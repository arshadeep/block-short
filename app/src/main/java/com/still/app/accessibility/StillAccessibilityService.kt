package com.still.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import com.still.app.data.SettingsRepository
import com.still.app.domain.InterventionMessage
import com.still.app.domain.InterventionMessages
import com.still.app.domain.ProtectionMode
import com.still.app.domain.SupportedApp
import java.lang.ref.WeakReference
import java.time.LocalDate
import java.time.LocalTime

class StillAccessibilityService : AccessibilityService() {
    private lateinit var repository: SettingsRepository
    private lateinit var windowManager: WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val delayedOverlayDismiss = Runnable { removeOverlayNow() }
    private val overlayScopeWatchdog = object : Runnable {
        override fun run() {
            val owner = overlayOwner ?: return
            val config = repository.loadConfig()
            if (!config.isActiveAt(LocalTime.now()) || owner !in config.enabledApps) {
                removeOverlayNow()
                return
            }

            val foregroundPackage = rootInActiveWindow?.packageName?.toString()
            if (foregroundPackage == null) {
                val now = System.currentTimeMillis()
                if (foregroundUnknownSince == 0L) foregroundUnknownSince = now
                if (now - foregroundUnknownSince >= UNKNOWN_FOREGROUND_GRACE_MS) {
                    removeOverlayNow()
                    return
                }
            } else if (
                foregroundPackage != this@StillAccessibilityService.packageName &&
                foregroundPackage !in owner.packageNames
            ) {
                removeOverlayNow()
                return
            } else {
                foregroundUnknownSince = 0L
            }
            mainHandler.postDelayed(this, OVERLAY_SCOPE_CHECK_MS)
        }
    }

    private var overlay: View? = null
    private var overlayOwner: SupportedApp? = null
    private var foregroundUnknownSince = 0L
    private var feedExitedForIntervention = false
    private var countdown: CountDownTimer? = null
    private var settingsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var sessionApp: SupportedApp? = null
    private var sessionCount = 0
    private var sessionAllowance = 10
    private var lastShortSeenAt = 0L
    private var lastCountedScrollAt = 0L
    private var lastContentFingerprint: Int? = null
    private var suppressInterventionsUntil = 0L
    private var hardOverrideActive = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = SettingsRepository(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        connectedInstance = WeakReference(this)
        settingsListener = repository.registerConfigChangeListener { config ->
            mainHandler.post {
                val owner = overlayOwner
                if (owner != null &&
                    (!config.isActiveAt(LocalTime.now()) || owner !in config.enabledApps)
                ) {
                    dismissOverlay()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (!::repository.isInitialized) return

        val packageName = event.packageName?.toString() ?: return
        overlayOwner?.let { owner ->
            if (packageName !in owner.packageNames) removeOverlayNow()
        }
        val app = SupportedApp.fromPackage(packageName) ?: return
        val config = repository.loadConfig()
        if (!config.isActiveAt(LocalTime.now()) || app !in config.enabledApps) {
            hardOverrideActive = false
            dismissOverlay()
            return
        }

        val now = System.currentTimeMillis()
        if (now < suppressInterventionsUntil) return
        val result = ShortFormDetector.detect(app, rootInActiveWindow)
        if (!result.isShortFormFeed) {
            // The feed is intentionally exited before an intervention so its
            // video and audio stop. Keep the resulting choice screen visible.
            if (overlay != null && feedExitedForIntervention) return
            // Accessibility snapshots briefly alternate while Instagram lays
            // out video controls. Only hide after the non-Reels state remains
            // stable; a valid Reels event cancels this pending removal.
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                dismissOverlay()
            } else {
                scheduleOverlayDismiss()
            }
            return
        }
        mainHandler.removeCallbacks(delayedOverlayDismiss)

        val isHardMode = config.mode == ProtectionMode.HARD
        if (!isHardMode) hardOverrideActive = false

        if (isHardMode && hardOverrideActive) {
            val overrideExpired = sessionApp != app || now - lastShortSeenAt > SESSION_GAP_MS
            if (overrideExpired) hardOverrideActive = false
        }
        if (isHardMode && !hardOverrideActive) {
            showHardIntervention(app)
            return
        }

        val sessionExpired = sessionApp != app || now - lastShortSeenAt > SESSION_GAP_MS
        if (sessionExpired) {
            sessionApp = app
            sessionCount = 0
            sessionAllowance = if (isHardMode) sessionAllowance else config.sessionLimit
            lastCountedScrollAt = 0L
            lastContentFingerprint = result.contentFingerprint.takeIf { it != 0 }
            if (!isHardMode) {
                Toast.makeText(
                    this,
                    "Soft mode on · ${config.sessionLimit} reels this session",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        lastShortSeenAt = now

        if (overlay != null) return

        val fingerprintChanged = result.contentFingerprint != 0 &&
            lastContentFingerprint != null &&
            result.contentFingerprint != lastContentFingerprint
        val isReelTransition = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && fingerprintChanged)
        if (!isReelTransition) return
        if (now - lastCountedScrollAt < SCROLL_DEBOUNCE_MS) return

        lastCountedScrollAt = now
        if (result.contentFingerprint != 0) lastContentFingerprint = result.contentFingerprint
        sessionCount += 1
        repository.recordScroll()

        val remaining = sessionAllowance - sessionCount
        if (remaining in 1..3) {
            val messageIndex = (3 - remaining).coerceIn(0, InterventionMessages.approaching.lastIndex)
            Toast.makeText(this, InterventionMessages.approaching[messageIndex], Toast.LENGTH_SHORT).show()
        }
        if (sessionCount >= sessionAllowance) {
            if (isHardMode) {
                hardOverrideActive = false
                showHardIntervention(app)
            } else {
                showLimitIntervention(app)
            }
        }
    }

    private fun showHardIntervention(app: SupportedApp) {
        if (overlay != null) return
        val messages = InterventionMessages.hard
        val message = messages[LocalDate.now().dayOfYear % messages.size]
        repository.recordBlock()
        exitDetectedFeed()
        showOverlay(
            app = app,
            message = message,
            primaryLabel = "${app.displayName} home",
            onPrimary = { returnToAppHome(app) },
            secondaryLabel = "Phone home",
            onSecondary = ::exitApp,
            tertiaryLabel = "Choose more scrolls",
            onTertiary = { showExtensionFriction(app, hardMode = true) },
        )
    }

    private fun showLimitIntervention(app: SupportedApp) {
        if (overlay != null) return
        repository.recordBlock()
        exitDetectedFeed()
        showOverlay(
            app = app,
            message = InterventionMessages.limit,
            primaryLabel = "${app.displayName} home",
            onPrimary = { returnToAppHome(app) },
            secondaryLabel = "Phone home",
            onSecondary = ::exitApp,
            tertiaryLabel = "Choose more scrolls",
            onTertiary = { showExtensionFriction(app, hardMode = false) },
        )
    }

    private fun showExtensionFriction(app: SupportedApp, hardMode: Boolean) {
        dismissOverlay()
        feedExitedForIntervention = true
        val root = createOverlayShell(InterventionMessages.extension)
        val actionArea = root.getChildAt(root.childCount - 1) as LinearLayout

        val exit = actionButton("${app.displayName} home", filled = true).apply {
            setOnClickListener { returnToAppHome(app) }
        }
        val extend = actionButton("Read for 8 seconds", filled = false).apply {
            isEnabled = false
            alpha = 0.55f
        }
        actionArea.addView(exit, matchWidthParams(top = 0))
        actionArea.addView(extend, matchWidthParams(top = 12))

        countdown = object : CountDownTimer(8_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                extend.text = "Read for ${(millisUntilFinished / 1_000L) + 1} seconds"
            }

            override fun onFinish() {
                actionArea.removeView(extend)
                listOf(5, 10, 20).forEachIndexed { index, amount ->
                    actionArea.addView(
                        actionButton("Add $amount scrolls", filled = false).apply {
                            setOnClickListener { extendSessionBy(amount, app, hardMode) }
                        },
                        matchWidthParams(top = if (index == 0) 12 else 10),
                    )
                }
            }
        }.start()
        attachOverlay(root, app)
    }

    private fun showOverlay(
        app: SupportedApp,
        message: InterventionMessage,
        primaryLabel: String,
        onPrimary: () -> Unit,
        secondaryLabel: String? = null,
        onSecondary: (() -> Unit)? = null,
        tertiaryLabel: String? = null,
        onTertiary: (() -> Unit)? = null,
    ) {
        val root = createOverlayShell(message)
        val actionArea = root.getChildAt(root.childCount - 1) as LinearLayout
        actionArea.addView(
            actionButton(primaryLabel, filled = true).apply { setOnClickListener { onPrimary() } },
            matchWidthParams(top = 0),
        )
        if (secondaryLabel != null && onSecondary != null) {
            actionArea.addView(
                actionButton(secondaryLabel, filled = false).apply { setOnClickListener { onSecondary() } },
                matchWidthParams(top = 12),
            )
        }
        if (tertiaryLabel != null && onTertiary != null) {
            actionArea.addView(
                actionButton(tertiaryLabel, filled = false).apply { setOnClickListener { onTertiary() } },
                matchWidthParams(top = 12),
            )
        }
        attachOverlay(root, app)
    }

    private fun extendSessionBy(amount: Int, app: SupportedApp, hardMode: Boolean) {
        val now = System.currentTimeMillis()
        if (hardMode) {
            hardOverrideActive = true
            sessionApp = app
            sessionCount = 0
            sessionAllowance = amount
            lastShortSeenAt = now
            lastCountedScrollAt = now
            lastContentFingerprint = ShortFormDetector.detect(
                app,
                rootInActiveWindow,
            ).contentFingerprint.takeIf { it != 0 }
        } else {
            sessionAllowance += amount
        }
        suppressInterventionsUntil = now + EXIT_SETTLE_MS
        repository.recordExtension()
        dismissOverlay()
        Toast.makeText(
            this,
            "$amount more scrolls added. Your choice, made consciously.",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun createOverlayShell(message: InterventionMessage): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(52), dp(28), dp(32))
            setBackgroundColor(PAPER)
        }

        root.addView(TextView(this).apply {
            text = "BLOCK SHORT  ·  FEED PAUSED"
            textSize = 12f
            setTextColor(MOSS)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.12f
        }, matchWidthParams())

        root.addView(Space(this), LinearLayout.LayoutParams(1, 0, 1f))

        root.addView(TextView(this).apply {
            text = message.eyebrow
            textSize = 13f
            setTextColor(MOSS)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.1f
        }, matchWidthParams())

        root.addView(TextView(this).apply {
            text = message.title
            textSize = 34f
            setTextColor(INK)
            typeface = Typeface.create("sans-serif-serif", Typeface.BOLD)
            setLineSpacing(0f, 0.95f)
        }, matchWidthParams(top = 14))

        root.addView(TextView(this).apply {
            text = message.body
            textSize = 17f
            setTextColor(SOFT_INK)
            setLineSpacing(dp(5).toFloat(), 1f)
        }, matchWidthParams(top = 22))

        root.addView(TextView(this).apply {
            text = "Research note  ·  ${message.sourceLabel}"
            textSize = 12f
            setTextColor(MOSS)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedDrawable(SAGE, 16f)
        }, matchWidthParams(top = 24))

        root.addView(Space(this), LinearLayout.LayoutParams(1, 0, 1f))
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }, matchWidthParams())
        return root
    }

    private fun actionButton(label: String, filled: Boolean): Button = Button(this).apply {
        text = label
        textSize = 16f
        isAllCaps = false
        setTypeface(typeface, Typeface.BOLD)
        minHeight = dp(56)
        setTextColor(if (filled) PAPER else INK)
        background = roundedDrawable(if (filled) INK else SUNRISE, 18f)
        stateListAnimator = null
    }

    private fun attachOverlay(view: View, app: SupportedApp) {
        if (overlay != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        runCatching {
            windowManager.addView(view, params)
            overlay = view
            overlayOwner = app
            foregroundUnknownSince = 0L
            mainHandler.removeCallbacks(overlayScopeWatchdog)
            mainHandler.postDelayed(overlayScopeWatchdog, OVERLAY_SCOPE_CHECK_MS)
        }
    }

    private fun exitDetectedFeed() {
        feedExitedForIntervention = true
        // Covering another app does not pause its media. Leaving the detected
        // feed first reliably stops playback without changing global volume or
        // requesting broader media-control permissions.
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun exitApp() {
        repository.recordExit()
        suppressInterventionsUntil = System.currentTimeMillis() + EXIT_SETTLE_MS
        dismissOverlay()
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun returnToAppHome(app: SupportedApp) {
        repository.recordExit()
        suppressInterventionsUntil = System.currentTimeMillis() + EXIT_SETTLE_MS
        dismissOverlay()

        Handler(Looper.getMainLooper()).postDelayed({
            val homeTarget = findHomeTab(rootInActiveWindow, app)
            val clickedHome = if (homeTarget?.alreadySelected == true) {
                true
            } else {
                homeTarget?.node?.performAction(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK,
                ) == true
            }
            if (!clickedHome) performGlobalAction(GLOBAL_ACTION_BACK)
        }, APP_HOME_CLICK_DELAY_MS)
    }

    private data class AppHomeTarget(
        val node: android.view.accessibility.AccessibilityNodeInfo,
        val alreadySelected: Boolean,
    )

    private fun findHomeTab(
        root: android.view.accessibility.AccessibilityNodeInfo?,
        app: SupportedApp,
    ): AppHomeTarget? {
        root ?: return null
        val queue = java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited++ < MAX_HOME_SEARCH_NODES) {
            val node = queue.removeFirst()
            val text = node.text?.toString().orEmpty().trim()
            val description = node.contentDescription?.toString().orEmpty().trim()
            val viewId = node.viewIdResourceName.orEmpty().lowercase()
            val isHomeLabel = description.equals("home", ignoreCase = true) ||
                description.startsWith("home,", ignoreCase = true) ||
                (app != SupportedApp.INSTAGRAM && text.equals("home", ignoreCase = true))
            val isKnownHomeId = when (app) {
                SupportedApp.INSTAGRAM -> "tab" in viewId && "home" in viewId
                SupportedApp.YOUTUBE -> "home" in viewId
                SupportedApp.TIKTOK -> "home" in viewId
            }

            if (isHomeLabel || isKnownHomeId) {
                var clickable: android.view.accessibility.AccessibilityNodeInfo? = node
                var selected = nodeLooksSelected(node)
                var levels = 0
                while (clickable != null && !clickable.isClickable && levels++ < 4) {
                    clickable = clickable.parent
                    if (clickable != null) selected = selected || nodeLooksSelected(clickable)
                }
                if (clickable?.isClickable == true) return AppHomeTarget(clickable, selected)
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::addLast)
            }
        }
        return null
    }

    private fun nodeLooksSelected(node: android.view.accessibility.AccessibilityNodeInfo): Boolean =
        node.isSelected ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                node.stateDescription?.toString()?.contains("selected", ignoreCase = true) == true)

    private fun dismissOverlay() {
        mainHandler.removeCallbacks(delayedOverlayDismiss)
        removeOverlayNow()
    }

    private fun scheduleOverlayDismiss() {
        if (overlay == null) return
        mainHandler.removeCallbacks(delayedOverlayDismiss)
        mainHandler.postDelayed(delayedOverlayDismiss, OVERLAY_HIDE_DEBOUNCE_MS)
    }

    private fun removeOverlayNow() {
        mainHandler.removeCallbacks(overlayScopeWatchdog)
        countdown?.cancel()
        countdown = null
        overlay?.let { view -> runCatching { windowManager.removeView(view) } }
        overlay = null
        overlayOwner = null
        foregroundUnknownSince = 0L
        feedExitedForIntervention = false
    }

    private fun matchWidthParams(top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(top) }

    private fun roundedDrawable(color: Int, radiusDp: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp.toInt()).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onInterrupt() = dismissOverlay()

    override fun onDestroy() {
        dismissOverlay()
        settingsListener?.let(repository::unregisterConfigChangeListener)
        settingsListener = null
        if (connectedInstance.get() === this) connectedInstance.clear()
        super.onDestroy()
    }

    companion object {
        private const val SESSION_GAP_MS = 5 * 60 * 1000L
        private const val SCROLL_DEBOUNCE_MS = 650L
        private const val OVERLAY_HIDE_DEBOUNCE_MS = 700L
        private const val OVERLAY_SCOPE_CHECK_MS = 200L
        private const val UNKNOWN_FOREGROUND_GRACE_MS = 600L
        private const val EXIT_SETTLE_MS = 1_500L
        private const val APP_HOME_CLICK_DELAY_MS = 180L
        private const val MAX_HOME_SEARCH_NODES = 450
        private val INK = Color.rgb(17, 22, 16)
        private val SOFT_INK = Color.rgb(57, 66, 54)
        private val PAPER = Color.rgb(242, 240, 232)
        private val SAGE = Color.rgb(220, 226, 207)
        private val MOSS = Color.rgb(64, 91, 63)
        private val SUNRISE = Color.rgb(255, 180, 94)

        private var connectedInstance = WeakReference<StillAccessibilityService>(null)

        /**
         * Accessibility cannot be silently re-enabled later. This intentionally
         * disables the service so finance apps that reject enabled accessibility
         * services can be used; the user can re-enable it from Android Settings.
         */
        fun disableForSensitiveApps(): Boolean {
            val service = connectedInstance.get() ?: return false
            service.dismissOverlay()
            service.disableSelf()
            return true
        }
    }
}
