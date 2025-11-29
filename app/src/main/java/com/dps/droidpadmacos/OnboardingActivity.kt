package com.dps.droidpadmacos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dps.droidpadmacos.ui.theme.DroidPadMacOSTheme

class OnboardingActivity : ComponentActivity() {

    private val prefs by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Mutable state that can be updated when activity resumes
    private val batteryOptimizationIgnored = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check initial state
        batteryOptimizationIgnored.value = isBatteryOptimizationIgnored()

        setContent {
            DroidPadMacOSTheme {
                OnboardingScreen(
                    isBatteryOptimizationIgnored = batteryOptimizationIgnored.value,
                    onRequestBatteryOptimizationException = { openBatteryOptimizationSettings() },
                    onContinue = {
                        markOnboardingComplete()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recheck permission status when returning from settings
        batteryOptimizationIgnored.value = isBatteryOptimizationIgnored()
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    private fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Log.d("OnboardingActivity", "Opening battery optimization settings for $packageName")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("OnboardingActivity", "Failed to open battery optimization dialog", e)
                // Fallback: open general battery optimization settings
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                } catch (e2: Exception) {
                    Log.e("OnboardingActivity", "Failed to open battery settings", e2)
                    Toast.makeText(
                        this,
                        "Please go to Settings > Battery > Battery Optimization and allow DroidPad",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun markOnboardingComplete() {
        prefs.edit()
            .putBoolean(KEY_COMPLETED_ONBOARDING, true)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "droidpad_onboarding"
        private const val KEY_COMPLETED_ONBOARDING = "completed_onboarding"

        fun hasCompletedOnboarding(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_COMPLETED_ONBOARDING, false)
        }
    }
}

@Composable
private fun OnboardingScreen(
    isBatteryOptimizationIgnored: Boolean,
    onRequestBatteryOptimizationException: () -> Unit,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Welcome to DroidPad",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "A few quick steps will make sure your Mac trackpad connection is smooth and reliable.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Keep connection alive",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "To prevent Android from stopping DroidPad in the background, allow it to ignore battery optimizations.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onRequestBatteryOptimizationException()
                        },
                        enabled = !isBatteryOptimizationIgnored,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBatteryOptimizationIgnored) "Already allowed" else "Allow background running"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }
    }
}

