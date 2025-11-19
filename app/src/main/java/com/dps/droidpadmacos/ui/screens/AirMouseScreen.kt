package com.dps.droidpadmacos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Air Mouse mode screen
 *
 * Single Responsibility: Only handles Air Mouse UI
 * Open/Closed: Easy to extend with new features without modifying existing code
 */
@Composable
fun AirMouseScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2E7D32).copy(alpha = 0.8f),
                        Color(0xFF1B5E20).copy(alpha = 0.95f)
                    ),
                    radius = 800f
                )
            )
    ) {
        AirMouseContent(onClose = onClose)
    }
}

@Composable
private fun AirMouseContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        AirMouseHeader(onClose = onClose)

        // Center content
        AirMouseInstructions()

        // Bottom hint
        AirMouseBottomHint()
    }
}

@Composable
private fun AirMouseHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Air Mouse Mode",
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
private fun AirMouseInstructions() {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Icon
        Text(
            text = "📱",
            fontSize = 64.sp
        )

        Text(
            text = "Tilt your phone to move the cursor",
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
                InstructionRow("⬆️", "Tilt up = Move cursor up")
                InstructionRow("⬇️", "Tilt down = Move cursor down")
                InstructionRow("⬅️", "Tilt left = Move cursor left")
                InstructionRow("➡️", "Tilt right = Move cursor right")
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
private fun AirMouseBottomHint() {
    Text(
        text = "Keep phone flat for neutral position",
        fontSize = 14.sp,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 32.dp)
    )
}