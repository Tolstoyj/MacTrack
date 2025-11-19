package com.dps.droidpadmacos

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.bluetooth.HidConstants
import com.dps.droidpadmacos.sensor.AirMouseSensor
import com.dps.droidpadmacos.sensor.DeskMouseSensor
import com.dps.droidpadmacos.settings.KeyboardLayout
import com.dps.droidpadmacos.settings.KeyboardTheme
import com.dps.droidpadmacos.touchpad.EnhancedGestureDetector
import com.dps.droidpadmacos.ui.*
import com.dps.droidpadmacos.ui.Dimens
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme
import com.dps.droidpadmacos.ui.theme.extendedColors
import com.dps.droidpadmacos.viewmodel.TrackpadViewModel
import com.dps.droidpadmacos.ui.components.TrackpadSettingsDrawer
import com.dps.droidpadmacos.ui.components.FloatingUIControls
import com.dps.droidpadmacos.ui.components.KeyPreset

class FullScreenTrackpadActivity : ComponentActivity() {

    private val viewModel: TrackpadViewModel by viewModels()
    private var airMouseSensor: AirMouseSensor? = null
    private var deskMouseSensor: DeskMouseSensor? = null

    // Hardware button state for Air Mouse mode
    private var isAirMouseEnabled = false
    private var isDeskMouseEnabled = false
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

    // Permission launcher for WRITE_SETTINGS
    private val writeSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Permission result handled - can try writing again if needed
    }

    companion object {
        private const val PREF_BACKGROUND_MODE = "background_mode"
        private const val PREF_AIR_MOUSE_ENABLED = "air_mouse_enabled"
        private const val PREF_DESK_MOUSE_ENABLED = "desk_mouse_enabled"
    }

    /**
     * Checks if app has permission to write system settings.
     * For Android M+, this requires runtime permission via Settings.
     */
    private fun canWriteSystemSettings(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(this)
        } else {
            // For older versions, assume permission is available if declared in manifest
            true
        }
    }

    /**
     * Requests WRITE_SETTINGS permission by opening system settings.
     */
    private fun requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            writeSettingsLauncher.launch(intent)
        }
    }

    /**
     * Safely sets system brightness with permission check.
     * Falls back to window-only brightness if system permission is not available.
     */
    private fun setBrightnessSafely(brightness: Float) {
        // Always apply to current window (doesn't require system permission)
        window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness.coerceIn(0f, 1f)
            window.attributes = layoutParams
        }

        // Try to set system brightness if permission is available
        if (canWriteSystemSettings()) {
            try {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (brightness * 255).toInt().coerceIn(0, 255)
                )
            } catch (e: SecurityException) {
                android.util.Log.w("FullScreenTrackpad", "No permission to write system brightness: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.w("FullScreenTrackpad", "Failed to write system brightness: ${e.message}")
            }
        } else {
            // Permission not available - window brightness is already set above
            android.util.Log.d("FullScreenTrackpad", "WRITE_SETTINGS permission not granted, using window brightness only")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make full screen and keep screen on using modern WindowInsets APIs
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize air mouse sensor
        airMouseSensor = AirMouseSensor(this) { deltaX, deltaY ->
            viewModel.sendMouseMovement(deltaX, deltaY)
        }

        // Initialize desk mouse sensor
        deskMouseSensor = DeskMouseSensor(this) { deltaX, deltaY ->
            viewModel.sendMouseMovement(deltaX, deltaY)
        }

        setContent {
            DroidPadMacOSTheme {
                FullScreenTrackpad(
                    viewModel = viewModel,
                    airMouseSensor = airMouseSensor,
                    deskMouseSensor = deskMouseSensor,
                    onBackPress = { finish() },
                    onAirMouseToggle = { enabled -> isAirMouseEnabled = enabled },
                    onDeskMouseToggle = { enabled -> isDeskMouseEnabled = enabled },
                    initialBackgroundMode = prefs.getInt(PREF_BACKGROUND_MODE, 0),
                    initialAirMouseEnabled = prefs.getBoolean(PREF_AIR_MOUSE_ENABLED, false),
                    initialDeskMouseEnabled = prefs.getBoolean(PREF_DESK_MOUSE_ENABLED, false),
                    onBackgroundModeChanged = { mode ->
                        prefs.edit().putInt(PREF_BACKGROUND_MODE, mode).apply()
                    },
                    onAirMouseEnabledChanged = { enabled ->
                        prefs.edit().putBoolean(PREF_AIR_MOUSE_ENABLED, enabled).apply()
                    },
                    onDeskMouseEnabledChanged = { enabled ->
                        prefs.edit().putBoolean(PREF_DESK_MOUSE_ENABLED, enabled).apply()
                    }
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Only handle volume buttons when Air Mouse or Desk Mouse is enabled
        if (!isAirMouseEnabled && !isDeskMouseEnabled) {
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
        // Only handle volume buttons when Air Mouse or Desk Mouse is enabled
        if (!isAirMouseEnabled && !isDeskMouseEnabled) {
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
        // Stop sensors when going to background
        airMouseSensor?.stop()
        deskMouseSensor?.stop()
    }

    override fun onResume() {
        super.onResume()
        // Restart sensors if they were enabled
        if (isAirMouseEnabled) {
            airMouseSensor?.start()
        }
        if (isDeskMouseEnabled) {
            deskMouseSensor?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        airMouseSensor?.stop()
        deskMouseSensor?.stop()
        airMouseSensor = null
        deskMouseSensor = null
    }
}

// macOS-style HUD Status Bar
@Composable
private fun MacOSStatusBar(context: Context) {
    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var batteryLevel by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }

    // Update time every minute
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(60000)
        }
    }

    // Get battery info
    LaunchedEffect(Unit) {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    Row(
        modifier = Modifier
            .background(
                color = Color(0xFF1C1C1E).copy(alpha = 0.85f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Battery indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isCharging) "⚡" else when {
                    batteryLevel > 75 -> "🔋"
                    batteryLevel > 25 -> "🔋"
                    else -> "🪫"
                },
                fontSize = 14.sp
            )
            Text(
                text = "$batteryLevel%",
                fontSize = 12.sp,
                color = when {
                    batteryLevel > 20 -> Color.White
                    else -> Color(0xFFFF3B30)
                },
                fontWeight = FontWeight.Medium
            )
        }

        // Time
        Text(
            text = currentTime,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        // Connection indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = Color(0xFF00D9FF),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}

// Extracted components are now in separate files
// TrackpadSettingsDrawer and FloatingUIControls are in ui/components/TrackpadSettingsDrawer.kt
// KeyPreset data class is also in TrackpadSettingsDrawer.kt

// Collapsible Volume/Brightness Control for Mac
private data class TouchFxPoint(
    val id: Int,
    val x: Float,
    val y: Float
)

@Composable
private fun TouchFxLayer(
    points: List<TouchFxPoint>,
    style: Int,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "touchFx")
    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "touchFxScale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "touchFxAlpha"
    )

    Box(modifier = modifier) {
        points.forEach { point ->
            val xDp = with(density) { point.x.toDp() }
            val yDp = with(density) { point.y.toDp() }

            when (style) {
                1 -> {
                    // Soft radial glow
                    Box(
                        modifier = Modifier
                            .offset(x = xDp - 40.dp, y = yDp - 40.dp)
                            .size(80.dp * pulseScale)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF00D9FF).copy(alpha = 0.30f * pulseAlpha),
                                        Color.Transparent
                                    )
                                ),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
                2 -> {
                    // Ripple ring
                    Box(
                        modifier = Modifier
                            .offset(x = xDp - 36.dp, y = yDp - 36.dp)
                            .size(72.dp * pulseScale)
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.45f * pulseAlpha),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleControl(
    icon: String,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,  // Send Mac command to increase
    onDecrement: () -> Unit   // Send Mac command to decrease
) {
    val width by animateDpAsState(
        targetValue = if (isExpanded) 80.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "controlWidth"
    )

    val height by animateDpAsState(
        targetValue = if (isExpanded) 240.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "controlHeight"
    )

    Surface(
        modifier = Modifier
            .width(width)
            .height(height)
            .clickable(onClick = onToggle),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = Color(0xFF2C2C2E).copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    color = Color.White
                )
                Text(
                    text = "${(value * 100).toInt()}%",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // + Button (Increment)
                Surface(
                    onClick = onIncrement,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color(0xFF4A4A4C),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+",
                            fontSize = 24.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Visual level indicator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(40.dp)
                        .background(
                            color = Color(0xFF4A4A4C),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(value)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF00D9FF),
                                        Color(0xFF0080FF)
                                    )
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                    )
                }

                // - Button (Decrement)
                Surface(
                    onClick = onDecrement,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color(0xFF4A4A4C),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "−",
                            fontSize = 24.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color(0xFFB0B0B0),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
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

// Dedicated Desk Mouse Screen with modern UI
@Composable
private fun DeskMouseScreen(
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
                        Color(0xFF1E2A1A),
                        Color(0xFF0F1A0E)
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
                    color = Color(0xFF2E3A2E).copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(i * gridSize, 0f),
                    end = androidx.compose.ui.geometry.Offset(i * gridSize, size.height),
                    strokeWidth = 1f
                )
            }
            for (i in 0 until (size.height / gridSize).toInt()) {
                drawLine(
                    color = Color(0xFF2E3A2E).copy(alpha = 0.3f),
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
                color = Color(0xFF2E3A2E).copy(alpha = 0.8f),
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
                                Color(0xFF00FF88).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🖱️",
                    fontSize = 64.sp
                )
            }

            // Title
            Text(
                text = "Desk Mouse Active",
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xFF00FF88).copy(alpha = 0.5f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                        blurRadius = 12f
                    )
                )
            )

            // Instructions card
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = Color(0xFF2E3A2E).copy(alpha = 0.6f),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AirMouseInstructionItem("🖱️", "Move phone on desk", "Move cursor")
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
    deskMouseSensor: DeskMouseSensor?,
    onBackPress: () -> Unit,
    onAirMouseToggle: (Boolean) -> Unit,
    onDeskMouseToggle: (Boolean) -> Unit,
    initialBackgroundMode: Int = 0,
    initialAirMouseEnabled: Boolean = false,
    initialDeskMouseEnabled: Boolean = false,
    onBackgroundModeChanged: (Int) -> Unit = {},
    onAirMouseEnabledChanged: (Boolean) -> Unit = {},
    onDeskMouseEnabledChanged: (Boolean) -> Unit = {}
) {
    // Load context and preferences first
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val settingsPrefs = remember { context.getSharedPreferences("DroidPadSettings", Context.MODE_PRIVATE) }

    var airMouseEnabled by remember { mutableStateOf(initialAirMouseEnabled) }
    var deskMouseEnabled by remember { mutableStateOf(initialDeskMouseEnabled) }
    var backgroundMode by remember { mutableStateOf(initialBackgroundMode) }
    var showAirMouseScreen by remember { mutableStateOf(false) }
    var showDeskMouseScreen by remember { mutableStateOf(false) }
    var gestureInfo by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }

    // Track if user has touched the screen to hide the initial overlay
    var userHasTouched by remember { mutableStateOf(false) }

    // Keyboard visibility state
    var showKeyboard by remember { mutableStateOf(true) }

    // Volume and brightness controls
    var brightnessExpanded by remember { mutableStateOf(false) }
    var volumeExpanded by remember { mutableStateOf(false) }

    // UI customization states
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var uiVisible by remember { mutableStateOf(true) }
    var keyboardScale by remember {
        mutableFloatStateOf(settingsPrefs.getFloat("keyboard_scale", 1.0f))
    }
    var selectedKeyboardLayout by remember {
        mutableStateOf(
            KeyboardLayout.valueOf(
                settingsPrefs.getString("keyboard_layout", KeyboardLayout.COMPACT.name) ?: KeyboardLayout.COMPACT.name
            )
        )
    }
    var keyboardTheme by remember {
        mutableStateOf(
            KeyboardTheme.valueOf(
                settingsPrefs.getString("keyboard_theme", KeyboardTheme.DARK.name) ?: KeyboardTheme.DARK.name
            )
        )
    }
    var showLayoutSwitcher by remember { mutableStateOf(false) }
    var visibleKeys by remember {
        mutableStateOf(
            settingsPrefs.getStringSet("visible_keys", null)?.toSet() ?: setOf(
                "copy", "paste", "cut", "undo", "select_all", "new_tab", "close_window",
                "quit_app", "esc", "delete", "enter", "spotlight", "mission_control",
                "app_switcher", "show_desktop"
            )
        )
    }

    // Auto-collapse after 3 seconds of no interaction
    LaunchedEffect(brightnessExpanded) {
        if (brightnessExpanded) {
            kotlinx.coroutines.delay(3000)
            brightnessExpanded = false
        }
    }

    LaunchedEffect(volumeExpanded) {
        if (volumeExpanded) {
            kotlinx.coroutines.delay(3000)
            volumeExpanded = false
        }
    }

    // Audio manager and user preferences
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Get current brightness
    var brightness by remember {
        mutableFloatStateOf(
            try {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
            } catch (e: Settings.SettingNotFoundException) {
                0.5f
            }
        )
    }

    // Get current volume
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var volume by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat()
        )
    }
    val touchFxEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("touch_fx_enabled", true)) }
    val touchFxStyle by remember { mutableStateOf(settingsPrefs.getInt("touch_fx_style", 1)) }
    val hideUIOverlay by remember { mutableStateOf(settingsPrefs.getBoolean("hide_ui_overlay", false)) }
    val showGestureGuide by remember { mutableStateOf(settingsPrefs.getBoolean("show_gesture_guide", true)) }
    val showConnectionStatus by remember { mutableStateOf(settingsPrefs.getBoolean("show_connection_status", true)) }
    val minimalistMode by remember { mutableStateOf(settingsPrefs.getBoolean("minimalist_mode", false)) }
    val trackpadSensitivity by remember { mutableFloatStateOf(settingsPrefs.getFloat("trackpad_sensitivity", 1.0f)) }
    val scrollSpeed by remember { mutableFloatStateOf(settingsPrefs.getFloat("scroll_speed", 1.0f)) }
    var touchFxPoints by remember { mutableStateOf<List<TouchFxPoint>>(emptyList()) }

    // Handle air mouse toggle
    LaunchedEffect(airMouseEnabled) {
        if (airMouseEnabled) {
            // Disable desk mouse if air mouse is enabled
            if (deskMouseEnabled) {
                deskMouseEnabled = false
                deskMouseSensor?.stop()
                onDeskMouseToggle(false)
                onDeskMouseEnabledChanged(false)
            }
            airMouseSensor?.start()
        } else {
            airMouseSensor?.stop()
        }
    }

    // Handle desk mouse toggle
    LaunchedEffect(deskMouseEnabled) {
        if (deskMouseEnabled) {
            // Disable air mouse if desk mouse is enabled
            if (airMouseEnabled) {
                airMouseEnabled = false
                airMouseSensor?.stop()
                onAirMouseToggle(false)
                onAirMouseEnabledChanged(false)
            }
            deskMouseSensor?.start()
        } else {
            deskMouseSensor?.stop()
        }
    }

    val gestureDetector = remember(trackpadSensitivity, scrollSpeed, haptic) {
        EnhancedGestureDetector(
            onMove = { deltaX, deltaY ->
                // Apply sensitivity setting
                val adjustedDeltaX = (deltaX * trackpadSensitivity).toInt()
                val adjustedDeltaY = (deltaY * trackpadSensitivity).toInt()
                viewModel.sendMouseMovement(adjustedDeltaX, adjustedDeltaY)
            },
            onLeftClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.sendLeftClick()
            },
            onRightClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

    // Ensure any pending callbacks are cleaned up when composable leaves the composition
    DisposableEffect(gestureDetector) {
        onDispose {
            gestureDetector.reset()
        }
    }

    // Auto-hide info removed - silent mode

    // Show dedicated Desk Mouse screen when enabled
    if (showDeskMouseScreen && deskMouseEnabled) {
        DeskMouseScreen(
            viewModel = viewModel,
            onClose = { showDeskMouseScreen = false },
            onDisable = {
                deskMouseEnabled = false
                showDeskMouseScreen = false
                onDeskMouseToggle(false)
                onDeskMouseEnabledChanged(false)
            }
        )
        return
    }

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
                    // Set userHasTouched to true on first touch
                    if (!userHasTouched && event.action == android.view.MotionEvent.ACTION_DOWN) {
                        userHasTouched = true
                    }

                    if (touchFxEnabled) {
                        when (event.actionMasked) {
                            android.view.MotionEvent.ACTION_DOWN,
                            android.view.MotionEvent.ACTION_POINTER_DOWN,
                            android.view.MotionEvent.ACTION_MOVE -> {
                                val points = mutableListOf<TouchFxPoint>()
                                for (i in 0 until event.pointerCount) {
                                    points.add(
                                        TouchFxPoint(
                                            id = event.getPointerId(i),
                                            x = event.getX(i),
                                            y = event.getY(i)
                                        )
                                    )
                                }
                                touchFxPoints = points
                            }
                            android.view.MotionEvent.ACTION_POINTER_UP -> {
                                val pointerId = event.getPointerId(event.actionIndex)
                                touchFxPoints = touchFxPoints.filterNot { it.id == pointerId }
                            }
                            android.view.MotionEvent.ACTION_UP,
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                touchFxPoints = emptyList()
                            }
                        }
                    } else if (touchFxPoints.isNotEmpty()) {
                        // Clear any lingering effects when disabled
                        touchFxPoints = emptyList()
                    }

                    gestureDetector.handleTouchEvent(event)
                }
        )

        if (touchFxEnabled && touchFxStyle != 0 && touchFxPoints.isNotEmpty()) {
            TouchFxLayer(
                points = touchFxPoints,
                style = touchFxStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top bar - ABOVE the trackpad area (conditionally shown)
        if (!hideUIOverlay) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
            ) {
                // First row: Close button and optional connection status
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
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Connection indicator - next to close button (respect user setting)
                    if (showConnectionStatus) {
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
                                text = "Connected",
                                fontSize = 14.sp,
                                color = MaterialTheme.extendedColors.onSuccessContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

            // Second row: Keyboard controls - always show toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                // Keyboard Toggle Button - always visible
                Surface(
                    onClick = { showKeyboard = !showKeyboard },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (showKeyboard)
                        Color(0xFF00D9FF).copy(alpha = 0.9f)
                    else
                        Color(0xFF4A4A4C).copy(alpha = 0.9f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(end = 8.dp, bottom = 8.dp)
                        .size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (showKeyboard) "⌨️" else "⌨",
                            fontSize = 20.sp
                        )
                    }
                }

                // Only show keyboard and layout switcher if keyboard is enabled
                if (showKeyboard) {
                    // Wrap both the layout switcher and keyboard in a Column
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Layout Switcher Button
                        Surface(
                            onClick = { showLayoutSwitcher = !showLayoutSwitcher },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color(0xFF2C2C2E).copy(alpha = 0.95f),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .padding(end = 8.dp, bottom = 8.dp)
                                .size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (selectedKeyboardLayout) {
                                        KeyboardLayout.COMPACT -> "⌨️"
                                        KeyboardLayout.FULL_QWERTY -> "🔤"
                                        KeyboardLayout.NUMERIC -> "🔢"
                                        KeyboardLayout.FUNCTION_KEYS -> "Fn"
                                        KeyboardLayout.ARROWS -> "⬆️"
                                        KeyboardLayout.CUSTOM -> "⚙️"
                                    },
                                    fontSize = 20.sp
                                )
                            }
                        }

                        // Keyboard Surface
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = Color(0xFF2C2C2E).copy(alpha = 0.95f),
                            shadowElevation = 8.dp
                        ) {
                    // Render keyboard based on selected layout
                    when (selectedKeyboardLayout) {
                        KeyboardLayout.COMPACT -> {
                            // Original compact layout
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

                                // Row 4: System Actions & Mouse Modes
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
                            KeyboardKey("Desk", null) {
                                viewModel.sendShowDesktop()
                                gestureInfo = "Show Desktop"
                                showInfo = true
                            }
                        }
                        if (deskMouseSensor?.isAvailable() == true) {
                            KeyboardKey("Desk", if (deskMouseEnabled) "ON" else "OFF") {
                                deskMouseEnabled = !deskMouseEnabled
                                onDeskMouseToggle(deskMouseEnabled)
                                onDeskMouseEnabledChanged(deskMouseEnabled)
                                if (deskMouseEnabled) {
                                    showDeskMouseScreen = true
                                }
                                gestureInfo = if (deskMouseEnabled) "Desk Mouse ON" else "Desk Mouse OFF"
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

                        KeyboardLayout.FULL_QWERTY -> {
                            FullQwertyKeyboard(
                                viewModel = viewModel,
                                theme = keyboardTheme,
                                scale = keyboardScale,
                                onGestureInfo = { info ->
                                    gestureInfo = info
                                    showInfo = true
                                }
                            )
                        }

                        KeyboardLayout.NUMERIC -> {
                            NumericKeypad(
                                viewModel = viewModel,
                                theme = keyboardTheme,
                                scale = keyboardScale,
                                onGestureInfo = { info ->
                                    gestureInfo = info
                                    showInfo = true
                                }
                            )
                        }

                        KeyboardLayout.FUNCTION_KEYS -> {
                            FunctionKeysLayout(
                                viewModel = viewModel,
                                theme = keyboardTheme,
                                scale = keyboardScale,
                                onGestureInfo = { info ->
                                    gestureInfo = info
                                    showInfo = true
                                }
                            )
                        }

                        KeyboardLayout.ARROWS -> {
                            ArrowKeysLayout(
                                viewModel = viewModel,
                                theme = keyboardTheme,
                                scale = keyboardScale,
                                onGestureInfo = { info ->
                                    gestureInfo = info
                                    showInfo = true
                                }
                            )
                        }

                        else -> {
                            // Fallback to compact layout
                            Text(
                                "Custom layouts coming soon",
                                modifier = Modifier.padding(16.dp),
                                color = Color.White
                            )
                        }
                    }
                        } // End of Surface
                    } // End of Column
                } // End of if (showKeyboard) for keyboard content
            } // End of Row

            // Layout Switcher Modal has been moved outside for proper z-ordering
        }

        // Center gesture hint or Mouse Mode indicator (conditionally shown)
        if (!hideUIOverlay && !minimalistMode && (gestureInfo.isEmpty() || airMouseEnabled || deskMouseEnabled)) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        color = if (airMouseEnabled || deskMouseEnabled)
                            MaterialTheme.extendedColors.successContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    deskMouseEnabled -> {
                        Text(
                            text = "🖱️",
                            fontSize = 60.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Desk Mouse Mode",
                            fontSize = 20.sp,
                            color = MaterialTheme.extendedColors.success,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Move phone on desk to move cursor",
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
                    }
                    airMouseEnabled -> {
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
                    }
                    else -> {
                        // Only show the "Touch anywhere to begin" overlay if user hasn't touched yet
                        if (!userHasTouched) {
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
            }
        }

        // Gesture feedback removed - silent mode

        // Left side controls - Brightness and Volume (conditionally shown)
        if (!hideUIOverlay && !minimalistMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Brightness control for Mac
                CollapsibleControl(
                    icon = "☀️",
                    label = "Bright",
                    value = brightness,
                    onValueChange = { newValue ->
                        brightness = newValue
                    },
                    isExpanded = brightnessExpanded,
                    onToggle = {
                        brightnessExpanded = !brightnessExpanded
                        if (brightnessExpanded && volumeExpanded) {
                            volumeExpanded = false  // Close volume if opening brightness
                        }
                    },
                    onIncrement = {
                        // Send Apple Vendor HID command (Brightness Up) to Mac
                        viewModel.sendBrightnessUp()
                        brightness = (brightness + 0.0625f).coerceIn(0f, 1f)  // Visual feedback
                    },
                    onDecrement = {
                        // Send Apple Vendor HID command (Brightness Down) to Mac
                        viewModel.sendBrightnessDown()
                        brightness = (brightness - 0.0625f).coerceIn(0f, 1f)  // Visual feedback
                    }
                )

                // Volume control for Mac
                CollapsibleControl(
                    icon = "🔊",
                    label = "Volume",
                    value = volume,
                    onValueChange = { newValue ->
                        volume = newValue
                    },
                    isExpanded = volumeExpanded,
                    onToggle = {
                        volumeExpanded = !volumeExpanded
                        if (volumeExpanded && brightnessExpanded) {
                            brightnessExpanded = false  // Close brightness if opening volume
                        }
                    },
                    onIncrement = {
                        // Send Consumer Control HID command (Volume Up) to Mac
                        viewModel.sendVolumeUp()
                        volume = (volume + 0.0625f).coerceIn(0f, 1f)  // Visual feedback
                    },
                    onDecrement = {
                        // Send Consumer Control HID command (Volume Down) to Mac
                        viewModel.sendVolumeDown()
                        volume = (volume - 0.0625f).coerceIn(0f, 1f)  // Visual feedback
                    }
                )
            }
        }

        // Bottom gesture guide (subtle) - conditionally shown based on settings
        // Also hide after user's first touch
        if (!hideUIOverlay && showGestureGuide && !userHasTouched) {
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
                GestureGuideItem("3⬆", "Mission")
                GestureGuideItem("3⬇", "Desktop")
                GestureGuideItem("3⬅➡", "Spaces")
            }
        }
    }

    // Layout Selector Item for the switcher modal (moved inside function for scope access)
    @Composable
    fun LayoutSelectorItemLocal(
        icon: String,
        label: String,
        description: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            color = if (isSelected)
                Color(0xFF00D9FF).copy(alpha = 0.15f)
            else
                Color.Transparent,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                if (isSelected) {
                    Text(
                        text = "✓",
                        fontSize = 20.sp,
                        color = Color(0xFF00D9FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Layout Switcher Modal - moved outside to ensure it's always on top
    if (showLayoutSwitcher) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { showLayoutSwitcher = false },
            contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = Color(0xFF2C2C2E),
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .padding(32.dp)
                        .clickable(enabled = false) { }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .widthIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    Text(
                        "Select Keyboard Layout",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LayoutSelectorItemLocal(
                        icon = "⌨️",
                        label = "Compact Shortcuts",
                        description = "Quick Mac shortcuts",
                        isSelected = selectedKeyboardLayout == KeyboardLayout.COMPACT,
                        onClick = {
                            selectedKeyboardLayout = KeyboardLayout.COMPACT
                            settingsPrefs.edit().putString("keyboard_layout", KeyboardLayout.COMPACT.name).apply()
                            showLayoutSwitcher = false
                        }
                    )

                    LayoutSelectorItemLocal(
                        icon = "🔤",
                        label = "Full QWERTY",
                        description = "Complete keyboard for typing",
                        isSelected = selectedKeyboardLayout == KeyboardLayout.FULL_QWERTY,
                        onClick = {
                            selectedKeyboardLayout = KeyboardLayout.FULL_QWERTY
                            settingsPrefs.edit().putString("keyboard_layout", KeyboardLayout.FULL_QWERTY.name).apply()
                            showLayoutSwitcher = false
                        }
                    )

                    LayoutSelectorItemLocal(
                        icon = "🔢",
                        label = "Numeric Keypad",
                        description = "Calculator-style numbers",
                        isSelected = selectedKeyboardLayout == KeyboardLayout.NUMERIC,
                        onClick = {
                            selectedKeyboardLayout = KeyboardLayout.NUMERIC
                            settingsPrefs.edit().putString("keyboard_layout", KeyboardLayout.NUMERIC.name).apply()
                            showLayoutSwitcher = false
                        }
                    )

                    LayoutSelectorItemLocal(
                        icon = "Fn",
                        label = "Function Keys",
                        description = "F1-F12 & media controls",
                        isSelected = selectedKeyboardLayout == KeyboardLayout.FUNCTION_KEYS,
                        onClick = {
                            selectedKeyboardLayout = KeyboardLayout.FUNCTION_KEYS
                            settingsPrefs.edit().putString("keyboard_layout", KeyboardLayout.FUNCTION_KEYS.name).apply()
                            showLayoutSwitcher = false
                        }
                    )

                    LayoutSelectorItemLocal(
                        icon = "⬆️",
                        label = "Arrow Keys",
                        description = "Navigation keys",
                        isSelected = selectedKeyboardLayout == KeyboardLayout.ARROWS,
                        onClick = {
                            selectedKeyboardLayout = KeyboardLayout.ARROWS
                            settingsPrefs.edit().putString("keyboard_layout", KeyboardLayout.ARROWS.name).apply()
                            showLayoutSwitcher = false
                        }
                    )
                }
            }
        }
    }
}
}
