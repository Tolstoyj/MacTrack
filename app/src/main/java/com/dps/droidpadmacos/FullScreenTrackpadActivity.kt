package com.dps.droidpadmacos

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.bluetooth.HidConstants
import com.dps.droidpadmacos.sensor.AirMouseSensor
import com.dps.droidpadmacos.touchpad.EnhancedGestureDetector
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme
import com.dps.droidpadmacos.viewmodel.TrackpadViewModel

class FullScreenTrackpadActivity : ComponentActivity() {

    private val viewModel: TrackpadViewModel by viewModels()
    private var airMouseSensor: AirMouseSensor? = null

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
                    onBackPress = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        airMouseSensor?.stop()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FullScreenTrackpad(
    viewModel: TrackpadViewModel,
    airMouseSensor: AirMouseSensor?,
    onBackPress: () -> Unit
) {
    var gestureInfo by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(true) }
    var airMouseEnabled by remember { mutableStateOf(false) }

    // Handle air mouse toggle
    LaunchedEffect(airMouseEnabled) {
        if (airMouseEnabled) {
            airMouseSensor?.start()
        } else {
            airMouseSensor?.stop()
        }
    }

    val gestureDetector = remember {
        EnhancedGestureDetector(
            onMove = { deltaX, deltaY ->
                viewModel.sendMouseMovement(deltaX, deltaY)
                gestureInfo = "Move"
            },
            onLeftClick = {
                viewModel.sendLeftClick()
                gestureInfo = "Click"
                showInfo = true
            },
            onRightClick = {
                viewModel.sendRightClick()
                gestureInfo = "Right Click"
                showInfo = true
            },
            onMiddleClick = {
                // Middle click - we'll need to add this to ViewModel
                gestureInfo = "Middle Click"
                showInfo = true
            },
            onScroll = { deltaY ->
                viewModel.sendScroll(deltaY)
                gestureInfo = "Scroll"
            },
            onThreeFingerSwipeUp = {
                viewModel.sendMissionControl()
                gestureInfo = "3-Finger Up: Mission Control"
                showInfo = true
            },
            onThreeFingerSwipeDown = {
                viewModel.sendShowDesktop()
                gestureInfo = "3-Finger Down: Show Desktop"
                showInfo = true
            },
            onFourFingerSwipeLeft = {
                // Mac swipe between desktops
                gestureInfo = "4-Finger Left: Previous Desktop"
                showInfo = true
            },
            onFourFingerSwipeRight = {
                // Mac swipe between desktops
                gestureInfo = "4-Finger Right: Next Desktop"
                showInfo = true
            },
            onPinchZoom = { scale ->
                gestureInfo = if (scale > 1.0f) "Zoom In" else "Zoom Out"
                showInfo = true
            }
        )
    }

    // Auto-hide info after 2 seconds
    LaunchedEffect(gestureInfo) {
        if (gestureInfo.isNotEmpty() && showInfo) {
            kotlinx.coroutines.delay(2000)
            showInfo = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22)
                    )
                )
            )
            .pointerInteropFilter { event ->
                gestureDetector.handleTouchEvent(event)
            }
    ) {
        // Top bar with back button and status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onBackPress,
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            ) {
                Text(
                    text = "✕",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Connection indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = Color(0xFF4CAF50),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Text(
                    text = "Connected",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Air Mouse Toggle Button
                if (airMouseSensor?.isAvailable() == true) {
                    IconButton(
                        onClick = {
                            airMouseEnabled = !airMouseEnabled
                            gestureInfo = if (airMouseEnabled) "Air Mouse ON" else "Air Mouse OFF"
                            showInfo = true
                        },
                        modifier = Modifier
                            .background(
                                color = if (airMouseEnabled)
                                    Color(0xFF4CAF50).copy(alpha = 0.5f)
                                else
                                    Color(0xFF607D8B).copy(alpha = 0.3f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📱",
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (airMouseEnabled) "ON" else "Air",
                                fontSize = 7.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Mission Control Button
                IconButton(
                    onClick = {
                        viewModel.sendMissionControl()
                        gestureInfo = "Mission Control"
                        showInfo = true
                    },
                    modifier = Modifier
                        .background(
                            color = Color(0xFF2196F3).copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⌘",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "MC",
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // App Switcher Button (Cmd+Tab)
                IconButton(
                    onClick = {
                        viewModel.sendAppSwitcher()
                        gestureInfo = "App Switcher"
                        showInfo = true
                    },
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFF9800).copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⌘⇥",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Apps",
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Show Desktop Button
                IconButton(
                    onClick = {
                        viewModel.sendShowDesktop()
                        gestureInfo = "Show Desktop"
                        showInfo = true
                    },
                    modifier = Modifier
                        .background(
                            color = Color(0xFF9C27B0).copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🖥",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Desk",
                            fontSize = 7.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Center gesture hint or Air Mouse indicator
        if (gestureInfo.isEmpty() || airMouseEnabled) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        color = if (airMouseEnabled)
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else
                            Color.White.copy(alpha = 0.05f),
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
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tilt phone to move cursor",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                } else if (gestureInfo.isEmpty()) {
                    Text(
                        text = "🖐️",
                        fontSize = 60.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Touch anywhere to begin",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Gesture feedback
        if (showInfo && gestureInfo.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .background(
                        color = Color(0xFF2196F3).copy(alpha = 0.9f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = gestureInfo,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom gesture guide (subtle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(
                    color = Color.White.copy(alpha = 0.05f),
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
            GestureGuideItem("4⬅➡", "Switch")
        }
    }
}

@Composable
private fun GestureGuideItem(icon: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(50.dp)
    ) {
        Text(
            text = icon,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}
