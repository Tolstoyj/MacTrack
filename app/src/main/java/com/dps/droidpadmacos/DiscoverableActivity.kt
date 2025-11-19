package com.dps.droidpadmacos

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.bluetooth.BluetoothHidService
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme
import com.dps.droidpadmacos.ui.theme.extendedColors
import com.dps.droidpadmacos.viewmodel.TrackpadViewModel
import kotlinx.coroutines.delay

class DiscoverableActivity : ComponentActivity() {

    companion object {
        private const val TAG = "DiscoverableActivity"
    }

    private val viewModel: TrackpadViewModel by viewModels()
    private var bluetoothAdapter: BluetoothAdapter? = null

    // BroadcastReceiver to listen for Bluetooth device connections
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    Log.d(TAG, "Device connected: ${device?.name} - ${device?.address}")
                }
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    val scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE)
                    Log.d(TAG, "Scan mode changed: $scanMode")

                    // If device is no longer discoverable, user may have cancelled
                    if (scanMode == BluetoothAdapter.SCAN_MODE_NONE) {
                        Log.d(TAG, "Device no longer discoverable")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        // Register for Bluetooth connection events
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, filter)

        // Listen for HID connection state changes
        lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                Log.d(TAG, "Connection state: $state")

                when (state) {
                    is BluetoothHidService.ConnectionState.Connected -> {
                        Log.d(TAG, "Mac connected! Navigating to trackpad...")
                        // Navigate to full screen trackpad
                        delay(500) // Small delay for smooth transition
                        val intent = Intent(this@DiscoverableActivity, FullScreenTrackpadActivity::class.java)
                        intent.putExtra("CONNECTION_MODE", "BLUETOOTH")
                        startActivity(intent)
                        finish()
                    }
                    else -> {
                        // Still waiting
                    }
                }
            }
        }

        setContent {
            DroidPadMacOSTheme {
                DiscoverableScreen(
                    connectionState = viewModel.connectionState.collectAsState().value,
                    onCancel = {
                        Log.d(TAG, "User cancelled pairing")
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }
}

@Composable
fun DiscoverableScreen(
    connectionState: BluetoothHidService.ConnectionState,
    onCancel: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    // Trigger animation
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Pulsating animation for the radar effect
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Multiple expanding rings
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )

    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )

    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing, delayMillis = 2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    // Fade effect for rings
    val ring1Alpha = (1f - (ring1Scale - 0.5f) / 2f).coerceIn(0f, 0.6f)
    val ring2Alpha = (1f - (ring2Scale - 0.5f) / 2f).coerceIn(0f, 0.6f)
    val ring3Alpha = (1f - (ring3Scale - 0.5f) / 2f).coerceIn(0f, 0.6f)

    // Rotation animation for connected state
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val slideIn by animateFloatAsState(
        targetValue = if (showContent) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slideIn"
    )

    val fadeIn by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(600),
        label = "fadeIn"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = slideIn.dp)
                    .graphicsLayer { alpha = fadeIn },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Radar/Connection Animation
                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Expanding rings (radar effect)
                    if (connectionState !is BluetoothHidService.ConnectionState.Connected) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(ring1Scale)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.extendedColors.info.copy(alpha = ring1Alpha),
                                    shape = CircleShape
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(ring2Scale)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.extendedColors.info.copy(alpha = ring2Alpha),
                                    shape = CircleShape
                                )
                        )

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(ring3Scale)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.extendedColors.info.copy(alpha = ring3Alpha),
                                    shape = CircleShape
                                )
                        )
                    }

                    // Center icon
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(
                                color = when (connectionState) {
                                    is BluetoothHidService.ConnectionState.Connected -> MaterialTheme.extendedColors.successContainer
                                    else -> MaterialTheme.extendedColors.infoContainer
                                },
                                shape = CircleShape
                            )
                            .border(
                                width = 4.dp,
                                color = when (connectionState) {
                                    is BluetoothHidService.ConnectionState.Connected -> MaterialTheme.extendedColors.success
                                    else -> MaterialTheme.extendedColors.info
                                },
                                shape = CircleShape
                            )
                            .graphicsLayer {
                                if (connectionState is BluetoothHidService.ConnectionState.Connected) {
                                    rotationZ = rotation
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_macbook),
                            contentDescription = "Mac",
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Status Text
                Text(
                    text = when (connectionState) {
                        is BluetoothHidService.ConnectionState.Registered -> "Waiting for Mac..."
                        is BluetoothHidService.ConnectionState.Connecting -> "Pairing..."
                        is BluetoothHidService.ConnectionState.Connected -> "Connected!"
                        else -> "Ready to Pair"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.extendedColors.infoContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "On your Mac:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        InstructionItem("1", "Open System Settings > Bluetooth")
                        InstructionItem("2", "Look for \"DroidPad Trackpad\"")
                        InstructionItem("3", "Click \"Connect\" next to the device")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your phone is now discoverable and ready to pair.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel button
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun InstructionItem(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.extendedColors.info.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.extendedColors.info
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
