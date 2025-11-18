package com.dps.droidpadmacos.common

/**
 * Application-wide constants for DroidPad
 * Centralizes magic numbers and configuration values
 */
object AppConstants {

    /**
     * Timing constants (in milliseconds)
     */
    object Timing {
        const val SPLASH_DURATION_MS = 2500L
        const val DOUBLE_CLICK_THRESHOLD_MS = 300L
        const val LONG_PRESS_THRESHOLD_MS = 500L
        const val TAP_TIME_THRESHOLD_MS = 200L
        const val CLICK_DELAY_MS = 50L
        const val GESTURE_INFO_DISPLAY_DURATION_MS = 2000L
    }

    /**
     * Gesture detection thresholds
     */
    object Gesture {
        const val TAP_MOVEMENT_THRESHOLD_PX = 20f
        const val SWIPE_GESTURE_THRESHOLD_PX = 100f
        const val MOVEMENT_SENSITIVITY_DEFAULT = 2.5f
        const val SCROLL_SENSITIVITY_DEFAULT = 1.0f
        const val PINCH_ZOOM_THRESHOLD_PX = 20f
        const val SCROLL_MOVEMENT_THRESHOLD_PX = 5f
    }

    /**
     * HID (Human Interface Device) protocol constants
     */
    object HID {
        const val MAX_COORDINATE_VALUE: Byte = 127
        const val MIN_COORDINATE_VALUE: Byte = -127
        const val SCROLL_DIVISOR = 10
    }

    /**
     * Bluetooth constants
     */
    object Bluetooth {
        const val DEVICE_NAME = "DroidPad Trackpad"
        const val DISCOVERABILITY_DURATION_SECONDS = 300 // 5 minutes
        const val PROFILE_READY_TIMEOUT_MS = 5000L
        const val REGISTRATION_DELAY_MS = 1000L
    }

    /**
     * UI Layout constants
     */
    object UI {
        const val KEYBOARD_BUTTON_WIDTH_DP = 50
        const val KEYBOARD_BUTTON_HEIGHT_DP = 42
        const val GRID_SIZE_PX = 50f
        const val ANIMATION_DURATION_MS = 800
        const val ANIMATION_FADE_DURATION_MS = 600
    }

    /**
     * Theme colors (used when theme engine is not available)
     */
    object Colors {
        const val BACKGROUND_DARK_1 = 0xFF0D1117
        const val BACKGROUND_DARK_2 = 0xFF161B22
        const val SURFACE_DARK = 0xFF1C1C1E
        const val SURFACE_VARIANT = 0xFF2C2C2E
        const val KEYBOARD_KEY_BG = 0xFF3A3A3C
    }

    /**
     * Performance monitoring thresholds
     */
    object Performance {
        const val FRAME_TIME_THRESHOLD_MS = 16 // 60fps = 16.67ms per frame
        const val STARTUP_TIME_WARNING_MS = 3000L
    }
}
