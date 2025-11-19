package com.dps.droidpadmacos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Desk Mouse mode screen
 *
 * Single Responsibility: Only handles Desk Mouse UI
 * Open/Closed: Easy to extend with new features without modifying existing code
 */
@Composable
fun DeskMouseScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1565C0).copy(alpha = 0.8f),
                        Color(0xFF0D47A1).copy(alpha = 0.95f)
                    ),
                    radius = 800f
                )
            )
    ) {
        DeskMouseContent(onClose = onClose)
    }
}

@Composable
private fun DeskMouseContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        DeskMouseHeader(onClose = onClose)

        // Center content
        DeskMouseInstructions()

        // Bottom hint
        DeskMouseBottomHint()
    }
}

@Composable
private fun DeskMouseHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Desk Mouse Mode",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Surface(
            onClick = onClose,
            shape = CircleShape,
            color = Color(0xFFFF5252).copy(alpha = 0.9f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "✕",
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeskMouseInstructions() {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Icon
        Text(
            text = "🖱️",
            fontSize = 64.sp
        )

        Text(
            text = "Move your phone on the desk like a mouse",
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        // Visual instructions
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InstructionRow("⬆️", "Move forward = Cursor up")
                InstructionRow("⬇️", "Move backward = Cursor down")
                InstructionRow("⬅️", "Move left = Cursor left")
                InstructionRow("➡️", "Move right = Cursor right")
                InstructionRow("✋", "Keep phone flat on desk")
            }
        }

        Text(
            text = "Press Volume Down to exit",
            fontSize = 16.sp,
            color = Color(0xFFFFEB3B),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InstructionRow(icon: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun DeskMouseBottomHint() {
    Column(
        modifier = Modifier.padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Sensitivity: Adjustable in Settings",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = "Lift phone to reset position",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}