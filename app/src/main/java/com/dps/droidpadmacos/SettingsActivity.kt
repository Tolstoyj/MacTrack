package com.dps.droidpadmacos

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.settings.KeyboardLayout
import com.dps.droidpadmacos.settings.KeyboardTheme
import com.dps.droidpadmacos.settings.HapticIntensity
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme

class SettingsActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("DroidPadSettings", Context.MODE_PRIVATE)

        setContent {
            DroidPadMacOSTheme {
                SettingsScreen(
                    prefs = prefs,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit
) {
    // Load saved preferences
    var hideUIOverlay by remember { mutableStateOf(prefs.getBoolean("hide_ui_overlay", false)) }
    var showGestureGuide by remember { mutableStateOf(prefs.getBoolean("show_gesture_guide", true)) }
    var showConnectionStatus by remember { mutableStateOf(prefs.getBoolean("show_connection_status", true)) }
    var enableHapticFeedback by remember { mutableStateOf(prefs.getBoolean("haptic_feedback", true)) }
    var enableSoundFeedback by remember { mutableStateOf(prefs.getBoolean("sound_feedback", false)) }
    var backgroundMode by remember { mutableStateOf(prefs.getInt("background_mode", 0)) }
    var trackpadSensitivity by remember { mutableFloatStateOf(prefs.getFloat("trackpad_sensitivity", 1.0f)) }
    var scrollSpeed by remember { mutableFloatStateOf(prefs.getFloat("scroll_speed", 1.0f)) }
    var enableThreeFingerGestures by remember { mutableStateOf(prefs.getBoolean("three_finger_gestures", true)) }
    var enableFourFingerGestures by remember { mutableStateOf(prefs.getBoolean("four_finger_gestures", true)) }
    var enablePinchZoom by remember { mutableStateOf(prefs.getBoolean("pinch_zoom", true)) }
    var autoReconnect by remember { mutableStateOf(prefs.getBoolean("auto_reconnect", true)) }
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", true)) }
    var minimalistMode by remember { mutableStateOf(prefs.getBoolean("minimalist_mode", false)) }
    var touchFxEnabled by remember { mutableStateOf(prefs.getBoolean("touch_fx_enabled", true)) }
    var touchFxStyle by remember { mutableStateOf(prefs.getInt("touch_fx_style", 1)) }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A0F16),
                Color(0xFF0F1621)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onBack) {
                    Text(
                        text = "Done",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // UI Appearance Settings
                SettingsSection(title = "UI Appearance") {
                    SwitchSetting(
                        title = "Hide All UI Overlays",
                        description = "Hide all buttons and overlays in trackpad mode for a clean interface",
                        checked = hideUIOverlay,
                        onCheckedChange = {
                            hideUIOverlay = it
                            prefs.edit().putBoolean("hide_ui_overlay", it).apply()
                            if (it) {
                                minimalistMode = true
                                prefs.edit().putBoolean("minimalist_mode", true).apply()
                            }
                        }
                    )

                    SwitchSetting(
                        title = "Minimalist Mode",
                        description = "Show only essential UI elements",
                        checked = minimalistMode,
                        onCheckedChange = {
                            minimalistMode = it
                            prefs.edit().putBoolean("minimalist_mode", it).apply()
                        },
                        enabled = !hideUIOverlay
                    )

                    SwitchSetting(
                        title = "Show Gesture Guide",
                        description = "Display gesture hints at the bottom of the screen",
                        checked = showGestureGuide,
                        onCheckedChange = {
                            showGestureGuide = it
                            prefs.edit().putBoolean("show_gesture_guide", it).apply()
                        },
                        enabled = !hideUIOverlay
                    )

                    SwitchSetting(
                        title = "Show Connection Status",
                        description = "Display Bluetooth connection indicator",
                        checked = showConnectionStatus,
                        onCheckedChange = {
                            showConnectionStatus = it
                            prefs.edit().putBoolean("show_connection_status", it).apply()
                        },
                        enabled = !hideUIOverlay
                    )

                    BackgroundModeSetting(
                        selectedMode = backgroundMode,
                        onModeChange = {
                            backgroundMode = it
                            prefs.edit().putInt("background_mode", it).apply()
                        }
                    )
                }

                // Trackpad Settings
                SettingsSection(title = "Trackpad") {
                    SliderSetting(
                        title = "Trackpad Sensitivity",
                        value = trackpadSensitivity,
                        onValueChange = {
                            trackpadSensitivity = it
                            prefs.edit().putFloat("trackpad_sensitivity", it).apply()
                        },
                        valueRange = 0.5f..2.0f,
                        displayValue = String.format("%.1fx", trackpadSensitivity)
                    )

                    SliderSetting(
                        title = "Scroll Speed",
                        value = scrollSpeed,
                        onValueChange = {
                            scrollSpeed = it
                            prefs.edit().putFloat("scroll_speed", it).apply()
                        },
                        valueRange = 0.5f..3.0f,
                        displayValue = String.format("%.1fx", scrollSpeed)
                    )
                }

                // Gestures
                SettingsSection(title = "Gestures") {
                    SwitchSetting(
                        title = "Three Finger Gestures",
                        description = "Enable Mission Control, Desktop, and Space switching",
                        checked = enableThreeFingerGestures,
                        onCheckedChange = {
                            enableThreeFingerGestures = it
                            prefs.edit().putBoolean("three_finger_gestures", it).apply()
                        }
                    )

                    SwitchSetting(
                        title = "Four Finger Gestures",
                        description = "Enable advanced multi-finger gestures",
                        checked = enableFourFingerGestures,
                        onCheckedChange = {
                            enableFourFingerGestures = it
                            prefs.edit().putBoolean("four_finger_gestures", it).apply()
                        }
                    )

                    SwitchSetting(
                        title = "Pinch to Zoom",
                        description = "Enable pinch gesture for zooming",
                        checked = enablePinchZoom,
                        onCheckedChange = {
                            enablePinchZoom = it
                            prefs.edit().putBoolean("pinch_zoom", it).apply()
                        }
                    )
                }

                // Feedback Settings
                SettingsSection(title = "Feedback") {
                    SwitchSetting(
                        title = "Haptic Feedback",
                        description = "Vibrate on clicks and gestures",
                        checked = enableHapticFeedback,
                        onCheckedChange = {
                            enableHapticFeedback = it
                            prefs.edit().putBoolean("haptic_feedback", it).apply()
                        }
                    )

                    SwitchSetting(
                        title = "Sound Feedback",
                        description = "Play sounds for actions",
                        checked = enableSoundFeedback,
                        onCheckedChange = {
                            enableSoundFeedback = it
                            prefs.edit().putBoolean("sound_feedback", it).apply()
                        }
                    )
                }

                // Keyboard Settings
                SettingsSection(title = "Keyboard") {
                    KeyboardLayoutSetting(prefs = prefs)
                    KeyboardThemeSetting(prefs = prefs)

                    SliderSetting(
                        title = "Keyboard Size",
                        value = prefs.getFloat("keyboard_scale", 1.0f),
                        onValueChange = {
                            prefs.edit().putFloat("keyboard_scale", it).apply()
                        },
                        valueRange = 0.6f..1.4f,
                        displayValue = String.format("%.0f%%", prefs.getFloat("keyboard_scale", 1.0f) * 100)
                    )

                    SwitchSetting(
                        title = "Show Key Hints",
                        description = "Display secondary key labels",
                        checked = prefs.getBoolean("show_keyboard_hints", true),
                        onCheckedChange = {
                            prefs.edit().putBoolean("show_keyboard_hints", it).apply()
                        }
                    )
                }

                // Advanced Trackpad Settings
                SettingsSection(title = "Advanced Trackpad") {
                    SwitchSetting(
                        title = "Natural Scrolling",
                        description = "Scroll direction follows finger movement",
                        checked = prefs.getBoolean("natural_scrolling", true),
                        onCheckedChange = {
                            prefs.edit().putBoolean("natural_scrolling", it).apply()
                        }
                    )

                    SwitchSetting(
                        title = "Tap to Click",
                        description = "Single tap performs a click",
                        checked = prefs.getBoolean("tap_to_click", true),
                        onCheckedChange = {
                            prefs.edit().putBoolean("tap_to_click", it).apply()
                        }
                    )

                    SwitchSetting(
                        title = "Two-Finger Right Click",
                        description = "Tap with two fingers for right click",
                        checked = prefs.getBoolean("two_finger_right_click", true),
                        onCheckedChange = {
                            prefs.edit().putBoolean("two_finger_right_click", it).apply()
                        }
                    )

                    SliderSetting(
                        title = "Air Mouse Sensitivity",
                        value = prefs.getFloat("air_mouse_sensitivity", 1.5f),
                        onValueChange = {
                            prefs.edit().putFloat("air_mouse_sensitivity", it).apply()
                        },
                        valueRange = 0.5f..5.0f,
                        displayValue = String.format("%.1fx", prefs.getFloat("air_mouse_sensitivity", 1.5f))
                    )

                    SliderSetting(
                        title = "Desk Mouse Sensitivity",
                        value = prefs.getFloat("desk_mouse_sensitivity", 2500f),
                        onValueChange = {
                            prefs.edit().putFloat("desk_mouse_sensitivity", it).apply()
                        },
                        valueRange = 1000f..5000f,
                        displayValue = String.format("%.0f", prefs.getFloat("desk_mouse_sensitivity", 2500f))
                    )
                }

                // Haptic & Visual Feedback
                SettingsSection(title = "Feedback & Appearance") {
                    HapticIntensitySetting(prefs = prefs)

                    SliderSetting(
                        title = "UI Opacity",
                        value = prefs.getFloat("ui_opacity", 0.95f),
                        onValueChange = {
                            prefs.edit().putFloat("ui_opacity", it).apply()
                        },
                        valueRange = 0.5f..1.0f,
                        displayValue = String.format("%.0f%%", prefs.getFloat("ui_opacity", 0.95f) * 100)
                    )

                    SwitchSetting(
                        title = "Touch Effects",
                        description = "Show animated glow under your fingers on the trackpad",
                        checked = touchFxEnabled,
                        onCheckedChange = {
                            touchFxEnabled = it
                            prefs.edit().putBoolean("touch_fx_enabled", it).apply()
                        }
                    )

                    if (touchFxEnabled) {
                        TouchEffectStyleSetting(
                            selectedStyle = touchFxStyle,
                            onStyleChange = { style ->
                                touchFxStyle = style
                                prefs.edit().putInt("touch_fx_style", style).apply()
                            }
                        )
                    }
                }

                // System Settings
                SettingsSection(title = "System") {
                    SwitchSetting(
                        title = "Keep Screen On",
                        description = "Prevent screen timeout in trackpad mode",
                        checked = keepScreenOn,
                        onCheckedChange = {
                            keepScreenOn = it
                            prefs.edit().putBoolean("keep_screen_on", it).apply()
                        }
                    )

                    SwitchSetting(
                        title = "Auto Reconnect",
                        description = "Automatically reconnect to last device",
                        checked = autoReconnect,
                        onCheckedChange = {
                            autoReconnect = it
                            prefs.edit().putBoolean("auto_reconnect", it).apply()
                        }
                    )
                }

                // Reset Settings Button
                OutlinedButton(
                    onClick = {
                        prefs.edit().clear().apply()
                        hideUIOverlay = false
                        showGestureGuide = true
                        showConnectionStatus = true
                        enableHapticFeedback = true
                        enableSoundFeedback = false
                        backgroundMode = 0
                        trackpadSensitivity = 1.0f
                        scrollSpeed = 1.0f
                        enableThreeFingerGestures = true
                        enableFourFingerGestures = true
                        enablePinchZoom = true
                        autoReconnect = true
                        keepScreenOn = true
                        minimalistMode = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Text("Reset to Defaults")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SliderSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = displayValue,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(top = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun BackgroundModeSetting(
    selectedMode: Int,
    onModeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Background Style",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BackgroundOption(
                label = "Gradient",
                isSelected = selectedMode == 0,
                onClick = { onModeChange(0) }
            )
            BackgroundOption(
                label = "Solid",
                isSelected = selectedMode == 1,
                onClick = { onModeChange(1) }
            )
            BackgroundOption(
                label = "Grid",
                isSelected = selectedMode == 2,
                onClick = { onModeChange(2) }
            )
        }
    }
}

@Composable
fun BackgroundOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (isSelected) 0.6f else 0.35f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun TouchEffectStyleSetting(
    selectedStyle: Int,
    onStyleChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Touch Effect Style",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BackgroundOption(
                label = "Soft Glow",
                isSelected = selectedStyle == 1,
                onClick = { onStyleChange(1) }
            )
            BackgroundOption(
                label = "Ripple",
                isSelected = selectedStyle == 2,
                onClick = { onStyleChange(2) }
            )
        }
    }
}

@Composable
fun KeyboardLayoutSetting(prefs: SharedPreferences) {
    var selectedLayout by remember {
        mutableStateOf(
            KeyboardLayout.valueOf(
                prefs.getString("keyboard_layout", KeyboardLayout.COMPACT.name) ?: KeyboardLayout.COMPACT.name
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Keyboard Layout",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LayoutOption(
                    label = "Compact",
                    icon = "⌨️",
                    isSelected = selectedLayout == KeyboardLayout.COMPACT,
                    onClick = {
                        selectedLayout = KeyboardLayout.COMPACT
                        prefs.edit().putString("keyboard_layout", KeyboardLayout.COMPACT.name).apply()
                    },
                    modifier = Modifier.weight(1f)
                )
                LayoutOption(
                    label = "QWERTY",
                    icon = "🔤",
                    isSelected = selectedLayout == KeyboardLayout.FULL_QWERTY,
                    onClick = {
                        selectedLayout = KeyboardLayout.FULL_QWERTY
                        prefs.edit().putString("keyboard_layout", KeyboardLayout.FULL_QWERTY.name).apply()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LayoutOption(
                    label = "Numeric",
                    icon = "🔢",
                    isSelected = selectedLayout == KeyboardLayout.NUMERIC,
                    onClick = {
                        selectedLayout = KeyboardLayout.NUMERIC
                        prefs.edit().putString("keyboard_layout", KeyboardLayout.NUMERIC.name).apply()
                    },
                    modifier = Modifier.weight(1f)
                )
                LayoutOption(
                    label = "Function",
                    icon = "Fn",
                    isSelected = selectedLayout == KeyboardLayout.FUNCTION_KEYS,
                    onClick = {
                        selectedLayout = KeyboardLayout.FUNCTION_KEYS
                        prefs.edit().putString("keyboard_layout", KeyboardLayout.FUNCTION_KEYS.name).apply()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            LayoutOption(
                label = "Arrow Keys",
                icon = "⬆️",
                isSelected = selectedLayout == KeyboardLayout.ARROWS,
                onClick = {
                    selectedLayout = KeyboardLayout.ARROWS
                    prefs.edit().putString("keyboard_layout", KeyboardLayout.ARROWS.name).apply()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LayoutOption(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surface,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (isSelected) 0.6f else 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun KeyboardThemeSetting(prefs: SharedPreferences) {
    var selectedTheme by remember {
        mutableStateOf(
            KeyboardTheme.valueOf(
                prefs.getString("keyboard_theme", KeyboardTheme.DARK.name) ?: KeyboardTheme.DARK.name
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Keyboard Theme",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeOption(
                label = "Dark",
                color = Color(0xFF2C2C2E),
                isSelected = selectedTheme == KeyboardTheme.DARK,
                onClick = {
                    selectedTheme = KeyboardTheme.DARK
                    prefs.edit().putString("keyboard_theme", KeyboardTheme.DARK.name).apply()
                },
                modifier = Modifier.weight(1f)
            )
            ThemeOption(
                label = "Light",
                color = Color(0xFFE5E5EA),
                isSelected = selectedTheme == KeyboardTheme.LIGHT,
                onClick = {
                    selectedTheme = KeyboardTheme.LIGHT
                    prefs.edit().putString("keyboard_theme", KeyboardTheme.LIGHT.name).apply()
                },
                modifier = Modifier.weight(1f)
            )
            ThemeOption(
                label = "Colorful",
                color = Color(0xFF667EEA),
                isSelected = selectedTheme == KeyboardTheme.COLORFUL,
                onClick = {
                    selectedTheme = KeyboardTheme.COLORFUL
                    prefs.edit().putString("keyboard_theme", KeyboardTheme.COLORFUL.name).apply()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ThemeOption(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .border(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text("✓", fontSize = 24.sp, color = Color.White)
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun HapticIntensitySetting(prefs: SharedPreferences) {
    var selectedIntensity by remember {
        mutableStateOf(
            HapticIntensity.valueOf(
                prefs.getString("haptic_intensity", HapticIntensity.MEDIUM.name) ?: HapticIntensity.MEDIUM.name
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Haptic Feedback Intensity",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HapticIntensity.values().forEach { intensity ->
                IntensityOption(
                    label = intensity.name.lowercase().replaceFirstChar { it.uppercase() },
                    isSelected = selectedIntensity == intensity,
                    onClick = {
                        selectedIntensity = intensity
                        prefs.edit().putString("haptic_intensity", intensity.name).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun IntensityOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}
