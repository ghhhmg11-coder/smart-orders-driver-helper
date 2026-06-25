package com.smartorders.driverhelper

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppState {

    private const val OWN_PKG = "com.smartorders.driverhelper"

    data class DebugInfo(
        val serviceConnected: Boolean = false,
        // ALL events counter
        val totalEvents: Int = 0,
        // Last event from ANY package (will be own app when debug screen open)
        val lastPackage: String = "",
        val lastEventType: String = "",
        val lastClassName: String = "",
        // Last event NOT from our own app — never overwritten by own-app events
        val lastExternalPackage: String = "",
        val lastExternalEvent: String = "",
        val externalEventCount: Int = 0,
        // Recent unique packages (ring buffer, newest first)
        val recentPackages: List<String> = emptyList(),
        // Jeeny-specific tracking
        val lastJeenyPackage: String = "",
        val lastJeenyEventTime: String = "",
        val jeenyEventCount: Int = 0,
        val jeenyDetected: Boolean = false,
        // Screen text from all windows
        val lastVisibleText: String = "",
        val lastJeenyText: String = "",
        // Click tracking
        val acceptButtonFound: Boolean = false,
        val clickMethod: String = "",
        val clickSuccess: Boolean? = null,
        val logs: List<String> = emptyList()
    )

    data class Stats(
        val detected: Int = 0,
        val accepted: Int = 0,
        val rejected: Int = 0,
        val totalSar: Float = 0f
    )

    private val _debugInfo = MutableStateFlow(DebugInfo())
    val debugInfo: StateFlow<DebugInfo> = _debugInfo.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private val _autoAccept = MutableStateFlow(false)
    val autoAccept: StateFlow<Boolean> = _autoAccept.asStateFlow()

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private val logBuffer = ArrayDeque<String>()
    private const val MAX_LOGS = 300

    // Ring buffer of recent unique external packages (max 15)
    private val recentPkgBuffer = ArrayDeque<String>()
    private const val MAX_RECENT_PKGS = 15

    fun setAutoAccept(value: Boolean) { _autoAccept.value = value }

    fun setServiceConnected(value: Boolean) {
        _serviceConnected.value = value
        _debugInfo.value = _debugInfo.value.copy(serviceConnected = value)
        appendLog(if (value) "✅ Service connected — monitoring ALL packages" else "❌ Service disconnected")
    }

    fun updateDebug(update: DebugInfo.() -> DebugInfo) {
        _debugInfo.value = _debugInfo.value.update()
    }

    /** Call on every accessibility event. Handles own-pkg filtering internally. */
    fun onAccessibilityEvent(pkg: String, evtType: String, cls: String) {
        val isOwn = pkg == OWN_PKG || pkg.isEmpty()
        val isJeeny = pkg.contains("jeeny", ignoreCase = true)
        val now = timeNow()

        // Track recent external packages
        if (!isOwn && pkg.isNotEmpty()) {
            if (recentPkgBuffer.isEmpty() || recentPkgBuffer.first() != pkg) {
                recentPkgBuffer.addFirst(pkg)
                while (recentPkgBuffer.size > MAX_RECENT_PKGS) recentPkgBuffer.removeLast()
            }
        }

        _debugInfo.value = _debugInfo.value.let { d ->
            var updated = d.copy(
                totalEvents = d.totalEvents + 1,
                lastPackage = pkg,
                lastEventType = evtType,
                lastClassName = cls
            )
            if (!isOwn && pkg.isNotEmpty()) {
                updated = updated.copy(
                    lastExternalPackage = pkg,
                    lastExternalEvent = "$evtType @ $now",
                    externalEventCount = d.externalEventCount + 1,
                    recentPackages = recentPkgBuffer.toList()
                )
            }
            if (isJeeny) {
                updated = updated.copy(
                    lastJeenyPackage = pkg,
                    lastJeenyEventTime = now,
                    jeenyEventCount = d.jeenyEventCount + 1
                )
            }
            updated
        }
    }

    fun updateJeenyDetected(detected: Boolean, windowText: String = "") {
        _debugInfo.value = _debugInfo.value.copy(
            jeenyDetected = detected,
            lastVisibleText = windowText.take(600)
        )
        if (detected && windowText.isNotEmpty()) {
            _debugInfo.value = _debugInfo.value.copy(lastJeenyText = windowText.take(600))
        }
    }

    fun updateWindowText(text: String) {
        _debugInfo.value = _debugInfo.value.copy(lastVisibleText = text.take(600))
    }

    fun appendLog(message: String) {
        val entry = "[${timeNow()}] $message"
        logBuffer.addLast(entry)
        while (logBuffer.size > MAX_LOGS) logBuffer.removeFirst()
        _debugInfo.value = _debugInfo.value.copy(logs = logBuffer.toList())
    }

    fun clearLogs() {
        logBuffer.clear()
        _debugInfo.value = _debugInfo.value.copy(logs = emptyList())
    }

    fun updateStats(detected: Int, accepted: Int, rejected: Int, totalSar: Float) {
        _stats.value = Stats(detected, accepted, rejected, totalSar)
    }

    fun onTripDetected(price: Float) {
        _stats.value = _stats.value.copy(detected = _stats.value.detected + 1)
    }

    fun onTripAccepted(price: Float) {
        _stats.value = _stats.value.copy(
            accepted = _stats.value.accepted + 1,
            totalSar = _stats.value.totalSar + price
        )
    }

    fun onTripRejected() {
        _stats.value = _stats.value.copy(rejected = _stats.value.rejected + 1)
    }

    fun resetStats() { _stats.value = Stats() }

    private fun timeNow(): String =
        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
}
