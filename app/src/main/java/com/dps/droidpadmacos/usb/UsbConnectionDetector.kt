package com.dps.droidpadmacos.usb

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.dps.droidpadmacos.common.AppConstants
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Detects USB connection to a computer
 * Uses multiple detection methods since Android in peripheral mode doesn't show in UsbManager
 */
object UsbConnectionDetector {

    private const val TAG = "UsbConnectionDetector"

    // Cache for system properties to avoid spawning shell processes repeatedly
    private val propertyCache = mutableMapOf<String, Pair<String?, Long>>()

    data class ConnectionInfo(
        val isConnected: Boolean,
        val chargingViaUsb: Boolean,
        val isAdbConnected: Boolean,
        val usbConfigured: Boolean,
        val connectionType: ConnectionType
    )

    enum class ConnectionType {
        NONE,           // Not connected
        USB_CHARGING,   // USB connected but just charging
        USB_DATA,       // USB with data connection (ADB or MTP)
        UNKNOWN         // Connected but type unclear
    }

    /**
     * Battery status result to avoid duplicate queries
     */
    private data class BatteryStatus(val plugged: Int)

    /**
     * Get battery status once to avoid duplicate broadcast receiver registrations
     */
    private fun getBatteryStatus(context: Context): BatteryStatus {
        val batteryStatus: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return BatteryStatus(plugged)
    }

    /**
     * Comprehensive USB connection detection
     */
    fun detectConnection(context: Context): ConnectionInfo {
        val battery = getBatteryStatus(context)
        val chargingViaUsb = battery.plugged == BatteryManager.BATTERY_PLUGGED_USB
        val isPluggedIn = battery.plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                          battery.plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                          battery.plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        val adbConnected = isAdbConnected(context)
        val usbConfigured = isUsbConfigured()

        val isConnected = chargingViaUsb || adbConnected || usbConfigured || isPluggedIn

        // For USB_DATA, ADB connected is the strongest indicator
        // Some devices report USB as AC when using fast charging, so we check if ANY power source is connected
        val hasDataConnection = adbConnected || (usbConfigured && (chargingViaUsb || isPluggedIn))

        val connectionType = when {
            hasDataConnection -> ConnectionType.USB_DATA
            chargingViaUsb -> ConnectionType.USB_CHARGING
            isConnected -> ConnectionType.UNKNOWN
            else -> ConnectionType.NONE
        }

        Log.d(TAG, "USB Detection - Connected: $isConnected, Charging: $chargingViaUsb, " +
                "ADB: $adbConnected, Configured: $usbConfigured, PluggedIn: $isPluggedIn, Type: $connectionType")

        return ConnectionInfo(
            isConnected = isConnected,
            chargingViaUsb = chargingViaUsb,
            isAdbConnected = adbConnected,
            usbConfigured = usbConfigured,
            connectionType = connectionType
        )
    }


    /**
     * Check if ADB is connected (indicates USB data connection)
     */
    private fun isAdbConnected(context: Context): Boolean {
        return try {
            // Method 1: Check ADB system property
            val adbEnabled = getSystemProperty(AppConstants.SystemProperties.ADB_DAEMON) == "running"

            // Method 2: Check if USB debugging is enabled
            val usbDebugging = try {
                android.provider.Settings.Global.getString(
                    context.contentResolver,
                    android.provider.Settings.Global.ADB_ENABLED
                ) == "1"
            } catch (e: Exception) {
                Log.w(TAG, "Could not read ADB_ENABLED setting", e)
                false
            }

            // Both must be true: USB debugging enabled AND ADB daemon actually running
            val connected = adbEnabled && usbDebugging
            Log.d(TAG, "ADB check - daemon running: $adbEnabled, debugging enabled: $usbDebugging, connected: $connected")

            connected
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ADB status", e)
            false
        }
    }

    /**
     * Check if USB is configured (via sys filesystem)
     */
    private fun isUsbConfigured(): Boolean {
        return try {
            // Check USB state from sysfs
            val usbStatePaths = AppConstants.UsbStatePaths.PATHS

            for (path in usbStatePaths) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val state = file.readText().trim()
                    Log.d(TAG, "USB state at $path: $state")

                    // States indicating USB is configured
                    if (state.contains("CONFIGURED", ignoreCase = true) ||
                        state.contains("CONNECTED", ignoreCase = true)) {
                        return true
                    }
                }
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking USB configuration", e)
            false
        }
    }

    /**
     * Get system property value with caching to avoid excessive shell process spawning
     */
    private fun getSystemProperty(key: String): String? {
        // Check cache first
        val cached = propertyCache[key]
        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.second) < AppConstants.USB.PROPERTY_CACHE_DURATION_MS) {
            return cached.first
        }

        // Cache miss or expired - fetch new value
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val value = reader.readLine()?.trim()
            reader.close()
            process.waitFor()

            // Update cache
            propertyCache[key] = Pair(value, now)
            value
        } catch (e: Exception) {
            Log.e(TAG, "Error reading system property $key", e)
            null
        }
    }

    /**
     * Detect if connected device is likely a Mac
     * This is a heuristic-based detection
     */
    fun isLikelyConnectedToMac(context: Context): Boolean {
        // Check if device is connected via USB
        val connectionInfo = detectConnection(context)
        if (!connectionInfo.isConnected) {
            return false
        }

        // Heuristics for Mac detection:
        // 1. Check for specific USB vendor strings (requires root, so we skip this)
        // 2. Check network interfaces for Mac-like patterns
        // 3. For now, we assume any USB data connection could be a Mac

        // If ADB is connected or USB is configured, there's a good chance it's a computer
        return connectionInfo.isAdbConnected || connectionInfo.usbConfigured
    }

    /**
     * Get a user-friendly description of the connection
     */
    fun getConnectionDescription(connectionInfo: ConnectionInfo): String {
        return when {
            !connectionInfo.isConnected -> "Not connected via USB"
            connectionInfo.connectionType == ConnectionType.USB_DATA ->
                "Connected to computer via USB"
            connectionInfo.connectionType == ConnectionType.USB_CHARGING ->
                "USB charging only (no data connection)"
            else -> "USB connection detected"
        }
    }

    /**
     * Check if the connection is suitable for trackpad mode
     * We want USB data connection, not just charging
     */
    fun isSuitableForTrackpad(connectionInfo: ConnectionInfo): Boolean {
        return connectionInfo.connectionType == ConnectionType.USB_DATA
    }
}
