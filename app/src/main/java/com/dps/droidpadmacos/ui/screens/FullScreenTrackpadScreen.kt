package com.dps.droidpadmacos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dps.droidpadmacos.sensor.MouseSensor
import com.dps.droidpadmacos.sensor.MouseSensorFactory
import com.dps.droidpadmacos.settings.TrackpadPreferences
import com.dps.droidpadmacos.touchpad.EnhancedGestureDetector
import com.dps.droidpadmacos.ui.components.MacOSStatusBar
import com.dps.droidpadmacos.ui.components.MediaControls
import com.dps.droidpadmacos.viewmodel.TrackpadViewModel

/**
 * Main trackpad screen composable
 *
 * Single Responsibility: Orchestrates the trackpad UI components
 * Open/Closed: Easy to add new UI features without modifying existing ones
 * Dependency Inversion: Depends on abstractions (MouseSensor, TrackpadPreferences)
 */
@Composable
fun FullScreenTrackpadScreen(
    viewModel: TrackpadViewModel,
    onBackPressed: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember { TrackpadPreferences(context) }

    // Collect preferences as state
    val pointerSpeed by preferences.pointerSpeed.collectAsStateWithLifecycle()
    val scrollSpeed by preferences.scrollSpeed.collectAsStateWithLifecycle()
    val naturalScrolling by preferences.naturalScrolling.collectAsStateWithLifecycle()
    val tapToClick by preferences.tapToClick.collectAsStateWithLifecycle()
    val showGestureGuide by preferences.showGestureGuide.collectAsStateWithLifecycle()
    val backgroundMode by remember { mutableStateOf(0) } // Could be moved to preferences

    // Mouse sensor states
    var airMouseEnabled by remember { mutableStateOf(false) }
    var deskMouseEnabled by remember { mutableStateOf(false) }

    // Create sensors using factory
    val airMouseSensor = remember {
        if (isAirMouseAvailable(context)) {
            MouseSensorFactory.createSensor(
                MouseSensorFactory.SensorType.AIR_MOUSE,
                context
            ) { deltaX, deltaY ->
                viewModel.sendMouseMovement(
                    (deltaX * pointerSpeed).toInt(),
                    (deltaY * pointerSpeed).toInt()
                )
            }
        } else null
    }

    val deskMouseSensor = remember {
        if (isDeskMouseAvailable(context)) {
            MouseSensorFactory.createSensor(
                MouseSensorFactory.SensorType.DESK_MOUSE,
                context
            ) { deltaX, deltaY ->
                viewModel.sendMouseMovement(
                    (deltaX * pointerSpeed).toInt(),
                    (deltaY * pointerSpeed).toInt()
                )
            }
        } else null
    }

    // Sensor lifecycle
    DisposableEffect(airMouseEnabled) {
        if (airMouseEnabled) {
            airMouseSensor?.start()
        }
        onDispose {
            airMouseSensor?.stop()
        }
    }

    DisposableEffect(deskMouseEnabled) {
        if (deskMouseEnabled) {
            deskMouseSensor?.start()
        }
        onDispose {
            deskMouseSensor?.stop()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getBackgroundBrush(backgroundMode))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main trackpad area
            TrackpadArea(
                viewModel = viewModel,
                pointerSpeed = pointerSpeed,
                scrollSpeed = scrollSpeed,
                naturalScrolling = naturalScrolling,
                tapToClick = tapToClick,
                modifier = Modifier.weight(1f)
            )

            // Keyboard area (simplified for now)
            KeyboardArea(
                viewModel = viewModel,
                preferences = preferences
            )
        }

        // Overlay components
        TrackpadOverlay(
            viewModel = viewModel,
            preferences = preferences,
            airMouseEnabled = airMouseEnabled,
            deskMouseEnabled = deskMouseEnabled,
            onAirMouseToggle = { enabled ->
                airMouseEnabled = enabled
                if (enabled && deskMouseEnabled) {
                    deskMouseEnabled = false
                }
            },
            onDeskMouseToggle = { enabled ->
                deskMouseEnabled = enabled
                if (enabled && airMouseEnabled) {
                    airMouseEnabled = false
                }
            },
            onOpenSettings = onOpenSettings
        )

        // Mouse mode screens
        if (airMouseEnabled) {
            AirMouseScreen(
                onClose = { airMouseEnabled = false }
            )
        }

        if (deskMouseEnabled) {
            DeskMouseScreen(
                onClose = { deskMouseEnabled = false }
            )
        }
    }
}

@Composable
private fun TrackpadArea(
    viewModel: TrackpadViewModel,
    pointerSpeed: Float,
    scrollSpeed: Float,
    naturalScrolling: Boolean,
    tapToClick: Boolean,
    modifier: Modifier = Modifier
) {
    // Enhanced gesture detector for trackpad
    val gestureDetector = remember(pointerSpeed, scrollSpeed, naturalScrolling, tapToClick) {
        EnhancedGestureDetector(
            onMove = { deltaX, deltaY ->
                viewModel.sendMouseMovement(
                    (deltaX * pointerSpeed).toInt(),
                    (deltaY * pointerSpeed).toInt()
                )
            },
            onLeftClick = {
                if (tapToClick) viewModel.sendLeftClick()
            },
            onRightClick = { viewModel.sendRightClick() },
            onMiddleClick = { /* Optional: could be configured */ },
            onScroll = { deltaY ->
                val scrollDirection = if (naturalScrolling) 1 else -1
                viewModel.sendScroll((deltaY * scrollSpeed * scrollDirection).toInt())
            },
            onThreeFingerSwipeUp = { viewModel.sendMissionControl() },
            onThreeFingerSwipeDown = { viewModel.sendShowDesktop() },
            onThreeFingerSwipeLeft = { viewModel.sendSwitchToNextDesktop() },
            onThreeFingerSwipeRight = { viewModel.sendSwitchToPreviousDesktop() },
            onFourFingerSwipeLeft = { /* Could be used for back navigation */ },
            onFourFingerSwipeRight = { /* Could be used for forward navigation */ },
            onPinchZoom = { scale -> /* Could be used for zoom gestures */ },
            onDragStart = { /* Could trigger visual feedback */ },
            onDragEnd = { /* Could end visual feedback */ }
        )
    }

    DisposableEffect(gestureDetector) {
        onDispose {
            gestureDetector.reset()
        }
    }

    // Trackpad touch surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Add gesture detection here
    ) {
        // Trackpad implementation
    }
}

@Composable
private fun KeyboardArea(
    viewModel: TrackpadViewModel,
    preferences: TrackpadPreferences
) {
    // Keyboard implementation - could be extracted further
    // This is where the keyboard layouts would be rendered
}

@Composable
private fun TrackpadOverlay(
    viewModel: TrackpadViewModel,
    preferences: TrackpadPreferences,
    airMouseEnabled: Boolean,
    deskMouseEnabled: Boolean,
    onAirMouseToggle: (Boolean) -> Unit,
    onDeskMouseToggle: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Status bar at top
        MacOSStatusBar(
            context = LocalContext.current,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Media controls on left
        MediaControls(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )

        // Settings and mode buttons would go here
    }
}

private fun getBackgroundBrush(mode: Int): Brush {
    return when (mode) {
        0 -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D1117),
                Color(0xFF161B22)
            )
        )
        1 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF1C1C1E),
                Color(0xFF1C1C1E)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D1117),
                Color(0xFF161B22)
            )
        )
    }
}

private fun isAirMouseAvailable(context: android.content.Context): Boolean {
    // Check if gyroscope is available
    val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
    return sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE) != null
}

private fun isDeskMouseAvailable(context: android.content.Context): Boolean {
    // Check if linear acceleration is available
    val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
    return sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_LINEAR_ACCELERATION) != null
}
