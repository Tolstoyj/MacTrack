package com.dps.droidpadmacos

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.dps.droidpadmacos.usb.UsbConnectionDetector
import com.dps.droidpadmacos.usb.UsbConnectionMonitor
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme
import kotlinx.coroutines.delay

class UsbConnectionActivity : ComponentActivity() {

    companion object {
        private const val TAG = "UsbConnectionActivity"
    }

    private var connectionInfo: UsbConnectionDetector.ConnectionInfo? = null
    private lateinit var usbMonitor: UsbConnectionMonitor

    data class DeviceInfo(
        val connectionType: String,
        val description: String,
        val isLikelyMac: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize USB monitor
        usbMonitor = UsbConnectionMonitor(this)

        // Detect USB connection details
        connectionInfo = UsbConnectionDetector.detectConnection(this)
        val isLikelyMac = UsbConnectionDetector.isLikelyConnectedToMac(this)

        Log.d(TAG, "Connection info: $connectionInfo, Likely Mac: $isLikelyMac")

        val deviceInfo = connectionInfo?.let {
            DeviceInfo(
                connectionType = it.connectionType.name,
                description = UsbConnectionDetector.getConnectionDescription(it),
                isLikelyMac = isLikelyMac
            )
        }

        // Start monitoring for disconnections
        usbMonitor.startMonitoring()

        // Listen for USB disconnection
        lifecycleScope.launch {
            usbMonitor.connectionState.collect { newConnectionInfo ->
                if (newConnectionInfo != null) {
                    Log.d(TAG, "USB state changed: $newConnectionInfo")

                    // If USB is no longer suitable for trackpad, dismiss this screen
                    if (!UsbConnectionDetector.isSuitableForTrackpad(newConnectionInfo)) {
                        Log.d(TAG, "USB disconnected or not suitable, dismissing screen")
                        runOnUiThread {
                            // Navigate to Bluetooth mode
                            val intent = Intent(this@UsbConnectionActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        }

        setContent {
            DroidPadMacOSTheme {
                UsbConnectionScreen(
                    deviceInfo = deviceInfo,
                    onUseAsTrackpad = {
                        Log.d(TAG, "User selected USB trackpad mode")
                        // Start trackpad mode with USB
                        val intent = Intent(this@UsbConnectionActivity, FullScreenTrackpadActivity::class.java)
                        intent.putExtra("CONNECTION_MODE", "USB")
                        startActivity(intent)
                        finish()
                    },
                    onUseBluetooth = {
                        Log.d(TAG, "User selected Bluetooth mode")
                        // Go to main activity for Bluetooth connection with flag to disable USB monitoring
                        val intent = Intent(this@UsbConnectionActivity, MainActivity::class.java)
                        intent.putExtra("DISABLE_USB_MONITORING", true)
                        startActivity(intent)
                        finish()
                    },
                    onDismiss = {
                        Log.d(TAG, "User dismissed USB detection")
                        // Go to main activity with flag to disable USB monitoring
                        val intent = Intent(this@UsbConnectionActivity, MainActivity::class.java)
                        intent.putExtra("DISABLE_USB_MONITORING", true)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop monitoring when activity is destroyed
        usbMonitor.stopMonitoring()
    }
}

@Composable
fun UsbConnectionScreen(
    deviceInfo: UsbConnectionActivity.DeviceInfo?,
    onUseAsTrackpad: () -> Unit,
    onUseBluetooth: () -> Unit,
    onDismiss: () -> Unit
) {
    Log.d("UsbConnectionScreen", "Rendering with deviceInfo: $deviceInfo")
    var showContent by remember { mutableStateOf(false) }

    // Trigger animation
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    // Pulsating animation for detection indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val slideIn by animateFloatAsState(
        targetValue = if (showContent) 0f else 50f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "slideIn"
    )

    val fadeIn by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(700),
        label = "fadeIn"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = slideIn.dp)
                    .scale(fadeIn),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Detection Indicator with glow
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
                                )
                        )

                        // Main circle
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(
                                    if (deviceInfo?.connectionType == "USB_DATA")
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (deviceInfo?.connectionType) {
                                    "USB_DATA" -> "🖥️"
                                    "USB_CHARGING" -> "🔋"
                                    else -> "💻"
                                },
                                fontSize = 56.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title
                    Text(
                        text = when {
                            deviceInfo?.connectionType == "USB_DATA" -> "USB Connection Detected"
                            deviceInfo?.connectionType == "USB_CHARGING" -> "USB Charging Only"
                            deviceInfo != null -> "USB Connected"
                            else -> "No Connection"
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle
                    Text(
                        text = when {
                            deviceInfo?.connectionType == "USB_DATA" -> "USB data connection established"
                            deviceInfo?.connectionType == "USB_CHARGING" -> "Charging only, no data connection"
                            deviceInfo != null -> "Use your phone as a trackpad"
                            else -> "Connect via USB to detect"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Device Info
                    if (deviceInfo != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                InfoRow("Connection", deviceInfo.connectionType)
                                InfoRow("Status", deviceInfo.description)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // USB Trackpad Button
                            Button(
                                onClick = onUseAsTrackpad,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 8.dp
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔌",
                                        fontSize = 24.sp,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = "Use as USB Trackpad",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Start using trackpad now",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            // Bluetooth Option
                            OutlinedButton(
                                onClick = onUseBluetooth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📡",
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(end = 10.dp)
                                    )
                                    Text(
                                        text = "Use Bluetooth Instead",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        // No USB connection
                        Text(
                            text = "Connect your Android device to a computer via USB cable to use USB trackpad mode.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Button(
                            onClick = onUseBluetooth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Use Bluetooth",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dismiss button
                    TextButton(onClick = onDismiss) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
