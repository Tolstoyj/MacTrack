# USB HID Trackpad Feature

## Overview

DroidPad now supports **dual-mode connectivity**:
- **Bluetooth Mode** (Wireless) - Original mode
- **USB Mode** (Wired) - New feature with lower latency

## Features

### USB Connection Detection
When you connect your Android device to a Mac via USB cable and open DroidPad:

1. **Automatic Detection**: The app detects if a USB device is connected
2. **Mac Recognition**: Identifies if the connected device is a Mac (Apple Vendor ID)
3. **Connection Prompt**: Shows a beautiful UI asking if you want to use USB trackpad mode

### USB HID Mode Benefits

✅ **Lower Latency** - Direct USB connection provides faster response
✅ **No Pairing Required** - Plug and play
✅ **More Reliable** - No Bluetooth interference
✅ **Better Battery** - No Bluetooth radio needed
✅ **Works When Bluetooth is Off** - Independent of Bluetooth state

## User Flow

### 1. Launch App
```
SplashActivity
    ↓
    Checks for USB connection
    ↓
    ├─ USB Connected → UsbConnectionActivity (Mac detection screen)
    └─ No USB       → MainActivity (Bluetooth mode)
```

### 2. USB Connection Screen
Shows:
- 🍎 Mac detected indicator (or 💻 for other devices)
- Device information (manufacturer, product, vendor ID)
- Connection options:
  - **"Use as USB Trackpad"** - Start USB HID mode
  - **"Use Bluetooth Instead"** - Go to Bluetooth setup
  - **"Skip"** - Dismiss and go to main screen

### 3. Trackpad Screen
- **Connection Indicator**: Shows 🔌 for USB or 📡 for Bluetooth
- **Status Badge**: Displays device name and connection state
- All gesture controls work identically in both modes

## Architecture

### New Components

#### 1. `UsbConnectionActivity.kt`
- Detects connected USB devices
- Shows Mac detection UI
- Routes to USB or Bluetooth mode

#### 2. `UsbHidService.kt`
- USB HID communication service
- Handles HID report descriptor
- Sends mouse movements, clicks, and scrolls
- Manages USB permissions

#### 3. `UsbConnectionManager.kt`
- Manages USB connection events
- Listens for device attach/detach
- Provides device information
- Detects Mac devices (Vendor ID 0x05AC)

### Updated Components

#### 1. `SplashActivity.kt`
- Added USB connection check
- Routes to `UsbConnectionActivity` if USB is connected

#### 2. `FullScreenTrackpadActivity.kt`
- Supports both Bluetooth and USB modes
- Routes gestures to appropriate service (UsbHidService or TrackpadViewModel)
- Shows connection mode indicator

#### 3. `AndroidManifest.xml`
- Added USB permissions
- Added USB intent filters for device attach events
- Registered `UsbConnectionActivity`

## USB HID Protocol

The implementation uses standard USB HID mouse protocol:

```kotlin
// HID Report Format: [buttons, x, y, wheel]
val report = byteArrayOf(
    buttonByte,    // Bit 0: Left, Bit 1: Right, Bit 2: Middle
    deltaX,        // X movement (-127 to 127)
    deltaY,        // Y movement (-127 to 127)
    wheelDelta     // Scroll wheel (-127 to 127)
)
```

### Supported Gestures in USB Mode

✅ Mouse movement (1 finger drag)
✅ Left click (1 finger tap)
✅ Right click (2 finger tap)
✅ Middle click (3 finger tap)
✅ Scroll (2 finger swipe)

**Note**: macOS-specific gestures (Mission Control, Show Desktop, etc.) are Bluetooth-only as they require keyboard shortcuts that USB HID mouse doesn't support directly.

## Technical Details

### USB Device Detection

```kotlin
// Check for USB connection
val usbManager = getSystemService(USB_SERVICE) as UsbManager
val deviceList = usbManager.deviceList
val isConnected = deviceList.isNotEmpty()

// Detect Mac
val isMac = device.vendorId == 0x05AC ||
            manufacturer.contains("Apple", ignoreCase = true)
```

### USB Permissions

The app requests USB permissions dynamically:
```kotlin
val permissionIntent = PendingIntent.getBroadcast(
    context, 0,
    Intent(ACTION_USB_PERMISSION),
    PendingIntent.FLAG_IMMUTABLE
)
usbManager.requestPermission(device, permissionIntent)
```

### Connection States

```kotlin
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
```

## Limitations

### USB Mode Limitations

1. **Device Compatibility**: Requires USB OTG support
2. **Android Version**: Best on Android 6.0+
3. **macOS Gestures**: Advanced gestures not available (Mission Control, etc.)
4. **Cable Required**: Must stay physically connected

### When to Use Each Mode

**Use USB Mode:**
- When you need lowest latency
- When Bluetooth is unreliable or unavailable
- When you're near your Mac and don't mind a cable
- For gaming or precision work

**Use Bluetooth Mode:**
- When you want wireless freedom
- When you need macOS-specific gestures
- When presenting or working from a distance
- For better mobility

## Future Enhancements

Potential improvements:
- [ ] USB keyboard HID support for macOS shortcuts
- [ ] Auto-switching between USB and Bluetooth
- [ ] USB charging while in trackpad mode
- [ ] USB-C specific optimizations
- [ ] Support for USB keyboard shortcuts in USB mode

## Testing

To test the USB feature:

1. Connect your Android device to a Mac via USB cable
2. Enable USB debugging (if needed)
3. Launch DroidPad
4. You should see the USB connection detection screen
5. Tap "Use as USB Trackpad"
6. Start using gestures on the trackpad screen

## Troubleshooting

**USB not detected:**
- Check USB cable is properly connected
- Try a different USB port
- Ensure USB debugging is enabled (Settings → Developer Options)

**Permission denied:**
- Allow USB permission when prompted
- Check app permissions in Settings

**Not working as trackpad:**
- Some devices may have limited USB OTG support
- Try Bluetooth mode as alternative

## Code References

- [UsbConnectionActivity.kt](app/src/main/java/com/dps/droidpadmacos/UsbConnectionActivity.kt)
- [UsbHidService.kt](app/src/main/java/com/dps/droidpadmacos/usb/UsbHidService.kt)
- [UsbConnectionManager.kt](app/src/main/java/com/dps/droidpadmacos/usb/UsbConnectionManager.kt)
- [FullScreenTrackpadActivity.kt:36-49](app/src/main/java/com/dps/droidpadmacos/FullScreenTrackpadActivity.kt#L36-L49) - USB mode initialization
- [SplashActivity.kt:36-44](app/src/main/java/com/dps/droidpadmacos/SplashActivity.kt#L36-L44) - USB detection flow
