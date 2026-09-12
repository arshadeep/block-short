package com.still.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.ViewCompat
import com.still.app.data.SettingsRepository
import com.still.app.domain.InterventionMessages
import com.still.app.domain.SupportedApp
import java.lang.ref.WeakReference

class StillAccessibilityService : AccessibilityService() {
    private lateinit var repository: SettingsRepository
    private lateinit var windowManager: WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val overlayScopeWatchdog = object : Runnable {
        override fun run() {
            val owner = overlayOwner ?: return
            val config = repository.loadConfig()
            if (!config.enabled) {
                removeOverlayNow()
                return
            }

            val foregroundPackage = foregroundRoot()?.packageName?.toString()
            if (foregroundPackage == null) {
                val now = SystemClock.elapsedRealtime()
                if (foregroundUnknownSince == 0L) foregroundUnknownSince = now
                if (now - foregroundUnknownSince >= UNKNOWN_FOREGROUND_GRACE_MS) {
                    removeOverlayNow()
                    return
                }
            } else if (
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
    private var settingsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var suppressInterventionsUntil = 0L
    private var pendingHomeNavigation: Runnable? = null
    private val hardEntryGate = HardModeEntryGate()
    private val hardModeScan = object : Runnable {
        override fun run() {
            val config = repository.loadConfig()
            var delay = IDLE_SCAN_MS
            if (config.enabled) {
                val root = foregroundRoot(refresh = true)
                val app = root?.packageName?.toString()?.let(SupportedApp::fromPackage)
                if (app != null) {
                    delay = HARD_MODE_SCAN_MS
                    val now = SystemClock.elapsedRealtime()
                    if (overlay == null && now >= suppressInterventionsUntil) {
                        checkHardEntry(app, ShortFormDetector.detect(app, root), now)
                    } else hardEntryGate.reset()
                } else if (root != null) {
                    // Only a known different foreground app clears entry immediately.
                    hardEntryGate.reset()
                }
            } else hardEntryGate.reset()
            mainHandler.postDelayed(this, delay)
        }
    }

    private fun checkHardEntry(app: SupportedApp, result: DetectionResult, now: Long) {
        if (hardEntryGate.observe(app, result, now)) {
            hardEntryGate.reset()
            showHardIntervention(app)
        }
    }

    // A focusable accessibility overlay can itself become rootInActiveWindow.
    // Inspect the foreground window underneath it, never our own choice screen.
    private fun foregroundRoot(refresh: Boolean = false): AccessibilityNodeInfo? {
        // Polls must not repeatedly confirm a cached snapshot when events are absent.
        if (refresh && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) clearCache()
        val activeRoot = rootInActiveWindow
        if (refresh && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && activeRoot?.refresh() == false) return null
        if (activeRoot != null && activeRoot.packageName?.toString() != packageName) return activeRoot
        return windows.sortedByDescending { it.layer }
            .firstOrNull {
                it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
                    (it.isActive || it.isFocused)
            }?.root
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = SettingsRepository(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        connectedInstance = WeakReference(this)
        settingsListener = repository.registerConfigChangeListener { config ->
            mainHandler.post {
                hardEntryGate.reset()
                if (!config.enabled) {
                    pendingHomeNavigation?.let(mainHandler::removeCallbacks)
                    pendingHomeNavigation = null
                    dismissOverlay()
                }
            }
        }
        mainHandler.removeCallbacks(hardModeScan)
        mainHandler.post(hardModeScan)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (!::repository.isInitialized) return

        val packageName = event.packageName?.toString() ?: return
        val root = foregroundRoot()
        val foregroundPackage = root?.packageName?.toString()
        overlayOwner?.let { owner ->
            if (foregroundPackage != null && foregroundPackage !in owner.packageNames) dismissOverlay()
        }
        // Queued events may belong to a screen that has already been left.
        if (foregroundPackage != packageName) return
        val app = SupportedApp.fromPackage(packageName) ?: return
        val config = repository.loadConfig()
        if (!config.enabled) {
            dismissOverlay()
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now < suppressInterventionsUntil || overlay != null) return
        checkHardEntry(app, ShortFormDetector.detect(app, root), now)
    }

    private fun showHardIntervention(app: SupportedApp) {
        if (overlay != null || !repository.loadConfig().enabled) return
        exitDetectedFeed()
        val root = InterventionOverlayLayout(this, InterventionMessages.hard)
        root.addAction("Instagram home", filled = true) { returnToAppHome(app) }
        root.addAction("Phone home", filled = false, onClick = ::exitApp)
        attachOverlay(root, app)
    }

    private fun attachOverlay(view: View, app: SupportedApp) {
        if (overlay != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // NOT_FOCUSABLE affects key input, not touch. Keep the social app's
            // foreground identity available while buttons still receive touches.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        runCatching {
            windowManager.addView(view, params)
            ViewCompat.requestApplyInsets(view)
            overlay = view
            overlayOwner = app
            foregroundUnknownSince = 0L
            mainHandler.removeCallbacks(overlayScopeWatchdog)
            mainHandler.postDelayed(overlayScopeWatchdog, OVERLAY_SCOPE_CHECK_MS)
        }
    }

    private fun exitDetectedFeed() {
        // Covering another app does not pause its media. Leaving the detected
        // feed first reliably stops playback without changing global volume or
        // requesting broader media-control permissions.
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun exitApp() {
        repository.recordExit()
        suppressInterventionsUntil = SystemClock.elapsedRealtime() + EXIT_SETTLE_MS
        dismissOverlay()
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun returnToAppHome(app: SupportedApp) {
        repository.recordExit()
        suppressInterventionsUntil = SystemClock.elapsedRealtime() + EXIT_SETTLE_MS
        dismissOverlay()

        pendingHomeNavigation?.let(mainHandler::removeCallbacks)
        pendingHomeNavigation = Runnable {
            pendingHomeNavigation = null
            val root = foregroundRoot()
            // The user may have switched apps during the settling delay.
            if (!repository.loadConfig().enabled || root?.packageName?.toString() !in app.packageNames) return@Runnable
            val homeTarget = findHomeTab(root)
            val clickedHome = if (homeTarget?.alreadySelected == true) {
                true
            } else {
                homeTarget?.node?.performAction(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK,
                ) == true
            }
            if (!clickedHome) performGlobalAction(GLOBAL_ACTION_BACK)
        }.also { mainHandler.postDelayed(it, APP_HOME_CLICK_DELAY_MS) }
    }

    private data class AppHomeTarget(
        val node: android.view.accessibility.AccessibilityNodeInfo,
        val alreadySelected: Boolean,
    )

    private fun findHomeTab(
        root: android.view.accessibility.AccessibilityNodeInfo?,
    ): AppHomeTarget? {
        root ?: return null
        val queue = java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited++ < MAX_HOME_SEARCH_NODES) {
            val node = queue.removeFirst()
            val description = node.contentDescription?.toString().orEmpty().trim()
            val viewId = node.viewIdResourceName.orEmpty().lowercase()
            val isHomeLabel = description.equals("home", ignoreCase = true) ||
                description.startsWith("home,", ignoreCase = true)
            val isKnownHomeId = "tab" in viewId && "home" in viewId

            if (node.isVisibleToUser && (isHomeLabel || isKnownHomeId)) {
                var clickable: android.view.accessibility.AccessibilityNodeInfo? = node
                val selected = nodeLooksSelected(node)
                var levels = 0
                while (clickable != null && !clickable.isClickable && levels++ < 4) {
                    clickable = clickable.parent
                }
                if (clickable?.isClickable == true && clickable.isVisibleToUser) return AppHomeTarget(clickable, selected)
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
                ShortFormDetector.stateSaysSelected(node.stateDescription?.toString()))

    private fun dismissOverlay() {
        hardEntryGate.reset()
        removeOverlayNow()
    }

    private fun removeOverlayNow() {
        mainHandler.removeCallbacks(overlayScopeWatchdog)
        overlay?.let { view -> runCatching { windowManager.removeView(view) } }
        overlay = null
        overlayOwner = null
        foregroundUnknownSince = 0L
    }

    override fun onInterrupt() = dismissOverlay()

    override fun onDestroy() {
        mainHandler.removeCallbacks(hardModeScan)
        dismissOverlay()
        pendingHomeNavigation?.let(mainHandler::removeCallbacks)
        settingsListener?.let(repository::unregisterConfigChangeListener)
        settingsListener = null
        if (connectedInstance.get() === this) connectedInstance.clear()
        super.onDestroy()
    }

    companion object {
        private const val OVERLAY_SCOPE_CHECK_MS = 200L
        private const val UNKNOWN_FOREGROUND_GRACE_MS = 600L
        private const val EXIT_SETTLE_MS = 1_500L
        private const val APP_HOME_CLICK_DELAY_MS = 180L
        private const val HARD_MODE_SCAN_MS = 250L
        private const val IDLE_SCAN_MS = 1_000L
        private const val MAX_HOME_SEARCH_NODES = 450
        private var connectedInstance = WeakReference<StillAccessibilityService>(null)

        /**
         * Accessibility cannot be silently re-enabled later. This intentionally
         * disables the service so finance apps that reject enabled accessibility
         * services can be used; the user can re-enable it from Android Settings.
         */
        fun disableProtection(): Boolean {
            val service = connectedInstance.get() ?: return false
            service.mainHandler.removeCallbacks(service.hardModeScan)
            service.pendingHomeNavigation?.let(service.mainHandler::removeCallbacks)
            service.dismissOverlay()
            service.disableSelf()
            return true
        }
    }
}
