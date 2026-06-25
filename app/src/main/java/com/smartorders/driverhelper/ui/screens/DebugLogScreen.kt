package com.smartorders.driverhelper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartorders.driverhelper.AppState
import com.smartorders.driverhelper.ui.*
import kotlinx.coroutines.launch

@Composable
fun DebugLogScreen() {
    val d by AppState.debugInfo.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(d.logs.size) {
        if (d.logs.isNotEmpty()) listState.animateScrollToItem(d.logs.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Debug", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Row {
                IconButton(onClick = { scope.launch { if (d.logs.isNotEmpty()) listState.scrollToItem(d.logs.size - 1) } }) {
                    Icon(Icons.Default.ArrowDownward, null, tint = OnSurfaceVariant)
                }
                IconButton(onClick = { AppState.clearLogs() }) {
                    Icon(Icons.Default.Delete, null, tint = RedAccent)
                }
            }
        }

        // ── Service + Event Counters ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SRow("Service", if (d.serviceConnected) "Connected ✓" else "DISCONNECTED ✗",
                    if (d.serviceConnected) GreenAccent else RedAccent)
                SRow("All Events", d.totalEvents.toString(), OnSurface)
                SRow("External Events", "${d.externalEventCount} (non-own-app)", OnSurface)
            }
        }

        // ── External Package Tracking (KEY SECTION) ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (d.lastExternalPackage.contains("jeeny", ignoreCase = true))
                    Color(0xFF1A3A1A) else Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "External App Events",
                    color = Purple60,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                SRow(
                    "Last External Pkg",
                    d.lastExternalPackage.ifEmpty { "— none yet" },
                    if (d.lastExternalPackage.contains("jeeny", ignoreCase = true)) GreenAccent else AmberAccent
                )
                SRow("Last External Evt", d.lastExternalEvent.ifEmpty { "—" }, OnSurfaceVariant)
                // Recent packages list
                if (d.recentPackages.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text("Recent packages:", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    d.recentPackages.take(8).forEach { pkg ->
                        val isJeeny = pkg.contains("jeeny", ignoreCase = true)
                        Text(
                            "  • $pkg",
                            color = if (isJeeny) GreenAccent else OnSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (isJeeny) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // ── Jeeny-Specific Tracking ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (d.jeenyEventCount > 0) Color(0xFF1A2A1A) else Color(0xFF2A1A1A)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Jeeny Detection",
                    color = if (d.jeenyEventCount > 0) GreenAccent else RedAccent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                SRow("Jeeny Events", if (d.jeenyEventCount > 0) "${d.jeenyEventCount} ✓" else "0 — NOT seen yet",
                    if (d.jeenyEventCount > 0) GreenAccent else RedAccent)
                SRow("Last Jeeny Pkg", d.lastJeenyPackage.ifEmpty { "— not seen" },
                    if (d.lastJeenyPackage.isNotEmpty()) GreenAccent else OnSurfaceVariant)
                SRow("Last Jeeny Time", d.lastJeenyEventTime.ifEmpty { "—" }, OnSurfaceVariant)
                SRow("Trip Screen", if (d.jeenyDetected) "DETECTED ✓" else "NOT DETECTED",
                    if (d.jeenyDetected) GreenAccent else OnSurfaceVariant)
                SRow("Accept Button", if (d.acceptButtonFound) "FOUND ✓" else "NOT FOUND",
                    if (d.acceptButtonFound) GreenAccent else OnSurfaceVariant)
                if (d.clickMethod.isNotEmpty()) SRow("Click Method", d.clickMethod, AmberAccent)
                if (d.clickSuccess != null)
                    SRow("Click Result", if (d.clickSuccess == true) "SUCCESS ✓" else "FAILED ✗",
                        if (d.clickSuccess == true) GreenAccent else RedAccent)
            }
        }

        // ── Window Text (ALL windows) ──
        if (d.lastVisibleText.isNotEmpty() || d.lastJeenyText.isNotEmpty()) {
            val showText = if (d.lastJeenyText.isNotEmpty()) d.lastJeenyText else d.lastVisibleText
            val label = if (d.lastJeenyText.isNotEmpty()) "Last JEENY Window Text" else "Last Window Text"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A0F)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(label, color = AmberAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = showText.take(400),
                        color = Color(0xFFBBF7D0),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    )
                }
            }
        }

        // ── Log ──
        Text("Event Log (${d.logs.size})", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF060810), RoundedCornerShape(8.dp))
                .padding(8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            if (d.logs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No logs yet. Enable Accessibility Service, then open Jeeny.",
                            color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else {
                items(d.logs) { log ->
                    val color = when {
                        log.contains("JEENY") || log.contains("jeeny") || log.contains("🟢") -> GreenAccent
                        log.contains("✅") || log.contains("SUCCESS") -> Color(0xFF86EFAC)
                        log.contains("❌") || log.contains("FAIL") -> RedAccent
                        log.contains("⚠") || log.contains("WARN") -> AmberAccent
                        log.contains("📡") -> Purple60
                        log.contains("💰") || log.contains("﷼") -> Color(0xFFFCD34D)
                        log.contains("🎯") -> Color(0xFF7DD3FC)
                        else -> OnSurfaceVariant
                    }
                    Text(log, color = color, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.42f))
        Text(value, color = valueColor, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f),
            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}
