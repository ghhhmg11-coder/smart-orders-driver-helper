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
        @Volatile var instance: DriverHelperAccessibilityService? = null

        private const val OWN_PKG = "com.smartorders.driverhelper"
        private val JEENY_PACKAGES = setOf("com.jeeny.driver", "com.jeeny.drivers")
        private const val ACCEPT_TEXT = "قبول العرض"
        private val TRIP_MARKERS = listOf(
            "قبول العرض", "مشوار داخل المدينة", "مشوار", "استريح", "ستريح", "يبعد"
        )
        private const val COOLDOWN_MS = 4000L
        @Volatile private var lastClickTime = 0L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppState.setServiceConnected(true)
        AppState.appendLog("✅ Service ON — listening to ALL packages (typeAllMask)")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val pkg = event.packageName?.toString() ?: ""
        val evtType = eventTypeName(event.eventType)
        val cls = event.className?.toString() ?: ""

        // ── 1. Track in AppState — own-pkg filtering done inside AppState ──
        AppState.onAccessibilityEvent(pkg, evtType, cls)

        // ── 2. If this is a Jeeny event, log it prominently ──
        val isJeeny = JEENY_PACKAGES.contains(pkg) || pkg.contains("jeeny", ignoreCase = true)
        if (isJeeny) {
            AppState.appendLog("📡 JEENY EVENT: $evtType | cls=$cls")
        }

        // ── 3. On EVERY event, scan ALL open windows for Jeeny trip content ──
        //    (The trip sheet can be visible even if the triggering event is from another pkg)
        val allText = collectAllWindowsText()
        if (allText.isNotBlank()) {
            AppState.updateWindowText(allText)
        }

        val jeenyDetected = isJeenyTripScreen(allText)
        AppState.updateJeenyDetected(jeenyDetected, if (jeenyDetected) allText else "")

        if (!jeenyDetected) return

        // ── 4. Trip screen detected ──
        AppState.appendLog("🟢 TRIP SCREEN DETECTED | pkg=$pkg")
        AppState.appendLog("   text: ${allText.take(100)}")

        if (!AppPreferences.isAutoAccept(this)) return

        val now = System.currentTimeMillis()
        if (now - lastClickTime < COOLDOWN_MS) return
        lastClickTime = now

        val trip = parseTripInfo(allText)
        if (trip != null) {
            AppState.appendLog("💰 Trip: ﷼${trip.price} | ${trip.pickupMinutes}min | ${trip.pickupDistanceKm}km")
            AppState.onTripDetected(trip.price)
            AppPreferences.incrementDetected(this)

            val priceOk = trip.price in AppPreferences.getMinPrice(this)..AppPreferences.getMaxPrice(this)
            val minOk   = trip.pickupMinutes in AppPreferences.getMinPickupMinutes(this)..AppPreferences.getMaxPickupMinutes(this)
            val distOk  = trip.pickupDistanceKm <= AppPreferences.getMaxPickupDistance(this)

            if (!priceOk || !minOk || !distOk) {
                AppState.appendLog("❌ Rejected: price=$priceOk min=$minOk dist=$distOk")
                AppState.onTripRejected()
                AppPreferences.incrementRejected(this)
                return
            }
            AppState.appendLog("✅ Rules OK — clicking…")
            attemptAcceptClick(trip.price, recordStats = true)
        } else {
            // Detected screen but can't parse — still try to click
            AppState.appendLog("⚠ Could not parse trip — attempting click anyway")
            attemptAcceptClick(0f, recordStats = false)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Collect text from ALL open windows (not just active window)
    // ──────────────────────────────────────────────────────────────────────────
    private fun collectAllWindowsText(): String {
        val sb = StringBuilder()
        try {
            val wins: List<AccessibilityWindowInfo>? = windows
            if (!wins.isNullOrEmpty()) {
                for (win in wins) {
                    val root = try { win.root } catch (_: Exception) { null } ?: continue
                    collectNodeText(root, sb)
                    try { root.recycle() } catch (_: Exception) {}
                }
            } else {
                // Fallback to rootInActiveWindow
                val root = try { rootInActiveWindow } catch (_: Exception) { null }
                if (root != null) {
                    collectNodeText(root, sb)
                    try { root.recycle() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            AppState.appendLog("⚠ collectWindows: ${e.message?.take(60)}")
        }
        return sb.toString().trim()
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

    // ──────────────────────────────────────────────────────────────────────────
    // Detection
    // ──────────────────────────────────────────────────────────────────────────
    private fun isJeenyTripScreen(text: String): Boolean {
        if (text.isBlank()) return false
        val hasAccept  = text.contains(ACCEPT_TEXT)
        val hasMarker  = TRIP_MARKERS.any { text.contains(it) }
        val hasPrice   = text.contains("﷼") || text.contains("ر.س") ||
                         Regex("""\d+[.,]\d+""").containsMatchIn(text)
        return (hasAccept || hasMarker) && hasPrice
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Click — node first, gesture fallback automatically
    // ──────────────────────────────────────────────────────────────────────────
    fun attemptAcceptClick(price: Float, recordStats: Boolean) {
        val nodeOk = tryNodeClick()
        if (nodeOk) {
            AppState.appendLog("✅ Node click SUCCESS")
            AppState.updateDebug { copy(acceptButtonFound = true, clickMethod = "node click", clickSuccess = true) }
            if (recordStats && price > 0f) {
                AppState.onTripAccepted(price)
                AppPreferences.incrementAccepted(this)
                AppPreferences.addToTotalSar(this, price)
            }
        } else {
            AppState.appendLog("⚠ Node click failed → gesture at 85%")
            AppState.updateDebug { copy(clickMethod = "gesture fallback") }
            performGestureTap(price, recordStats, yPercent = 0.85f)
        }
    }

    private fun tryNodeClick(): Boolean {
        try {
            val wins = windows
            if (!wins.isNullOrEmpty()) {
                for (win in wins) {
                    val root = try { win.root } catch (_: Exception) { null } ?: continue
                    val hit = findAndClick(root)
                    try { root.recycle() } catch (_: Exception) {}
                    if (hit) return true
                }
            }
            val root = try { rootInActiveWindow } catch (_: Exception) { null } ?: return false
            val hit = findAndClick(root)
            try { root.recycle() } catch (_: Exception) {}
            return hit
        } catch (e: Exception) {
            AppState.appendLog("⚠ tryNodeClick: ${e.message?.take(60)}")
            return false
        }
    }

    private fun findAndClick(root: AccessibilityNodeInfo): Boolean {
        AppState.updateDebug { copy(acceptButtonFound = false) }
        // 1. findAccessibilityNodeInfosByText
        val byText = try { root.findAccessibilityNodeInfosByText(ACCEPT_TEXT) } catch (_: Exception) { null }
        if (!byText.isNullOrEmpty()) {
            AppState.updateDebug { copy(acceptButtonFound = true) }
            for (n in byText) {
                if (clickNodeOrParent(n)) { try { n.recycle() } catch (_: Exception) {}; return true }
                try { n.recycle() } catch (_: Exception) {}
            }
        }
        // 2. Recursive deep search
        return searchAndClick(root, 0)
    }

    private fun searchAndClick(node: AccessibilityNodeInfo, depth: Int): Boolean {
        if (depth > 35) return false
        try {
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            if (text.contains(ACCEPT_TEXT) || desc.contains(ACCEPT_TEXT)) {
                AppState.updateDebug { copy(acceptButtonFound = true) }
                if (clickNodeOrParent(node)) return true
            }
            for (i in 0 until node.childCount) {
                val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                val found = searchAndClick(child, depth + 1)
                try { child.recycle() } catch (_: Exception) {}
                if (found) return true
            }
        } catch (_: Exception) {}
        return false
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) return try { node.performAction(AccessibilityNodeInfo.ACTION_CLICK) } catch (_: Exception) { false }
        var cur: AccessibilityNodeInfo? = try { node.parent } catch (_: Exception) { null }
        repeat(6) {
            val c = cur ?: return false
            if (c.isClickable) {
                val ok = try { c.performAction(AccessibilityNodeInfo.ACTION_CLICK) } catch (_: Exception) { false }
                try { c.recycle() } catch (_: Exception) {}
                return ok
            }
            val p = try { c.parent } catch (_: Exception) { null }
            try { c.recycle() } catch (_: Exception) {}
            cur = p
        }
        try { cur?.recycle() } catch (_: Exception) {}
        return false
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Gesture tap — also callable from floating panel (Force Test)
    // ──────────────────────────────────────────────────────────────────────────
    fun performGestureTap(price: Float = 0f, recordStats: Boolean = false, yPercent: Float = 0.85f) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            AppState.appendLog("❌ Gesture requires Android 7+"); return
        }
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)

        val tapX = (dm.widthPixels / 2).toFloat()
        val tapY = dm.heightPixels * yPercent
        AppState.appendLog("🎯 Gesture → (${tapX.toInt()}, ${tapY.toInt()}) on ${dm.widthPixels}×${dm.heightPixels}")

        val path = Path().apply { moveTo(tapX, tapY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 100L))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription) {
                AppState.appendLog("✅ Gesture COMPLETED")
                AppState.updateDebug { copy(clickSuccess = true, clickMethod = "gesture ${(yPercent * 100).toInt()}%") }
                if (recordStats && price > 0f) {
                    AppState.onTripAccepted(price)
                    AppPreferences.incrementAccepted(this@DriverHelperAccessibilityService)
                    AppPreferences.addToTotalSar(this@DriverHelperAccessibilityService, price)
                }
            }
            override fun onCancelled(g: GestureDescription) {
                AppState.appendLog("❌ Gesture CANCELLED — check overlay permission")
                AppState.updateDebug { copy(clickSuccess = false) }
            }
        }, mainHandler)
    }

    private fun eventTypeName(type: Int): String = when (type) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED   -> "WIN_STATE"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WIN_CONTENT"
        AccessibilityEvent.TYPE_WINDOWS_CHANGED        -> "WINS_CHANGED"
        AccessibilityEvent.TYPE_VIEW_CLICKED           -> "VIEW_CLICK"
        AccessibilityEvent.TYPE_VIEW_FOCUSED           -> "VIEW_FOCUS"
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED      -> "TEXT_CHANGED"
        else -> "TYPE_${type}"
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
