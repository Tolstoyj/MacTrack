package com.dps.droidpadmacos.bluetooth

import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

object HidConstants {
    // HID Device SDP record
    const val DEVICE_NAME = "DroidPad Trackpad"
    const val DEVICE_DESCRIPTION = "Android Virtual Trackpad"
    const val DEVICE_PROVIDER = "DroidPad"

    // HID Report IDs
    const val REPORT_ID_MOUSE = 1.toByte()
    const val REPORT_ID_KEYBOARD = 2.toByte()

    // HID Descriptor for Mouse with scroll wheel
    // This descriptor defines a standard mouse with left/right buttons, X/Y movement, and scroll wheel
    val MOUSE_REPORT_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(),        // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), REPORT_ID_MOUSE,      //   Report ID (1)
        0x09.toByte(), 0x01.toByte(),        //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(),        //   Collection (Physical)

        // Buttons (3 buttons: left, right, middle)
        0x05.toByte(), 0x09.toByte(),        //     Usage Page (Buttons)
        0x19.toByte(), 0x01.toByte(),        //     Usage Minimum (1)
        0x29.toByte(), 0x03.toByte(),        //     Usage Maximum (3)
        0x15.toByte(), 0x00.toByte(),        //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //     Logical Maximum (1)
        0x95.toByte(), 0x03.toByte(),        //     Report Count (3)
        0x75.toByte(), 0x01.toByte(),        //     Report Size (1)
        0x81.toByte(), 0x02.toByte(),        //     Input (Data, Variable, Absolute)

        // Padding (5 bits to complete the byte)
        0x95.toByte(), 0x01.toByte(),        //     Report Count (1)
        0x75.toByte(), 0x05.toByte(),        //     Report Size (5)
        0x81.toByte(), 0x03.toByte(),        //     Input (Constant)

        // X and Y position (relative movement)
        0x05.toByte(), 0x01.toByte(),        //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),        //     Usage (X)
        0x09.toByte(), 0x31.toByte(),        //     Usage (Y)
        0x15.toByte(), 0x81.toByte(),        //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),        //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),        //     Report Size (8)
        0x95.toByte(), 0x02.toByte(),        //     Report Count (2)
        0x81.toByte(), 0x06.toByte(),        //     Input (Data, Variable, Relative)

        // Scroll wheel (vertical)
        0x09.toByte(), 0x38.toByte(),        //     Usage (Wheel)
        0x15.toByte(), 0x81.toByte(),        //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(),        //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(),        //     Report Size (8)
        0x95.toByte(), 0x01.toByte(),        //     Report Count (1)
        0x81.toByte(), 0x06.toByte(),        //     Input (Data, Variable, Relative)

        0xC0.toByte(),                        //   End Collection (Physical)
        0xC0.toByte()                         // End Collection (Application)
    )

    // HID Descriptor for Keyboard
    val KEYBOARD_REPORT_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),        // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),        // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD,   //   Report ID (2)

        // Modifier keys (Ctrl, Shift, Alt, GUI/Cmd)
        0x05.toByte(), 0x07.toByte(),        //   Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(),        //   Usage Minimum (224)
        0x29.toByte(), 0xE7.toByte(),        //   Usage Maximum (231)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),        //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),        //   Report Count (8)
        0x81.toByte(), 0x02.toByte(),        //   Input (Data, Variable, Absolute)

        // Reserved byte
        0x95.toByte(), 0x01.toByte(),        //   Report Count (1)
        0x75.toByte(), 0x08.toByte(),        //   Report Size (8)
        0x81.toByte(), 0x01.toByte(),        //   Input (Constant)

        // Key codes (6 simultaneous keys)
        0x95.toByte(), 0x06.toByte(),        //   Report Count (6)
        0x75.toByte(), 0x08.toByte(),        //   Report Size (8)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(),        //   Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(),        //   Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(),        //   Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(),        //   Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(),        //   Input (Data, Array)

        0xC0.toByte()                         // End Collection (Application)
    )

    // Combined Mouse + Keyboard Descriptor
    val COMBINED_DESCRIPTOR = MOUSE_REPORT_DESCRIPTOR + KEYBOARD_REPORT_DESCRIPTOR

    // SDP Settings
    // Subclass values from Bluetooth HID spec:
    // 0xC0 = Combo keyboard+pointing device (most compatible)
    // 0x80 = Pointing device only
    // 0x40 = Keyboard only
    fun getSdpSettings(): BluetoothHidDeviceAppSdpSettings {
        return BluetoothHidDeviceAppSdpSettings(
            DEVICE_NAME,
            DEVICE_DESCRIPTION,
            DEVICE_PROVIDER,
            0xC0.toByte(), // Combo device - most compatible with macOS
            COMBINED_DESCRIPTOR
        )
    }

    // QoS Settings
    fun getQosSettings(): BluetoothHidDeviceAppQosSettings {
        return BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
    }

    // Mouse button constants
    const val BUTTON_LEFT = 0x01.toByte()
    const val BUTTON_RIGHT = 0x02.toByte()
    const val BUTTON_MIDDLE = 0x04.toByte()
    const val BUTTON_NONE = 0x00.toByte()

    // Keyboard modifier constants (bit positions in modifier byte)
    const val MOD_LEFT_CTRL = 0x01.toByte()
    const val MOD_LEFT_SHIFT = 0x02.toByte()
    const val MOD_LEFT_ALT = 0x04.toByte()
    const val MOD_LEFT_GUI = 0x08.toByte()    // Cmd/Windows key
    const val MOD_RIGHT_CTRL = 0x10.toByte()
    const val MOD_RIGHT_SHIFT = 0x20.toByte()
    const val MOD_RIGHT_ALT = 0x40.toByte()
    const val MOD_RIGHT_GUI = 0x80.toByte()
    const val MOD_NONE = 0x00.toByte()

    // Keyboard key codes (USB HID usage codes)
    const val KEY_A = 0x04.toByte()
    const val KEY_C = 0x06.toByte()
    const val KEY_Q = 0x14.toByte()
    const val KEY_T = 0x17.toByte()
    const val KEY_V = 0x19.toByte()
    const val KEY_W = 0x1A.toByte()
    const val KEY_X = 0x1B.toByte()
    const val KEY_Z = 0x1D.toByte()
    const val KEY_ENTER = 0x28.toByte()
    const val KEY_ESCAPE = 0x29.toByte()
    const val KEY_BACKSPACE = 0x2A.toByte()
    const val KEY_TAB = 0x2B.toByte()
    const val KEY_SPACE = 0x2C.toByte()
    const val KEY_UP_ARROW = 0x52.toByte()
    const val KEY_DOWN_ARROW = 0x51.toByte()
    const val KEY_LEFT_ARROW = 0x50.toByte()
    const val KEY_RIGHT_ARROW = 0x4F.toByte()
    const val KEY_F3 = 0x3C.toByte()
    const val KEY_F11 = 0x44.toByte()
    const val KEY_NONE = 0x00.toByte()

    // macOS Desktop/Space switching
    // Ctrl + Left Arrow: Switch to previous desktop/space
    // Ctrl + Right Arrow: Switch to next desktop/space
}
