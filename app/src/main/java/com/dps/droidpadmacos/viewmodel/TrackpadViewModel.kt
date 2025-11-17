package com.dps.droidpadmacos.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dps.droidpadmacos.bluetooth.BluetoothHidService
import com.dps.droidpadmacos.bluetooth.HidConstants
import com.dps.droidpadmacos.data.DeviceHistoryManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class TrackpadViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothService = BluetoothHidService.getInstance(application)
    private val deviceHistoryManager = DeviceHistoryManager(application)
    private val bluetoothManager = application.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    val connectionState: StateFlow<BluetoothHidService.ConnectionState> = bluetoothService.connectionState
    val isRegistered: StateFlow<Boolean> = bluetoothService.isRegistered
    val isProfileReady: StateFlow<Boolean> = bluetoothService.isProfileReady
    val recentDevices: StateFlow<List<com.dps.droidpadmacos.data.ConnectedDevice>> = deviceHistoryManager.recentDevices

    // Mouse movement accumulator for smoother movement
    private var accumulatedX = 0f
    private var accumulatedY = 0f

    init {
        bluetoothService.initialize()

        // Observe connection state and save connected devices
        viewModelScope.launch {
            connectionState.collect { state ->
                android.util.Log.d("TrackpadViewModel", "Connection state changed: $state")
                if (state is BluetoothHidService.ConnectionState.Connected) {
                    android.util.Log.d("TrackpadViewModel", "Saving device: ${state.deviceName} (${state.deviceAddress})")
                    if (state.deviceAddress.isNotEmpty()) {
                        deviceHistoryManager.addDevice(state.deviceName, state.deviceAddress)
                    }
                }
            }
        }
    }

    fun registerDevice() {
        android.util.Log.d("TrackpadViewModel", "registerDevice() called")
        viewModelScope.launch {
            val result = bluetoothService.registerHidDevice()
            android.util.Log.d("TrackpadViewModel", "registerDevice() result: $result")
        }
    }

    fun unregisterDevice() {
        viewModelScope.launch {
            bluetoothService.unregisterHidDevice()
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            bluetoothService.connect(device)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothService.disconnect()
        }
    }

    fun sendMouseMovement(deltaX: Int, deltaY: Int) {
        // Accumulate movement
        accumulatedX += deltaX
        accumulatedY += deltaY

        // Send movement in chunks that fit in byte range (-127 to 127)
        var sendX = accumulatedX.toInt().coerceIn(-127, 127).toByte()
        var sendY = accumulatedY.toInt().coerceIn(-127, 127).toByte()

        if (sendX != 0.toByte() || sendY != 0.toByte()) {
            android.util.Log.d("TrackpadViewModel", "Sending to BT - X: $sendX, Y: $sendY (input was X:$deltaX, Y:$deltaY)")
            bluetoothService.sendMouseReport(
                HidConstants.BUTTON_NONE,
                sendX,
                sendY,
                0
            )

            // Subtract what we sent from accumulator
            accumulatedX -= sendX.toInt()
            accumulatedY -= sendY.toInt()
        }
    }

    fun sendLeftClick() {
        viewModelScope.launch {
            // Press
            bluetoothService.sendMouseReport(
                HidConstants.BUTTON_LEFT,
                0,
                0,
                0
            )
            // Small delay
            kotlinx.coroutines.delay(50)
            // Release
            bluetoothService.sendMouseReport(
                HidConstants.BUTTON_NONE,
                0,
                0,
                0
            )
        }
    }

    fun sendRightClick() {
        viewModelScope.launch {
            // Press
            bluetoothService.sendMouseReport(
                HidConstants.BUTTON_RIGHT,
                0,
                0,
                0
            )
            // Small delay
            kotlinx.coroutines.delay(50)
            // Release
            bluetoothService.sendMouseReport(
                HidConstants.BUTTON_NONE,
                0,
                0,
                0
            )
        }
    }

    fun sendScroll(deltaY: Int) {
        if (deltaY != 0) {
            val scrollAmount = deltaY.coerceIn(-127, 127).toByte()
            bluetoothService.sendMouseReport(
                HidConstants.BUTTON_NONE,
                0,
                0,
                scrollAmount
            )
        }
    }

    fun sendMissionControl() {
        viewModelScope.launch {
            bluetoothService.sendMissionControl()
        }
    }

    fun sendShowDesktop() {
        viewModelScope.launch {
            bluetoothService.sendShowDesktop()
        }
    }

    fun sendAppSwitcher() {
        viewModelScope.launch {
            bluetoothService.sendAppSwitcher()
        }
    }

    fun connectToDeviceByAddress(address: String): Boolean {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        return if (device != null) {
            connectToDevice(device)
            true
        } else {
            android.util.Log.e("TrackpadViewModel", "Device not found: $address")
            false
        }
    }

    fun clearDeviceHistory() {
        deviceHistoryManager.clearHistory()
    }

    fun resetAllConnections() {
        viewModelScope.launch {
            android.util.Log.d("TrackpadViewModel", "Resetting all connections and clearing history...")
            bluetoothService.resetAndClearConnections()
            deviceHistoryManager.clearHistory()
        }
    }

    fun attemptAutoReconnect() {
        viewModelScope.launch {
            android.util.Log.d("TrackpadViewModel", "Attempting auto-reconnect...")

            // Wait for HID profile to be ready (with timeout)
            android.util.Log.d("TrackpadViewModel", "Waiting for HID profile to be ready...")
            try {
                kotlinx.coroutines.withTimeout(5000) {
                    isProfileReady.first { it }
                }
                android.util.Log.d("TrackpadViewModel", "HID profile is ready")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.e("TrackpadViewModel", "Timeout waiting for HID profile")
                return@launch
            }

            // Check if already registered
            if (!isRegistered.value) {
                android.util.Log.d("TrackpadViewModel", "Not registered, registering HID device first...")
                registerDevice()
                // Wait a bit for registration to complete
                kotlinx.coroutines.delay(1000)
            }

            // Check if already connected
            if (connectionState.value is BluetoothHidService.ConnectionState.Connected) {
                android.util.Log.d("TrackpadViewModel", "Already connected, skipping auto-reconnect")
                return@launch
            }

            // Try to connect to last device
            val lastDeviceAddress = deviceHistoryManager.getLastConnectedDeviceAddress()
            if (lastDeviceAddress != null) {
                android.util.Log.d("TrackpadViewModel", "Attempting to reconnect to last device: $lastDeviceAddress")
                connectToDeviceByAddress(lastDeviceAddress)
            } else {
                android.util.Log.d("TrackpadViewModel", "No last device found, waiting for manual connection")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Don't cleanup singleton service when ViewModel is destroyed
        // Service will be cleaned up when application is destroyed
    }
}
