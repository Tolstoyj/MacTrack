package com.dps.droidpadmacos.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Comprehensive settings manager for DroidPad trackpad
 * Handles all user preferences with reactive state flows
 */
class TrackpadPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Trackpad Settings
    private val _pointerSpeed = MutableStateFlow(prefs.getFloat(KEY_POINTER_SPEED, 1.0f))
    val pointerSpeed: StateFlow<Float> = _pointerSpeed.asStateFlow()

    private val _scrollSpeed = MutableStateFlow(prefs.getFloat(KEY_SCROLL_SPEED, 1.0f))
    val scrollSpeed: StateFlow<Float> = _scrollSpeed.asStateFlow()

    private val _naturalScrolling = MutableStateFlow(prefs.getBoolean(KEY_NATURAL_SCROLLING, true))
    val naturalScrolling: StateFlow<Boolean> = _naturalScrolling.asStateFlow()

    private val _tapToClick = MutableStateFlow(prefs.getBoolean(KEY_TAP_TO_CLICK, true))
    val tapToClick: StateFlow<Boolean> = _tapToClick.asStateFlow()

    private val _twoFingerRightClick = MutableStateFlow(prefs.getBoolean(KEY_TWO_FINGER_RIGHT_CLICK, true))
    val twoFingerRightClick: StateFlow<Boolean> = _twoFingerRightClick.asStateFlow()

    private val _threeFingerDrag = MutableStateFlow(prefs.getBoolean(KEY_THREE_FINGER_DRAG, false))
    val threeFingerDrag: StateFlow<Boolean> = _threeFingerDrag.asStateFlow()

    private val _edgeSwipeEnabled = MutableStateFlow(prefs.getBoolean(KEY_EDGE_SWIPE, true))
    val edgeSwipeEnabled: StateFlow<Boolean> = _edgeSwipeEnabled.asStateFlow()

    // Keyboard Settings
    private val _keyboardLayout = MutableStateFlow(
        KeyboardLayout.valueOf(prefs.getString(KEY_KEYBOARD_LAYOUT, KeyboardLayout.COMPACT.name) ?: KeyboardLayout.COMPACT.name)
    )
    val keyboardLayout: StateFlow<KeyboardLayout> = _keyboardLayout.asStateFlow()

    private val _keyboardScale = MutableStateFlow(prefs.getFloat(KEY_KEYBOARD_SCALE, 1.0f))
    val keyboardScale: StateFlow<Float> = _keyboardScale.asStateFlow()

    private val _keyboardTheme = MutableStateFlow(
        KeyboardTheme.valueOf(prefs.getString(KEY_KEYBOARD_THEME, KeyboardTheme.DARK.name) ?: KeyboardTheme.DARK.name)
    )
    val keyboardTheme: StateFlow<KeyboardTheme> = _keyboardTheme.asStateFlow()

    private val _showKeyboardHints = MutableStateFlow(prefs.getBoolean(KEY_SHOW_KEYBOARD_HINTS, true))
    val showKeyboardHints: StateFlow<Boolean> = _showKeyboardHints.asStateFlow()

    // Haptic Feedback
    private val _hapticFeedback = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true))
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    private val _hapticIntensity = MutableStateFlow(
        HapticIntensity.valueOf(prefs.getString(KEY_HAPTIC_INTENSITY, HapticIntensity.MEDIUM.name) ?: HapticIntensity.MEDIUM.name)
    )
    val hapticIntensity: StateFlow<HapticIntensity> = _hapticIntensity.asStateFlow()

    // UI Settings
    private val _showGestureGuide = MutableStateFlow(prefs.getBoolean(KEY_SHOW_GESTURE_GUIDE, true))
    val showGestureGuide: StateFlow<Boolean> = _showGestureGuide.asStateFlow()

    private val _statusBarStyle = MutableStateFlow(
        StatusBarStyle.valueOf(prefs.getString(KEY_STATUS_BAR_STYLE, StatusBarStyle.FULL.name) ?: StatusBarStyle.FULL.name)
    )
    val statusBarStyle: StateFlow<StatusBarStyle> = _statusBarStyle.asStateFlow()

    private val _uiOpacity = MutableStateFlow(prefs.getFloat(KEY_UI_OPACITY, 0.95f))
    val uiOpacity: StateFlow<Float> = _uiOpacity.asStateFlow()

    // Advanced Settings
    private val _airMouseSensitivity = MutableStateFlow(prefs.getFloat(KEY_AIR_MOUSE_SENSITIVITY, 1.5f))
    val airMouseSensitivity: StateFlow<Float> = _airMouseSensitivity.asStateFlow()

    private val _deskMouseSensitivity = MutableStateFlow(prefs.getFloat(KEY_DESK_MOUSE_SENSITIVITY, 2500f))
    val deskMouseSensitivity: StateFlow<Float> = _deskMouseSensitivity.asStateFlow()

    // Setter methods
    fun setPointerSpeed(value: Float) {
        _pointerSpeed.value = value
        prefs.edit().putFloat(KEY_POINTER_SPEED, value).apply()
    }

    fun setScrollSpeed(value: Float) {
        _scrollSpeed.value = value
        prefs.edit().putFloat(KEY_SCROLL_SPEED, value).apply()
    }

    fun setNaturalScrolling(enabled: Boolean) {
        _naturalScrolling.value = enabled
        prefs.edit().putBoolean(KEY_NATURAL_SCROLLING, enabled).apply()
    }

    fun setTapToClick(enabled: Boolean) {
        _tapToClick.value = enabled
        prefs.edit().putBoolean(KEY_TAP_TO_CLICK, enabled).apply()
    }

    fun setTwoFingerRightClick(enabled: Boolean) {
        _twoFingerRightClick.value = enabled
        prefs.edit().putBoolean(KEY_TWO_FINGER_RIGHT_CLICK, enabled).apply()
    }

    fun setThreeFingerDrag(enabled: Boolean) {
        _threeFingerDrag.value = enabled
        prefs.edit().putBoolean(KEY_THREE_FINGER_DRAG, enabled).apply()
    }

    fun setEdgeSwipeEnabled(enabled: Boolean) {
        _edgeSwipeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_EDGE_SWIPE, enabled).apply()
    }

    fun setKeyboardLayout(layout: KeyboardLayout) {
        _keyboardLayout.value = layout
        prefs.edit().putString(KEY_KEYBOARD_LAYOUT, layout.name).apply()
    }

    fun setKeyboardScale(scale: Float) {
        _keyboardScale.value = scale
        prefs.edit().putFloat(KEY_KEYBOARD_SCALE, scale).apply()
    }

    fun setKeyboardTheme(theme: KeyboardTheme) {
        _keyboardTheme.value = theme
        prefs.edit().putString(KEY_KEYBOARD_THEME, theme.name).apply()
    }

    fun setShowKeyboardHints(show: Boolean) {
        _showKeyboardHints.value = show
        prefs.edit().putBoolean(KEY_SHOW_KEYBOARD_HINTS, show).apply()
    }

    fun setHapticFeedback(enabled: Boolean) {
        _hapticFeedback.value = enabled
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    }

    fun setHapticIntensity(intensity: HapticIntensity) {
        _hapticIntensity.value = intensity
        prefs.edit().putString(KEY_HAPTIC_INTENSITY, intensity.name).apply()
    }

    fun setShowGestureGuide(show: Boolean) {
        _showGestureGuide.value = show
        prefs.edit().putBoolean(KEY_SHOW_GESTURE_GUIDE, show).apply()
    }

    fun setStatusBarStyle(style: StatusBarStyle) {
        _statusBarStyle.value = style
        prefs.edit().putString(KEY_STATUS_BAR_STYLE, style.name).apply()
    }

    fun setUiOpacity(opacity: Float) {
        _uiOpacity.value = opacity
        prefs.edit().putFloat(KEY_UI_OPACITY, opacity).apply()
    }

    fun setAirMouseSensitivity(value: Float) {
        _airMouseSensitivity.value = value
        prefs.edit().putFloat(KEY_AIR_MOUSE_SENSITIVITY, value).apply()
    }

    fun setDeskMouseSensitivity(value: Float) {
        _deskMouseSensitivity.value = value
        prefs.edit().putFloat(KEY_DESK_MOUSE_SENSITIVITY, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "trackpad_preferences"

        // Keys
        private const val KEY_POINTER_SPEED = "pointer_speed"
        private const val KEY_SCROLL_SPEED = "scroll_speed"
        private const val KEY_NATURAL_SCROLLING = "natural_scrolling"
        private const val KEY_TAP_TO_CLICK = "tap_to_click"
        private const val KEY_TWO_FINGER_RIGHT_CLICK = "two_finger_right_click"
        private const val KEY_THREE_FINGER_DRAG = "three_finger_drag"
        private const val KEY_EDGE_SWIPE = "edge_swipe"

        private const val KEY_KEYBOARD_LAYOUT = "keyboard_layout"
        private const val KEY_KEYBOARD_SCALE = "keyboard_scale"
        private const val KEY_KEYBOARD_THEME = "keyboard_theme"
        private const val KEY_SHOW_KEYBOARD_HINTS = "show_keyboard_hints"

        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"

        private const val KEY_SHOW_GESTURE_GUIDE = "show_gesture_guide"
        private const val KEY_STATUS_BAR_STYLE = "status_bar_style"
        private const val KEY_UI_OPACITY = "ui_opacity"

        private const val KEY_AIR_MOUSE_SENSITIVITY = "air_mouse_sensitivity"
        private const val KEY_DESK_MOUSE_SENSITIVITY = "desk_mouse_sensitivity"
    }
}

// Enums for settings
enum class KeyboardLayout {
    COMPACT,        // Original compact shortcuts layout
    FULL_QWERTY,    // Full QWERTY keyboard
    NUMERIC,        // Numeric keypad
    FUNCTION_KEYS,  // F1-F12 + media controls
    ARROWS,         // Arrow keys + navigation
    CUSTOM          // User-defined layout
}

enum class KeyboardTheme {
    DARK,           // Dark gray/black theme
    LIGHT,          // Light theme
    COLORFUL,       // Colorful gradient theme
    MINIMAL,        // Minimal transparent theme
    MAC_STYLE       // macOS-inspired theme
}

enum class HapticIntensity {
    OFF,
    LIGHT,
    MEDIUM,
    STRONG
}

enum class StatusBarStyle {
    FULL,           // Battery, time, connection
    MINIMAL,        // Connection only
    HIDDEN          // No status bar
}
