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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dps.droidpadmacos.bluetooth.BluetoothHidService
import com.dps.droidpadmacos.ui.Dimens
import com.dps.droidpadmacos.ui.RecentDevicesList
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
        } else
        {
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

    @SuppressLint("MissingPermission")
    fun requestDiscoverable() {
        android.util.Log.d("MainActivity", "🔵 Requesting Bluetooth discoverability...")

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

    // Smooth animated values for state transitions
    val targetIconScale = if (connectionState is BluetoothHidService.ConnectionState.Registered) 1.08f else 1f
    val animatedIconScale by animateFloatAsState(
        targetValue = targetIconScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    val targetContentAlpha = 1f
    val animatedContentAlpha by animateFloatAsState(
        targetValue = targetContentAlpha,
        animationSpec = tween(600, easing = LinearOutSlowInEasing),
        label = "contentAlpha"
    )

    val targetContentOffset = 0.dp
    val animatedContentOffset by animateDpAsState(
        targetValue = targetContentOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "contentOffset"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = Dimens.mainHorizontalPadding(),
                        vertical = Dimens.mainVerticalPadding()
                    )
                    .graphicsLayer {
                        alpha = animatedContentAlpha
                        translationY = animatedContentOffset.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated Status Icon with glow effect
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = EaseInOutCubic),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glow"
                )

                Box(contentAlignment = Alignment.Center) {
                    // Outer glow
                    if (connectionState is BluetoothHidService.ConnectionState.Registered) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.statusIconGlowSize())
                                .scale(animatedIconScale)
                                .background(
                                    color = MaterialTheme.extendedColors.infoContainer.copy(alpha = glowAlpha),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }

                    // Main icon circle
                    Box(
                        modifier = Modifier
                            .size(Dimens.statusIconSize())
                            .scale(animatedIconScale)
                            .background(
                                color = when (connectionState) {
                                    is BluetoothHidService.ConnectionState.Connected -> MaterialTheme.extendedColors.successContainer
                                    is BluetoothHidService.ConnectionState.Registered -> MaterialTheme.extendedColors.infoContainer
                                    is BluetoothHidService.ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .border(
                                width = 3.dp,
                                color = when (connectionState) {
                                    is BluetoothHidService.ConnectionState.Connected -> MaterialTheme.extendedColors.success.copy(alpha = 0.3f)
                                    is BluetoothHidService.ConnectionState.Registered -> MaterialTheme.extendedColors.info.copy(alpha = 0.3f)
                                    else -> Color.Transparent
                                },
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (connectionState) {
                            is BluetoothHidService.ConnectionState.Registered -> {
                                Image(
                                    painter = painterResource(id = R.drawable.img_macbook),
                                    contentDescription = "Waiting for Mac",
                                    modifier = Modifier
                                        .size(Dimens.macbookIconSize())
                                        .graphicsLayer {
                                            scaleX = animatedIconScale
                                            scaleY = animatedIconScale
                                        }
                                )
                            }
                            is BluetoothHidService.ConnectionState.Registering,
                            is BluetoothHidService.ConnectionState.Connecting -> {
                                // Show loading indicator
                                CircularProgressIndicator(
                                    modifier = Modifier.size(64.dp),
                                    color = MaterialTheme.extendedColors.info,
                                    strokeWidth = 6.dp
                                )
                            }
                            else -> {
                                Text(
                                    text = when (connectionState) {
                                        is BluetoothHidService.ConnectionState.Connected -> "✓"
                                        is BluetoothHidService.ConnectionState.Error -> "⚠️"
                                        else -> "📱"
                                    },
                                    fontSize = 64.sp,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = animatedIconScale
                                        scaleY = animatedIconScale
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingXLarge()))

                // Status Text with better typography
                Text(
                    text = when (val state = connectionState) {
                        is BluetoothHidService.ConnectionState.Disconnected -> "Ready to Connect"
                        is BluetoothHidService.ConnectionState.Registering -> "Setting Up..."
                        is BluetoothHidService.ConnectionState.Registered -> "Waiting for Mac"
                        is BluetoothHidService.ConnectionState.Connecting -> "Connecting..."
                        is BluetoothHidService.ConnectionState.Connected -> "Connected!"
                        is BluetoothHidService.ConnectionState.Error -> "Connection Error"
                    },
                    fontSize = Dimens.statusTextSize(),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.spacingMedium()))

                // Subtitle with improved line height
                Text(
                    text = when (val state = connectionState) {
                        is BluetoothHidService.ConnectionState.Disconnected -> "Transform your phone into a wireless trackpad"
                        is BluetoothHidService.ConnectionState.Registered -> "Open Bluetooth on your Mac\nand connect to 'DroidPad Trackpad'"
                        is BluetoothHidService.ConnectionState.Connected -> state.deviceName
                        is BluetoothHidService.ConnectionState.Error -> state.message
                        else -> ""
                    },
                    fontSize = Dimens.subtitleTextSize(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = Dimens.subtitleTextSize() * 1.5f,
                    modifier = Modifier.padding(horizontal = Dimens.spacingXLarge())
                )

                // Animated help card for Registered state
                androidx.compose.animation.AnimatedVisibility(
                    visible = connectionState is BluetoothHidService.ConnectionState.Registered,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        initialOffsetY = { it / 4 }
                    ),
                    exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(Dimens.spacingLarge()))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.spacingSmall()),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.extendedColors.infoContainer.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(Dimens.cardPadding()),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Text(
                                        text = "💡",
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "Troubleshooting",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.extendedColors.onInfoContainer
                                    )
                                }

                                TroubleshootStep("Make sure this phone is discoverable")
                                TroubleshootStep("Refresh Bluetooth on your Mac")
                                TroubleshootStep("Look for 'DroidPad Trackpad' (not your phone name)")

                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.extendedColors.onInfoContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Open Bluetooth Settings",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingLarge() + Dimens.spacingMedium()))

                // Main Action Button with elevation and press effect
                val buttonScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "buttonScale"
                )

                Button(
                    onClick = {
                        when (connectionState) {
                            is BluetoothHidService.ConnectionState.Disconnected -> {
                                android.util.Log.d("MainActivity", "Starting Bluetooth connection - requesting discoverability")
                                onRequestDiscoverable()
                            }
                            is BluetoothHidService.ConnectionState.Connected -> {
                                // Open trackpad
                                val intent = Intent(context, FullScreenTrackpadActivity::class.java)
                                context.startActivity(intent)
                            }
                            is BluetoothHidService.ConnectionState.Registered -> {
                                // Make discoverable again
                                android.util.Log.d("MainActivity", "Re-requesting discoverability")
                                onRequestDiscoverable()
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeight())
                        .scale(buttonScale),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (connectionState) {
                            is BluetoothHidService.ConnectionState.Connected -> MaterialTheme.extendedColors.success
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp,
                        disabledElevation = 0.dp
                    ),
                    enabled = connectionState is BluetoothHidService.ConnectionState.Disconnected ||
                              connectionState is BluetoothHidService.ConnectionState.Connected ||
                              connectionState is BluetoothHidService.ConnectionState.Registered
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (connectionState) {
                                is BluetoothHidService.ConnectionState.Disconnected -> "Get Started"
                                is BluetoothHidService.ConnectionState.Connected -> "Open Trackpad"
                                is BluetoothHidService.ConnectionState.Registered -> "Retry Connection"
                                else -> "Please Wait..."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Animated Recent Devices
                AnimatedVisibility(
                    visible = recentDevices.isNotEmpty() && connectionState !is BluetoothHidService.ConnectionState.Connected,
                    enter = fadeIn(tween(400)) + expandVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ),
                    exit = fadeOut(tween(300)) + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = "Quick Connect",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        recentDevices.take(3).forEachIndexed { index, device ->
                            val delay = index * 50
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(delay.toLong())
                                visible = true
                            }

                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(tween(300)) + slideInHorizontally(
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    initialOffsetX = { -it / 2 }
                                )
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.connectToDeviceByAddress(device.address) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Text(
                                            text = "💻",
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                        Text(
                                            device.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Settings and Advanced buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Settings button
                    TextButton(
                        onClick = {
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "⚙️",
                                fontSize = 16.sp
                            )
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Advanced toggle with subtle styling
                    TextButton(
                        onClick = { showAdvanced = !showAdvanced },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                if (showAdvanced) "Advanced" else "Advanced",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (showAdvanced) "▲" else "▼",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Advanced Options
                if (showAdvanced) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isRegistered) {
                            OutlinedButton(
                                onClick = { viewModel.unregisterDevice() },
                                modifier = Modifier.weight(1f),
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
                                modifier = Modifier.weight(1f)
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

@Composable
private fun TroubleshootStep(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.extendedColors.onInfoContainer,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.extendedColors.onInfoContainer,
            lineHeight = 20.sp
        )
    }
}