package com.dps.droidpadmacos.ui.components

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * macOS-style status bar component
 * Displays battery level, time, and connection status
 *
 * Single Responsibility: Only handles status bar display logic
 */
@Composable
fun MacOSStatusBar(
    context: Context,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }

    // Update time every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Update every minute
            currentTime = getCurrentTime()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Battery indicator
        BatteryIndicator(context)

        // Time display
        Text(
            text = currentTime,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        // Connection status
        ConnectionStatusIndicator()
    }
}

@Composable
private fun BatteryIndicator(context: Context) {
    val batteryStatus = remember {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            Triple(level, scale, isCharging)
        } ?: Triple(0, 100, false)
    }

    val batteryPercent = if (batteryStatus.second > 0) {
        (batteryStatus.first * 100) / batteryStatus.second
    } else 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (batteryStatus.third) "⚡" else "🔋",
            fontSize = 14.sp
        )
        Text(
            text = "$batteryPercent%",
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

@Composable
private fun ConnectionStatusIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Connection dot (could be made reactive to actual connection state)
        Text(
            text = "•",
            fontSize = 20.sp,
            color = Color(0xFF00D9FF) // Cyan for connected
        )
    }
}

private fun getCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}