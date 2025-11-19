package com.dps.droidpadmacos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.viewmodel.TrackpadViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Media controls for Mac brightness and volume
 * Uses HID commands to control the Mac, not Android device
 *
 * Single Responsibility: Only handles media control UI and interaction
 */
@Composable
fun MediaControls(
    viewModel: TrackpadViewModel,
    modifier: Modifier = Modifier
) {
    var brightnessExpanded by remember { mutableStateOf(false) }
    var volumeExpanded by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var volume by remember { mutableFloatStateOf(0.5f) }

    val scope = rememberCoroutineScope()

    // Auto-collapse controls after 3 seconds
    LaunchedEffect(brightnessExpanded) {
        if (brightnessExpanded) {
            delay(3000)
            brightnessExpanded = false
        }
    }

    LaunchedEffect(volumeExpanded) {
        if (volumeExpanded) {
            delay(3000)
            volumeExpanded = false
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brightness control
        CollapsibleControl(
            icon = "☀️",
            label = "Bright",
            value = brightness,
            isExpanded = brightnessExpanded,
            onToggle = {
                brightnessExpanded = !brightnessExpanded
                if (brightnessExpanded && volumeExpanded) {
                    volumeExpanded = false
                }
            },
            onIncrement = {
                viewModel.sendBrightnessUp()
                brightness = (brightness + 0.0625f).coerceIn(0f, 1f)
            },
            onDecrement = {
                viewModel.sendBrightnessDown()
                brightness = (brightness - 0.0625f).coerceIn(0f, 1f)
            }
        )

        // Volume control
        CollapsibleControl(
            icon = "🔊",
            label = "Volume",
            value = volume,
            isExpanded = volumeExpanded,
            onToggle = {
                volumeExpanded = !volumeExpanded
                if (volumeExpanded && brightnessExpanded) {
                    brightnessExpanded = false
                }
            },
            onIncrement = {
                viewModel.sendVolumeUp()
                volume = (volume + 0.0625f).coerceIn(0f, 1f)
            },
            onDecrement = {
                viewModel.sendVolumeDown()
                volume = (volume - 0.0625f).coerceIn(0f, 1f)
            }
        )
    }
}

/**
 * Collapsible control component with expand/collapse animation
 * Used for both brightness and volume controls
 */
@Composable
private fun CollapsibleControl(
    icon: String,
    label: String,
    value: Float,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
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
            ExpandedControlContent(
                icon = icon,
                label = label,
                value = value,
                onIncrement = onIncrement,
                onDecrement = onDecrement
            )
        } else {
            CollapsedControlContent(icon = icon)
        }
    }
}

@Composable
private fun ExpandedControlContent(
    icon: String,
    label: String,
    value: Float,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
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

        // + Button
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

        // - Button
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
}

@Composable
private fun CollapsedControlContent(icon: String) {
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