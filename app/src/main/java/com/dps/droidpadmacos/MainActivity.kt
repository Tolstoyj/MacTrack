package com.dps.droidpadmacos

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dps.droidpadmacos.bluetooth.BluetoothHidService
import com.dps.droidpadmacos.touchpad.TouchpadGestureDetector
import com.dps.droidpadmacos.ui.RecentDevicesList
import com.dps.droidpadmacos.ui.TrackpadSurface
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme
import com.dps.droidpadmacos.viewmodel.TrackpadViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TrackpadViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted
        }
    }

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Result codes: RESULT_CANCELED (-1), or the duration in seconds (e.g., 120, 300)
        if (result.resultCode > 0) {
            // Device is now discoverable - register HID device
            android.util.Log.d("MainActivity", "Device is discoverable for ${result.resultCode} seconds")
            viewModel.registerDevice()
        } else {
            android.util.Log.d("MainActivity", "User denied discoverability request")
        }
    }

    fun requestDiscoverable() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300) // 5 minutes
        }
        discoverableLauncher.launch(discoverableIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestBluetoothPermissions()

        // Observe connection state to play beep and navigate
        lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                if (state is BluetoothHidService.ConnectionState.Connected) {
                    playConnectionBeep()
                    // Navigate to full-screen trackpad
                    val intent = Intent(this@MainActivity, FullScreenTrackpadActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        setContent {
            DroidPadMacOSTheme {
                TrackpadScreen(
                    viewModel = viewModel,
                    onRequestDiscoverable = { requestDiscoverable() }
                )
            }
        }

        // Attempt auto-reconnect on app start
        viewModel.attemptAutoReconnect()
    }

    private fun playConnectionBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, 200)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to play beep", e)
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission) {
            permissionLauncher.launch(permissions)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TrackpadScreen(
    viewModel: TrackpadViewModel,
    onRequestDiscoverable: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()
    val recentDevices by viewModel.recentDevices.collectAsState()

    var gestureInfo by remember { mutableStateOf("Ready") }

    // Get current device name
    val currentDeviceName = when (val state = connectionState) {
        is BluetoothHidService.ConnectionState.Connected -> state.deviceName
        else -> null
    }

    var showResetDialog by remember { mutableStateOf(false) }

    val gestureDetector = remember {
        TouchpadGestureDetector(
            onMove = { deltaX, deltaY ->
                android.util.Log.d("TrackpadScreen", "Gesture Move - ΔX=$deltaX, ΔY=$deltaY")
                viewModel.sendMouseMovement(deltaX, deltaY)
                gestureInfo = "Moving: X=$deltaX, Y=$deltaY"
            },
            onLeftClick = {
                android.util.Log.d("TrackpadScreen", "Left Click")
                viewModel.sendLeftClick()
                gestureInfo = "Left Click"
            },
            onRightClick = {
                android.util.Log.d("TrackpadScreen", "Right Click")
                viewModel.sendRightClick()
                gestureInfo = "Right Click"
            },
            onScroll = { deltaY ->
                android.util.Log.d("TrackpadScreen", "Scroll - ΔY=$deltaY")
                viewModel.sendScroll(deltaY)
                gestureInfo = "Scrolling: ΔY=$deltaY"
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "DroidPad",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        is BluetoothHidService.ConnectionState.Connected -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        is BluetoothHidService.ConnectionState.Registered -> Color(0xFF2196F3).copy(alpha = 0.1f)
                        is BluetoothHidService.ConnectionState.Error -> Color(0xFFF44336).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Status",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (val state = connectionState) {
                            is BluetoothHidService.ConnectionState.Disconnected -> "Disconnected"
                            is BluetoothHidService.ConnectionState.Registering -> "Registering..."
                            is BluetoothHidService.ConnectionState.Registered -> "Waiting for connection"
                            is BluetoothHidService.ConnectionState.Connecting -> "Connecting..."
                            is BluetoothHidService.ConnectionState.Connected ->
                                "Connected to ${state.deviceName}"
                            is BluetoothHidService.ConnectionState.Error ->
                                "Error: ${state.message}"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (connectionState) {
                            is BluetoothHidService.ConnectionState.Connected -> Color(0xFF4CAF50)
                            is BluetoothHidService.ConnectionState.Error -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isRegistered) {
                            viewModel.unregisterDevice()
                        } else {
                            onRequestDiscoverable()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRegistered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isRegistered) "Unregister" else "Register")
                }

                if (connectionState is BluetoothHidService.ConnectionState.Connected) {
                    Button(
                        onClick = { viewModel.disconnect() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Disconnect")
                    }
                }
            }

            // Reset All Button
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE53935)
                )
            ) {
                Text("⚠️ Reset All Connections & Clear History")
            }

            // Recent Devices List
            if (recentDevices.isNotEmpty() || connectionState is BluetoothHidService.ConnectionState.Registered) {
                RecentDevicesList(
                    devices = recentDevices,
                    currentDeviceName = currentDeviceName,
                    onDeviceClick = { device ->
                        viewModel.connectToDeviceByAddress(device.address)
                    },
                    onClearHistory = {
                        viewModel.clearDeviceHistory()
                    }
                )
            }

            // Instructions
            if (connectionState is BluetoothHidService.ConnectionState.Registered) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "✓ Device Registered as HID Trackpad!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEB3B).copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚠️ IMPORTANT: First Time Setup",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57F17),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "If this device was previously paired with your Mac, you MUST forget/unpair it first in Mac Bluetooth settings, then pair fresh as 'DroidPad Trackpad'.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFF57F17)
                                )
                            }
                        }

                        Text(
                            text = "To connect from your Mac:",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "1. System Settings → Bluetooth",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        Text(
                            text = "2. Look for 'DroidPad Trackpad' (NOT your phone name)",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        Text(
                            text = "3. Click 'Connect'",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        Text(
                            text = "4. Wait for connection (status will update above)",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Gesture Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Text(
                    text = gestureInfo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            // New Trackpad Surface
            TrackpadSurface(
                isConnected = connectionState is BluetoothHidService.ConnectionState.Connected,
                gestureDetector = gestureDetector,
                onGestureInfo = { info -> gestureInfo = info },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Text(text = "⚠️", fontSize = 40.sp)
            },
            title = {
                Text(
                    text = "Reset All Connections?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "This will:",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("• Disconnect from current device")
                    Text("• Unregister HID device")
                    Text("• Remove all Bluetooth pairings")
                    Text("• Clear device history")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use this if devices won't connect or you want a fresh start.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllConnections()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}