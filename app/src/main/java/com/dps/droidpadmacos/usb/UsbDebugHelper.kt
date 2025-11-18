package com.dps.droidpadmacos.usb

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Debug helper to diagnose USB detection issues
 */
object UsbDebugHelper {

    private const val TAG = "UsbDebugHelper"

    fun printFullDiagnostics(context: Context): String {
        val report = StringBuilder()
        report.appendLine("========== USB DETECTION DIAGNOSTICS ==========")

        // 1. Battery Status
        report.appendLine("\n--- Battery & Charging Status ---")
        try {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1

            report.appendLine("Plugged value: $plugged")
            report.appendLine("  USB (2): ${plugged == BatteryManager.BATTERY_PLUGGED_USB}")
            report.appendLine("  AC (1): ${plugged == BatteryManager.BATTERY_PLUGGED_AC}")
            report.appendLine("  Wireless (4): ${plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS}")

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            report.appendLine("Battery status: $status (charging=${status == BatteryManager.BATTERY_STATUS_CHARGING})")

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            report.appendLine("Battery level: $level%")
        } catch (e: Exception) {
            report.appendLine("ERROR reading battery: ${e.message}")
            Log.e(TAG, "Battery check error", e)
        }

        // 2. ADB Status
        report.appendLine("\n--- ADB Status ---")
        try {
            val adbDaemonRunning = getSystemProperty("init.svc.adbd")
            report.appendLine("ADB daemon (init.svc.adbd): $adbDaemonRunning")

            val adbEnabled = try {
                Settings.Global.getString(
                    context.contentResolver,
                    Settings.Global.ADB_ENABLED
                )
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            }
            report.appendLine("ADB_ENABLED setting: $adbEnabled")

            val adbTcpPort = getSystemProperty("service.adb.tcp.port")
            report.appendLine("ADB TCP port: $adbTcpPort")

        } catch (e: Exception) {
            report.appendLine("ERROR reading ADB: ${e.message}")
            Log.e(TAG, "ADB check error", e)
        }

        // 3. USB State Files
        report.appendLine("\n--- USB State Files ---")
        val usbStatePaths = listOf(
            "/sys/class/android_usb/android0/state",
            "/sys/devices/virtual/android_usb/android0/state",
            "/sys/class/usb_device/usb_device/device/status",
            "/config/usb_gadget/g1/UDC",
            "/sys/class/android_usb/android0/functions"
        )

        for (path in usbStatePaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val readable = file.canRead()
                    if (readable) {
                        val content = file.readText().trim()
                        report.appendLine("✓ $path = $content")
                    } else {
                        report.appendLine("⚠ $path exists but not readable")
                    }
                } else {
                    report.appendLine("✗ $path does not exist")
                }
            } catch (e: Exception) {
                report.appendLine("✗ $path ERROR: ${e.message}")
            }
        }

        // 4. System Properties
        report.appendLine("\n--- USB-Related System Properties ---")
        val properties = listOf(
            "sys.usb.state",
            "sys.usb.config",
            "persist.sys.usb.config",
            "ro.bootmode"
        )

        for (prop in properties) {
            val value = getSystemProperty(prop)
            report.appendLine("$prop = $value")
        }

        // 5. Developer Options
        report.appendLine("\n--- Developer Options ---")
        try {
            val devEnabled = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            )
            report.appendLine("Developer options enabled: ${devEnabled == 1}")
        } catch (e: Exception) {
            report.appendLine("ERROR reading dev options: ${e.message}")
        }

        // 6. Final Detection Result
        report.appendLine("\n--- Detection Result ---")
        val connectionInfo = UsbConnectionDetector.detectConnection(context)
        report.appendLine("isConnected: ${connectionInfo.isConnected}")
        report.appendLine("chargingViaUsb: ${connectionInfo.chargingViaUsb}")
        report.appendLine("isAdbConnected: ${connectionInfo.isAdbConnected}")
        report.appendLine("usbConfigured: ${connectionInfo.usbConfigured}")
        report.appendLine("connectionType: ${connectionInfo.connectionType}")
        report.appendLine("isSuitableForTrackpad: ${UsbConnectionDetector.isSuitableForTrackpad(connectionInfo)}")

        report.appendLine("\n==============================================")

        val fullReport = report.toString()
        Log.d(TAG, fullReport)
        return fullReport
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val value = reader.readLine()?.trim()
            reader.close()
            process.waitFor()
            value?.ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading property $key", e)
            null
        }
    }
}
