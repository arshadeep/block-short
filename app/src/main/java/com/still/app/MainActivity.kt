package com.still.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.still.app.accessibility.StillAccessibilityService
import com.still.app.data.SettingsRepository
import com.still.app.ui.StillApp
import com.still.app.ui.theme.StillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.rgb(17, 22, 16)),
        )

        val repository = SettingsRepository(this)
        setContent {
            StillTheme {
                var refreshKey by remember { mutableIntStateOf(0) }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) refreshKey += 1
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                StillApp(
                    refreshKey = refreshKey,
                    initialConfig = repository.loadConfig(),
                    stats = repository.loadTodayStats(),
                    serviceEnabled = isStillServiceEnabled(this),
                    onConfigChanged = {
                        repository.saveConfig(it)
                        refreshKey += 1
                    },
                )
            }
        }
    }
}

private fun isStillServiceEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val expected = ComponentName(context, StillAccessibilityService::class.java)
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { info ->
            val component = ComponentName(
                info.resolveInfo.serviceInfo.packageName,
                info.resolveInfo.serviceInfo.name,
            )
            component == expected
        } || Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty().split(':').mapNotNull(ComponentName::unflattenFromString).contains(expected)
}
