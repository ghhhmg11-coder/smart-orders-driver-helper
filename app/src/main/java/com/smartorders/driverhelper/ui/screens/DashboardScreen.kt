package com.smartorders.driverhelper.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.unit.sp
import com.smartorders.driverhelper.AppState
import com.smartorders.driverhelper.data.AppPreferences
import com.smartorders.driverhelper.service.FloatingOverlayService
import com.smartorders.driverhelper.ui.*

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val stats by AppState.stats.collectAsState()
    val autoAccept by AppState.autoAccept.collectAsState()
    val serviceConnected by AppState.serviceConnected.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Smart Orders",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Driver Helper",
            style = MaterialTheme.typography.titleSmall,
            color = OnSurfaceVariant
        )

        // Auto Accept Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (autoAccept) Color(0xFF1A3A1A) else Color(0xFF2A1A1A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Auto Accept",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (autoAccept) "● ACTIVE — Monitoring Jeeny" else "● INACTIVE — Paused",
                        color = if (autoAccept) GreenAccent else RedAccent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = autoAccept,
                    onCheckedChange = { enabled ->
                        AppState.setAutoAccept(enabled)
                        AppPreferences.setAutoAccept(context, enabled)
                        AppState.appendLog(if (enabled) "▶ Auto-accept ENABLED" else "⏸ Auto-accept DISABLED")
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GreenAccent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = RedAccent
                    )
                )
            }
        }

        // Service Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Accessibility,
                        contentDescription = null,
                        tint = if (serviceConnected) GreenAccent else RedAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Accessibility Service",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (serviceConnected) "Connected ✓" else "Not Connected",
                            color = if (serviceConnected) GreenAccent else RedAccent,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                // Floating overlay toggle
                FloatingOverlayToggle(context)
            }
        }

        // Stats Grid
        Text(
            text = "Today's Stats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Detected",
                value = stats.detected.toString(),
                icon = Icons.Default.Radar,
                color = AmberAccent
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Accepted",
                value = stats.accepted.toString(),
                icon = Icons.Default.CheckCircle,
                color = GreenAccent
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Rejected",
                value = stats.rejected.toString(),
                icon = Icons.Default.Cancel,
                color = RedAccent
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Total SAR",
                value = "﷼ %.2f".format(stats.totalSar),
                icon = Icons.Default.MonetizationOn,
                color = Purple60
            )
        }

        // Clear Stats Button
        OutlinedButton(
            onClick = {
                AppPreferences.clearTodayStats(context)
                AppState.resetStats()
                AppState.appendLog("🗑 Today stats cleared")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent),
            border = androidx.compose.foundation.BorderStroke(1.dp, RedAccent)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Clear Today Stats")
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun FloatingOverlayToggle(context: Context) {
    var overlayActive by remember { mutableStateOf(false) }
    IconButton(
        onClick = {
            if (!overlayActive) {
                context.startService(Intent(context, FloatingOverlayService::class.java))
                overlayActive = true
            } else {
                context.stopService(Intent(context, FloatingOverlayService::class.java))
                overlayActive = false
            }
        }
    ) {
        Icon(
            if (overlayActive) Icons.Default.LayersClear else Icons.Default.Layers,
            contentDescription = "Floating overlay",
            tint = if (overlayActive) GreenAccent else OnSurfaceVariant
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
}
