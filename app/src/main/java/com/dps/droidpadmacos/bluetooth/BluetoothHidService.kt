package com.dps.droidpadmacos.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class BluetoothHidService private constructor(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _isProfileReady = MutableStateFlow(false)
    val isProfileReady: StateFlow<Boolean> = _isProfileReady.asStateFlow()

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Registering : ConnectionState()
        object Registered : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val deviceName: String, val deviceAddress: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    private val hidDeviceCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered, device=${pluggedDevice?.name}")
            _isRegistered.value = registered

            if (registered) {
                _connectionState.value = ConnectionState.Registered
            } else {
                _connectionState.value = ConnectionState.Disconnected
                connectedDevice = null
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d(TAG, "onConnectionStateChanged: state=$state, device=${device?.name}, address=${device?.address}")

            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    _connectionState.value = ConnectionState.Connected(
                        device?.name ?: "Unknown",
                        device?.address ?: ""
                    )
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.Connecting
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevice = null
                    if (_isRegistered.value) {
                        _connectionState.value = ConnectionState.Registered
                    } else {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "onGetReport: type=$type, id=$id, bufferSize=$bufferSize")
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            Log.d(TAG, "onSetReport: type=$type, id=$id")
        }

        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            Log.d(TAG, "onSetProtocol: protocol=$protocol")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            Log.d(TAG, "onInterruptData: reportId=$reportId")
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            Log.d(TAG, "onVirtualCableUnplug: device=${device?.name}")
            connectedDevice = null
            _connectionState.value = if (_isRegistered.value) {
                ConnectionState.Registered
            } else {
                ConnectionState.Disconnected
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            Log.d(TAG, "HID Device profile connected")
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                _isProfileReady.value = true
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            Log.d(TAG, "HID Device profile disconnected")
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _isProfileReady.value = false
            }
        }
    }

    fun initialize() {
        if (bluetoothAdapter == null) {
            _connectionState.value = ConnectionState.Error("Bluetooth not supported")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is disabled")
            return
        }

        // Set the Bluetooth device name
        try {
            bluetoothAdapter.name = HidConstants.DEVICE_NAME
            Log.d(TAG, "Set Bluetooth name to: ${HidConstants.DEVICE_NAME}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Bluetooth name", e)
        }

        bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
    }

    fun registerHidDevice(): Boolean {
        val device = hidDevice
        if (device == null) {
            Log.e(TAG, "HID Device profile not available")
            _connectionState.value = ConnectionState.Error("HID Device profile not available")
            return false
        }

        Log.d(TAG, "Registering HID device...")
        _connectionState.value = ConnectionState.Registering

        val sdpSettings = HidConstants.getSdpSettings()
        val qosSettings = HidConstants.getQosSettings()

        Log.d(TAG, "SDP Settings - Name: ${sdpSettings.name}, Subclass: 0x${sdpSettings.subclass.toString(16)}")

        val result = device.registerApp(
            sdpSettings,
            null,
            qosSettings,
            { it.run() },
            hidDeviceCallback
        )

        Log.d(TAG, "Registration result: $result")

        if (!result) {
            _connectionState.value = ConnectionState.Error("Failed to register HID device")
        }

        return result
    }

    fun unregisterHidDevice() {
        hidDevice?.unregisterApp()
        _isRegistered.value = false
        _connectionState.value = ConnectionState.Disconnected
    }

    fun connect(device: BluetoothDevice): Boolean {
        val hidDev = hidDevice ?: return false
        if (!_isRegistered.value) return false

        return hidDev.connect(device)
    }

    fun disconnect() {
        val device = connectedDevice ?: return
        hidDevice?.disconnect(device)
    }

    fun sendMouseReport(buttons: Byte, x: Byte, y: Byte, wheel: Byte): Boolean {
        val device = connectedDevice
        val hid = hidDevice

        if (device == null || hid == null) {
            Log.w(TAG, "Cannot send report: device or HID not available")
            return false
        }

        // Don't include Report ID in the data - it's specified in sendReport() parameter
        val report = byteArrayOf(
            buttons,
            x,
            y,
            wheel
        )

        if (x != 0.toByte() || y != 0.toByte()) {
            Log.d(TAG, "Sending mouse report - X: $x, Y: $y, Buttons: $buttons, Wheel: $wheel, Report array: ${report.contentToString()}")
        }

        return hid.sendReport(device, HidConstants.REPORT_ID_MOUSE.toInt(), report)
    }

    fun sendKeyboardReport(modifiers: Byte, key1: Byte = 0, key2: Byte = 0, key3: Byte = 0, key4: Byte = 0, key5: Byte = 0, key6: Byte = 0): Boolean {
        val device = connectedDevice
        val hid = hidDevice

        if (device == null || hid == null) {
            Log.w(TAG, "Cannot send keyboard report: device or HID not available")
            return false
        }

        // Keyboard report format: [modifiers, reserved, key1, key2, key3, key4, key5, key6]
        val report = byteArrayOf(
            modifiers,
            0x00,  // Reserved byte
            key1,
            key2,
            key3,
            key4,
            key5,
            key6
        )

        Log.d(TAG, "Sending keyboard report - Modifiers: $modifiers, Keys: [$key1, $key2, $key3, $key4, $key5, $key6]")
        return hid.sendReport(device, HidConstants.REPORT_ID_KEYBOARD.toInt(), report)
    }

    suspend fun sendKeyPress(modifiers: Byte, keyCode: Byte) {
        Log.d(TAG, "sendKeyPress - Modifiers: ${modifiers.toInt()}, Key: ${keyCode.toInt()}")

        // Press (modifiers + key)
        val pressResult = sendKeyboardReport(modifiers, keyCode)
        Log.d(TAG, "Key press result: $pressResult")

        // Small delay
        kotlinx.coroutines.delay(100)

        // Release (all zeros)
        val releaseResult = sendKeyboardReport(
            HidConstants.MOD_NONE,
            HidConstants.KEY_NONE,
            HidConstants.KEY_NONE,
            HidConstants.KEY_NONE,
            HidConstants.KEY_NONE,
            HidConstants.KEY_NONE,
            HidConstants.KEY_NONE
        )
        Log.d(TAG, "Key release result: $releaseResult")
    }

    suspend fun sendMissionControl() {
        // Mac Mission Control: Ctrl + Up Arrow
        Log.d(TAG, "=== Sending Mission Control command (Ctrl+Up) ===")
        sendKeyPress(HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_UP_ARROW)
    }

    suspend fun sendShowDesktop() {
        // Mac Show Desktop: F11
        Log.d(TAG, "=== Sending Show Desktop command (F11) ===")
        sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F11)
    }

    suspend fun sendAppSwitcher() {
        // Mac App Switcher: Cmd + Tab
        Log.d(TAG, "=== Sending App Switcher command (Cmd+Tab) ===")
        sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_TAB)
    }

    fun makeDiscoverable() {
        // The device needs to be discoverable for initial pairing
        // This is typically done through system settings, but we can log it
        Log.d(TAG, "Device should be made discoverable through Bluetooth settings")
    }

    fun resetAndClearConnections(): Boolean {
        try {
            Log.d(TAG, "Resetting all connections...")

            // First disconnect current device
            connectedDevice?.let { device ->
                Log.d(TAG, "Disconnecting from: ${device.name}")
                hidDevice?.disconnect(device)
            }

            // Unregister HID device
            unregisterHidDevice()

            // Wait a bit for unregistration
            Thread.sleep(500)

            // Try to unpair bonded devices (requires reflection on some Android versions)
            val bondedDevices = bluetoothAdapter?.bondedDevices
            bondedDevices?.forEach { device ->
                try {
                    Log.d(TAG, "Attempting to remove bond with: ${device.name} (${device.address})")
                    val removeBondMethod = device.javaClass.getMethod("removeBond")
                    removeBondMethod.invoke(device)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not remove bond for ${device.name}: ${e.message}")
                }
            }

            connectedDevice = null
            _connectionState.value = ConnectionState.Disconnected

            Log.d(TAG, "Reset complete")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error during reset", e)
            return false
        }
    }

    fun cleanup() {
        unregisterHidDevice()
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
    }

    companion object {
        private const val TAG = "BluetoothHidService"

        @Volatile
        private var instance: BluetoothHidService? = null

        fun getInstance(context: Context): BluetoothHidService {
            return instance ?: synchronized(this) {
                instance ?: BluetoothHidService(context.applicationContext).also { instance = it }
            }
        }
    }
}
