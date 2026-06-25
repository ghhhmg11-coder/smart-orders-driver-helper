package com.smartorders.driverhelper

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppState {

    data class DebugInfo(
        val serviceConnected: Boolean = false,
        val totalEvents: Int = 0,
        val lastPackage: String = "",
        val lastEventType: String = "",
        val lastClassName: String = "",
        val lastVisibleText: String = "",
        val jeenyDetected: Boolean = false,
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

    private val logBuffer = ArrayDeque<String>(maxOf(200, 0))
    private val MAX_LOGS = 200

    fun setAutoAccept(value: Boolean) {
        _autoAccept.value = value
    }

    fun setServiceConnected(value: Boolean) {
        _serviceConnected.value = value
        _debugInfo.value = _debugInfo.value.copy(serviceConnected = value)
        appendLog(if (value) "✅ Service connected" else "❌ Service disconnected")
    }

    fun updateDebug(update: DebugInfo.() -> DebugInfo) {
        _debugInfo.value = _debugInfo.value.update()
    }

    fun appendLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val entry = "[$timestamp] $message"
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
        _stats.value = _stats.value.copy(
            detected = _stats.value.detected + 1
        )
    }

    fun onTripAccepted(price: Float) {
        _stats.value = _stats.value.copy(
            accepted = _stats.value.accepted + 1,
            totalSar = _stats.value.totalSar + price
        )
    }

    fun onTripRejected() {
        _stats.value = _stats.value.copy(
            rejected = _stats.value.rejected + 1
        )
    }

    fun resetStats() {
        _stats.value = Stats()
    }
}
