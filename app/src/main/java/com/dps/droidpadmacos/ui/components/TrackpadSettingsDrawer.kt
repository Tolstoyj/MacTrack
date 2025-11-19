package com.dps.droidpadmacos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Settings drawer for trackpad customization
 *
 * Single Responsibility: Manages settings UI only
 * Open/Closed: Easy to add new settings without modifying existing code
 */
@Composable
fun TrackpadSettingsDrawer(
    isExpanded: Boolean,
    keyboardScale: Float,
    onKeyboardScaleChange: (Float) -> Unit,
    visibleKeys: Set<String>,
    onVisibleKeysChange: (Set<String>) -> Unit,
    allKeyPresets: List<KeyPreset>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val width by animateDpAsState(
        targetValue = if (isExpanded) 320.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "drawerWidth"
    )

    if (width > 0.dp) {
        Surface(
            modifier = modifier
                .width(width)
                .fillMaxHeight(),
            color = Color(0xFF1C1C1E).copy(alpha = 0.98f),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                SettingsHeader(onClose = onClose)

                Divider(color = Color.White.copy(alpha = 0.2f))

                // Keyboard Size Control
                KeyboardSizeControl(
                    keyboardScale = keyboardScale,
                    onKeyboardScaleChange = onKeyboardScaleChange
                )

                Divider(color = Color.White.copy(alpha = 0.2f))

                // Key Visibility Controls
                KeyVisibilityControls(
                    visibleKeys = visibleKeys,
                    onVisibleKeysChange = onVisibleKeysChange,
                    allKeyPresets = allKeyPresets
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⚙️ Settings",
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onClose) {
            Text(
                text = "✕",
                fontSize = 24.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun KeyboardSizeControl(
    keyboardScale: Float,
    onKeyboardScaleChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Keyboard Size",
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Small",
                fontSize = 12.sp,
                color = Color(0xFFB0B0B0)
            )
            Text(
                text = "${(keyboardScale * 100).toInt()}%",
                fontSize = 14.sp,
                color = Color(0xFF00D9FF),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Large",
                fontSize = 12.sp,
                color = Color(0xFFB0B0B0)
            )
        }
        Slider(
            value = keyboardScale,
            onValueChange = onKeyboardScaleChange,
            valueRange = 0.6f..1.4f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00D9FF),
                activeTrackColor = Color(0xFF00D9FF),
                inactiveTrackColor = Color(0xFF4A4A4C)
            )
        )
    }
}

@Composable
private fun KeyVisibilityControls(
    visibleKeys: Set<String>,
    onVisibleKeysChange: (Set<String>) -> Unit,
    allKeyPresets: List<KeyPreset>
) {
    Column {
        Text(
            text = "Visible Keys",
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        // Scrollable key list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allKeyPresets.groupBy { it.category }.forEach { (category, keys) ->
                Text(
                    text = category,
                    fontSize = 14.sp,
                    color = Color(0xFF00D9FF),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                keys.forEach { preset ->
                    KeyVisibilityItem(
                        preset = preset,
                        isVisible = visibleKeys.contains(preset.id),
                        onToggle = {
                            val newSet = if (visibleKeys.contains(preset.id)) {
                                visibleKeys - preset.id
                            } else {
                                visibleKeys + preset.id
                            }
                            onVisibleKeysChange(newSet)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyVisibilityItem(
    preset: KeyPreset,
    isVisible: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .background(
                color = if (isVisible) Color(0xFF2C2C2E) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${preset.text}${if (preset.label != null) " (${preset.label})" else ""}",
            fontSize = 13.sp,
            color = Color.White
        )
        Checkbox(
            checked = isVisible,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF00D9FF),
                uncheckedColor = Color(0xFF4A4A4C)
            )
        )
    }
}

/**
 * Floating action buttons for UI control
 */
@Composable
fun FloatingUIControls(
    onToggleUI: () -> Unit,
    onOpenSettings: () -> Unit,
    uiVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Settings button
        Surface(
            onClick = onOpenSettings,
            shape = CircleShape,
            color = Color(0xFF2C2C2E).copy(alpha = 0.95f),
            modifier = Modifier.size(48.dp),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "⚙️",
                    fontSize = 24.sp
                )
            }
        }

        // UI Toggle button
        Surface(
            onClick = onToggleUI,
            shape = CircleShape,
            color = if (uiVisible)
                Color(0xFF00D9FF).copy(alpha = 0.9f)
            else
                Color(0xFF4A4A4C).copy(alpha = 0.9f),
            modifier = Modifier.size(48.dp),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiVisible) "👁" else "👁",
                    fontSize = 20.sp
                )
            }
        }
    }
}

/**
 * Data class for keyboard key presets
 */
data class KeyPreset(
    val id: String,
    val text: String,
    val label: String?,
    val action: () -> Unit,
    val category: String
)