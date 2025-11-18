package com.dps.droidpadmacos.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

/**
 * USB HID Service for Android USB Accessory Mode
 * Enables Android device to act as a USB HID trackpad
 */
class UsbHidService(private val context: Context) : Closeable {

    companion object {
        private const val TAG = "UsbHidService"
        private const val ACTION_USB_PERMISSION = "com.dps.droidpadmacos.USB_PERMISSION"

        // HID Report Descriptor for Mouse/Trackpad
        private val MOUSE_REPORT_DESCRIPTOR = byteArrayOf(
            0x05, 0x01.toByte(),  // USAGE_PAGE (Generic Desktop)
            0x09, 0x02,           // USAGE (Mouse)
            0xa1.toByte(), 0x01,  // COLLECTION (Application)
            0x09, 0x01,           //   USAGE (Pointer)
            0xa1.toByte(), 0x00,  //   COLLECTION (Physical)
            0x05, 0x09,           //     USAGE_PAGE (Button)
            0x19, 0x01,           //     USAGE_MINIMUM (Button 1)
            0x29, 0x03,           //     USAGE_MAXIMUM (Button 3)
            0x15, 0x00,           //     LOGICAL_MINIMUM (0)
            0x25, 0x01,           //     LOGICAL_MAXIMUM (1)
            0x95.toByte(), 0x03,  //     REPORT_COUNT (3)
            0x75, 0x01,           //     REPORT_SIZE (1)
            0x81.toByte(), 0x02,  //     INPUT (Data,Var,Abs)
            0x95.toByte(), 0x01,  //     REPORT_COUNT (1)
            0x75, 0x05,           //     REPORT_SIZE (5)
            0x81.toByte(), 0x03,  //     INPUT (Cnst,Var,Abs)
            0x05, 0x01,           //     USAGE_PAGE (Generic Desktop)
            0x09, 0x30,           //     USAGE (X)
            0x09, 0x31,           //     USAGE (Y)
            0x09, 0x38,           //     USAGE (Wheel)
            0x15, 0x81.toByte(),  //     LOGICAL_MINIMUM (-127)
            0x25, 0x7f,           //     LOGICAL_MAXIMUM (127)
            0x75, 0x08,           //     REPORT_SIZE (8)
            0x95.toByte(), 0x03,  //     REPORT_COUNT (3)
            0x81.toByte(), 0x06,  //     INPUT (Data,Var,Rel)
            0xc0.toByte(),        //   END_COLLECTION
            0xc0.toByte()         // END_COLLECTION
        )
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val deviceName: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let {
                                Log.d(TAG, "Permission granted for device ${it.deviceName}")
                                connectToDevice(it)
                            }
                        } else {
                            Log.d(TAG, "Permission denied for device $device")
                            _connectionState.value = ConnectionState.Error("USB permission denied")
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (device == usbDevice) {
                        Log.d(TAG, "USB device detached")
                        disconnect()
                    }
                }
            }
        }
    }

    init {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        // Register USB receivers
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(
            context,
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Initialize USB HID connection
     */
    fun initialize() {
        val manager = usbManager ?: run {
            _connectionState.value = ConnectionState.Error("USB Manager not available")
            return
        }

        val deviceList = manager.deviceList
        if (deviceList.isEmpty()) {
            _connectionState.value = ConnectionState.Error("No USB devices found")
            return
        }

        // Get the first connected device (typically the host computer)
        val device = deviceList.values.firstOrNull() ?: run {
            _connectionState.value = ConnectionState.Error("No USB device available")
            return
        }

        // Request permission
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        manager.requestPermission(device, permissionIntent)
    }

    private fun connectToDevice(device: UsbDevice) {
        _connectionState.value = ConnectionState.Connecting

        try {
            val manager = usbManager ?: throw IllegalStateException("USB Manager not available")

            // Find appropriate interface
            val usbInterface = findHidInterface(device) ?: run {
                // If no HID interface, try to use the first available interface
                if (device.interfaceCount > 0) {
                    device.getInterface(0)
                } else {
                    throw IllegalStateException("No suitable USB interface found")
                }
            }

            val connection = manager.openDevice(device) ?: throw IllegalStateException("Failed to open USB device")

            if (!connection.claimInterface(usbInterface, true)) {
                connection.close()
                throw IllegalStateException("Failed to claim USB interface")
            }

            this.usbDevice = device
            this.usbConnection = connection
            this.usbInterface = usbInterface

            val deviceName = device.productName ?: device.manufacturerName ?: "Unknown Device"
            _connectionState.value = ConnectionState.Connected(deviceName)

            Log.d(TAG, "Connected to USB device: $deviceName")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to USB device", e)
            _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
            disconnect()
        }
    }

    private fun findHidInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            // Look for HID interface (class 3)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_HID) {
                return intf
            }
        }
        return null
    }

    /**
     * Send mouse movement
     */
    fun sendMouseMovement(deltaX: Int, deltaY: Int) {
        if (_connectionState.value !is ConnectionState.Connected) {
            Log.w(TAG, "Not connected - cannot send mouse movement")
            return
        }

        try {
            // HID Report: [buttons, x, y, wheel]
            val report = byteArrayOf(
                0x00,                           // No buttons pressed
                deltaX.coerceIn(-127, 127).toByte(),  // X movement
                deltaY.coerceIn(-127, 127).toByte(),  // Y movement
                0x00                            // No wheel
            )

            sendHidReport(report)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send mouse movement", e)
        }
    }

    /**
     * Send mouse click
     */
    fun sendMouseClick(button: MouseButton) {
        if (_connectionState.value !is ConnectionState.Connected) {
            Log.w(TAG, "Not connected - cannot send mouse click")
            return
        }

        try {
            val buttonByte = when (button) {
                MouseButton.LEFT -> 0x01
                MouseButton.RIGHT -> 0x02
                MouseButton.MIDDLE -> 0x04
            }

            // Press
            val pressReport = byteArrayOf(buttonByte.toByte(), 0x00, 0x00, 0x00)
            sendHidReport(pressReport)

            // Release
            Thread.sleep(10)
            val releaseReport = byteArrayOf(0x00, 0x00, 0x00, 0x00)
            sendHidReport(releaseReport)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send mouse click", e)
        }
    }

    /**
     * Send scroll wheel
     */
    fun sendScroll(deltaY: Int) {
        if (_connectionState.value !is ConnectionState.Connected) {
            Log.w(TAG, "Not connected - cannot send scroll")
            return
        }

        try {
            val report = byteArrayOf(
                0x00,                           // No buttons
                0x00,                           // No X movement
                0x00,                           // No Y movement
                deltaY.coerceIn(-127, 127).toByte()  // Wheel
            )

            sendHidReport(report)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send scroll", e)
        }
    }

    /**
     * Press and hold mouse button (for dragging)
     */
    fun sendMouseButtonPress(button: MouseButton) {
        if (_connectionState.value !is ConnectionState.Connected) {
            Log.w(TAG, "Not connected - cannot send mouse button press")
            return
        }

        try {
            val buttonByte = when (button) {
                MouseButton.LEFT -> 0x01
                MouseButton.RIGHT -> 0x02
                MouseButton.MIDDLE -> 0x04
            }

            // Press button (don't release)
            val pressReport = byteArrayOf(buttonByte.toByte(), 0x00, 0x00, 0x00)
            sendHidReport(pressReport)
            Log.d(TAG, "Mouse button pressed: $button")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send mouse button press", e)
        }
    }

    /**
     * Release mouse button
     */
    fun sendMouseButtonRelease() {
        if (_connectionState.value !is ConnectionState.Connected) {
            Log.w(TAG, "Not connected - cannot send mouse button release")
            return
        }

        try {
            // Release all buttons
            val releaseReport = byteArrayOf(0x00, 0x00, 0x00, 0x00)
            sendHidReport(releaseReport)
            Log.d(TAG, "Mouse button released")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send mouse button release", e)
        }
    }

    private fun sendHidReport(report: ByteArray) {
        val connection = usbConnection ?: return
        val intf = usbInterface ?: return

        // Try to find interrupt OUT endpoint
        var endpoint = intf.getEndpoint(0)

        // If we have endpoints, use bulk or interrupt transfer
        if (endpoint != null) {
            val result = connection.bulkTransfer(endpoint, report, report.size, 100)
            if (result < 0) {
                Log.w(TAG, "Failed to send HID report via bulk transfer")
            }
        } else {
            // Fallback: try control transfer
            val result = connection.controlTransfer(
                0x21, // REQUEST_TYPE_CLASS | RECIPIENT_INTERFACE
                0x09, // HID SET_REPORT
                0x0200, // Report Type: Output
                0,    // Interface
                report,
                report.size,
                100
            )
            if (result < 0) {
                Log.w(TAG, "Failed to send HID report via control transfer")
            }
        }
    }

    fun disconnect() {
        try {
            usbInterface?.let { intf ->
                usbConnection?.releaseInterface(intf)
            }
            usbConnection?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            usbConnection = null
            usbInterface = null
            usbDevice = null
            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "Disconnected from USB device")
        }
    }

    override fun close() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        disconnect()
    }

    enum class MouseButton {
        LEFT, RIGHT, MIDDLE
    }
}
