package com.dps.droidpadmacos.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Manages USB connection events and detection
 */
class UsbConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "UsbConnectionManager"

        // Apple's USB Vendor ID
        private const val APPLE_VENDOR_ID = 0x05AC
    }

    interface UsbConnectionListener {
        fun onUsbDeviceAttached(device: UsbDevice, isMac: Boolean)
        fun onUsbDeviceDetached(device: UsbDevice)
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var listener: UsbConnectionListener? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    device?.let {
                        Log.d(TAG, "USB device attached: ${it.deviceName}")
                        val isMac = isMacDevice(it)
                        listener?.onUsbDeviceAttached(it, isMac)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    device?.let {
                        Log.d(TAG, "USB device detached: ${it.deviceName}")
                        listener?.onUsbDeviceDetached(it)
                    }
                }
            }
        }
    }

    init {
        registerReceiver()
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(
            context,
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d(TAG, "USB receiver registered")
    }

    fun setListener(listener: UsbConnectionListener?) {
        this.listener = listener
    }

    /**
     * Check if any USB device is currently connected
     */
    fun isUsbConnected(): Boolean {
        val deviceList = usbManager.deviceList
        return deviceList.isNotEmpty()
    }

    /**
     * Get the currently connected USB device
     */
    fun getConnectedDevice(): UsbDevice? {
        val deviceList = usbManager.deviceList
        return deviceList.values.firstOrNull()
    }

    /**
     * Check if connected device is a Mac
     */
    fun isConnectedToMac(): Boolean {
        val device = getConnectedDevice() ?: return false
        return isMacDevice(device)
    }

    /**
     * Detect if a USB device is a Mac
     */
    private fun isMacDevice(device: UsbDevice): Boolean {
        val manufacturer = device.manufacturerName ?: ""
        val product = device.productName ?: ""

        return manufacturer.contains("Apple", ignoreCase = true) ||
               device.vendorId == APPLE_VENDOR_ID ||
               product.contains("Mac", ignoreCase = true)
    }

    /**
     * Get device information
     */
    fun getDeviceInfo(device: UsbDevice): DeviceInfo {
        return DeviceInfo(
            name = device.deviceName,
            manufacturer = device.manufacturerName,
            product = device.productName,
            vendorId = device.vendorId,
            productId = device.productId,
            isMac = isMacDevice(device)
        )
    }

    fun unregister() {
        try {
            context.unregisterReceiver(usbReceiver)
            Log.d(TAG, "USB receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering USB receiver", e)
        }
    }

    data class DeviceInfo(
        val name: String,
        val manufacturer: String?,
        val product: String?,
        val vendorId: Int,
        val productId: Int,
        val isMac: Boolean
    )
}
