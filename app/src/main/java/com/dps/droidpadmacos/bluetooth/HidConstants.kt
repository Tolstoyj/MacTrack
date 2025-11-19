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
    const val REPORT_ID_CONSUMER = 3.toByte()
    const val REPORT_ID_APPLE_VENDOR = 4.toByte()

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

    // HID Descriptor for Consumer Control (Volume, Media Keys)
    // This is used for standard consumer controls like volume up/down
    val CONSUMER_CONTROL_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x0C.toByte(),        // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(),        // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER,   //   Report ID (3)
        0x05.toByte(), 0x0C.toByte(),        //   Usage Page (Consumer)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),        //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),        //   Report Count (8) - 8 bits for different controls

        // Define each consumer control bit (8 bits -> 2 media + mute + play + next/prev + brightness up/down)
        0x09.toByte(), 0xE9.toByte(),        //   Usage (Volume Increment)
        0x09.toByte(), 0xEA.toByte(),        //   Usage (Volume Decrement)
        0x09.toByte(), 0xE2.toByte(),        //   Usage (Mute)
        0x09.toByte(), 0xCD.toByte(),        //   Usage (Play/Pause)
        0x09.toByte(), 0xB5.toByte(),        //   Usage (Scan Next Track)
        0x09.toByte(), 0xB6.toByte(),        //   Usage (Scan Previous Track)
        0x09.toByte(), 0x6F.toByte(),        //   Usage (Brightness Increment)
        0x09.toByte(), 0x70.toByte(),        //   Usage (Brightness Decrement)

        0x81.toByte(), 0x02.toByte(),        //   Input (Data, Variable, Absolute)
        0xC0.toByte()                         // End Collection
    )

    // HID Descriptor for Apple Vendor-Specific Controls (Brightness)
    // This is required for macOS brightness control
    val APPLE_VENDOR_DESCRIPTOR = byteArrayOf(
        0x06.toByte(), 0x01.toByte(), 0xFF.toByte(),  // Usage Page (Apple Vendor, 0xFF01)
        0x09.toByte(), 0x01.toByte(),        // Usage (Vendor Usage 1)
        0xA1.toByte(), 0x01.toByte(),        // Collection (Application)
        0x85.toByte(), REPORT_ID_APPLE_VENDOR, //   Report ID (4)
        0x05.toByte(), 0xFF.toByte(),        //   Usage Page (Vendor Defined)
        0x15.toByte(), 0x00.toByte(),        //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),        //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),        //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),        //   Report Count (8) - 8 bits

        // Apple-specific brightness controls
        0x09.toByte(), 0x20.toByte(),        //   Usage (Brightness Up - 0x20)
        0x09.toByte(), 0x21.toByte(),        //   Usage (Brightness Down - 0x21)
        0x09.toByte(), 0x04.toByte(),        //   Usage (Launchpad - 0x04)
        0x09.toByte(), 0x05.toByte(),        //   Usage (Reserved)
        0x09.toByte(), 0x06.toByte(),        //   Usage (Reserved)
        0x09.toByte(), 0x07.toByte(),        //   Usage (Reserved)
        0x09.toByte(), 0x08.toByte(),        //   Usage (Reserved)
        0x09.toByte(), 0x09.toByte(),        //   Usage (Reserved)

        0x81.toByte(), 0x02.toByte(),        //   Input (Data, Variable, Absolute)
        0xC0.toByte()                         // End Collection
    )

    // Combined Mouse + Keyboard + Consumer Control + Apple Vendor Descriptor
    val COMBINED_DESCRIPTOR = MOUSE_REPORT_DESCRIPTOR + KEYBOARD_REPORT_DESCRIPTOR +
                              CONSUMER_CONTROL_DESCRIPTOR + APPLE_VENDOR_DESCRIPTOR

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
    // Letters
    const val KEY_A = 0x04.toByte()
    const val KEY_B = 0x05.toByte()
    const val KEY_C = 0x06.toByte()
    const val KEY_D = 0x07.toByte()
    const val KEY_E = 0x08.toByte()
    const val KEY_F = 0x09.toByte()
    const val KEY_G = 0x0A.toByte()
    const val KEY_H = 0x0B.toByte()
    const val KEY_I = 0x0C.toByte()
    const val KEY_J = 0x0D.toByte()
    const val KEY_K = 0x0E.toByte()
    const val KEY_L = 0x0F.toByte()
    const val KEY_M = 0x10.toByte()
    const val KEY_N = 0x11.toByte()
    const val KEY_O = 0x12.toByte()
    const val KEY_P = 0x13.toByte()
    const val KEY_Q = 0x14.toByte()
    const val KEY_R = 0x15.toByte()
    const val KEY_S = 0x16.toByte()
    const val KEY_T = 0x17.toByte()
    const val KEY_U = 0x18.toByte()
    const val KEY_V = 0x19.toByte()
    const val KEY_W = 0x1A.toByte()
    const val KEY_X = 0x1B.toByte()
    const val KEY_Y = 0x1C.toByte()
    const val KEY_Z = 0x1D.toByte()

    // Number row
    const val KEY_1 = 0x1E.toByte()
    const val KEY_2 = 0x1F.toByte()
    const val KEY_3 = 0x20.toByte()
    const val KEY_4 = 0x21.toByte()
    const val KEY_5 = 0x22.toByte()
    const val KEY_6 = 0x23.toByte()
    const val KEY_7 = 0x24.toByte()
    const val KEY_8 = 0x25.toByte()
    const val KEY_9 = 0x26.toByte()
    const val KEY_0 = 0x27.toByte()
    const val KEY_ENTER = 0x28.toByte()
    const val KEY_ESCAPE = 0x29.toByte()
    const val KEY_BACKSPACE = 0x2A.toByte()
    const val KEY_TAB = 0x2B.toByte()
    const val KEY_SPACE = 0x2C.toByte()

    // Common punctuation
    const val KEY_MINUS = 0x2D.toByte()          // '-'
    const val KEY_EQUAL = 0x2E.toByte()          // '='
    const val KEY_LEFT_BRACKET = 0x2F.toByte()   // '['
    const val KEY_RIGHT_BRACKET = 0x30.toByte()  // ']'
    const val KEY_BACKSLASH = 0x31.toByte()      // '\'
    const val KEY_SEMICOLON = 0x33.toByte()      // ';'
    const val KEY_APOSTROPHE = 0x34.toByte()     // '\''
    const val KEY_GRAVE = 0x35.toByte()          // '`'
    const val KEY_COMMA = 0x36.toByte()          // ','
    const val KEY_PERIOD = 0x37.toByte()         // '.'
    const val KEY_SLASH = 0x38.toByte()          // '/'
    const val KEY_UP_ARROW = 0x52.toByte()
    const val KEY_DOWN_ARROW = 0x51.toByte()
    const val KEY_LEFT_ARROW = 0x50.toByte()
    const val KEY_RIGHT_ARROW = 0x4F.toByte()

    // Function keys for Mac media controls
    const val KEY_F1 = 0x3A.toByte()   // Brightness Down on Mac
    const val KEY_F2 = 0x3B.toByte()   // Brightness Up on Mac
    const val KEY_F3 = 0x3C.toByte()   // Mission Control
    const val KEY_F4 = 0x3D.toByte()
    const val KEY_F5 = 0x3E.toByte()
    const val KEY_F6 = 0x3F.toByte()
    const val KEY_F7 = 0x40.toByte()
    const val KEY_F8 = 0x41.toByte()
    const val KEY_F9 = 0x42.toByte()
    const val KEY_F10 = 0x43.toByte()  // Mute on Mac
    const val KEY_F11 = 0x44.toByte()  // Volume Down on Mac
    const val KEY_F12 = 0x45.toByte()  // Volume Up on Mac
    const val KEY_NONE = 0x00.toByte()

    // Consumer Control bit positions (for Report ID 3)
    // Each bit in the 1-byte report corresponds to a specific control
    const val CONSUMER_VOLUME_UP = 0x01.toByte()      // Bit 0
    const val CONSUMER_VOLUME_DOWN = 0x02.toByte()    // Bit 1
    const val CONSUMER_MUTE = 0x04.toByte()           // Bit 2
    const val CONSUMER_PLAY_PAUSE = 0x08.toByte()     // Bit 3
    const val CONSUMER_NEXT_TRACK = 0x10.toByte()     // Bit 4
    const val CONSUMER_PREV_TRACK = 0x20.toByte()     // Bit 5
    const val CONSUMER_BRIGHTNESS_UP = 0x40.toByte()  // Bit 6
    const val CONSUMER_BRIGHTNESS_DOWN = 0x80.toByte() // Bit 7
    const val CONSUMER_NONE = 0x00.toByte()

    // Apple Vendor bit positions (for Report ID 4)
    // Each bit corresponds to an Apple-specific control
    const val APPLE_BRIGHTNESS_UP = 0x01.toByte()     // Bit 0 (Usage 0x20)
    const val APPLE_BRIGHTNESS_DOWN = 0x02.toByte()   // Bit 1 (Usage 0x21)
    const val APPLE_LAUNCHPAD = 0x04.toByte()         // Bit 2 (Usage 0x04)
    const val APPLE_NONE = 0x00.toByte()

    // macOS Desktop/Space switching
    // Ctrl + Left Arrow: Switch to previous desktop/space
    // Ctrl + Right Arrow: Switch to next desktop/space
}
