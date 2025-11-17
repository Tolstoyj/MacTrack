# Changelog

All notable changes to DroidPad will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-01-XX

### Added
- Initial release of DroidPad
- Bluetooth HID device registration and connection
- Touchpad functionality with multi-touch gesture support
  - Single finger drag for cursor movement
  - Single tap for left click
  - Two-finger tap for right click
  - Two-finger vertical scrolling
  - Pinch-to-zoom gestures
  - Three-finger gestures (Mission Control, Show Desktop)
  - Four-finger swipe for desktop switching
- Air Mouse mode using device gyroscope
- macOS-specific shortcuts:
  - Mission Control (3-finger swipe up)
  - Show Desktop (3-finger swipe down)
  - App Switcher (Cmd+Tab)
- Device history management with auto-reconnect
- Full-screen trackpad mode
- Modern UI built with Jetpack Compose
- Connection status indicators
- Visual touch feedback
- Gesture hints and guides

### Technical
- Android 10+ (API 29) support
- Kotlin-based implementation
- Jetpack Compose UI framework
- Material 3 design system
- Bluetooth HID protocol implementation
- Standard HID mouse and keyboard descriptors

---

## [Unreleased]

### Planned
- Customizable gesture sensitivity
- Additional macOS shortcuts
- Windows/Linux support
- Battery optimization improvements
- Dark mode theme
- Multi-device support
- Keyboard input support

---

[1.0.0]: https://github.com/Tolstoyj/MacTrack/releases/tag/v1.0.0

