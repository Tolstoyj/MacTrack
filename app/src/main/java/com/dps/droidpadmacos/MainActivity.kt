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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dps.droidpadmacos.bluetooth.BluetoothHidService
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme
import com.dps.droidpadmacos.ui.theme.extendedColors
import com.dps.droidpadmacos.viewmodel.TrackpadViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TrackpadViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted - now safe to initialize Bluetooth
            viewModel.attemptAutoReconnect()
        } else {
            android.util.Log.e("MainActivity", "Bluetooth permissions not granted")
        }
    }

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("MainActivity", "Discoverability result code: ${result.resultCode}")

        // Result codes: RESULT_CANCELED (-1), or the duration in seconds (e.g., 120, 300)
        if (result.resultCode > 0) {
            // Device is now discoverable - register HID device
            android.util.Log.d("MainActivity", "✅ Device is NOW DISCOVERABLE for ${result.resultCode} seconds")
            viewModel.registerDevice()

            // Navigate to DiscoverableActivity to show pairing screen
            android.util.Log.d("MainActivity", "Navigating to DiscoverableActivity...")
            val intent = Intent(this, DiscoverableActivity::class.java)
            startActivity(intent)
        } else {
            android.util.Log.w("MainActivity", "❌ User denied discoverability request or cancelled (code: ${result.resultCode})")
            android.widget.Toast.makeText(
                this,
                "Bluetooth discoverability is required to pair with Mac",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestDiscoverable() {
        android.util.Log.d("MainActivity", "🔵 Requesting Bluetooth discoverability...")

        // Ensure we have required Bluetooth permissions on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val requiredPermissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )

            val missingPermission = requiredPermissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermission) {
                android.util.Log.w("MainActivity", "Bluetooth permissions missing, requesting before discoverable")
                requestBluetoothPermissions()
                android.widget.Toast.makeText(
                    this,
                    "Bluetooth permissions are required to make device discoverable",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        // Check if Bluetooth is enabled
        @Suppress("DEPRECATION")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            android.util.Log.e("MainActivity", "Bluetooth adapter is null")
            android.widget.Toast.makeText(
                this,
                "Bluetooth is not supported on this device",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            android.util.Log.e("MainActivity", "Bluetooth is not enabled")
            android.widget.Toast.makeText(
                this,
                "Please enable Bluetooth first",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        // Log current scan mode
        val scanMode = bluetoothAdapter.scanMode
        android.util.Log.d("MainActivity", "Current scan mode: $scanMode (${getScanModeName(scanMode)})")

        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300) // 5 minutes
        }

        try {
            discoverableLauncher.launch(discoverableIntent)
            android.util.Log.d("MainActivity", "Discoverability intent launched successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to launch discoverability intent", e)
            android.widget.Toast.makeText(
                this,
                "Failed to request Bluetooth discoverability: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getScanModeName(scanMode: Int): String {
        return when (scanMode) {
            BluetoothAdapter.SCAN_MODE_NONE -> "NONE (not discoverable)"
            BluetoothAdapter.SCAN_MODE_CONNECTABLE -> "CONNECTABLE (not discoverable)"
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> "CONNECTABLE_DISCOVERABLE"
            else -> "UNKNOWN ($scanMode)"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestBluetoothPermissions()

        // Observe Bluetooth connection state to play beep and navigate
        lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                if (state is BluetoothHidService.ConnectionState.Connected) {
                    // Start foreground service to keep connection alive
                    ConnectionForegroundService.start(
                        context = this@MainActivity,
                        deviceName = state.deviceName,
                        deviceAddress = state.deviceAddress
                    )

                    playConnectionBeep()
                    // Navigate to full-screen trackpad
                    val intent = Intent(this@MainActivity, FullScreenTrackpadActivity::class.java)
                    startActivity(intent)
                } else if (state is BluetoothHidService.ConnectionState.Disconnected ||
                    state is BluetoothHidService.ConnectionState.Error
                ) {
                    ConnectionForegroundService.stop(this@MainActivity)
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

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
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
        } else {
            // Permissions already granted - safe to initialize
            viewModel.attemptAutoReconnect()
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

    var showAdvanced by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A0F16),
                Color(0xFF0F1621)
            )
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ConnectionBadge(connectionState)

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Connect",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Available Bluetooth devices",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    DeviceList(
                        recentDevices = recentDevices,
                        connectionState = connectionState,
                        onDeviceClick = { viewModel.connectToDeviceByAddress(it.address) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    when (val state = connectionState) {
                        is BluetoothHidService.ConnectionState.Registered -> {
                            StatusHintCard(
                                title = "Ready to pair",
                                message = "Open Bluetooth on your Mac and select 'DroidPad Trackpad'.",
                                actionLabel = "Open Bluetooth",
                                onAction = {
                                    val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                    context.startActivity(intent)
                                }
                            )
                        }

                        is BluetoothHidService.ConnectionState.Error -> {
                            StatusHintCard(
                                title = "Connection error",
                                message = state.message,
                                accent = MaterialTheme.colorScheme.error
                            )
                        }

                        else -> {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            when (connectionState) {
                                is BluetoothHidService.ConnectionState.Disconnected -> onRequestDiscoverable()
                                is BluetoothHidService.ConnectionState.Connected -> {
                                    val intent = Intent(context, FullScreenTrackpadActivity::class.java)
                                    context.startActivity(intent)
                                }
                                is BluetoothHidService.ConnectionState.Registered -> onRequestDiscoverable()
                                else -> {}
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (connectionState) {
                                is BluetoothHidService.ConnectionState.Connected -> MaterialTheme.extendedColors.success
                                else -> MaterialTheme.colorScheme.primary
                            },
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = connectionState !is BluetoothHidService.ConnectionState.Connecting &&
                                connectionState !is BluetoothHidService.ConnectionState.Registering
                    ) {
                        Text(
                            text = when (connectionState) {
                                is BluetoothHidService.ConnectionState.Disconnected -> "Connect"
                                is BluetoothHidService.ConnectionState.Connected -> "Open Trackpad"
                                is BluetoothHidService.ConnectionState.Registered -> "Make Discoverable"
                                is BluetoothHidService.ConnectionState.Connecting -> "Connecting..."
                                is BluetoothHidService.ConnectionState.Registering -> "Preparing..."
                                is BluetoothHidService.ConnectionState.Error -> "Try Again"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    FilledTonalButton(
                        onClick = { onRequestDiscoverable() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "Scan Again",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(context, SettingsActivity::class.java)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = { showAdvanced = !showAdvanced },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (showAdvanced) "Hide Advanced" else "Advanced",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(showAdvanced) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isRegistered) {
                                OutlinedButton(
                                    onClick = { viewModel.unregisterDevice() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Reset")
                                }
                            }
                            if (connectionState is BluetoothHidService.ConnectionState.Connected) {
                                OutlinedButton(
                                    onClick = { viewModel.disconnect() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionBadge(connectionState: BluetoothHidService.ConnectionState) {
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val badgeColor = when (connectionState) {
        is BluetoothHidService.ConnectionState.Connected -> MaterialTheme.extendedColors.successContainer
        is BluetoothHidService.ConnectionState.Registered -> MaterialTheme.extendedColors.infoContainer
        is BluetoothHidService.ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.32f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = badgeColor.copy(alpha = 0.9f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, outline)
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = "Bluetooth",
                    tint = when (connectionState) {
                        is BluetoothHidService.ConnectionState.Connected -> MaterialTheme.extendedColors.success
                        else -> accent
                    },
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceList(
    recentDevices: List<com.dps.droidpadmacos.data.ConnectedDevice>,
    connectionState: BluetoothHidService.ConnectionState,
    onDeviceClick: (com.dps.droidpadmacos.data.ConnectedDevice) -> Unit
) {
    val activeName = (connectionState as? BluetoothHidService.ConnectionState.Connected)?.deviceName

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (recentDevices.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (connectionState) {
                            is BluetoothHidService.ConnectionState.Connecting -> "Connecting..."
                            is BluetoothHidService.ConnectionState.Registering -> "Setting up..."
                            else -> "Searching..."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                recentDevices.take(3).forEachIndexed { index, device ->
                    val isActive = activeName?.equals(device.name, ignoreCase = true) == true
                    DeviceRow(
                        device = device,
                        status = if (isActive) "Connected" else "Tap to connect",
                        isActive = isActive,
                        onClick = { onDeviceClick(device) }
                    )

                    if (index != recentDevices.take(3).lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: com.dps.droidpadmacos.data.ConnectedDevice,
    status: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (isActive) 0.4f else 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💻", fontSize = 18.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Text(
                    text = if (isActive) "Connected" else "Available",
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusHintCard(
    title: String,
    message: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(actionLabel, color = accent)
                }
            }
        }
    }
}
