package com.smartorders.driverhelper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.smartorders.driverhelper.AppState
import com.smartorders.driverhelper.data.AppPreferences
import com.smartorders.driverhelper.data.parseTripInfo

class DriverHelperAccessibilityService : AccessibilityService() {

    companion object {
        // Live instance — used by FloatingOverlayService for force-tap
        @Volatile var instance: DriverHelperAccessibilityService? = null

        private val JEENY_PACKAGES = setOf("com.jeeny.driver", "com.jeeny.drivers")

        // Arabic markers that indicate the Jeeny trip offer sheet
        private val TRIP_MARKERS = listOf(
            "قبول العرض",
            "مشوار داخل المدينة",
            "مشوار",
            "استريح",
            "ستريح",
            "يبعد"
        )
        private const val ACCEPT_TEXT = "قبول العرض"
        private const val COOLDOWN_MS = 4000L
        private var lastClickTime = 0L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppState.setServiceConnected(true)
        AppState.appendLog("✅ Accessibility service connected — monitoring ALL windows")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val pkg = event.packageName?.toString() ?: ""
        val evtType = eventTypeName(event.eventType)
        val cls = event.className?.toString() ?: ""

        // Count every event and track last seen
        AppState.updateDebug {
            copy(
                totalEvents = totalEvents + 1,
                lastPackage = pkg,
                lastEventType = evtType,
                lastClassName = cls
            )
        }

        // ── Scan ALL open windows on every event (not just Jeeny events) ──
        // This is critical: the trip sheet may fire events under any package
        val windowDump = scanAllWindows()
        val allText = windowDump.text

        if (allText.isNotBlank()) {
            AppState.updateDebug { copy(lastVisibleText = allText.take(600)) }
        }

        // Detect Jeeny trip screen by Arabic text markers
        val hasJeenyScreen = isJeenyTripScreen(allText)
        AppState.updateDebug { copy(jeenyDetected = hasJeenyScreen) }

        // Log detailed window dump only when Jeeny screen detected (avoid spam)
        if (hasJeenyScreen) {
            AppState.appendLog("🟢 JEENY TRIP SCREEN DETECTED")
            AppState.appendLog("📦 Pkg=${pkg} evt=${evtType}")
            AppState.appendLog("📝 Text preview: ${allText.take(120)}")
        }

        if (!hasJeenyScreen) return

        // Auto-accept gate
        if (!AppPreferences.isAutoAccept(this)) {
            // Don't log repeatedly — only log once when first seen without auto-accept
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastClickTime < COOLDOWN_MS) return

        // Parse trip data
        val trip = parseTripInfo(allText)
        if (trip == null) {
            AppState.appendLog("⚠ Trip screen found but couldn't parse price/distance — raw: ${allText.take(80)}")
            // Still attempt click even without parsed data — don't bail
            lastClickTime = now
            attemptAcceptClick(0f, autoAccepted = false)
            return
        }

        AppState.appendLog("📋 Trip: ﷼${trip.price} | ${trip.pickupMinutes}min | ${trip.pickupDistanceKm}km")
        AppState.onTripDetected(trip.price)
        AppPreferences.incrementDetected(this)

        // Evaluate rules
        val minPrice   = AppPreferences.getMinPrice(this)
        val maxPrice   = AppPreferences.getMaxPrice(this)
        val minMin     = AppPreferences.getMinPickupMinutes(this)
        val maxMin     = AppPreferences.getMaxPickupMinutes(this)
        val maxDist    = AppPreferences.getMaxPickupDistance(this)

        val priceOk  = trip.price in minPrice..maxPrice
        val minOk    = trip.pickupMinutes in minMin..maxMin
        val distOk   = trip.pickupDistanceKm <= maxDist

        AppState.appendLog("📊 price=$priceOk min=$minOk dist=$distOk")

        if (!priceOk || !minOk || !distOk) {
            AppState.appendLog("❌ Rejected by rules")
            AppState.onTripRejected()
            AppPreferences.incrementRejected(this)
            return
        }

        AppState.appendLog("✅ Rules matched — clicking accept…")
        lastClickTime = now
        attemptAcceptClick(trip.price, autoAccepted = true)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Window scanning — reads ALL open windows, logs package + class + text
    // ─────────────────────────────────────────────────────────────────────────
    data class WindowDump(val text: String)

    private fun scanAllWindows(): WindowDump {
        val sb = StringBuilder()
        try {
            val wins: List<AccessibilityWindowInfo>? = windows
            if (!wins.isNullOrEmpty()) {
                for (win in wins) {
                    val root = win.root ?: continue
                    val winPkg = try { root.packageName?.toString() ?: "" } catch (_: Exception) { "" }
                    collectNodeText(root, sb)
                    root.recycle()
                }
            } else {
                // Fallback: rootInActiveWindow
                val root = rootInActiveWindow
                if (root != null) {
                    collectNodeText(root, sb)
                    root.recycle()
                }
            }
        } catch (e: Exception) {
            AppState.appendLog("⚠ scanAllWindows: ${e.message}")
        }
        return WindowDump(sb.toString().trim())
    }

    private fun collectNodeText(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int = 0) {
        if (depth > 40) return
        try {
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            if (!text.isNullOrBlank()) sb.append(text).append(" | ")
            if (!desc.isNullOrBlank() && desc != text) sb.append(desc).append(" | ")
            for (i in 0 until node.childCount) {
                val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                collectNodeText(child, sb, depth + 1)
                try { child.recycle() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Jeeny screen detection
    // ─────────────────────────────────────────────────────────────────────────
    private fun isJeenyTripScreen(text: String): Boolean {
        if (text.isBlank()) return false
        val hasAcceptBtn = text.contains(ACCEPT_TEXT)
        val hasMarker = TRIP_MARKERS.any { text.contains(it) }
        val hasPrice = text.contains("﷼") || text.contains("ر.س") ||
                Regex("""\d+[.,]\d+""").containsMatchIn(text)
        return (hasAcceptBtn || hasMarker) && hasPrice
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Click logic — node first, then gesture fallback automatically
    // ─────────────────────────────────────────────────────────────────────────
    fun attemptAcceptClick(price: Float, autoAccepted: Boolean = true) {
        val nodeClicked = tryNodeClick()
        if (nodeClicked) {
            AppState.appendLog("✅ Node click SUCCESS")
            AppState.updateDebug { copy(acceptButtonFound = true, clickMethod = "node click", clickSuccess = true) }
            if (autoAccepted) {
                AppState.onTripAccepted(price)
                AppPreferences.incrementAccepted(this)
                AppPreferences.addToTotalSar(this, price)
            }
        } else {
            AppState.appendLog("⚠ Node click failed → gesture fallback")
            AppState.updateDebug { copy(clickMethod = "gesture fallback") }
            performGestureTap(price, autoAccepted, yPercent = 0.85f)
        }
    }

    private fun tryNodeClick(): Boolean {
        try {
            // Try all windows first
            val wins = windows
            if (!wins.isNullOrEmpty()) {
                for (win in wins) {
                    val root = win.root ?: continue
                    val hit = findAndClickNode(root)
                    try { root.recycle() } catch (_: Exception) {}
                    if (hit) return true
                }
            }
            // Fallback
            val root = rootInActiveWindow ?: return false
            val hit = findAndClickNode(root)
            try { root.recycle() } catch (_: Exception) {}
            return hit
        } catch (e: Exception) {
            AppState.appendLog("⚠ tryNodeClick ex: ${e.message}")
            return false
        }
    }

    private fun findAndClickNode(root: AccessibilityNodeInfo): Boolean {
        // Exact text match for accept button
        val nodes = try { root.findAccessibilityNodeInfosByText(ACCEPT_TEXT) } catch (_: Exception) { null }
        if (!nodes.isNullOrEmpty()) {
            AppState.updateDebug { copy(acceptButtonFound = true) }
            for (node in nodes) {
                val clicked = clickNodeOrParent(node)
                try { node.recycle() } catch (_: Exception) {}
                if (clicked) return true
            }
        }
        // Also try recursive search for any clickable node with the accept text
        return findClickableRecursive(root, ACCEPT_TEXT)
    }

    private fun findClickableRecursive(node: AccessibilityNodeInfo, target: String, depth: Int = 0): Boolean {
        if (depth > 30) return false
        try {
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            if ((text.contains(target) || desc.contains(target))) {
                if (clickNodeOrParent(node)) return true
            }
            for (i in 0 until node.childCount) {
                val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                if (findClickableRecursive(child, target, depth + 1)) {
                    try { child.recycle() } catch (_: Exception) {}
                    return true
                }
                try { child.recycle() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return false
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return try { node.performAction(AccessibilityNodeInfo.ACTION_CLICK) } catch (_: Exception) { false }
        }
        var current: AccessibilityNodeInfo? = try { node.parent } catch (_: Exception) { null }
        repeat(6) {
            val c = current ?: return false
            if (c.isClickable) {
                val result = try { c.performAction(AccessibilityNodeInfo.ACTION_CLICK) } catch (_: Exception) { false }
                try { c.recycle() } catch (_: Exception) {}
                return result
            }
            val parent = try { c.parent } catch (_: Exception) { null }
            try { c.recycle() } catch (_: Exception) {}
            current = parent
        }
        try { current?.recycle() } catch (_: Exception) {}
        return false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gesture tap — called as fallback OR from Force Test button
    // ─────────────────────────────────────────────────────────────────────────
    fun performGestureTap(price: Float = 0f, recordStats: Boolean = false, yPercent: Float = 0.85f) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            AppState.appendLog("❌ Gestures require Android 7+")
            AppState.updateDebug { copy(clickSuccess = false) }
            return
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)

        val tapX = (dm.widthPixels / 2).toFloat()
        val tapY = dm.heightPixels * yPercent

        AppState.appendLog("🎯 Gesture tap → (${tapX.toInt()}, ${tapY.toInt()}) on ${dm.widthPixels}×${dm.heightPixels} [y=${(yPercent*100).toInt()}%]")

        val path = Path().apply { moveTo(tapX, tapY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 100L))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                AppState.appendLog("✅ Gesture COMPLETED")
                AppState.updateDebug { copy(clickSuccess = true, clickMethod = "gesture tap ${(yPercent*100).toInt()}%") }
                if (recordStats && price > 0f) {
                    AppState.onTripAccepted(price)
                    AppPreferences.incrementAccepted(this@DriverHelperAccessibilityService)
                    AppPreferences.addToTotalSar(this@DriverHelperAccessibilityService, price)
                }
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                AppState.appendLog("❌ Gesture CANCELLED — check overlay permission")
                AppState.updateDebug { copy(clickSuccess = false) }
            }
        }, mainHandler)
    }

    private fun eventTypeName(type: Int): String = when (type) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED  -> "WIN_STATE"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WIN_CONTENT"
        AccessibilityEvent.TYPE_WINDOWS_CHANGED        -> "WINS_CHANGED"
        AccessibilityEvent.TYPE_VIEW_CLICKED           -> "VIEW_CLICK"
        AccessibilityEvent.TYPE_VIEW_FOCUSED           -> "VIEW_FOCUS"
        else -> "TYPE_$type"
    }

    override fun onInterrupt() {
        AppState.setServiceConnected(false)
        AppState.appendLog("⚠ Service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        AppState.setServiceConnected(false)
        AppState.appendLog("❌ Service destroyed")
    }
}
