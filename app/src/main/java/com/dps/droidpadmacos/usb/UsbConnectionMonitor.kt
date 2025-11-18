package com.dps.droidpadmacos.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors USB connection state changes in real-time
 */
class UsbConnectionMonitor(private val context: Context) {

    companion object {
        private const val TAG = "UsbConnectionMonitor"
    }

    private val _connectionState = MutableStateFlow<UsbConnectionDetector.ConnectionInfo?>(null)
    val connectionState: StateFlow<UsbConnectionDetector.ConnectionInfo?> = _connectionState.asStateFlow()

    private var isMonitoring = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED,
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                UsbManager.ACTION_USB_DEVICE_DETACHED,
                Intent.ACTION_BATTERY_CHANGED -> {
                    Log.d(TAG, "USB state changed: ${intent.action}")
                    updateConnectionState()
                }
            }
        }
    }

    /**
     * Start monitoring USB connection changes
     */
    fun startMonitoring() {
        if (isMonitoring) return

        Log.d(TAG, "Starting USB connection monitoring")

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }

        context.registerReceiver(usbReceiver, filter)
        isMonitoring = true

        // Get initial state
        updateConnectionState()
    }

    /**
     * Stop monitoring USB connection changes
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        Log.d(TAG, "Stopping USB connection monitoring")

        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }

        isMonitoring = false
    }

    /**
     * Update the current connection state
     */
    private fun updateConnectionState() {
        val connectionInfo = UsbConnectionDetector.detectConnection(context)
        Log.d(TAG, "Connection state updated: $connectionInfo")
        _connectionState.value = connectionInfo
    }

    /**
     * Get current connection state immediately (non-flow)
     */
    fun getCurrentState(): UsbConnectionDetector.ConnectionInfo {
        return UsbConnectionDetector.detectConnection(context)
    }
}
