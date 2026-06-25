package com.smartorders.driverhelper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    val debugInfo by AppState.debugInfo.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when logs update
    LaunchedEffect(debugInfo.logs.size) {
        if (debugInfo.logs.isNotEmpty()) {
            listState.animateScrollToItem(debugInfo.logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Debug Log",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (debugInfo.logs.isNotEmpty())
                                listState.scrollToItem(debugInfo.logs.size - 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll to bottom", tint = OnSurfaceVariant)
                }
                IconButton(onClick = { AppState.clearLogs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear logs", tint = RedAccent)
                }
            }
        }

        // Status Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusRow("Service", if (debugInfo.serviceConnected) "Connected ✓" else "Disconnected ✗", if (debugInfo.serviceConnected) GreenAccent else RedAccent)
                StatusRow("Total Events", debugInfo.totalEvents.toString(), OnSurface)
                StatusRow("Last Package", debugInfo.lastPackage.ifEmpty { "—" }, OnSurfaceVariant)
                StatusRow("Last Event", debugInfo.lastEventType.ifEmpty { "—" }, OnSurfaceVariant)
                StatusRow("Last Class", debugInfo.lastClassName.ifEmpty { "—" }, OnSurfaceVariant)
                HorizontalDivider(color = SurfaceVariant)
                StatusRow("Jeeny Detected", if (debugInfo.jeenyDetected) "YES ✓" else "NO", if (debugInfo.jeenyDetected) GreenAccent else OnSurfaceVariant)
                StatusRow("Accept Button", if (debugInfo.acceptButtonFound) "FOUND ✓" else "NOT FOUND", if (debugInfo.acceptButtonFound) GreenAccent else OnSurfaceVariant)
                if (debugInfo.clickMethod.isNotEmpty()) {
                    StatusRow("Click Method", debugInfo.clickMethod, AmberAccent)
                }
                if (debugInfo.clickSuccess != null) {
                    StatusRow("Click Result", if (debugInfo.clickSuccess == true) "SUCCESS ✓" else "FAILED ✗",
                        if (debugInfo.clickSuccess == true) GreenAccent else RedAccent)
                }
            }
        }

        // Last visible text
        if (debugInfo.lastVisibleText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Last Screen Text", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = debugInfo.lastVisibleText.take(200),
                        color = AmberAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Log List
        Text(
            text = "Event Log (${debugInfo.logs.size})",
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0A0A14), RoundedCornerShape(8.dp))
                .padding(8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (debugInfo.logs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No logs yet. Enable the Accessibility Service to start monitoring.",
                            color = OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                items(debugInfo.logs) { log ->
                    val color = when {
                        log.contains("✅") || log.contains("SUCCESS") || log.contains("accepted") -> GreenAccent
                        log.contains("❌") || log.contains("FAILED") || log.contains("error") -> RedAccent
                        log.contains("⚠") || log.contains("WARN") -> AmberAccent
                        log.contains("🔍") || log.contains("Jeeny") || log.contains("قبول") -> Purple60
                        else -> OnSurfaceVariant
                    }
                    Text(
                        text = log,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.55f)
        )
    }
}
