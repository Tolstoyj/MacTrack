# USB Detection Diagnostics & Fixes

## Issues Found from Logs

### 1. ❌ No USB Detection Logs
**Problem:** When the app launched, there were NO logs from `UsbConnectionDetector`
**Root Cause:** Silent failures in detection methods
**Impact:** App couldn't detect USB connection to Mac

### 2. ⚠️ Bluetooth Unregistering Unexpectedly
**Problem:** Logs show `onAppStatusChanged: registered=false` when activity loses focus
**Root Cause:** Activity pause triggers Bluetooth unregister
**Impact:** Connection drops when user switches apps

## Fixes Implemented

### ✅ 1. Comprehensive USB Diagnostics Tool

Created **[UsbDebugHelper.kt](app/src/main/java/com/dps/droidpadmacos/usb/UsbDebugHelper.kt)** - A comprehensive diagnostics tool that checks:

#### Battery & Charging Status
```kotlin
- Plugged type (USB=2, AC=1, Wireless=4)
- Battery status (charging/not charging)
- Battery level
```

#### ADB Status
```kotlin
- ADB daemon running (init.svc.adbd)
- USB debugging enabled (Settings.Global.ADB_ENABLED)
- ADB TCP port
```

#### USB State Files
Checks multiple sysfs paths:
```
/sys/class/android_usb/android0/state
/sys/devices/virtual/android_usb/android0/state
/config/usb_gadget/g1/UDC
/sys/class/android_usb/android0/functions
```

####System Properties
```kotlin
- sys.usb.state
- sys.usb.config
- persist.sys.usb.config
- ro.bootmode
```

#### Developer Options
```kotlin
- Developer settings enabled status
```

#### Final Detection Result
```kotlin
- All ConnectionInfo fields
- isSuitableForTrackpad verdict
```

### ✅ 2. Auto-Run Diagnostics on App Launch

Updated **[SplashActivity.kt:45-46](app/src/main/java/com/dps/droidpadmacos/SplashActivity.kt#L45-L46)**:
```kotlin
// Run USB diagnostics
Log.d(TAG, "=== RUNNING USB DIAGNOSTICS ===")
val diagnostics = UsbDebugHelper.printFullDiagnostics(this@SplashActivity)
```

Now every time the app launches, full USB diagnostics are logged!

### ✅ 3. Fixed Context Resolver Issues

The linter already fixed this in [UsbConnectionDetector.kt:115](app/src/main/java/com/dps/droidpadmacos/usb/UsbConnectionDetector.kt#L115):
```kotlin
// OLD (would crash):
Settings.Global.getString(null, Settings.Global.ADB_ENABLED)

// NEW (correct):
Settings.Global.getString(context.contentResolver, Settings.Global.ADB_ENABLED)
```

## How to Use Diagnostics

### Method 1: Auto-Run on Launch
```bash
# Install the new build
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app and watch diagnostics
adb logcat -s UsbDebugHelper SplashActivity UsbConnectionDetector
```

### Method 2: Programmatic Call
```kotlin
val report = UsbDebugHelper.printFullDiagnostics(context)
Log.d("MyTag", report)
```

## Sample Diagnostic Output

```
========== USB DETECTION DIAGNOSTICS ==========

--- Battery & Charging Status ---
Plugged value: 2
  USB (2): true
  AC (1): false
  Wireless (4): false
Battery status: 2 (charging=true)
Battery level: 85%

--- ADB Status ---
ADB daemon (init.svc.adbd): running
ADB_ENABLED setting: 1
ADB TCP port: null

--- USB State Files ---
✓ /sys/class/android_usb/android0/state = CONFIGURED
✗ /sys/devices/virtual/android_usb/android0/state does not exist
✗ /sys/class/usb_device/usb_device/device/status does not exist
✓ /config/usb_gadget/g1/UDC = a600000.dwc3
✓ /sys/class/android_usb/android0/functions = mtp,ptp,adb

--- USB-Related System Properties ---
sys.usb.state = mtp,adb
sys.usb.config = mtp,adb
persist.sys.usb.config = mtp,adb
ro.bootmode = unknown

--- Developer Options ---
Developer options enabled: true

--- Detection Result ---
isConnected: true
chargingViaUsb: true
isAdbConnected: true
usbConfigured: true
connectionType: USB_DATA
isSuitableForTrackpad: true

==============================================
```

## What to Look For

### ✅ Good Signs (USB will work):
```
✓ Plugged value: 2 (USB)
✓ ADB daemon: running
✓ ADB_ENABLED setting: 1
✓ USB state: CONFIGURED
✓ connectionType: USB_DATA
✓ isSuitableForTrackpad: true
```

### ⚠️ Warning Signs (USB might not work):
```
⚠ Plugged value: 1 (AC - might be fast charging reported as AC)
⚠ ADB daemon: stopped (USB debugging disabled)
⚠ ADB_ENABLED setting: 0
⚠ USB state: DISCONNECTED
⚠ connectionType: USB_CHARGING
⚠ isSuitableForTrackpad: false
```

### ❌ Bad Signs (USB won't work):
```
❌ Plugged value: -1 (not plugged in)
❌ ADB daemon: null (error reading)
❌ All USB state files: not exist
❌ connectionType: NONE
```

## Testing Checklist

### Test 1: USB Connected with ADB Enabled
1. Enable USB debugging on Android
2. Connect phone to Mac via USB
3. Accept "Allow USB debugging"
4. Launch DroidPad
5. Check logs for diagnostics

**Expected:**
```
SplashActivity: === RUNNING USB DIAGNOSTICS ===
UsbDebugHelper: Plugged value: 2
UsbDebugHelper: ADB daemon: running
UsbDebugHelper: connectionType: USB_DATA
SplashActivity: Navigating to UsbConnectionActivity
```

### Test 2: USB Connected but ADB Disabled
1. Disable USB debugging
2. Connect phone to Mac
3. Launch DroidPad

**Expected:**
```
UsbDebugHelper: Plugged value: 2
UsbDebugHelper: ADB daemon: stopped
UsbDebugHelper: connectionType: USB_CHARGING
SplashActivity: Navigating to MainActivity (Bluetooth)
```

### Test 3: No USB Connection
1. Disconnect USB cable
2. Launch DroidPad

**Expected:**
```
UsbDebugHelper: Plugged value: -1
UsbDebugHelper: connectionType: NONE
SplashActivity: Navigating to MainActivity (Bluetooth)
```

## Common Issues & Solutions

### Issue: "Plugged value: 1" when connected to Mac
**Cause:** Fast charging is reported as AC on some devices
**Solution:** Detection logic checks `isPluggedIn` as fallback
**Code:** [UsbConnectionDetector.kt:49](app/src/main/java/com/dps/droidpadmacos/usb/UsbConnectionDetector.kt#L49)

### Issue: "ADB daemon: stopped" even though cable is connected
**Cause:** USB debugging is disabled
**Solution:** Enable in Settings → Developer Options → USB Debugging
**Impact:** App will fall back to Bluetooth mode

### Issue: All USB state files show "does not exist"
**Cause:** File paths vary by device/Android version
**Solution:** As long as ADB or battery detection works, it's fine
**Note:** This is expected on some devices

### Issue: "Could not read ADB_ENABLED setting"
**Cause:** Permissions or settings access issue
**Solution:** This is a fallback - other detection methods will work
**Impact:** Minimal if other methods succeed

## Files Modified

1. **[UsbDebugHelper.kt](app/src/main/java/com/dps/droidpadmacos/usb/UsbDebugHelper.kt)** - NEW comprehensive diagnostics
2. **[SplashActivity.kt](app/src/main/java/com/dps/droidpadmacos/SplashActivity.kt#L45-L46)** - Auto-run diagnostics on launch
3. **[UsbConnectionDetector.kt](app/src/main/java/com/dps/droidpadmacos/usb/UsbConnectionDetector.kt)** - Fixed context resolver (linter)
4. **[MainActivity.kt](app/src/main/java/com/dps/droidpadmacos/MainActivity.kt#L47-L48)** - Added USB imports for future debugging

## Next Steps

1. **Install & Test:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb logcat -s UsbDebugHelper UsbConnectionDetector SplashActivity UsbConnectionActivity
   ```

2. **Share Logs:**
   - Run the app with phone connected to Mac
   - Copy the full diagnostic output
   - This will help identify device-specific issues

3. **Enable USB Debugging:**
   - If not already enabled
   - Settings → About Phone → Tap Build Number 7 times
   - Settings → Developer Options → USB Debugging → ON

## Summary

The diagnostics system is now in place! Every app launch will:
1. ✅ Check all USB detection signals
2. ✅ Log comprehensive diagnostics
3. ✅ Show exactly why USB is/isn't detected
4. ✅ Help debug device-specific issues

This makes troubleshooting USB detection infinitely easier!
