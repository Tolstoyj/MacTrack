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
import java.util.concurrent.atomic.AtomicInteger

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

    // Mouse movement accumulator for smoother movement (thread-safe with atomic operations)
    private val accumulatedX = AtomicInteger(0)
    private val accumulatedY = AtomicInteger(0)
    private val SCALE_FACTOR = 100 // To convert float to int for atomic operations

    // Current mouse button state so movement reports include pressed buttons (for drag/select)
    private val currentButtons = AtomicInteger(HidConstants.BUTTON_NONE.toInt())

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
        android.util.Log.d("TrackpadViewModel", "connectToDevice() called for: ${device.name} (${device.address})")
        viewModelScope.launch {
            android.util.Log.d("TrackpadViewModel", "Calling bluetoothService.connect()...")
            bluetoothService.connect(device)
            android.util.Log.d("TrackpadViewModel", "bluetoothService.connect() returned")
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bluetoothService.disconnect()
        }
    }

    fun sendMouseMovement(deltaX: Int, deltaY: Int) {
        // Accumulate movement using atomic operations
        val newX = accumulatedX.addAndGet(deltaX * SCALE_FACTOR)
        val newY = accumulatedY.addAndGet(deltaY * SCALE_FACTOR)

        // Convert back from scaled integer to actual value
        val currentX = newX / SCALE_FACTOR
        val currentY = newY / SCALE_FACTOR

        // Send movement in chunks that fit in byte range (-127 to 127)
        val sendX = currentX.coerceIn(-127, 127).toByte()
        val sendY = currentY.coerceIn(-127, 127).toByte()

        if (sendX != 0.toByte() || sendY != 0.toByte()) {
            val buttons = currentButtons.get().toByte()
            android.util.Log.d(
                "TrackpadViewModel",
                "Sending to BT - X: $sendX, Y: $sendY, Buttons: $buttons (input was X:$deltaX, Y:$deltaY)"
            )
            bluetoothService.sendMouseReport(
                buttons,
                sendX,
                sendY,
                0
            )

            // Subtract what we sent from accumulator (thread-safe)
            accumulatedX.addAndGet(-sendX.toInt() * SCALE_FACTOR)
            accumulatedY.addAndGet(-sendY.toInt() * SCALE_FACTOR)
        }
    }

    fun sendLeftClick() {
        viewModelScope.launch {
            // Press
            sendMouseButtonPress(HidConstants.BUTTON_LEFT)
            // Small delay
            kotlinx.coroutines.delay(50)
            // Release
            sendMouseButtonRelease()
        }
    }

    fun sendRightClick() {
        viewModelScope.launch {
            // Press
            sendMouseButtonPress(HidConstants.BUTTON_RIGHT)
            // Small delay
            kotlinx.coroutines.delay(50)
            // Release
            sendMouseButtonRelease()
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

    fun sendSwitchToPreviousDesktop() {
        viewModelScope.launch {
            bluetoothService.sendSwitchToPreviousDesktop()
        }
    }

    fun sendSwitchToNextDesktop() {
        viewModelScope.launch {
            bluetoothService.sendSwitchToNextDesktop()
        }
    }

    fun sendSpotlight() {
        viewModelScope.launch {
            // Cmd + Space for Spotlight
            bluetoothService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_SPACE)
        }
    }

    fun sendKeyPress(keyCode: Byte) {
        // Convenience overload for keys without modifiers
        sendKeyPress(HidConstants.MOD_NONE, keyCode)
    }

    fun sendKeyPress(modifiers: Byte, keyCode: Byte) {
        viewModelScope.launch {
            bluetoothService.sendKeyPress(modifiers, keyCode)
        }
    }

    fun sendMouseButtonPress(button: Byte) {
        // Update current button state and send a button-only report
        val newButtons = currentButtons.updateAndGet { it or button.toInt() }.toByte()
        bluetoothService.sendMouseReport(newButtons, 0, 0, 0)
    }

    fun sendMouseButtonRelease() {
        // Clear all buttons and send a button-only report
        currentButtons.set(HidConstants.BUTTON_NONE.toInt())
        bluetoothService.sendMouseReport(HidConstants.BUTTON_NONE, 0, 0, 0)
    }

    // Common macOS keyboard shortcuts
    fun sendCopy() {
        viewModelScope.launch {
            bluetoothService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_C)
        }
    }

    fun sendPaste() {
        viewModelScope.launch {
            bluetoothService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_V)
        }
    }

    fun sendCut() {
        viewModelScope.launch {
            bluetoothService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_X)
        }
    }

    fun sendUndo() {
        viewModelScope.launch {
            bluetoothService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_Z)
        }
    }

    fun sendSelectAll() {
        viewModelScope.launch {
            bluetoothService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_A)
        }
    }

    fun sendCloseWindow() {
        viewModelScope.launch {
            bluetoothService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_W)
        }
    }

    fun sendQuitApp() {
        viewModelScope.launch {
            bluetoothService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_Q)
        }
    }

    fun sendNewTab() {
        viewModelScope.launch {
            bluetoothService.sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_T)
        }
    }

    // Mac Volume Controls (Consumer Control HID)
    fun sendVolumeUp() {
        viewModelScope.launch {
            bluetoothService.sendVolumeUp()
        }
    }

    fun sendVolumeDown() {
        viewModelScope.launch {
            bluetoothService.sendVolumeDown()
        }
    }

    fun sendMute() {
        viewModelScope.launch {
            bluetoothService.sendMute()
        }
    }

    fun sendPlayPause() {
        viewModelScope.launch {
            bluetoothService.sendPlayPause()
        }
    }

    fun sendNextTrack() {
        viewModelScope.launch {
            bluetoothService.sendNextTrack()
        }
    }

    fun sendPreviousTrack() {
        viewModelScope.launch {
            bluetoothService.sendPreviousTrack()
        }
    }

    // Mac Brightness Controls (Apple Vendor HID)
    fun sendBrightnessUp() {
        viewModelScope.launch {
            bluetoothService.sendBrightnessUp()
        }
    }

    fun sendBrightnessDown() {
        viewModelScope.launch {
            bluetoothService.sendBrightnessDown()
        }
    }

    fun connectToDeviceByAddress(address: String): Boolean {
        android.util.Log.d("TrackpadViewModel", "=== Quick Connect Started ===")
        android.util.Log.d("TrackpadViewModel", "Attempting to connect to device: $address")
        android.util.Log.d("TrackpadViewModel", "Current connection state: ${connectionState.value}")
        android.util.Log.d("TrackpadViewModel", "Is registered: ${isRegistered.value}")
        android.util.Log.d("TrackpadViewModel", "Is profile ready: ${isProfileReady.value}")

        // Check if HID is registered
        if (!isRegistered.value) {
            android.util.Log.w("TrackpadViewModel", "HID device not registered! Registering first...")
            registerDevice()
            // Give it a moment to register, then try connecting
            viewModelScope.launch {
                kotlinx.coroutines.delay(1500)
                val device = bluetoothAdapter?.getRemoteDevice(address)
                if (device != null) {
                    android.util.Log.d("TrackpadViewModel", "After registration, connecting to: ${device.name} (${device.address})")
                    connectToDevice(device)
                } else {
                    android.util.Log.e("TrackpadViewModel", "Device not found after registration: $address")
                }
            }
            return true
        }

        val device = bluetoothAdapter?.getRemoteDevice(address)
        return if (device != null) {
            android.util.Log.d("TrackpadViewModel", "Device found: ${device.name} (${device.address})")
            android.util.Log.d("TrackpadViewModel", "Calling connectToDevice()...")
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
