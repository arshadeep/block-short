package com.still.app.ui

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.still.app.MainActivity
import com.still.app.R
import com.still.app.accessibility.StillAccessibilityService
import com.still.app.domain.DailyStats
import com.still.app.domain.ProtectionConfig
import com.still.app.domain.ProtectionMode
import com.still.app.domain.ProtectionWindow
import com.still.app.domain.SupportedApp
import com.still.app.ui.theme.Coral
import com.still.app.ui.theme.Ink
import com.still.app.ui.theme.Moss
import com.still.app.ui.theme.Paper
import com.still.app.ui.theme.PaperMuted
import com.still.app.ui.theme.Sage
import com.still.app.ui.theme.Sunrise
import java.time.LocalTime

private enum class AppTab(val label: String, val icon: ImageVector) {
    PROTECT("Protect", Icons.Outlined.Security),
    INSIGHTS("Insights", Icons.Outlined.Analytics),
    RESEARCH("Research", Icons.AutoMirrored.Outlined.MenuBook),
}

@Composable
fun StillApp(
    refreshKey: Int,
    initialConfig: ProtectionConfig,
    stats: DailyStats,
    serviceEnabled: Boolean,
    onConfigChanged: (ProtectionConfig) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(AppTab.PROTECT) }
    var config by remember { mutableStateOf(initialConfig) }

    LaunchedEffect(refreshKey, initialConfig) { config = initialConfig }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(
                containerColor = Ink,
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink,
                            selectedTextColor = Sunrise,
                            indicatorColor = Sunrise,
                            unselectedIconColor = Sage,
                            unselectedTextColor = Sage,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            AppTab.PROTECT -> ProtectScreen(
                config = config,
                serviceEnabled = serviceEnabled,
                onConfigChanged = {
                    config = it
                    onConfigChanged(it)
                },
                modifier = Modifier.padding(padding),
            )
            AppTab.INSIGHTS -> InsightsScreen(stats, Modifier.padding(padding))
            AppTab.RESEARCH -> ResearchScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun ProtectScreen(
    config: ProtectionConfig,
    serviceEnabled: Boolean,
    onConfigChanged: (ProtectionConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val protectionActive = config.isActiveAt(LocalTime.now()) && serviceEnabled
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            postWakeUpDemoNotification(context)
        } else {
            Toast.makeText(
                context,
                "Notifications are off. Allow them to run the wake-up demo.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val triggerWakeUpDemo = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            postWakeUpDemoNotification(context)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(modifier = Modifier.statusBarsPadding()) {
                Eyebrow("BLOCK SHORT  /  PROTECTION")
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Keep the good.\nCut the loop.",
                    style = MaterialTheme.typography.displayLarge,
                    color = Paper,
                )
            }
        }

        item {
            StatusHero(
                active = protectionActive,
                enabled = config.enabled,
                mode = config.mode,
                onEnabledChange = { onConfigChanged(config.copy(enabled = it)) },
            )
        }

        item {
            WakeUpDemoCard(onTrigger = triggerWakeUpDemo)
        }

        if (!serviceEnabled) {
            item {
                SetupCard {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        } else {
            item {
                BankingCompatibilityCard {
                    val disabled = StillAccessibilityService.disableForSensitiveApps()
                    Toast.makeText(
                        context,
                        if (disabled) {
                            "Accessibility access turned off. Re-enable it here after banking."
                        } else {
                            "Turn off Block Short protection in Accessibility settings."
                        },
                        Toast.LENGTH_LONG,
                    ).show()
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }

        item { SectionTitle("Choose your boundary", "The useful parts of social stay available.") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeCard(
                    title = "Hard",
                    subtitle = "Short-form stays closed",
                    icon = Icons.Outlined.Lock,
                    selected = config.mode == ProtectionMode.HARD,
                    modifier = Modifier.weight(1f),
                ) { onConfigChanged(config.copy(mode = ProtectionMode.HARD)) }
                ModeCard(
                    title = "Soft",
                    subtitle = "Count, warn, then pause",
                    icon = Icons.Outlined.Tune,
                    selected = config.mode == ProtectionMode.SOFT,
                    modifier = Modifier.weight(1f),
                ) { onConfigChanged(config.copy(mode = ProtectionMode.SOFT)) }
            }
        }

        item {
            AnimatedVisibility(visible = config.mode == ProtectionMode.SOFT) {
                SoftLimitCard(config.sessionLimit) { limit ->
                    onConfigChanged(config.copy(sessionLimit = limit))
                }
            }
        }

        item { SectionTitle("When", "Choose any windows—or leave all off for all-day protection.") }

        item {
            SettingsCard {
                ProtectionWindow.entries.forEachIndexed { index, window ->
                    val (label, hours) = when (window) {
                        ProtectionWindow.MORNING -> "Morning" to "6am–12pm"
                        ProtectionWindow.AFTERNOON -> "Afternoon" to "12pm–6pm"
                        ProtectionWindow.NIGHT -> "Night" to "10pm–7am"
                    }
                    SettingSwitchRow(
                        title = label,
                        subtitle = hours,
                        checked = window in config.enabledWindows,
                        onCheckedChange = { checked ->
                            val windows = config.enabledWindows.toMutableSet().apply {
                                if (checked) add(window) else remove(window)
                            }
                            onConfigChanged(config.copy(enabledWindows = windows))
                        },
                    )
                    if (index != ProtectionWindow.entries.lastIndex) {
                        HorizontalDivider(
                            color = Ink.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                    }
                }
            }
        }

        item { SectionTitle("Feeds to quiet", "Messages and non-short-form screens remain available.") }

        item {
            SettingsCard {
                SupportedApp.entries.forEachIndexed { index, app ->
                    SettingSwitchRow(
                        title = app.displayName,
                        subtitle = when (app) {
                            SupportedApp.INSTAGRAM -> "Reels"
                            SupportedApp.YOUTUBE -> "Shorts"
                            SupportedApp.TIKTOK -> "For You / Following feed"
                        },
                        checked = app in config.enabledApps,
                        onCheckedChange = { checked ->
                            val apps = config.enabledApps.toMutableSet().apply {
                                if (checked) add(app) else remove(app)
                            }
                            onConfigChanged(config.copy(enabledApps = apps))
                        },
                    )
                    if (index != SupportedApp.entries.lastIndex) {
                        HorizontalDivider(color = Ink.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 14.dp))
                    }
                }
            }
        }

        item {
            SectionTitle(
                "Adaptive limits",
                "A preview of signals that could tune your scroll allowance automatically.",
            )
        }

        item {
            SettingsCard {
                listOf(
                    "Fitbit" to "Sleep and recovery signals · Coming soon",
                    "Smartwatch" to "Sleep, activity and bedtime · Coming soon",
                    "Calendar" to "Upcoming commitments · Coming soon",
                ).forEachIndexed { index, (title, subtitle) ->
                    SettingSwitchRow(
                        title = title,
                        subtitle = subtitle,
                        checked = false,
                        enabled = false,
                        onCheckedChange = {},
                    )
                    if (index != 2) {
                        HorizontalDivider(
                            color = Ink.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Surface(color = Sage, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "Future version: fewer scrolls before an early morning, more flexibility after strong sleep.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Moss,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }

        item {
            SectionTitle(
                "Message persona",
                "Coming soon: choose how Block Short interrupts the spiral.",
            )
        }

        item {
            SettingsCard {
                Eyebrow("PREVIEW ONLY", Moss)
                Spacer(Modifier.height(8.dp))
                Text("How should we call you out?", style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Gentle", "Coach", "Chaotic").forEach { persona ->
                        Surface(
                            color = if (persona == "Chaotic") Sunrise else PaperMuted,
                            border = BorderStroke(
                                1.dp,
                                if (persona == "Chaotic") Ink else Ink.copy(alpha = 0.12f),
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                persona,
                                style = MaterialTheme.typography.labelLarge,
                                color = Ink.copy(alpha = if (persona == "Chaotic") 1f else 0.55f),
                                modifier = Modifier.padding(vertical = 11.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "V0 uses Chaotic: irreverent, research-backed, and never cruel. Persona controls are not active yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Moss,
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Sage.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null, tint = Sage)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Private by default. Detection and counters stay on this phone. Block Short never stores message text or feed content.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sage,
                )
            }
        }
    }
}

@Composable
private fun WakeUpDemoCard(onTrigger: () -> Unit) {
    Surface(color = Coral, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = Ink, shape = CircleShape) {
                Icon(
                    Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = Sunrise,
                    modifier = Modifier.padding(12.dp).size(26.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("DEMO TOOL", Ink.copy(alpha = 0.65f))
                Spacer(Modifier.height(4.dp))
                Text("Wake-up notification", style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sends now. Automatic wake-up timing is coming later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.copy(alpha = 0.72f),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onTrigger,
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Trigger notification")
                }
            }
        }
    }
}

private fun postWakeUpDemoNotification(context: android.content.Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
        NotificationChannel(
            WAKE_UP_CHANNEL_ID,
            "Wake-up check-ins",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Morning reminders to choose an intentional short-form limit"
        },
    )

    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val message = "Your thumb slept all night. Let’s not wake it with an infinite feed. Pick today’s boundary before the algorithm picks for you."
    val notification = Notification.Builder(context, WAKE_UP_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("Morning, main character.")
        .setContentText(message)
        .setStyle(Notification.BigTextStyle().bigText(message))
        .setCategory(Notification.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(openApp)
        .build()

    manager.notify(WAKE_UP_NOTIFICATION_ID, notification)
    Toast.makeText(context, "Wake-up demo sent.", Toast.LENGTH_SHORT).show()
}

private const val WAKE_UP_CHANNEL_ID = "wake_up_check_ins"
private const val WAKE_UP_NOTIFICATION_ID = 2401

@Composable
private fun StatusHero(
    active: Boolean,
    enabled: Boolean,
    mode: ProtectionMode,
    onEnabledChange: (Boolean) -> Unit,
) {
    val rotation by animateFloatAsState(if (active) 0f else 24f, label = "status")
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = if (active) Sunrise else PaperMuted,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .rotate(rotation)
                    .background(Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (active) Icons.Outlined.CheckCircle else Icons.Outlined.Bedtime,
                    contentDescription = null,
                    tint = if (active) Sunrise else Paper,
                    modifier = Modifier.size(29.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow(if (active) "ACTIVE NOW" else "CURRENTLY RESTING", Ink.copy(alpha = 0.65f))
                Spacer(Modifier.height(5.dp))
                Text(
                    if (!enabled) "Protection is off" else "${mode.name.lowercase().replaceFirstChar { it.uppercase() }} mode",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
            }
            StillSwitch(enabled, onEnabledChange)
        }
    }
}

@Composable
private fun SetupCard(onSetup: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Coral),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(Modifier.padding(22.dp)) {
            Eyebrow("ONE-TIME SETUP", Ink.copy(alpha = 0.7f))
            Spacer(Modifier.height(10.dp))
            Text("Let Block Short notice short-form feeds", style = MaterialTheme.typography.headlineMedium, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text(
                "With Accessibility access, Block Short reads on-screen labels and scroll events in Instagram, YouTube, and TikTok to identify short-form feeds and enforce your limits. Processing stays on this phone; labels and messages are never saved or shared.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onSetup,
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("I understand — open settings") }
        }
    }
}

@Composable
private fun BankingCompatibilityCard(onDisable: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperMuted),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(Modifier.padding(22.dp)) {
            Eyebrow("BANKING / UPI COMPATIBILITY", Moss)
            Spacer(Modifier.height(10.dp))
            Text(
                "A finance app refusing to open?",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Some finance apps reject every enabled accessibility service, even though Block Short is limited to supported social apps. Turn off its system access before banking, then re-enable it afterward. The protection switch above does not disable system access.",
                style = MaterialTheme.typography.bodyMedium,
                color = Moss,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onDisable,
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Turn off access for banking") }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) Sage else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Ink else Paper,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (selected) Sage else MaterialTheme.colorScheme.outline),
        modifier = modifier
            .height(166.dp)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.weight(1f))
                if (selected) Box(Modifier.size(10.dp).background(Moss, CircleShape))
            }
            Column {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(5.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (selected) Moss else Sage)
            }
        }
    }
}

@Composable
private fun SoftLimitCard(limit: Int, onLimitChange: (Int) -> Unit) {
    SettingsCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Eyebrow("SESSION LIMIT", Moss)
                Text("Swipes before pause", style = MaterialTheme.typography.titleLarge, color = Ink)
            }
            Text("$limit", style = MaterialTheme.typography.headlineLarge, color = Moss)
        }
        Spacer(Modifier.height(16.dp))
        Slider(
            value = limit.toFloat(),
            onValueChange = { onLimitChange((it / 5).toInt() * 5) },
            valueRange = 5f..30f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = Ink,
                activeTrackColor = Moss,
                inactiveTrackColor = Sage,
                activeTickColor = Paper,
                inactiveTickColor = Moss,
            ),
        )
        Text("Choose 5, 10, or 20 more after an 8-second pause.", style = MaterialTheme.typography.bodyMedium, color = Moss)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Paper, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = Ink.copy(alpha = if (enabled) 1f else 0.6f),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Moss.copy(alpha = if (enabled) 1f else 0.7f),
            )
        }
        StillSwitch(checked, onCheckedChange, enabled)
    }
}

@Composable
private fun StillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Paper,
            checkedTrackColor = Moss,
            uncheckedThumbColor = Moss,
            uncheckedTrackColor = PaperMuted,
            uncheckedBorderColor = Moss,
        ),
    )
}

@Composable
private fun InsightsScreen(stats: DailyStats, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 24.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Eyebrow("TODAY  /  ${stats.date}")
                Spacer(Modifier.height(18.dp))
                Text("A quieter feed,\nmeasured lightly.", style = MaterialTheme.typography.displayLarge, color = Paper)
            }
        }
        item {
            Surface(color = Sage, shape = RoundedCornerShape(30.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column {
                            Eyebrow("PROTECTED-DAY STREAK", Moss)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${stats.currentStreak}",
                                style = MaterialTheme.typography.displayLarge,
                                color = Ink,
                            )
                            Text(
                                if (stats.currentStreak == 1) "day" else "days",
                                style = MaterialTheme.typography.titleLarge,
                                color = Moss,
                            )
                        }
                        Surface(color = Ink, shape = CircleShape) {
                            Text(
                                "✦",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Sunrise,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        when {
                            stats.protectedToday -> "Today is protected. Tiny boundary, real win."
                            stats.currentStreak > 0 -> "Choose an exit today to keep your streak going."
                            else -> "Choose app home or phone home at a paused feed to begin."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Ink,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Best: ${stats.bestStreak} days · Extensions never erase progress.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Moss,
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            val shareText = "Small win: I’m on a ${stats.currentStreak}-day " +
                                "Block Short streak ✦\n\nI’ve been choosing to leave short-form " +
                                "feeds when they start looping. Join me?"
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "My Block Short streak")
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    },
                                    "Share your streak",
                                ),
                            )
                        },
                        enabled = stats.currentStreak > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Ink,
                            contentColor = Paper,
                            disabledContainerColor = Ink.copy(alpha = 0.35f),
                            disabledContentColor = Paper.copy(alpha = 0.7f),
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.width(9.dp))
                        Text(if (stats.currentStreak > 0) "Share my streak" else "Start a streak to share")
                    }
                }
            }
        }
        item {
            Surface(color = Sunrise, shape = RoundedCornerShape(30.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Eyebrow("ESTIMATED TIME PROTECTED", Ink.copy(alpha = 0.65f))
                    Spacer(Modifier.height(8.dp))
                    Text("${stats.estimatedMinutesSaved} min", style = MaterialTheme.typography.displayLarge, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("A conservative estimate, not a clinical measure.", style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.7f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("${stats.scrollsSeen}", "feed swipes", Modifier.weight(1f))
                MetricCard("${stats.blocksShown}", "pauses shown", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("${stats.exitsChosen}", "exits chosen", Modifier.weight(1f))
                MetricCard("${stats.extensionsChosen}", "extensions", Modifier.weight(1f))
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp),
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = Sage)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Sleep impact comes later", style = MaterialTheme.typography.titleLarge, color = Paper)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "V0 tracks behaviour only. Sleep estimates need health or wearable data and will never be presented as a diagnosis.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Sage,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier) {
    Surface(color = Paper, shape = RoundedCornerShape(24.dp), modifier = modifier.aspectRatio(1.05f)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(value, style = MaterialTheme.typography.displayLarge, color = Ink)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Moss)
        }
    }
}

private data class ResearchItem(
    val tag: String,
    val title: String,
    val takeaway: String,
    val url: String,
)

private val researchItems = listOf(
    ResearchItem(
        "SYSTEMATIC REVIEW · 2023",
        "Digital media use and sleep in ages 16–25",
        "Across moderate- and high-quality observational studies, digital media use was associated with shorter sleep and poorer sleep quality; night-time use was especially relevant.",
        "https://pubmed.ncbi.nlm.nih.gov/36638702/",
    ),
    ResearchItem(
        "THREE STUDIES · 2023",
        "Doomscrolling, distress and wellbeing",
        "Doomscrolling was associated with psychological distress, social-media addiction, fear of missing out and lower wellbeing. This is association, not proof of causation.",
        "https://pubmed.ncbi.nlm.nih.gov/36275044/",
    ),
    ResearchItem(
        "LAB STUDY · 2021",
        "Pre-sleep social media and sleep",
        "Thirty minutes of use had little direct effect after blue light was controlled. The clearer risk was bedtime displacement—staying awake to keep using media.",
        "https://pubmed.ncbi.nlm.nih.gov/34627122/",
    ),
    ResearchItem(
        "RANDOMISED TRIAL · 2026",
        "Short-video intervention and sleep quality",
        "In 60 college students with heavy short-video use and poor sleep, a multi-part intervention reduced use and improved sleep scores. The intervention included more than time limits alone.",
        "https://pubmed.ncbi.nlm.nih.gov/41743515/",
    ),
    ResearchItem(
        "MEDIATION STUDY · 2026",
        "FOMO, short-video addiction and sleep",
        "Among 553 college students, short-video addiction was associated with poorer self-reported sleep both directly and through FOMO. Loss of control was linked with difficulty falling asleep. The cross-sectional design cannot prove causation.",
        "https://pubmed.ncbi.nlm.nih.gov/42058094/",
    ),
    ResearchItem(
        "MEDIATION STUDY · 2024",
        "Doomscrolling and being pulled out of the present",
        "In 400 adults, lower mindfulness and greater secondary traumatic stress statistically explained the relationship between doomscrolling and lower mental wellbeing. This is observational evidence.",
        "https://pubmed.ncbi.nlm.nih.gov/38429976/",
    ),
)

@Composable
private fun ResearchScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 24.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Eyebrow("RESEARCH  /  NO FEAR-MONGERING")
                Spacer(Modifier.height(18.dp))
                Text("Evidence, with\nthe volume down.", style = MaterialTheme.typography.displayLarge, color = Paper)
                Spacer(Modifier.height(14.dp))
                Text(
                    "Block Short’s messages are deliberately careful. Social media is not uniformly harmful, and most studies cannot establish cause and effect.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Sage,
                )
            }
        }
        items(researchItems) { item ->
            ResearchCard(item) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            }
        }
        item {
            Text(
                "Block Short is a wellbeing tool, not medical advice. If sleep or mental health problems persist, talk with a qualified professional.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sage,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun ResearchCard(item: ResearchItem, onClick: () -> Unit) {
    Surface(
        color = Paper,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(21.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(item.tag, Moss, Modifier.weight(1f))
                Icon(Icons.Outlined.ArrowOutward, contentDescription = "Open source", tint = Moss)
            }
            Spacer(Modifier.height(12.dp))
            Text(item.title, style = MaterialTheme.typography.headlineMedium, color = Ink)
            Spacer(Modifier.height(11.dp))
            Text(item.takeaway, style = MaterialTheme.typography.bodyMedium, color = Moss)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Paper)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Sage)
    }
}

@Composable
private fun Eyebrow(text: String, color: Color = Sunrise, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}
