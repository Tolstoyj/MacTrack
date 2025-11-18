package com.dps.droidpadmacos

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
// import android.hardware.usb.UsbManager  // USB detection disabled
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// import com.dps.droidpadmacos.usb.UsbConnectionDetector  // USB detection disabled
import com.dps.droidpadmacos.usb.UsbDebugHelper
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DroidPadMacOSTheme {
                SplashScreen {
                    // Run USB diagnostics only in debug mode for performance
                    // Comment this out in production builds to improve startup time by 150-250ms
                    val enableDiagnostics = false // Set to true for debugging USB issues
                    if (enableDiagnostics) {
                        Log.d(TAG, "=== RUNNING USB DIAGNOSTICS ===")
                        UsbDebugHelper.printFullDiagnostics(this@SplashActivity)
                    }

                    // USB detection disabled - always navigate to MainActivity for Bluetooth mode
                    /*
                    // Check for USB connection first
                    val connectionInfo = UsbConnectionDetector.detectConnection(this@SplashActivity)

                    Log.d(TAG, "USB Connection Info: $connectionInfo")

                    if (UsbConnectionDetector.isSuitableForTrackpad(connectionInfo)) {
                        // USB data connection detected - show USB connection screen
                        Log.d(TAG, "Navigating to UsbConnectionActivity")
                        val intent = Intent(this@SplashActivity, UsbConnectionActivity::class.java)
                        intent.putExtra("CONNECTION_INFO", connectionInfo.connectionType.name)
                        startActivity(intent)
                    } else {
                        // No suitable USB connection - go to Bluetooth mode
                        Log.d(TAG, "Navigating to MainActivity (Bluetooth)")
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    }
                    */

                    // Always go to MainActivity (Bluetooth mode only)
                    Log.d(TAG, "Navigating to MainActivity (Bluetooth mode only)")
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // Trigger animation on composition
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500) // 2.5 seconds
        onTimeout()
    }

    // Logo scale and fade animation
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = LinearEasing
        ),
        label = "logoAlpha"
    )

    // Pulsating text animation
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    // Glitch effect for cryptic text
    val glitchOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0f at 0
                2f at 100
                -1f at 200
                0f at 300
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "glitch"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with scale and fade animation
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "DroidPad Logo",
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    }
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Cryptic tech loading text with animations
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INITIALIZING",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp,
                    modifier = Modifier
                        .alpha(textAlpha)
                        .graphicsLayer {
                            translationX = glitchOffset
                        }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Animated dots
                LoadingDots(startAnimation)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hexadecimal loading indicator
            Text(
                text = "0x${System.currentTimeMillis().toString(16).takeLast(8).uppercase()}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(textAlpha)
            )
        }
    }
}

@Composable
fun LoadingDots(started: Boolean) {
    val dots = remember { listOf(".", ".", ".") }

    Row {
        dots.forEachIndexed { index, dot ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot$index")

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200,
                        easing = EaseInOutCubic
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotAlpha$index"
            )

            Text(
                text = dot,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(if (started) alpha else 0f)
            )
        }
    }
}
