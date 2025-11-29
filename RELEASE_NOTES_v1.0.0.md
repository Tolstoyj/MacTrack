# DroidPad v1.0.0 - Initial Release

🎉 **First public release of DroidPad!**

Transform your Android device into a wireless trackpad and mouse for your macOS computer.

## ✨ Features

### Core Functionality
- **Bluetooth HID Device**: Implements standard Bluetooth HID protocol for maximum compatibility
- **Touchpad Controls**:
  - Single finger drag for cursor movement
  - Single tap for left click
  - Two-finger tap for right click
  - Two-finger scroll (vertical)
  - Pinch-to-zoom gestures
  - Three-finger gestures for Mission Control and Show Desktop
  - Four-finger swipe for desktop switching

- **Air Mouse Mode**: 
  - Gyroscope-based cursor control
  - Tilt your device to move the cursor
  - Perfect for presentations or hands-free control

- **macOS Integration**:
  - Mission Control (3-finger swipe up or button)
  - Show Desktop (3-finger swipe down or button)
  - App Switcher (Cmd+Tab via button)
  - Native macOS gesture support

- **Device Management**:
  - Recent devices list with quick reconnect
  - Auto-reconnect on app launch
  - Connection history tracking
  - Reset functionality for troubleshooting

- **User Experience**:
  - Full-screen trackpad mode
  - Visual touch feedback
  - Connection status indicators
  - Gesture hints and guides
  - Smooth animations and transitions
  - Modern UI built with Jetpack Compose

## 📋 Requirements

- **Android**: Android 10 (API 29) or later
- **macOS**: macOS 10.12 (Sierra) or later
- **Bluetooth**: Bluetooth 4.0+ support

## 🚀 Installation

1. Download the APK file from the assets below
2. Enable "Install from Unknown Sources" on your Android device
3. Install the APK file
4. Grant Bluetooth permissions when prompted
5. Follow the in-app setup instructions

## 📖 Quick Start

1. Launch DroidPad on your Android device
2. Tap "Register" to make your device discoverable
3. On your Mac, open System Settings → Bluetooth
4. Look for "DroidPad Trackpad" and click "Connect"
5. Start using your device as a trackpad!

## 📸 Screenshots

See the [README](README.md) for detailed screenshots and setup instructions.

## 🔧 Technical Details

- **Minimum SDK**: Android 10 (API 29)
- **Target SDK**: Android 14 (API 36)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3

## 🐛 Known Issues

- First-time pairing may require unpairing the device from Mac if previously connected
- Some Android devices may have limited Bluetooth HID support

## 🙏 Acknowledgments

Built with:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Android's [Bluetooth HID API](https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice)
- Material 3 Design

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Download the APK**: See the Assets section below ⬇️


