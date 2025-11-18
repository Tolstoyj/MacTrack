package com.dps.droidpadmacos

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
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
                        // If hiding all UI, also update related settings
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
                    description = "Display Bluetooth/USB connection indicator",
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

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // Gesture Settings
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

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(32.dp))

            // Reset Settings Button
            OutlinedButton(
                onClick = {
                    // Reset all settings to defaults
                    prefs.edit().clear().apply()
                    // Reload defaults
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
                )
            ) {
                Text("Reset to Defaults")
            }

            Spacer(modifier = Modifier.height(16.dp))
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
            .padding(vertical = 8.dp),
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
            enabled = enabled
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
            modifier = Modifier.padding(top = 8.dp)
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
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}