package com.dps.droidpadmacos

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.bluetooth.HidConstants
import com.dps.droidpadmacos.sensor.AirMouseSensor
import com.dps.droidpadmacos.touchpad.EnhancedGestureDetector
import com.dps.droidpadmacos.ui.Dimens
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme
import com.dps.droidpadmacos.ui.theme.extendedColors
import com.dps.droidpadmacos.viewmodel.TrackpadViewModel

class FullScreenTrackpadActivity : ComponentActivity() {

    private val viewModel: TrackpadViewModel by viewModels()
    private var airMouseSensor: AirMouseSensor? = null

    // Hardware button state for Air Mouse mode
    private var isAirMouseEnabled = false
    private var lastVolumeDownPressTime = 0L
    private var volumeDownPressTime = 0L
    private var isDragging = false
    private var isLongPressHandled = false
    private val DOUBLE_CLICK_THRESHOLD = 300L // ms
    private val LONG_PRESS_THRESHOLD = 500L // ms

    // State management
    private val prefs by lazy {
        getSharedPreferences("DroidPadSettings", Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREF_BACKGROUND_MODE = "background_mode"
        private const val PREF_AIR_MOUSE_ENABLED = "air_mouse_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make full screen and keep screen on
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize air mouse sensor
        airMouseSensor = AirMouseSensor(this) { deltaX, deltaY ->
            viewModel.sendMouseMovement(deltaX, deltaY)
        }

        setContent {
            DroidPadMacOSTheme {
                FullScreenTrackpad(
                    viewModel = viewModel,
                    airMouseSensor = airMouseSensor,
                    onBackPress = { finish() },
                    onAirMouseToggle = { enabled -> isAirMouseEnabled = enabled },
                    initialBackgroundMode = prefs.getInt(PREF_BACKGROUND_MODE, 0),
                    initialAirMouseEnabled = prefs.getBoolean(PREF_AIR_MOUSE_ENABLED, false),
                    onBackgroundModeChanged = { mode ->
                        prefs.edit().putInt(PREF_BACKGROUND_MODE, mode).apply()
                    },
                    onAirMouseEnabledChanged = { enabled ->
                        prefs.edit().putBoolean(PREF_AIR_MOUSE_ENABLED, enabled).apply()
                    }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Only handle volume buttons when Air Mouse is enabled
        if (!isAirMouseEnabled) {
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event?.repeatCount == 0) {
                    volumeDownPressTime = System.currentTimeMillis()
                    isLongPressHandled = false
                }

                // Check for long press (drag mode toggle)
                if (!isLongPressHandled && (System.currentTimeMillis() - volumeDownPressTime) >= LONG_PRESS_THRESHOLD) {
                    isDragging = !isDragging
                    isLongPressHandled = true

                    // Toggle mouse button state for dragging
                    if (isDragging) {
                        viewModel.sendMouseButtonPress(HidConstants.BUTTON_LEFT)
                        android.util.Log.d("FullScreenTrackpad", "Drag mode: ON (button pressed)")
                    } else {
                        viewModel.sendMouseButtonRelease()
                        android.util.Log.d("FullScreenTrackpad", "Drag mode: OFF (button released)")
                    }
                    return true
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                // Right click on volume up press
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // Only handle volume buttons when Air Mouse is enabled
        if (!isAirMouseEnabled) {
            return super.onKeyUp(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val currentTime = System.currentTimeMillis()
                val pressDuration = currentTime - volumeDownPressTime

                // Ignore if long press was already handled
                if (isLongPressHandled) {
                    return true
                }

                // Check for double click
                if (currentTime - lastVolumeDownPressTime < DOUBLE_CLICK_THRESHOLD) {
                    // Double click detected
                    android.util.Log.d("FullScreenTrackpad", "Double click")
                    viewModel.sendLeftClick()
                    lifecycleScope.launch {
                        delay(50)
                        viewModel.sendLeftClick()
                    }
                    lastVolumeDownPressTime = 0L // Reset to avoid triple click
                } else {
                    // Single click
                    android.util.Log.d("FullScreenTrackpad", "Single left click")
                    viewModel.sendLeftClick()
                    lastVolumeDownPressTime = currentTime
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                // Right click on volume up release
                android.util.Log.d("FullScreenTrackpad", "Right click")
                viewModel.sendRightClick()
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        // Stop air mouse when going to background
        airMouseSensor?.stop()
    }

    override fun onResume() {
        super.onResume()
        // Restart air mouse if it was enabled
        if (isAirMouseEnabled) {
            airMouseSensor?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        airMouseSensor?.stop()
    }
}

@Composable
private fun GestureGuideItem(icon: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(Dimens.keyboardKeyWidth())
    ) {
        Text(
            text = icon,
            fontSize = Dimens.gestureGuideIconSize(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            fontSize = Dimens.gestureGuideTextSize(),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )

    Surface(
        modifier = Modifier
            .width(64.dp)
            .height(84.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = if (isPressed) 2.dp else 6.dp,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 26.sp,
                    color = contentColor
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// Modern keyboard key with glassmorphism design
@Composable
private fun KeyboardKey(
    text: String,
    label: String? = null,
    width: Int = 50,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "keyScale"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "keyElevation"
    )

    val keyWidth = Dimens.keyboardKeyWidth()
    val keyHeight = Dimens.keyboardKeyHeight()

    Box(
        modifier = Modifier
            .width(keyWidth)
            .height(keyHeight)
            .scale(scale)
    ) {
        // Outer glow effect
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            color = Color.Transparent,
            shadowElevation = elevation.dp
        ) {}

        // Main key with gradient
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isPressed) {
                                listOf(
                                    Color(0xFF4A4A4C),
                                    Color(0xFF3A3A3C)
                                )
                            } else {
                                listOf(
                                    Color(0xFF5A5A5C),
                                    Color(0xFF4A4A4C)
                                )
                            }
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                    )
                    .then(
                        // Subtle top highlight
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = 40f
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = text,
                        fontSize = Dimens.keyboardKeyTextSize(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                blurRadius = 2f
                            )
                        )
                    )
                    if (label != null) {
                        Text(
                            text = label,
                            fontSize = Dimens.keyboardLabelTextSize(),
                            color = Color(0xFFB0B0B0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Dedicated Air Mouse Screen with modern UI
@Composable
private fun AirMouseScreen(
    viewModel: TrackpadViewModel,
    onClose: () -> Unit,
    onDisable: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF0F0F1E)
                    )
                )
            )
    ) {
        // Animated background particles effect
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Draw subtle grid
            val gridSize = 80f
            for (i in 0 until (size.width / gridSize).toInt()) {
                drawLine(
                    color = Color(0xFF2A2A3E).copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(i * gridSize, 0f),
                    end = androidx.compose.ui.geometry.Offset(i * gridSize, size.height),
                    strokeWidth = 1f
                )
            }
            for (i in 0 until (size.height / gridSize).toInt()) {
                drawLine(
                    color = Color(0xFF2A2A3E).copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(0f, i * gridSize),
                    end = androidx.compose.ui.geometry.Offset(size.width, i * gridSize),
                    strokeWidth = 1f
                )
            }
        }

        // Top bar with controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close button
            Surface(
                onClick = onClose,
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFF2A2A3E).copy(alpha = 0.8f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "←",
                        fontSize = 24.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Disable button
            Surface(
                onClick = onDisable,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = Color(0xFFFF4444).copy(alpha = 0.8f),
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✕",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Disable",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Center content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Main icon with pulse animation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00D9FF).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📱",
                    fontSize = 64.sp
                )
            }

            // Title
            Text(
                text = "Air Mouse Active",
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF00D9FF).copy(alpha = 0.5f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                        blurRadius = 12f
                    )
                )
            )

            // Instructions card
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = Color(0xFF2A2A3E).copy(alpha = 0.6f),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AirMouseInstructionItem("🎯", "Tilt phone", "Move cursor")
                    AirMouseInstructionItem("Vol−", "Single press", "Left click")
                    AirMouseInstructionItem("Vol+", "Single press", "Right click")
                    AirMouseInstructionItem("Vol−", "Double press", "Double click")
                    AirMouseInstructionItem("Vol−", "Long press", "Toggle drag")
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AirMouseActionButton(
                    icon = "⌘C",
                    label = "Copy",
                    onClick = { viewModel.sendCopy() }
                )
                AirMouseActionButton(
                    icon = "⌘V",
                    label = "Paste",
                    onClick = { viewModel.sendPaste() }
                )
                AirMouseActionButton(
                    icon = "⌘Z",
                    label = "Undo",
                    onClick = { viewModel.sendUndo() }
                )
            }
        }
    }
}

@Composable
private fun AirMouseInstructionItem(icon: String, action: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Column {
                Text(
                    text = action,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0B0)
                )
            }
        }
    }
}

@Composable
private fun AirMouseActionButton(icon: String, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = if (isPressed) Color(0xFF3A3A4E) else Color(0xFF2A2A3E),
        modifier = Modifier.size(90.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFFB0B0B0),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FullScreenTrackpad(
    viewModel: TrackpadViewModel,
    airMouseSensor: AirMouseSensor?,
    onBackPress: () -> Unit,
    onAirMouseToggle: (Boolean) -> Unit,
    initialBackgroundMode: Int = 0,
    initialAirMouseEnabled: Boolean = false,
    onBackgroundModeChanged: (Int) -> Unit = {},
    onAirMouseEnabledChanged: (Boolean) -> Unit = {}
) {
    var airMouseEnabled by remember { mutableStateOf(initialAirMouseEnabled) }
    var backgroundMode by remember { mutableStateOf(initialBackgroundMode) }
    var showAirMouseScreen by remember { mutableStateOf(false) }
    var gestureInfo by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }

    // Load user preferences
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("DroidPadSettings", Context.MODE_PRIVATE) }
    val hideUIOverlay by remember { mutableStateOf(settingsPrefs.getBoolean("hide_ui_overlay", false)) }
    val showGestureGuide by remember { mutableStateOf(settingsPrefs.getBoolean("show_gesture_guide", true)) }
    val showConnectionStatus by remember { mutableStateOf(settingsPrefs.getBoolean("show_connection_status", true)) }
    val minimalistMode by remember { mutableStateOf(settingsPrefs.getBoolean("minimalist_mode", false)) }
    val trackpadSensitivity by remember { mutableFloatStateOf(settingsPrefs.getFloat("trackpad_sensitivity", 1.0f)) }
    val scrollSpeed by remember { mutableFloatStateOf(settingsPrefs.getFloat("scroll_speed", 1.0f)) }

    // Handle air mouse toggle
    LaunchedEffect(airMouseEnabled) {
        if (airMouseEnabled) {
            airMouseSensor?.start()
        } else {
            airMouseSensor?.stop()
        }
    }

    val gestureDetector = remember(trackpadSensitivity, scrollSpeed) {
        EnhancedGestureDetector(
            onMove = { deltaX, deltaY ->
                // Apply sensitivity setting
                val adjustedDeltaX = (deltaX * trackpadSensitivity).toInt()
                val adjustedDeltaY = (deltaY * trackpadSensitivity).toInt()
                viewModel.sendMouseMovement(adjustedDeltaX, adjustedDeltaY)
            },
            onLeftClick = {
                viewModel.sendLeftClick()
            },
            onRightClick = {
                viewModel.sendRightClick()
            },
            onMiddleClick = {
                // Middle click - reserved for future use
            },
            onScroll = { deltaY ->
                // Apply scroll speed setting
                val adjustedDeltaY = (deltaY * scrollSpeed).toInt()
                viewModel.sendScroll(adjustedDeltaY)
            },
            onThreeFingerSwipeUp = {
                viewModel.sendMissionControl()
            },
            onThreeFingerSwipeDown = {
                viewModel.sendShowDesktop()
            },
            onThreeFingerSwipeLeft = {
                viewModel.sendSwitchToPreviousDesktop()
            },
            onThreeFingerSwipeRight = {
                viewModel.sendSwitchToNextDesktop()
            },
            onFourFingerSwipeLeft = {
                // Reserved for future use
            },
            onFourFingerSwipeRight = {
                // Reserved for future use
            },
            onPinchZoom = { scale ->
                // Pinch zoom gesture
            },
            onDragStart = {
                // Press and hold left button for dragging
                viewModel.sendMouseButtonPress(HidConstants.BUTTON_LEFT)
            },
            onDragEnd = {
                // Release left button when drag ends
                viewModel.sendMouseButtonRelease()
            }
        )
    }

    // Auto-hide info removed - silent mode

    // Show dedicated Air Mouse screen when enabled
    if (showAirMouseScreen && airMouseEnabled) {
        AirMouseScreen(
            viewModel = viewModel,
            onClose = { showAirMouseScreen = false },
            onDisable = {
                airMouseEnabled = false
                showAirMouseScreen = false
                onAirMouseToggle(false)
                onAirMouseEnabledChanged(false)
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (backgroundMode) {
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
                    else -> Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2C2C2E),
                            Color(0xFF0D1117)
                        )
                    )
                }
            )
    ) {
        // Grid pattern overlay for mode 2
        if (backgroundMode == 2) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val gridSize = 50f
                for (i in 0 until (size.width / gridSize).toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = androidx.compose.ui.geometry.Offset(i * gridSize, 0f),
                        end = androidx.compose.ui.geometry.Offset(i * gridSize, size.height),
                        strokeWidth = 1f
                    )
                }
                for (i in 0 until (size.height / gridSize).toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = androidx.compose.ui.geometry.Offset(0f, i * gridSize),
                        end = androidx.compose.ui.geometry.Offset(size.width, i * gridSize),
                        strokeWidth = 1f
                    )
                }
            }
        }

        // Trackpad gesture area - BEHIND the controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    gestureDetector.handleTouchEvent(event)
                }
        )

        // Top bar - ABOVE the trackpad area (conditionally shown)
        if (!hideUIOverlay) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
            ) {
                // First row: Close button and connection status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close button - top left corner (always show if UI not hidden)
                    IconButton(
                        onClick = onBackPress,
                        modifier = Modifier
                            .size(Dimens.closeButtonSize())
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                Spacer(modifier = Modifier.width(8.dp))

                // Connection indicator - next to close button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.extendedColors.successContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                        .padding(
                            horizontal = Dimens.connectionBadgePaddingHorizontal(),
                            vertical = Dimens.connectionBadgePaddingVertical()
                        )
                ) {
                    Text(
                        text = "📡",
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = MaterialTheme.extendedColors.success,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Text(
                        text = "Bluetooth",
                        fontSize = 14.sp,
                        color = MaterialTheme.extendedColors.onSuccessContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Second row: Mini Keyboard Layout (aligned to right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = Color(0xFF2C2C2E).copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                Column(
                    modifier = Modifier.padding(Dimens.keyboardPadding()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.keyboardSpacing())
                ) {
                    // Row 1: Shortcuts
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.keyboardSpacing())) {
                        KeyboardKey("⌘C", "Copy") {
                            viewModel.sendCopy()
                            gestureInfo = "Copy (⌘C)"
                            showInfo = true
                        }
                        KeyboardKey("⌘V", "Paste") {
                            viewModel.sendPaste()
                            gestureInfo = "Paste (⌘V)"
                            showInfo = true
                        }
                        KeyboardKey("⌘X", "Cut") {
                            viewModel.sendCut()
                            gestureInfo = "Cut (⌘X)"
                            showInfo = true
                        }
                        KeyboardKey("⌘Z", "Undo") {
                            viewModel.sendUndo()
                            gestureInfo = "Undo (⌘Z)"
                            showInfo = true
                        }
                    }

                    // Row 2: Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.keyboardSpacing())) {
                        KeyboardKey("⌘A", "All") {
                            viewModel.sendSelectAll()
                            gestureInfo = "Select All"
                            showInfo = true
                        }
                        KeyboardKey("⌘T", "Tab") {
                            viewModel.sendNewTab()
                            gestureInfo = "New Tab"
                            showInfo = true
                        }
                        KeyboardKey("⌘W", "Close") {
                            viewModel.sendCloseWindow()
                            gestureInfo = "Close Window"
                            showInfo = true
                        }
                        KeyboardKey("⌘Q", "Quit") {
                            viewModel.sendQuitApp()
                            gestureInfo = "Quit App"
                            showInfo = true
                        }
                    }

                    // Row 3: Special Keys
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.keyboardSpacing())) {
                        KeyboardKey("ESC", null) {
                            viewModel.sendKeyPress(HidConstants.KEY_ESCAPE)
                            gestureInfo = "Escape"
                            showInfo = true
                        }
                        KeyboardKey("⌫", "Del") {
                            viewModel.sendKeyPress(HidConstants.KEY_BACKSPACE)
                            gestureInfo = "Delete"
                            showInfo = true
                        }
                        KeyboardKey("↵", "Enter") {
                            viewModel.sendKeyPress(HidConstants.KEY_ENTER)
                            gestureInfo = "Enter"
                            showInfo = true
                        }
                        KeyboardKey("⌘Sp", "Spot") {
                            viewModel.sendSpotlight()
                            gestureInfo = "Spotlight"
                            showInfo = true
                        }
                    }

                    // Row 4: System Actions & Air Mouse
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.keyboardSpacing())) {
                        KeyboardKey("MC", null) {
                            viewModel.sendMissionControl()
                            gestureInfo = "Mission Control"
                            showInfo = true
                        }
                        KeyboardKey("Apps", null) {
                            viewModel.sendAppSwitcher()
                            gestureInfo = "App Switcher"
                            showInfo = true
                        }
                        KeyboardKey("Desk", null) {
                            viewModel.sendShowDesktop()
                            gestureInfo = "Show Desktop"
                            showInfo = true
                        }
                        if (airMouseSensor?.isAvailable() == true) {
                            KeyboardKey("Air", if (airMouseEnabled) "ON" else "OFF") {
                                airMouseEnabled = !airMouseEnabled
                                onAirMouseToggle(airMouseEnabled)
                                onAirMouseEnabledChanged(airMouseEnabled)
                                if (airMouseEnabled) {
                                    showAirMouseScreen = true
                                }
                                gestureInfo = if (airMouseEnabled) "Air Mouse ON" else "Air Mouse OFF"
                                showInfo = true
                            }
                        } else {
                            KeyboardKey(
                                when (backgroundMode) {
                                    0 -> "🌈"
                                    1 -> "⬜"
                                    else -> "⊞"
                                }, "BG"
                            ) {
                                backgroundMode = (backgroundMode + 1) % 3
                                onBackgroundModeChanged(backgroundMode)
                                gestureInfo = when (backgroundMode) {
                                    0 -> "Gradient"
                                    1 -> "Solid"
                                    else -> "Grid"
                                }
                                showInfo = true
                            }
                        }
                    }
                }
            }
        }
        }
        }

        // Center gesture hint or Air Mouse indicator (conditionally shown)
        if (!hideUIOverlay && !minimalistMode && (gestureInfo.isEmpty() || airMouseEnabled)) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        color = if (airMouseEnabled)
                            MaterialTheme.extendedColors.successContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (airMouseEnabled) {
                    Text(
                        text = "📱",
                        fontSize = 60.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Air Mouse Mode",
                        fontSize = 20.sp,
                        color = MaterialTheme.extendedColors.success,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tilt phone to move cursor",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Vol− = Click • Vol+ = Right Click",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = "Vol− (2x) = Double Click",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = "Vol− (long) = Toggle Drag",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal
                    )
                } else {
                    Text(
                        text = "🖐️",
                        fontSize = Dimens.gestureHintIconSize()
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacingMedium()))
                    Text(
                        text = "Touch anywhere to begin",
                        fontSize = Dimens.gestureHintTextSize(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Gesture feedback removed - silent mode

        // Bottom gesture guide (subtle) - conditionally shown based on settings
        if (!hideUIOverlay && showGestureGuide) {
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GestureGuideItem("1👆", "Move")
            GestureGuideItem("👆", "Click")
            GestureGuideItem("2👆", "Right")
            GestureGuideItem("2⇅", "Scroll")
            GestureGuideItem("3⬆", "MissCtr")
            GestureGuideItem("3⬇", "Desktop")
            GestureGuideItem("3⬅➡", "Spaces")
            }
        }
    }
}
