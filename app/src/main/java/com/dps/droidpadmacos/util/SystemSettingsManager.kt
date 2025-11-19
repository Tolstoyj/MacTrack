package com.dps.droidpadmacos.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Manages system settings and permissions
 *
 * Single Responsibility: Only handles system settings interactions
 * Dependency Inversion: Can be easily mocked for testing
 */
class SystemSettingsManager(
    private val context: Context
) {

    /**
     * Check if the app has permission to write system settings
     */
    fun canWriteSystemSettings(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true // Permission not required on older versions
        }
    }

    /**
     * Request permission to write system settings
     */
    fun requestWriteSettingsPermission(activity: ComponentActivity): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Permission result is checked via canWriteSystemSettings()
            Logger.d(TAG, "Write settings permission result: ${canWriteSystemSettings()}")
        }
    }

    /**
     * Create intent to request write settings permission
     */
    fun createWriteSettingsIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Set device brightness safely
     * Falls back to window-only brightness if system permission is not available
     */
    fun setBrightness(window: Window?, brightness: Float): BrightnessResult {
        val clampedBrightness = brightness.coerceIn(0f, 1f)

        // Always try to set window brightness first
        window?.let { w ->
            val layoutParams = w.attributes
            layoutParams.screenBrightness = clampedBrightness
            w.attributes = layoutParams
        }

        // Try to set system brightness if we have permission
        return if (canWriteSystemSettings()) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (clampedBrightness * 255).toInt()
                )
                BrightnessResult.SystemSet(clampedBrightness)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to set system brightness", e)
                BrightnessResult.WindowOnly(clampedBrightness)
            }
        } else {
            BrightnessResult.WindowOnly(clampedBrightness)
        }
    }

    /**
     * Get current system brightness
     */
    fun getCurrentBrightness(): Float {
        return try {
            val brightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            brightness / 255f
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to get system brightness", e)
            0.5f // Default to 50%
        }
    }

    /**
     * Keep screen on for this window
     */
    fun setKeepScreenOn(window: Window?, keepOn: Boolean) {
        window?.let { w ->
            if (keepOn) {
                w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    /**
     * Result of brightness setting operation
     */
    sealed class BrightnessResult {
        data class SystemSet(val value: Float) : BrightnessResult()
        data class WindowOnly(val value: Float) : BrightnessResult()
    }

    companion object {
        private const val TAG = "SystemSettingsManager"
    }
}

/**
 * Interface for system settings operations
 * This allows for easy mocking in tests
 */
interface ISystemSettings {
    fun canWriteSystemSettings(): Boolean
    fun requestWriteSettingsPermission(activity: ComponentActivity): ActivityResultLauncher<Intent>
    fun setBrightness(window: Window?, brightness: Float): SystemSettingsManager.BrightnessResult
    fun getCurrentBrightness(): Float
    fun setKeepScreenOn(window: Window?, keepOn: Boolean)
}