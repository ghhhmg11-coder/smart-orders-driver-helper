package com.smartorders.driverhelper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.smartorders.driverhelper.AppState
import com.smartorders.driverhelper.data.AppPreferences
import com.smartorders.driverhelper.ui.*

class FloatingOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var floatingButtonView: ComposeView? = null
    private var panelView: ComposeView? = null
    private var isPanelOpen = false
    private var buttonParams: WindowManager.LayoutParams? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val CHANNEL_ID = "smart_orders_overlay"
        private const val NOTIF_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        showFloatingButton()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Smart Orders Overlay",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Orders Active")
            .setContentText("Monitoring Jeeny trips")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    private fun getWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    private fun showFloatingButton() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 300
        }
        buttonParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
        }

        view.setContent {
            val autoAccept by AppState.autoAccept.collectAsState()
            SmartOrdersTheme {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .pointerInput(Unit) {
                            var totalDragX = 0f
                            var totalDragY = 0f
                            detectDragGestures(
                                onDragStart = { totalDragX = 0f; totalDragY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y
                                    params.x = (params.x - dragAmount.x.toInt()).coerceAtLeast(0)
                                    params.y = (params.y + dragAmount.y.toInt()).coerceAtLeast(0)
                                    try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
                                },
                                onDragEnd = {
                                    if (kotlin.math.abs(totalDragX) < 12 && kotlin.math.abs(totalDragY) < 12) {
                                        togglePanel()
                                    }
                                }
                            )
                        }
                        .background(
                            color = if (autoAccept) Color(0xFF6B21A8) else Color(0xFF374151),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (autoAccept) "ON" else "OFF",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (autoAccept) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                        }
                    }
                }
            }
        }

        floatingButtonView = view
        windowManager.addView(view, params)
    }

    private fun togglePanel() {
        if (isPanelOpen) closePanel() else openPanel()
    }

    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true

        val bp = buttonParams ?: return
        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = bp.y + 72
        }

        val panel = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
        }

        panel.setContent {
            val autoAccept by AppState.autoAccept.collectAsState()
            SmartOrdersTheme {
                Card(
                    modifier = Modifier.width(220.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Smart Orders",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = { closePanel() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Status dot
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (autoAccept) GreenAccent else RedAccent,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (autoAccept) "ACTIVE" else "INACTIVE",
                                color = if (autoAccept) GreenAccent else RedAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = SurfaceVariant)

                        // START
                        Button(
                            onClick = {
                                AppState.setAutoAccept(true)
                                AppPreferences.setAutoAccept(this@FloatingOverlayService, true)
                                AppState.appendLog("▶ [Overlay] Auto-accept STARTED")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("START / تشغيل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        // STOP
                        Button(
                            onClick = {
                                AppState.setAutoAccept(false)
                                AppPreferences.setAutoAccept(this@FloatingOverlayService, false)
                                AppState.appendLog("⏸ [Overlay] Auto-accept STOPPED")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("STOP / إيقاف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = SurfaceVariant)

                        // FORCE ACCEPT TEST — taps 85% screen height coordinate
                        Button(
                            onClick = {
                                val svc = DriverHelperAccessibilityService.instance
                                if (svc != null) {
                                    AppState.appendLog("🔴 FORCE ACCEPT TEST — gesture at 85%")
                                    svc.performGestureTap(price = 0f, recordStats = false, yPercent = 0.85f)
                                    closePanel()
                                } else {
                                    AppState.appendLog("❌ FORCE TEST: Accessibility Service not connected!")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("FORCE ACCEPT TEST", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Show service status for quick diagnosis
                        val svcConnected by AppState.serviceConnected.collectAsState()
                        Text(
                            text = if (svcConnected) "● Service: connected" else "● Service: DISCONNECTED",
                            color = if (svcConnected) GreenAccent else RedAccent,
                            fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        panelView = panel
        windowManager.addView(panel, panelParams)
    }

    private fun closePanel() {
        isPanelOpen = false
        panelView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        panelView = null
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        closePanel()
        floatingButtonView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
