# USB Detection Implementation Guide

## Problem Statement

When an Android device is connected to a computer via USB, it operates in **peripheral/accessory mode**, not USB Host mode. This means:

- `UsbManager.deviceList` is **empty** (this API is for USB Host mode only)
- Standard USB device detection doesn't work
- We need alternative detection methods

## Solution: Multi-Signal Detection

We implemented [UsbConnectionDetector.kt](app/src/main/java/com/dps/droidpadmacos/usb/UsbConnectionDetector.kt) which uses multiple signals to detect USB connectivity.

### Detection Methods

#### 1. **Battery Charging Status** ✅
**What it detects:** Whether device is charging via USB

```kotlin
val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
val isUsbCharge = plugged == BatteryManager.BATTERY_PLUGGED_USB
```

**Pros:**
- Always works
- No special permissions needed
- Instant detection

**Cons:**
- Can't distinguish between computer and wall charger
- Not sufficient alone for trackpad mode

#### 2. **ADB Connection Status** ✅ (Most Reliable)
**What it detects:** Whether USB debugging (ADB) is active

```kotlin
// Check if ADB daemon is running
val adbEnabled = getSystemProperty("init.svc.adbd") == "running"

// Check if USB debugging is enabled
val usbDebugging = Settings.Global.getString(
    null,
    Settings.Global.ADB_ENABLED
) == "1"
```

**Pros:**
- **Strongly indicates computer connection**
- Very reliable when enabled
- Available on most debug builds

**Cons:**
- Requires USB debugging to be enabled
- Not available on production builds without USB debugging

#### 3. **USB Configuration State** ✅
**What it detects:** USB peripheral configuration from sysfs

```kotlin
val usbStatePaths = listOf(
    "/sys/class/android_usb/android0/state",
    "/sys/devices/virtual/android_usb/android0/state"
)

// Read USB state
val state = File(path).readText().trim()
val configured = state.contains("CONFIGURED", ignoreCase = true)
```

**Pros:**
- Direct hardware-level detection
- Works without special permissions
- Indicates active USB data connection

**Cons:**
- File paths vary by Android version/device
- May require read permissions on some devices

## Connection Types

```kotlin
enum class ConnectionType {
    NONE,           // Not connected
    USB_CHARGING,   // USB connected but just charging (wall charger or dumb cable)
    USB_DATA,       // USB with data connection (ADB or MTP) - **THIS IS WHAT WE WANT**
    UNKNOWN         // Connected but type unclear
}
```

### Decision Logic

```
if (adbConnected || usbConfigured)
    → USB_DATA ✅ (SUITABLE FOR TRACKPAD)

else if (chargingViaUsb)
    → USB_CHARGING ⚠️ (Not suitable - might be wall charger)

else
    → NONE ❌ (No connection)
```

## Usage in App

### 1. **SplashActivity** ([SplashActivity.kt:44-58](app/src/main/java/com/dps/droidpadmacos/SplashActivity.kt#L44-L58))

```kotlin
val connectionInfo = UsbConnectionDetector.detectConnection(context)

if (UsbConnectionDetector.isSuitableForTrackpad(connectionInfo)) {
    // Show USB connection screen
    startActivity(Intent(this, UsbConnectionActivity::class.java))
} else {
    // Go to Bluetooth mode
    startActivity(Intent(this, MainActivity::class.java))
}
```

### 2. **UsbConnectionActivity** ([UsbConnectionActivity.kt:48-59](app/src/main/java/com/dps/droidpadmacos/UsbConnectionActivity.kt#L48-L59))

Shows detected connection with:
- Connection type (USB_DATA, USB_CHARGING, etc.)
- Connection description
- Icon (🍎 for likely Mac/computer, 💻 for generic)

## Testing Instructions

### Test Case 1: USB Connected to Mac with ADB
**Setup:**
1. Enable USB debugging on Android
2. Connect phone to Mac via USB cable
3. Accept "Allow USB debugging" if prompted

**Expected Result:**
```
✅ SplashActivity detects USB_DATA connection
✅ Navigates to UsbConnectionActivity
✅ Shows "Computer Detected!" with 🍎/💻 icon
✅ Displays connection info
```

**Logs to check:**
```
SplashActivity: USB Connection Info: ConnectionInfo(isConnected=true, ... connectionType=USB_DATA)
SplashActivity: Navigating to UsbConnectionActivity
UsbConnectionActivity: Connection info: ConnectionInfo(...), Likely Mac: true
```

### Test Case 2: USB Connected but ADB Disabled
**Setup:**
1. Disable USB debugging
2. Connect phone to Mac

**Expected Result:**
```
⚠️ May detect USB_CHARGING only
⚠️ Might not navigate to UsbConnectionActivity
✅ Falls back to MainActivity (Bluetooth mode)
```

**Note:** Without ADB, detection is less reliable. User can manually enable USB debugging for best experience.

### Test Case 3: No USB Connection
**Setup:**
1. Disconnect USB cable
2. Launch app

**Expected Result:**
```
✅ Detects NONE connection type
✅ Navigates directly to MainActivity
✅ Shows Bluetooth pairing screen
```

### Test Case 4: Charging from Wall Adapter
**Setup:**
1. Connect to wall charger (not computer)
2. Launch app

**Expected Result:**
```
✅ Detects USB_CHARGING (not USB_DATA)
✅ Not suitable for trackpad
✅ Navigates to MainActivity
```

## Debugging USB Detection

### View Detection Logs

Use `adb logcat` to see detailed detection info:

```bash
adb logcat -s UsbConnectionDetector SplashActivity UsbConnectionActivity
```

**Key log lines:**
```
UsbConnectionDetector: Battery plugged type: 2 (USB=2)
UsbConnectionDetector: ADB check - enabled: true, debugging: true
UsbConnectionDetector: USB state at /sys/...: CONFIGURED
UsbConnectionDetector: USB Detection - Connected: true, Charging: true, ADB: true, Configured: true, Type: USB_DATA
```

### Manual Testing Commands

```bash
# Check battery status
adb shell dumpsys battery

# Check ADB status
adb shell getprop init.svc.adbd

# Check USB state
adb shell cat /sys/class/android_usb/android0/state
```

## Recommendations for Users

### For Best USB Detection:

1. **Enable USB Debugging**
   - Settings → Developer Options → USB Debugging ✅
   - This provides the most reliable detection

2. **Use Quality USB Cable**
   - Ensure cable supports data transfer
   - Some cheap cables are charge-only

3. **Choose Correct USB Mode**
   - When prompted on Android, select "File Transfer" or "MTP"
   - Avoid "Charging only" mode

4. **Grant Permissions**
   - Accept "Allow USB debugging" prompt
   - Don't check "Always allow from this computer" initially (for testing)

## Fallback Behavior

If USB detection fails or is unreliable:

1. App falls back to **Bluetooth mode** automatically
2. User can still manually switch to USB mode from settings (future feature)
3. No functionality is lost - Bluetooth always works

## Architecture

```
SplashActivity
    ↓
    Uses: UsbConnectionDetector.detectConnection()
    ↓
    ├─ USB_DATA detected?
    │   ├─ Yes → UsbConnectionActivity (show Mac detected screen)
    │   └─ No  → MainActivity (Bluetooth mode)
    ↓
UsbConnectionActivity
    ↓
    User chooses:
    ├─ "Use as USB Trackpad" → FullScreenTrackpadActivity (USB mode)
    └─ "Use Bluetooth" → MainActivity
```

## Future Improvements

### Potential Enhancements:

1. **✨ Runtime USB Monitoring**
   - Detect USB connect/disconnect while app is running
   - Auto-switch between USB and Bluetooth

2. **✨ Manual Mode Selection**
   - Settings to force USB or Bluetooth mode
   - Override auto-detection

3. **✨ USB Configuration UI**
   - Guide users to enable USB debugging
   - Show troubleshooting steps

4. **✨ Improved Mac Detection**
   - Parse USB vendor strings (requires root)
   - Network-based Mac fingerprinting
   - mDNS/Bonjour detection

5. **✨ Connection Quality Metrics**
   - Measure USB vs Bluetooth latency
   - Show performance comparison

## Known Limitations

1. **ADB Requirement**: Most reliable detection requires USB debugging enabled
2. **Device Variations**: USB state paths vary by manufacturer
3. **No Mac Guarantee**: "Computer detected" doesn't guarantee it's a Mac (could be Windows/Linux)
4. **Production Builds**: ADB detection won't work on production APKs without debugging

## Code References

- [UsbConnectionDetector.kt](app/src/main/java/com/dps/droidpadmacos/usb/UsbConnectionDetector.kt) - Core detection logic
- [SplashActivity.kt:44-58](app/src/main/java/com/dps/droidpadmacos/SplashActivity.kt#L44-L58) - Detection integration
- [UsbConnectionActivity.kt:48-59](app/src/main/java/com/dps/droidpadmacos/UsbConnectionActivity.kt#L48-L59) - Connection info display

## Summary

The USB detection system uses a **multi-signal approach** to reliably detect when an Android device is connected to a computer via USB. The primary signal is **ADB connectivity**, which strongly indicates a computer connection with data transfer capability - exactly what we need for USB trackpad mode.

When connected with USB debugging enabled, the app will automatically detect the connection and offer USB trackpad mode. Otherwise, it gracefully falls back to Bluetooth mode.
