package com.smartorders.driverhelper.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartorders.driverhelper.AppState
import com.smartorders.driverhelper.LoginActivity
import com.smartorders.driverhelper.data.AppPreferences
import com.smartorders.driverhelper.ui.*

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings?", color = Color.White) },
            text = { Text("This will clear all rules and stats. You will stay logged in.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    AppPreferences.clearTodayStats(context)
                    AppPreferences.setMinPrice(context, 0f)
                    AppPreferences.setMaxPrice(context, 9999f)
                    AppPreferences.setMinPickupMinutes(context, 0f)
                    AppPreferences.setMaxPickupMinutes(context, 30f)
                    AppPreferences.setMaxPickupDistance(context, 10f)
                    AppState.resetStats()
                    AppState.appendLog("🔄 Settings reset to defaults")
                    showResetDialog = false
                }) { Text("Reset", color = RedAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel", color = OnSurfaceVariant) }
            },
            containerColor = Surface
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout?", color = Color.White) },
            text = { Text("You will be returned to the login screen.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    AppPreferences.setLoggedIn(context, false)
                    AppPreferences.setAutoAccept(context, false)
                    AppState.setAutoAccept(false)
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }) { Text("Logout", color = RedAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = OnSurfaceVariant) }
            },
            containerColor = Surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Permissions Section
        Text("Permissions", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
        SettingCard(
            title = "Accessibility Service",
            subtitle = "Required to read Jeeny trip screens",
            icon = Icons.Default.Accessibility,
            iconTint = Purple60,
            actionLabel = "Open Settings",
            onAction = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        )
        SettingCard(
            title = "Display Over Other Apps",
            subtitle = "Required for floating overlay button",
            icon = Icons.Default.Layers,
            iconTint = AmberAccent,
            actionLabel = "Open Settings",
            onAction = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
            }
        )

        HorizontalDivider(color = SurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

        // App Info Section
        Text("App Info", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryRow("App Name", "Smart Orders Driver Helper")
                SummaryRow("Version", "1.0.0")
                SummaryRow("Package", "com.smartorders.driverhelper")
                SummaryRow("Target App", "Jeeny Driver")
                SummaryRow("Min SDK", "26 (Android 8)")
            }
        }

        HorizontalDivider(color = SurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

        // Danger Zone
        Text("Danger Zone", style = MaterialTheme.typography.labelMedium, color = RedAccent)
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAccent),
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reset Settings")
        }
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1A1A))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Logout", color = RedAccent)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                    Text(subtitle, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onAction) {
                Text(actionLabel, color = Purple60, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
