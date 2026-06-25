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
import android.view.MotionEvent
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
            .setContentTitle("Smart Orders")
            .setContentText("Floating overlay active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    private fun getWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
    }

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

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
        }

        var offsetX = 0f
        var offsetY = 0f

        view.setContent {
            val autoAccept by AppState.autoAccept.collectAsState()
            SmartOrdersTheme {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .pointerInput(Unit) {
                            var totalDragX = 0f
                            var totalDragY = 0f
                            detectDragGestures(
                                onDragStart = {
                                    totalDragX = 0f
                                    totalDragY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y
                                    params.x = (params.x - dragAmount.x.toInt()).coerceAtLeast(0)
                                    params.y = (params.y + dragAmount.y.toInt()).coerceAtLeast(0)
                                    windowManager.updateViewLayout(view, params)
                                },
                                onDragEnd = {
                                    // if drag was small, treat as tap
                                    if (kotlin.math.abs(totalDragX) < 10 && kotlin.math.abs(totalDragY) < 10) {
                                        togglePanel(params)
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
                    Text(
                        text = if (autoAccept) "ON" else "OFF",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        floatingButtonView = view
        windowManager.addView(view, params)
    }

    private fun togglePanel(buttonParams: WindowManager.LayoutParams) {
        if (isPanelOpen) {
            closePanel()
        } else {
            openPanel(buttonParams)
        }
    }

    private fun openPanel(buttonParams: WindowManager.LayoutParams) {
        if (isPanelOpen) return
        isPanelOpen = true

        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = buttonParams.y + 70
        }

        val panel = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
        }

        panel.setContent {
            val autoAccept by AppState.autoAccept.collectAsState()
            SmartOrdersTheme {
                Card(
                    modifier = Modifier.width(200.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                        // Status indicator
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(color = SurfaceVariant)
                        // START button
                        Button(
                            onClick = {
                                AppState.setAutoAccept(true)
                                AppPreferences.setAutoAccept(this@FloatingOverlayService, true)
                                AppState.appendLog("▶ [Overlay] Auto-accept STARTED")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("START / تشغيل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        // STOP button
                        Button(
                            onClick = {
                                AppState.setAutoAccept(false)
                                AppPreferences.setAutoAccept(this@FloatingOverlayService, false)
                                AppState.appendLog("⏸ [Overlay] Auto-accept STOPPED")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("STOP / إيقاف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
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
