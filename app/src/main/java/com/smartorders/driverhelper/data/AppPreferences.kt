package com.smartorders.driverhelper.data

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREFS_NAME = "smart_orders_prefs"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_AUTO_ACCEPT = "auto_accept"
    private const val KEY_MIN_PRICE = "min_price"
    private const val KEY_MAX_PRICE = "max_price"
    private const val KEY_MIN_PICKUP_MINUTES = "min_pickup_minutes"
    private const val KEY_MAX_PICKUP_MINUTES = "max_pickup_minutes"
    private const val KEY_MAX_PICKUP_DISTANCE = "max_pickup_distance"
    private const val KEY_DETECTED = "detected_trips"
    private const val KEY_ACCEPTED = "accepted_trips"
    private const val KEY_REJECTED = "rejected_trips"
    private const val KEY_TOTAL_SAR = "total_sar"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(context: Context): Boolean = prefs(context).getBoolean(KEY_LOGGED_IN, false)
    fun setLoggedIn(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_LOGGED_IN, value).apply()

    fun isAutoAccept(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO_ACCEPT, false)
    fun setAutoAccept(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_ACCEPT, value).apply()

    fun getMinPrice(context: Context): Float = prefs(context).getFloat(KEY_MIN_PRICE, 0f)
    fun setMinPrice(context: Context, value: Float) = prefs(context).edit().putFloat(KEY_MIN_PRICE, value).apply()

    fun getMaxPrice(context: Context): Float = prefs(context).getFloat(KEY_MAX_PRICE, 9999f)
    fun setMaxPrice(context: Context, value: Float) = prefs(context).edit().putFloat(KEY_MAX_PRICE, value).apply()

    fun getMinPickupMinutes(context: Context): Float = prefs(context).getFloat(KEY_MIN_PICKUP_MINUTES, 0f)
    fun setMinPickupMinutes(context: Context, value: Float) = prefs(context).edit().putFloat(KEY_MIN_PICKUP_MINUTES, value).apply()

    fun getMaxPickupMinutes(context: Context): Float = prefs(context).getFloat(KEY_MAX_PICKUP_MINUTES, 30f)
    fun setMaxPickupMinutes(context: Context, value: Float) = prefs(context).edit().putFloat(KEY_MAX_PICKUP_MINUTES, value).apply()

    fun getMaxPickupDistance(context: Context): Float = prefs(context).getFloat(KEY_MAX_PICKUP_DISTANCE, 10f)
    fun setMaxPickupDistance(context: Context, value: Float) = prefs(context).edit().putFloat(KEY_MAX_PICKUP_DISTANCE, value).apply()

    fun getDetectedTrips(context: Context): Int = prefs(context).getInt(KEY_DETECTED, 0)
    fun incrementDetected(context: Context) = prefs(context).edit().putInt(KEY_DETECTED, getDetectedTrips(context) + 1).apply()

    fun getAcceptedTrips(context: Context): Int = prefs(context).getInt(KEY_ACCEPTED, 0)
    fun incrementAccepted(context: Context) = prefs(context).edit().putInt(KEY_ACCEPTED, getAcceptedTrips(context) + 1).apply()

    fun getRejectedTrips(context: Context): Int = prefs(context).getInt(KEY_REJECTED, 0)
    fun incrementRejected(context: Context) = prefs(context).edit().putInt(KEY_REJECTED, getRejectedTrips(context) + 1).apply()

    fun getTotalSar(context: Context): Float = prefs(context).getFloat(KEY_TOTAL_SAR, 0f)
    fun addToTotalSar(context: Context, value: Float) = prefs(context).edit().putFloat(KEY_TOTAL_SAR, getTotalSar(context) + value).apply()

    fun clearTodayStats(context: Context) {
        prefs(context).edit()
            .putInt(KEY_DETECTED, 0)
            .putInt(KEY_ACCEPTED, 0)
            .putInt(KEY_REJECTED, 0)
            .putFloat(KEY_TOTAL_SAR, 0f)
            .apply()
    }

    fun resetAll(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
