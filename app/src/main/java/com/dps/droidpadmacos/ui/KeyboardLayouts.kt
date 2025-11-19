package com.dps.droidpadmacos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.bluetooth.HidConstants
import com.dps.droidpadmacos.settings.KeyboardTheme
import com.dps.droidpadmacos.viewmodel.TrackpadViewModel

/**
 * Full QWERTY Keyboard Layout
 */
@Composable
fun FullQwertyKeyboard(
    viewModel: TrackpadViewModel,
    theme: KeyboardTheme,
    scale: Float,
    onGestureInfo: (String) -> Unit
) {
    val keySize = (48.dp * scale)
    val spacing = (4.dp * scale)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Row 1: Numbers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { key ->
                KeyButton(key, null, keySize, theme) {
                    val keyCode = when (key) {
                        "1" -> HidConstants.KEY_1
                        "2" -> HidConstants.KEY_2
                        "3" -> HidConstants.KEY_3
                        "4" -> HidConstants.KEY_4
                        "5" -> HidConstants.KEY_5
                        "6" -> HidConstants.KEY_6
                        "7" -> HidConstants.KEY_7
                        "8" -> HidConstants.KEY_8
                        "9" -> HidConstants.KEY_9
                        "0" -> HidConstants.KEY_0
                        else -> HidConstants.KEY_NONE
                    }
                    if (keyCode != HidConstants.KEY_NONE) {
                        viewModel.sendKeyPress(HidConstants.MOD_NONE, keyCode)
                    }
                    onGestureInfo("Key: $key")
                }
                Spacer(Modifier.width(spacing))
            }
        }

        // Row 2: QWERTY
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P").forEach { key ->
                KeyButton(key, null, keySize, theme) {
                    val keyCode = when (key) {
                        "Q" -> HidConstants.KEY_Q
                        "W" -> HidConstants.KEY_W
                        "E" -> HidConstants.KEY_E
                        "R" -> HidConstants.KEY_R
                        "T" -> HidConstants.KEY_T
                        "Y" -> HidConstants.KEY_Y
                        "U" -> HidConstants.KEY_U
                        "I" -> HidConstants.KEY_I
                        "O" -> HidConstants.KEY_O
                        "P" -> HidConstants.KEY_P
                        else -> HidConstants.KEY_NONE
                    }
                    if (keyCode != HidConstants.KEY_NONE) {
                        viewModel.sendKeyPress(HidConstants.MOD_NONE, keyCode)
                    }
                    onGestureInfo("Key: $key")
                }
                Spacer(Modifier.width(spacing))
            }
        }

        // Row 3: ASDFGH
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.width(keySize * 0.5f))
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L").forEach { key ->
                KeyButton(key, null, keySize, theme) {
                    val keyCode = when (key) {
                        "A" -> HidConstants.KEY_A
                        "S" -> HidConstants.KEY_S
                        "D" -> HidConstants.KEY_D
                        "F" -> HidConstants.KEY_F
                        "G" -> HidConstants.KEY_G
                        "H" -> HidConstants.KEY_H
                        "J" -> HidConstants.KEY_J
                        "K" -> HidConstants.KEY_K
                        "L" -> HidConstants.KEY_L
                        else -> HidConstants.KEY_NONE
                    }
                    if (keyCode != HidConstants.KEY_NONE) {
                        viewModel.sendKeyPress(HidConstants.MOD_NONE, keyCode)
                    }
                    onGestureInfo("Key: $key")
                }
                Spacer(Modifier.width(spacing))
            }
        }

        // Row 4: ZXCVBN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Shift key
            KeyButton("⇧", "Shift", keySize * 1.5f, theme) {
                // Handle shift
                onGestureInfo("Shift")
            }
            Spacer(Modifier.width(spacing))

            listOf("Z", "X", "C", "V", "B", "N", "M").forEach { key ->
                KeyButton(key, null, keySize, theme) {
                    val keyCode = when (key) {
                        "Z" -> HidConstants.KEY_Z
                        "X" -> HidConstants.KEY_X
                        "C" -> HidConstants.KEY_C
                        "V" -> HidConstants.KEY_V
                        "B" -> HidConstants.KEY_B
                        "N" -> HidConstants.KEY_N
                        "M" -> HidConstants.KEY_M
                        else -> HidConstants.KEY_NONE
                    }
                    if (keyCode != HidConstants.KEY_NONE) {
                        viewModel.sendKeyPress(HidConstants.MOD_NONE, keyCode)
                    }
                    onGestureInfo("Key: $key")
                }
                Spacer(Modifier.width(spacing))
            }

            // Backspace key
            KeyButton("⌫", "Del", keySize * 1.5f, theme) {
                viewModel.sendKeyPress(HidConstants.KEY_BACKSPACE)
                onGestureInfo("Backspace")
            }
        }

        // Row 5: Space bar and modifiers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyButton("Ctrl", null, keySize * 1.2f, theme) {
                onGestureInfo("Ctrl")
            }
            Spacer(Modifier.width(spacing))

            KeyButton("⌘", "Cmd", keySize, theme) {
                onGestureInfo("Cmd")
            }
            Spacer(Modifier.width(spacing))

            // Space bar
            KeyButton("Space", null, keySize * 4f, theme) {
                viewModel.sendKeyPress(HidConstants.KEY_SPACE)
                onGestureInfo("Space")
            }
            Spacer(Modifier.width(spacing))

            KeyButton("Alt", null, keySize, theme) {
                onGestureInfo("Alt")
            }
            Spacer(Modifier.width(spacing))

            KeyButton("↵", "Enter", keySize * 1.2f, theme) {
                viewModel.sendKeyPress(HidConstants.KEY_ENTER)
                onGestureInfo("Enter")
            }
        }
    }
}

/**
 * Numeric Keypad Layout
 */
@Composable
fun NumericKeypad(
    viewModel: TrackpadViewModel,
    theme: KeyboardTheme,
    scale: Float,
    onGestureInfo: (String) -> Unit
) {
    val keySize = (64.dp * scale)
    val spacing = (6.dp * scale)

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: 7 8 9 /
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            listOf("7", "8", "9", "/").forEach { key ->
                KeyButton(key, null, keySize, theme) {
                    when (key) {
                        "7" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_7)
                        "8" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_8)
                        "9" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_9)
                        "/" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_SLASH)
                    }
                    onGestureInfo("Num: $key")
                }
            }
        }

        // Row 2: 4 5 6 *
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            listOf("4", "5", "6", "*").forEach { key ->
                KeyButton(key, null, keySize, theme) {
                    when (key) {
                        "4" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_4)
                        "5" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_5)
                        "6" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_6)
                        "*" -> viewModel.sendKeyPress(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_8) // Shift+8 = '*'
                    }
                    onGestureInfo("Num: $key")
                }
            }
        }

        // Row 3: 1 2 3 -
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            listOf("1", "2", "3", "-").forEach { key ->
                KeyButton(key, null, keySize, theme) {
                    when (key) {
                        "1" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_1)
                        "2" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_2)
                        "3" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_3)
                        "-" -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_MINUS)
                    }
                    onGestureInfo("Num: $key")
                }
            }
        }

        // Row 4: 0 . Enter +
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            KeyButton("0", null, keySize * 2 + spacing, theme) {
                viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_0)
                onGestureInfo("Num: 0")
            }
            KeyButton(".", null, keySize, theme) {
                viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_PERIOD)
                onGestureInfo("Num: .")
            }
            KeyButton("+", null, keySize, theme) {
                // Shift + '=' produces '+' on standard layouts
                viewModel.sendKeyPress(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_EQUAL)
                onGestureInfo("Num: +")
            }
        }

        // Enter key (full width)
        KeyButton("Enter", null, keySize * 4 + spacing * 3, theme) {
            viewModel.sendKeyPress(HidConstants.KEY_ENTER)
            onGestureInfo("Enter")
        }
    }
}

/**
 * Function Keys Layout (F1-F12 + Media Controls)
 */
@Composable
fun FunctionKeysLayout(
    viewModel: TrackpadViewModel,
    theme: KeyboardTheme,
    scale: Float,
    onGestureInfo: (String) -> Unit
) {
    val keySize = (56.dp * scale)
    val spacing = (4.dp * scale)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing * 2)
    ) {
        // Row 1: F1-F6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 1..6) {
                KeyButton("F$i", null, keySize, theme) {
                    when (i) {
                        1 -> viewModel.sendBrightnessDown()
                        2 -> viewModel.sendBrightnessUp()
                        3 -> viewModel.sendMissionControl()
                        4 -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F4)
                        5 -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F5)
                        6 -> viewModel.sendKeyPress(HidConstants.MOD_NONE, HidConstants.KEY_F6)
                    }
                    onGestureInfo("F$i")
                }
            }
        }

        // Row 2: F7-F12
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 7..12) {
                KeyButton("F$i", null, keySize, theme) {
                    when (i) {
                        7 -> viewModel.sendPreviousTrack()
                        8 -> viewModel.sendPlayPause()
                        9 -> viewModel.sendNextTrack()
                        10 -> viewModel.sendMute()
                        11 -> viewModel.sendVolumeDown()
                        12 -> viewModel.sendVolumeUp()
                    }
                    onGestureInfo("F$i")
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

        // Media Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KeyButton("⏮", "Prev", keySize, theme) {
                viewModel.sendPreviousTrack()
                onGestureInfo("Previous Track")
            }
            KeyButton("⏯", "Play", keySize, theme) {
                viewModel.sendPlayPause()
                onGestureInfo("Play/Pause")
            }
            KeyButton("⏭", "Next", keySize, theme) {
                viewModel.sendNextTrack()
                onGestureInfo("Next Track")
            }
            KeyButton("🔇", "Mute", keySize, theme) {
                viewModel.sendMute()
                onGestureInfo("Mute")
            }
        }

        // System Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KeyButton("☀️+", "Bright+", keySize, theme) {
                viewModel.sendBrightnessUp()
                onGestureInfo("Brightness Up")
            }
            KeyButton("☀️−", "Bright-", keySize, theme) {
                viewModel.sendBrightnessDown()
                onGestureInfo("Brightness Down")
            }
            KeyButton("🔊+", "Vol+", keySize, theme) {
                viewModel.sendVolumeUp()
                onGestureInfo("Volume Up")
            }
            KeyButton("🔊−", "Vol-", keySize, theme) {
                viewModel.sendVolumeDown()
                onGestureInfo("Volume Down")
            }
        }
    }
}

/**
 * Arrow Keys & Navigation Layout
 */
@Composable
fun ArrowKeysLayout(
    viewModel: TrackpadViewModel,
    theme: KeyboardTheme,
    scale: Float,
    onGestureInfo: (String) -> Unit
) {
    val keySize = (72.dp * scale)
    val spacing = (8.dp * scale)

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing * 2)
    ) {
        // Navigation keys row 1
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            KeyButton("Home", null, keySize * 1.2f, theme) {
                onGestureInfo("Home")
            }
            KeyButton("PgUp", "Page Up", keySize * 1.2f, theme) {
                onGestureInfo("Page Up")
            }
            KeyButton("End", null, keySize * 1.2f, theme) {
                onGestureInfo("End")
            }
        }

        // Arrow keys in inverted T layout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            // Up arrow
            KeyButton("↑", "Up", keySize, theme) {
                viewModel.sendKeyPress(HidConstants.KEY_UP_ARROW)
                onGestureInfo("Up Arrow")
            }

            // Left, Down, Right
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                KeyButton("←", "Left", keySize, theme) {
                    viewModel.sendKeyPress(HidConstants.KEY_LEFT_ARROW)
                    onGestureInfo("Left Arrow")
                }
                KeyButton("↓", "Down", keySize, theme) {
                    viewModel.sendKeyPress(HidConstants.KEY_DOWN_ARROW)
                    onGestureInfo("Down Arrow")
                }
                KeyButton("→", "Right", keySize, theme) {
                    viewModel.sendKeyPress(HidConstants.KEY_RIGHT_ARROW)
                    onGestureInfo("Right Arrow")
                }
            }
        }

        // Navigation keys row 2
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            KeyButton("Del", "Delete", keySize * 1.2f, theme) {
                viewModel.sendKeyPress(HidConstants.KEY_BACKSPACE)
                onGestureInfo("Delete")
            }
            KeyButton("PgDn", "Page Down", keySize * 1.2f, theme) {
                onGestureInfo("Page Down")
            }
            KeyButton("Tab", null, keySize * 1.2f, theme) {
                viewModel.sendKeyPress(HidConstants.KEY_TAB)
                onGestureInfo("Tab")
            }
        }
    }
}

/**
 * Reusable Key Button with Theme Support
 */
@Composable
private fun KeyButton(
    text: String,
    subtitle: String?,
    width: androidx.compose.ui.unit.Dp,
    theme: KeyboardTheme,
    onClick: () -> Unit
) {
    val colors = getThemeColors(theme)

    Surface(
        modifier = Modifier
            .width(width)
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = colors.background,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (theme == KeyboardTheme.COLORFUL) {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF667EEA),
                                    Color(0xFF764BA2)
                                )
                            )
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 10.sp,
                        color = colors.text.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Theme color definitions
 */
private data class ThemeColors(
    val background: Color,
    val text: Color
)

private fun getThemeColors(theme: KeyboardTheme): ThemeColors {
    return when (theme) {
        KeyboardTheme.DARK -> ThemeColors(
            background = Color(0xFF2C2C2E),
            text = Color.White
        )
        KeyboardTheme.LIGHT -> ThemeColors(
            background = Color(0xFFE5E5EA),
            text = Color.Black
        )
        KeyboardTheme.COLORFUL -> ThemeColors(
            background = Color.Transparent,
            text = Color.White
        )
        KeyboardTheme.MINIMAL -> ThemeColors(
            background = Color(0x33FFFFFF),
            text = Color.White
        )
        KeyboardTheme.MAC_STYLE -> ThemeColors(
            background = Color(0xFF48484A),
            text = Color.White
        )
    }
}
