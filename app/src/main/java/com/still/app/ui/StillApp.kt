package com.still.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.still.app.accessibility.StillAccessibilityService
import com.still.app.domain.DailyStats
import com.still.app.domain.ProtectionConfig
import com.still.app.ui.theme.Ink
import com.still.app.ui.theme.Moss
import com.still.app.ui.theme.Paper
import com.still.app.ui.theme.PaperMuted
import com.still.app.ui.theme.Sage

@Composable
fun StillApp(
    initialConfig: ProtectionConfig,
    stats: DailyStats,
    serviceEnabled: Boolean,
    onConfigChanged: (ProtectionConfig) -> Unit,
) {
    val context = LocalContext.current
    var showDisclosure by rememberSaveable { mutableStateOf(false) }
    val hardModeOn = initialConfig.enabled && serviceEnabled

    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Column(
            modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column {
                Text("Block Short", style = MaterialTheme.typography.headlineMedium, color = Paper)
                Spacer(Modifier.height(6.dp))
                Text("Instagram Reels", style = MaterialTheme.typography.bodyMedium, color = Sage)
            }
            Surface(color = Sage, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("YOUR STREAK", style = MaterialTheme.typography.labelSmall, color = Moss)
                    Spacer(Modifier.height(20.dp))
                    Text("${stats.currentStreak}", style = MaterialTheme.typography.displayLarge, color = Ink)
                    Text(if (stats.currentStreak == 1) "day" else "days", style = MaterialTheme.typography.titleLarge, color = Moss)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        when {
                            stats.protectedToday -> "Today counts. See you tomorrow."
                            stats.currentStreak > 0 -> "Leave Reels today to keep your streak."
                            else -> "Choose an exit when Reels is blocked to start your streak."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Ink,
                    )
                }
            }
            Surface(color = Paper, shape = RoundedCornerShape(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().toggleable(
                        value = hardModeOn,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            if (enabled && !serviceEnabled) {
                                showDisclosure = true
                            } else {
                                onConfigChanged(ProtectionConfig(enabled))
                                if (!enabled) StillAccessibilityService.disableProtection()
                            }
                        },
                    ).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 16.dp)) {
                        Text("Block Reels", style = MaterialTheme.typography.titleLarge, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (hardModeOn) "On · Reels are blocked" else "Off · Reels are available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Moss,
                        )
                    }
                    Switch(
                        checked = hardModeOn,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Paper,
                            checkedTrackColor = Moss,
                            uncheckedThumbColor = Moss,
                            uncheckedTrackColor = PaperMuted,
                            uncheckedBorderColor = Moss,
                        ),
                    )
                }
            }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Enable Instagram protection") },
            text = {
                Text(
                    "Block Short uses accessibility access to read Instagram's on-screen labels and detect Reels. " +
                        "It exits Reels and shows a blocking screen. Messages and viewed content are never saved or sent. " +
                        "Your streak stays on this device.\n\n" +
                        "In Android settings, enable Block Short protection.\n\n" +
                        "If Android shows Restricted setting, first go to Settings > Apps > Block Short > " +
                        "the three-dot menu > Allow restricted settings. Then return to Accessibility " +
                        "and enable Block Short protection.\n\n" +
                        "Turning Block Reels off also turns off this access.",
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    onConfigChanged(ProtectionConfig(enabled = true))
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("I understand · Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) { Text("Cancel") }
            },
        )
    }
}
