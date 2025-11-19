package com.dps.droidpadmacos.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.dps.droidpadmacos.util.Logger
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
            Logger.d(TAG, "onAppStatusChanged: registered=$registered, device=${pluggedDevice?.name}")
            _isRegistered.value = registered

            if (registered) {
                _connectionState.value = ConnectionState.Registered
            } else {
                _connectionState.value = ConnectionState.Disconnected
                connectedDevice = null
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Logger.d(TAG, "onConnectionStateChanged: state=$state, device=${device?.name}, address=${device?.address}")

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
            Logger.d(TAG, "onGetReport: type=$type, id=$id, bufferSize=$bufferSize")
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            Logger.d(TAG, "onSetReport: type=$type, id=$id")
        }

        override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
            Logger.d(TAG, "onSetProtocol: protocol=$protocol")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            Logger.d(TAG, "onInterruptData: reportId=$reportId")
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            Logger.d(TAG, "onVirtualCableUnplug: device=${device?.name}")
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
            Logger.d(TAG, "HID Device profile connected")
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                _isProfileReady.value = true
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            Logger.d(TAG, "HID Device profile disconnected")
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _isProfileReady.value = false
            }
        }
    }

    fun initialize() {
        try {
            if (bluetoothAdapter == null) {
                val errorMsg = "Bluetooth is not supported on this device"
                Logger.e(TAG, errorMsg)
                _connectionState.value = ConnectionState.Error(errorMsg)
                return
            }

            if (!bluetoothAdapter.isEnabled) {
                val errorMsg = "Bluetooth is currently disabled. Please enable it in Settings."
                Logger.w(TAG, errorMsg)
                _connectionState.value = ConnectionState.Error(errorMsg)
                return
            }

            // Set the Bluetooth device name
            try {
                bluetoothAdapter.name = HidConstants.DEVICE_NAME
                Logger.d(TAG, "Set Bluetooth name to: ${HidConstants.DEVICE_NAME}")
            } catch (e: SecurityException) {
                Logger.e(TAG, "Security exception while setting Bluetooth name - missing permissions?", e)
                _connectionState.value = ConnectionState.Error("Bluetooth permissions required")
                return
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set Bluetooth name", e)
                // Non-critical - continue initialization
            }

            // Get HID Device profile with error handling
            try {
                val result = bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
                if (!result) {
                    val errorMsg = "Failed to get Bluetooth HID profile"
                    Logger.e(TAG, errorMsg)
                    _connectionState.value = ConnectionState.Error(errorMsg)
                }
            } catch (e: SecurityException) {
                Logger.e(TAG, "Security exception while getting HID profile", e)
                _connectionState.value = ConnectionState.Error("Bluetooth permissions required")
            } catch (e: Exception) {
                Logger.e(TAG, "Unexpected error during HID profile initialization", e)
                _connectionState.value = ConnectionState.Error("Failed to initialize Bluetooth: ${e.message}")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Critical error during Bluetooth initialization", e)
            _connectionState.value = ConnectionState.Error("Bluetooth initialization failed: ${e.message}")
        }
    }

    fun registerHidDevice(): Boolean {
        return try {
            val device = hidDevice
            if (device == null) {
                val errorMsg = "HID Device profile not available. Please wait for Bluetooth initialization."
                Logger.e(TAG, errorMsg)
                _connectionState.value = ConnectionState.Error(errorMsg)
                return false
            }

            Logger.d(TAG, "Registering HID device...")
            _connectionState.value = ConnectionState.Registering

            val sdpSettings = HidConstants.getSdpSettings()
            val qosSettings = HidConstants.getQosSettings()

            Logger.d(TAG, "SDP Settings - Name: ${sdpSettings.name}, Subclass: 0x${sdpSettings.subclass.toString(16)}")

            val result = device.registerApp(
                sdpSettings,
                null,
                qosSettings,
                { it.run() },
                hidDeviceCallback
            )

            Logger.d(TAG, "Registration result: $result")

            if (!result) {
                val errorMsg = "Failed to register HID device with system"
                Logger.e(TAG, errorMsg)
                _connectionState.value = ConnectionState.Error(errorMsg)
            }

            result
        } catch (e: SecurityException) {
            val errorMsg = "Bluetooth permissions required for HID registration"
            Logger.e(TAG, errorMsg, e)
            _connectionState.value = ConnectionState.Error(errorMsg)
            false
        } catch (e: IllegalStateException) {
            val errorMsg = "Bluetooth is in invalid state for registration"
            Logger.e(TAG, errorMsg, e)
            _connectionState.value = ConnectionState.Error(errorMsg)
            false
        } catch (e: Exception) {
            val errorMsg = "Unexpected error during HID registration: ${e.message}"
            Logger.e(TAG, errorMsg, e)
            _connectionState.value = ConnectionState.Error(errorMsg)
            false
        }
    }

    fun unregisterHidDevice() {
        hidDevice?.unregisterApp()
        _isRegistered.value = false
        _connectionState.value = ConnectionState.Disconnected
    }

    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val hidDev = hidDevice
            if (hidDev == null) {
                Logger.w(TAG, "Cannot connect: HID Device profile not available")
                return false
            }

            if (!_isRegistered.value) {
                Logger.w(TAG, "Cannot connect: HID device not registered")
                return false
            }

            Logger.d(TAG, "Attempting to connect to device: ${device.name} (${device.address})")
            hidDev.connect(device)
        } catch (e: SecurityException) {
            Logger.e(TAG, "Security exception during connection - missing permissions?", e)
            _connectionState.value = ConnectionState.Error("Bluetooth permissions required")
            false
        } catch (e: Exception) {
            Logger.e(TAG, "Error connecting to device", e)
            _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
            false
        }
    }

    fun disconnect() {
        val device = connectedDevice ?: return
        hidDevice?.disconnect(device)
    }

    fun sendMouseReport(buttons: Byte, x: Byte, y: Byte, wheel: Byte): Boolean {
        val device = connectedDevice
        val hid = hidDevice

        if (device == null || hid == null) {
            Logger.w(TAG, "Cannot send report: device or HID not available")
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
            Logger.d(TAG, "Sending mouse report - X: $x, Y: $y, Buttons: $buttons, Wheel: $wheel, Report array: ${report.contentToString()}")
        }

        return hid.sendReport(device, HidConstants.REPORT_ID_MOUSE.toInt(), report)
    }

    fun sendKeyboardReport(modifiers: Byte, key1: Byte = 0, key2: Byte = 0, key3: Byte = 0, key4: Byte = 0, key5: Byte = 0, key6: Byte = 0): Boolean {
        val device = connectedDevice
        val hid = hidDevice

        if (device == null || hid == null) {
            Logger.w(TAG, "Cannot send keyboard report: device or HID not available")
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

        Logger.d(TAG, "Sending keyboard report - Modifiers: $modifiers, Keys: [$key1, $key2, $key3, $key4, $key5, $key6]")
        return hid.sendReport(device, HidConstants.REPORT_ID_KEYBOARD.toInt(), report)
    }

    suspend fun sendKeyPress(modifiers: Byte, keyCode: Byte) {
        Logger.d(TAG, "sendKeyPress - Modifiers: ${modifiers.toInt()}, Key: ${keyCode.toInt()}")

        // Press (modifiers + key)
        val pressResult = sendKeyboardReport(modifiers, keyCode)
        Logger.d(TAG, "Key press result: $pressResult")

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
        Logger.d(TAG, "Key release result: $releaseResult")
    }

    fun sendConsumerReport(controlBits: Byte): Boolean {
        val device = connectedDevice
        val hid = hidDevice

        if (device == null || hid == null) {
            Logger.w(TAG, "Cannot send consumer report: device or HID not available")
            return false
        }

        // Consumer report format: [control bits] - 1 byte where each bit is a different control
        val report = byteArrayOf(controlBits)

        Logger.d(TAG, "Sending consumer report - Control bits: ${controlBits.toInt()}")
        return hid.sendReport(device, HidConstants.REPORT_ID_CONSUMER.toInt(), report)
    }

    suspend fun sendPlayPause() {
        Logger.d(TAG, "=== Sending Play/Pause (Consumer Control) ===")

        // Press
        sendConsumerReport(HidConstants.CONSUMER_PLAY_PAUSE)

        // Small delay
        kotlinx.coroutines.delay(50)

        // Release
        sendConsumerReport(HidConstants.CONSUMER_NONE)
    }

    suspend fun sendNextTrack() {
        Logger.d(TAG, "=== Sending Next Track (Consumer Control) ===")

        // Press
        sendConsumerReport(HidConstants.CONSUMER_NEXT_TRACK)

        // Small delay
        kotlinx.coroutines.delay(50)

        // Release
        sendConsumerReport(HidConstants.CONSUMER_NONE)
    }

    suspend fun sendPreviousTrack() {
        Logger.d(TAG, "=== Sending Previous Track (Consumer Control) ===")

        // Press
        sendConsumerReport(HidConstants.CONSUMER_PREV_TRACK)

        // Small delay
        kotlinx.coroutines.delay(50)

        // Release
        sendConsumerReport(HidConstants.CONSUMER_NONE)
    }

    fun sendAppleVendorReport(controlBits: Byte): Boolean {
        val device = connectedDevice
        val hid = hidDevice

        if (device == null || hid == null) {
            Logger.w(TAG, "Cannot send Apple vendor report: device or HID not available")
            return false
        }

        // Apple vendor report format: [control bits] - 1 byte where each bit is a different control
        val report = byteArrayOf(controlBits)

        Logger.d(TAG, "Sending Apple vendor report - Control bits: ${controlBits.toInt()}")
        return hid.sendReport(device, HidConstants.REPORT_ID_APPLE_VENDOR.toInt(), report)
    }

    suspend fun sendVolumeUp() {
        Logger.d(TAG, "=== Sending Volume Up (Consumer Control) ===")

        // Press
        sendConsumerReport(HidConstants.CONSUMER_VOLUME_UP)

        // Small delay
        kotlinx.coroutines.delay(50)

        // Release
        sendConsumerReport(HidConstants.CONSUMER_NONE)
    }

    suspend fun sendVolumeDown() {
        Logger.d(TAG, "=== Sending Volume Down (Consumer Control) ===")

        // Press
        sendConsumerReport(HidConstants.CONSUMER_VOLUME_DOWN)

        // Small delay
        kotlinx.coroutines.delay(50)

        // Release
        sendConsumerReport(HidConstants.CONSUMER_NONE)
    }

    suspend fun sendMute() {
        Logger.d(TAG, "=== Sending Mute (Consumer Control) ===")

        // Press
        sendConsumerReport(HidConstants.CONSUMER_MUTE)

        // Small delay
        kotlinx.coroutines.delay(50)

        // Release
        sendConsumerReport(HidConstants.CONSUMER_NONE)
    }

    suspend fun sendBrightnessUp() {
        Logger.d(TAG, "=== Sending Brightness Up (Apple Vendor) ===")

        // Press
        sendAppleVendorReport(HidConstants.APPLE_BRIGHTNESS_UP)

        // Small delay
        kotlinx.coroutines.delay(50)

        // Release
        sendAppleVendorReport(HidConstants.APPLE_NONE)
    }

    suspend fun sendBrightnessDown() {
        Logger.d(TAG, "=== Sending Brightness Down (Apple Vendor) ===")

        // Press
        sendAppleVendorReport(HidConstants.APPLE_BRIGHTNESS_DOWN)

        // Small delay
        kotlinx.coroutines.delay(50)

        // Release
        sendAppleVendorReport(HidConstants.APPLE_NONE)
    }

    suspend fun sendMissionControl() {
        // Mac Mission Control: Ctrl + Up Arrow
        Logger.d(TAG, "=== Sending Mission Control command (Ctrl+Up) ===")
        sendKeyPress(HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_UP_ARROW)
    }

    suspend fun sendShowDesktop() {
        // Mac Show Desktop: F11
        Logger.d(TAG, "=== Sending Show Desktop command (F11) ===")
        sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F11)
    }

    suspend fun sendAppSwitcher() {
        // Mac App Switcher: Cmd + Tab
        Logger.d(TAG, "=== Sending App Switcher command (Cmd+Tab) ===")
        sendKeyPress(HidConstants.MOD_LEFT_GUI, HidConstants.KEY_TAB)
    }

    suspend fun sendSwitchToPreviousDesktop() {
        // Mac Switch to Previous Desktop/Space: Ctrl + Left Arrow
        Logger.d(TAG, "=== Sending Switch to Previous Desktop command (Ctrl+Left) ===")
        sendKeyPress(HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_LEFT_ARROW)
    }

    suspend fun sendSwitchToNextDesktop() {
        // Mac Switch to Next Desktop/Space: Ctrl + Right Arrow
        Logger.d(TAG, "=== Sending Switch to Next Desktop command (Ctrl+Right) ===")
        sendKeyPress(HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_RIGHT_ARROW)
    }

    fun makeDiscoverable() {
        // The device needs to be discoverable for initial pairing
        // This is typically done through system settings, but we can log it
        Logger.d(TAG, "Device should be made discoverable through Bluetooth settings")
    }

    fun resetAndClearConnections(): Boolean {
        try {
            Logger.d(TAG, "Resetting all connections...")

            // First disconnect current device
            connectedDevice?.let { device ->
                Logger.d(TAG, "Disconnecting from: ${device.name}")
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
                    Logger.d(TAG, "Attempting to remove bond with: ${device.name} (${device.address})")
                    val removeBondMethod = device.javaClass.getMethod("removeBond")
                    removeBondMethod.invoke(device)
                } catch (e: Exception) {
                    Logger.w(TAG, "Could not remove bond for ${device.name}: ${e.message}")
                }
            }

            connectedDevice = null
            _connectionState.value = ConnectionState.Disconnected

            Logger.d(TAG, "Reset complete")
            return true
        } catch (e: Exception) {
            Logger.e(TAG, "Error during reset", e)
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
