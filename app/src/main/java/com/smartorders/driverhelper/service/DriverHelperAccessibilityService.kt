package com.smartorders.driverhelper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.smartorders.driverhelper.AppState
import com.smartorders.driverhelper.data.AppPreferences
import com.smartorders.driverhelper.data.parseTripInfo

class DriverHelperAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SmartOrdersAS"
        private val JEENY_PACKAGES = setOf("com.jeeny.driver", "com.jeeny.drivers")
        private val JEENY_KEYWORDS = listOf(
            "قبول العرض",
            "مشوار داخل المدينة",
            "يبعد",
            "استريح",
            "ستريح"
        )
        private val ACCEPT_BUTTON_TEXT = "قبول العرض"
        private var lastClickTime = 0L
        private const val CLICK_COOLDOWN_MS = 3000L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppState.setServiceConnected(true)
        AppState.appendLog("✅ Accessibility service started")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val packageName = event.packageName?.toString() ?: ""
        val eventType = eventTypeName(event.eventType)
        val className = event.className?.toString() ?: ""

        // Update debug counters
        AppState.updateDebug {
            copy(
                totalEvents = totalEvents + 1,
                lastPackage = packageName,
                lastEventType = eventType,
                lastClassName = className
            )
        }

        // Filter: only process Jeeny-related packages
        val isJeeny = JEENY_PACKAGES.contains(packageName) ||
                packageName.contains("jeeny", ignoreCase = true)
        if (!isJeeny) return

        // Collect all text from all windows
        val allText = collectAllWindowText()
        AppState.updateDebug { copy(lastVisibleText = allText.take(500)) }

        // Detect Jeeny trip request screen
        val isJeenyScreen = isJeenyTripScreen(allText)
        AppState.updateDebug { copy(jeenyDetected = isJeenyScreen) }

        if (!isJeenyScreen) return

        AppState.appendLog("🔍 Jeeny trip screen detected in $packageName")

        // Only attempt auto-accept if enabled
        if (!AppPreferences.isAutoAccept(this)) {
            AppState.appendLog("⏸ Auto-accept is OFF — skipping")
            return
        }

        // Cooldown check
        val now = System.currentTimeMillis()
        if (now - lastClickTime < CLICK_COOLDOWN_MS) {
            return
        }

        // Parse trip info
        val tripInfo = parseTripInfo(allText)
        if (tripInfo == null) {
            AppState.appendLog("⚠ Could not parse trip info from screen text")
            return
        }

        AppState.appendLog("📋 Trip: ﷼${tripInfo.price} | ${tripInfo.pickupMinutes}min | ${tripInfo.pickupDistanceKm}km")
        AppState.onTripDetected(tripInfo.price)
        AppPreferences.incrementDetected(this)

        // Evaluate rules
        val minPrice = AppPreferences.getMinPrice(this)
        val maxPrice = AppPreferences.getMaxPrice(this)
        val minMinutes = AppPreferences.getMinPickupMinutes(this)
        val maxMinutes = AppPreferences.getMaxPickupMinutes(this)
        val maxDistance = AppPreferences.getMaxPickupDistance(this)

        val priceOk = tripInfo.price in minPrice..maxPrice
        val minutesOk = tripInfo.pickupMinutes in minMinutes..maxMinutes
        val distanceOk = tripInfo.pickupDistanceKm <= maxDistance

        AppState.appendLog("📊 Rules: price=$priceOk(${tripInfo.price}∈[${minPrice},${maxPrice}]) minutes=$minutesOk dist=$distanceOk")

        if (!priceOk || !minutesOk || !distanceOk) {
            AppState.appendLog("❌ Trip rejected — rules not matched")
            AppState.onTripRejected()
            AppPreferences.incrementRejected(this)
            return
        }

        AppState.appendLog("✅ Rules matched — attempting to click accept button")
        lastClickTime = now
        attemptAcceptClick(tripInfo.price)
    }

    private fun collectAllWindowText(): String {
        val sb = StringBuilder()
        try {
            val windows = windows
            if (windows.isNullOrEmpty()) {
                // Fallback to rootInActiveWindow
                val root = rootInActiveWindow
                if (root != null) {
                    collectNodeText(root, sb)
                    root.recycle()
                }
            } else {
                for (window in windows) {
                    val root = window.root ?: continue
                    collectNodeText(root, sb)
                    root.recycle()
                }
            }
        } catch (e: Exception) {
            AppState.appendLog("⚠ Error collecting window text: ${e.message}")
        }
        return sb.toString()
    }

    private fun collectNodeText(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int = 0) {
        if (depth > 30) return
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if (!text.isNullOrBlank()) sb.append(text).append(" ")
        if (!desc.isNullOrBlank() && desc != text) sb.append(desc).append(" ")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectNodeText(child, sb, depth + 1)
            child.recycle()
        }
    }

    private fun isJeenyTripScreen(text: String): Boolean {
        val lowerText = text
        return JEENY_KEYWORDS.any { keyword -> lowerText.contains(keyword) } &&
               (text.contains("﷼") || Regex("""\d+\.\d+""").containsMatchIn(text))
    }

    private fun attemptAcceptClick(price: Float) {
        // Strategy 1: find the node with "قبول العرض" text and click it
        val clicked = tryNodeClick()
        if (clicked) {
            AppState.appendLog("✅ Node click succeeded")
            AppState.updateDebug { copy(acceptButtonFound = true, clickMethod = "node click", clickSuccess = true) }
            AppState.onTripAccepted(price)
            AppPreferences.incrementAccepted(this)
            AppPreferences.addToTotalSar(this, price)
        } else {
            AppState.appendLog("⚠ Node click failed — trying gesture fallback")
            AppState.updateDebug { copy(clickMethod = "gesture (fallback)" ) }
            tryGestureClick(price)
        }
    }

    private fun tryNodeClick(): Boolean {
        try {
            val allWindows = windows
            if (!allWindows.isNullOrEmpty()) {
                for (window in allWindows) {
                    val root = window.root ?: continue
                    val result = findAndClickAcceptNode(root)
                    root.recycle()
                    if (result) return true
                }
            }
            // Fallback: rootInActiveWindow
            val root = rootInActiveWindow ?: return false
            val result = findAndClickAcceptNode(root)
            root.recycle()
            return result
        } catch (e: Exception) {
            AppState.appendLog("⚠ tryNodeClick exception: ${e.message}")
            return false
        }
    }

    private fun findAndClickAcceptNode(root: AccessibilityNodeInfo): Boolean {
        // Try direct text match
        val nodes = root.findAccessibilityNodeInfosByText(ACCEPT_BUTTON_TEXT)
        if (!nodes.isNullOrEmpty()) {
            for (node in nodes) {
                AppState.updateDebug { copy(acceptButtonFound = true) }
                if (clickNodeOrParent(node)) {
                    node.recycle()
                    return true
                }
                node.recycle()
            }
        }
        return false
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        // Try the node itself
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        // Walk up to 5 parent levels
        var current: AccessibilityNodeInfo? = node.parent
        var levels = 0
        while (current != null && levels < 5) {
            if (current.isClickable) {
                val result = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                current.recycle()
                return result
            }
            val parent = current.parent
            current.recycle()
            current = parent
            levels++
        }
        current?.recycle()
        return false
    }

    private fun tryGestureClick(price: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            AppState.appendLog("❌ Gesture not supported on this Android version")
            AppState.updateDebug { copy(clickSuccess = false) }
            AppState.onTripRejected()
            return
        }

        // The accept button is at the bottom of the screen, roughly at 88% height
        val displayMetrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Click center-bottom (where the big purple button is)
        val tapX = (screenWidth / 2).toFloat()
        val tapY = (screenHeight * 0.88f)

        AppState.appendLog("🎯 Gesture tap at ($tapX, $tapY) on ${screenWidth}x${screenHeight}")

        val path = Path().apply { moveTo(tapX, tapY) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                super.onCompleted(gestureDescription)
                AppState.appendLog("✅ Gesture click completed")
                AppState.updateDebug { copy(clickSuccess = true) }
                AppState.onTripAccepted(price)
                AppPreferences.incrementAccepted(this@DriverHelperAccessibilityService)
                AppPreferences.addToTotalSar(this@DriverHelperAccessibilityService, price)
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                super.onCancelled(gestureDescription)
                AppState.appendLog("❌ Gesture click cancelled")
                AppState.updateDebug { copy(clickSuccess = false) }
                AppState.onTripRejected()
                AppPreferences.incrementRejected(this@DriverHelperAccessibilityService)
            }
        }, null)
    }

    private fun eventTypeName(type: Int): String = when (type) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE_CHANGED"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "WINDOW_CONTENT_CHANGED"
        AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "WINDOWS_CHANGED"
        AccessibilityEvent.TYPE_VIEW_CLICKED -> "VIEW_CLICKED"
        AccessibilityEvent.TYPE_VIEW_FOCUSED -> "VIEW_FOCUSED"
        else -> "TYPE_$type"
    }

    override fun onInterrupt() {
        AppState.setServiceConnected(false)
        AppState.appendLog("⚠ Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppState.setServiceConnected(false)
        AppState.appendLog("❌ Accessibility service destroyed")
    }
}
