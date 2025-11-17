package com.dps.droidpadmacos.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConnectedDevice(
    val name: String,
    val address: String,
    val lastConnectedTimestamp: Long
)

class DeviceHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _recentDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val recentDevices: StateFlow<List<ConnectedDevice>> = _recentDevices.asStateFlow()

    init {
        loadDevices()
    }

    private fun loadDevices() {
        val devicesJson = prefs.getString(KEY_DEVICES, null)
        if (devicesJson != null) {
            try {
                val devices = parseDevices(devicesJson)
                _recentDevices.value = devices.sortedByDescending { it.lastConnectedTimestamp }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading devices", e)
                _recentDevices.value = emptyList()
            }
        } else {
            _recentDevices.value = emptyList()
        }
    }

    fun addDevice(name: String, address: String) {
        val currentDevices = _recentDevices.value.toMutableList()

        // Remove if already exists
        currentDevices.removeAll { it.address == address }

        // Add to top
        currentDevices.add(0, ConnectedDevice(name, address, System.currentTimeMillis()))

        // Keep only last 5 devices
        val limitedDevices = currentDevices.take(MAX_DEVICES)

        _recentDevices.value = limitedDevices
        saveDevices(limitedDevices)

        // Save as last connected device
        setLastConnectedDevice(address)
    }

    fun getLastConnectedDeviceAddress(): String? {
        return prefs.getString(KEY_LAST_DEVICE, null)
    }

    private fun setLastConnectedDevice(address: String) {
        prefs.edit().putString(KEY_LAST_DEVICE, address).apply()
    }

    fun clearHistory() {
        _recentDevices.value = emptyList()
        prefs.edit()
            .remove(KEY_DEVICES)
            .remove(KEY_LAST_DEVICE)
            .apply()
    }

    private fun saveDevices(devices: List<ConnectedDevice>) {
        val json = serializeDevices(devices)
        prefs.edit().putString(KEY_DEVICES, json).apply()
    }

    private fun serializeDevices(devices: List<ConnectedDevice>): String {
        // Simple pipe-separated format: name|address|timestamp;name|address|timestamp
        return devices.joinToString(";") { device ->
            "${device.name}|${device.address}|${device.lastConnectedTimestamp}"
        }
    }

    private fun parseDevices(json: String): List<ConnectedDevice> {
        if (json.isBlank()) return emptyList()

        return json.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 3) {
                try {
                    ConnectedDevice(
                        name = parts[0],
                        address = parts[1],
                        lastConnectedTimestamp = parts[2].toLong()
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    companion object {
        private const val TAG = "DeviceHistoryManager"
        private const val PREFS_NAME = "device_history"
        private const val KEY_DEVICES = "recent_devices"
        private const val KEY_LAST_DEVICE = "last_connected_device"
        private const val MAX_DEVICES = 5
    }
}
