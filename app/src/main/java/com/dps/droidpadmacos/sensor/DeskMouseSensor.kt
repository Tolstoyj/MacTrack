package com.dps.droidpadmacos.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Desk Mouse Sensor - Tracks physical movement of the phone on a desk surface
 * Uses LINEAR_ACCELERATION sensor to detect when the device is moved horizontally
 * Integrates acceleration to velocity and converts to cursor movement
 */
class DeskMouseSensor(
    context: Context,
    private val onMove: (deltaX: Int, deltaY: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Primary sensor: Linear acceleration (gravity already removed)
    private val linearAccelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    // Fallback sensors for older devices
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    init {
        when {
            linearAccelSensor != null -> {
                Log.d(TAG, "Using LINEAR_ACCELERATION sensor: ${linearAccelSensor.name}")
            }
            accelerometer != null && gravitySensor != null -> {
                Log.d(TAG, "Using ACCELEROMETER + GRAVITY fallback")
            }
            else -> {
                Log.e(TAG, "No suitable motion sensors available!")
            }
        }
    }

    private var isActive = false

    // Sensitivity controls how much cursor moves per unit of acceleration
    // Much higher than velocity-based because we're using acceleration directly
    var sensitivity = 2500f  // Adjust this for responsiveness
        private set  // Only allow setting through setSensitivity method

    // Velocity tracking (for smoothing, not primary movement)
    private var velocityX = 0f
    private var velocityY = 0f

    // Timestamp for integration
    private var lastTimestamp = 0L

    // Gravity values for fallback mode
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f

    // Friction/decay factor to prevent drift (0.0 to 1.0)
    // Lower value = faster decay = less drift but less smooth
    private val frictionDecay = 0.92f

    // Threshold for considering device "stationary" (m/s²)
    // Lowered significantly to detect gentle desk movements
    private val stationaryThreshold = 0.02f

    // Velocity threshold - reset if too slow
    private val velocityResetThreshold = 0.001f

    // Low-pass filter alpha (0.0 to 1.0)
    // Lower = more filtering = smoother but less responsive
    private val filterAlpha = 0.5f

    // Filtered acceleration values
    private var filteredAccelX = 0f
    private var filteredAccelY = 0f

    private var sampleCount = 0

    override fun onSensorChanged(event: SensorEvent) {
        if (!isActive) return

        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                handleLinearAcceleration(event)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Store raw acceleration for fallback mode
                handleAccelerometer(event)
            }
            Sensor.TYPE_GRAVITY -> {
                // Store gravity for fallback mode
                gravityX = event.values[0]
                gravityY = event.values[1]
                gravityZ = event.values[2]
            }
        }
    }

    private fun handleLinearAcceleration(event: SensorEvent) {
        val currentTime = event.timestamp

        // Initialize timestamp on first reading
        if (lastTimestamp == 0L) {
            lastTimestamp = currentTime
            return
        }

        // Calculate time delta in seconds
        val dt = (currentTime - lastTimestamp) / 1_000_000_000f
        lastTimestamp = currentTime

        // Prevent huge deltas if sensor was paused
        if (dt > 0.1f) return

        // Linear acceleration values (gravity already removed by sensor)
        // Device laying flat on desk:
        // X axis: left-right (+ right when looking at screen)
        // Y axis: up-down (+ down when looking at screen)
        // Z axis: perpendicular to screen (+ up away from desk)

        val accelX = event.values[0]  // Left-right movement
        val accelY = event.values[1]  // Forward-back movement

        // Apply low-pass filter to reduce noise
        filteredAccelX = filterAlpha * accelX + (1 - filterAlpha) * filteredAccelX
        filteredAccelY = filterAlpha * accelY + (1 - filterAlpha) * filteredAccelY

        // Calculate magnitude to detect if device is moving
        val accelMagnitude = sqrt(filteredAccelX * filteredAccelX + filteredAccelY * filteredAccelY)

        // Integrate acceleration to velocity for smoothing
        if (accelMagnitude > stationaryThreshold) {
            velocityX += filteredAccelX * dt
            velocityY += filteredAccelY * dt
        } else {
            // Apply stronger decay when nearly stationary
            velocityX *= 0.7f
            velocityY *= 0.7f
        }

        // Apply friction decay to prevent drift
        velocityX *= frictionDecay
        velocityY *= frictionDecay

        // Reset very small velocities to prevent accumulation
        if (abs(velocityX) < velocityResetThreshold) velocityX = 0f
        if (abs(velocityY) < velocityResetThreshold) velocityY = 0f

        // Convert acceleration DIRECTLY to cursor movement (not velocity)
        // This is more responsive for desk mouse movement
        // Use a combination of acceleration and velocity for best results
        val accelDeltaX = filteredAccelX * sensitivity * dt
        val accelDeltaY = filteredAccelY * sensitivity * dt
        val velocityDeltaX = velocityX * sensitivity * 0.3f
        val velocityDeltaY = velocityY * sensitivity * 0.3f

        val deltaX = (accelDeltaX + velocityDeltaX).toInt()
        val deltaY = (accelDeltaY + velocityDeltaY).toInt()

        // Log periodically for debugging
        if (sampleCount++ % 30 == 0) {
            Log.d(TAG, "Accel: X=%.3f, Y=%.3f | Vel: X=%.3f, Y=%.3f | Delta: X=%d, Y=%d"
                .format(filteredAccelX, filteredAccelY, velocityX, velocityY, deltaX, deltaY))
        }

        // Send movement if significant
        if (deltaX != 0 || deltaY != 0) {
            onMove(deltaX, deltaY)
        }
    }

    private fun handleAccelerometer(event: SensorEvent) {
        // Fallback mode: subtract gravity manually
        val accelX = event.values[0] - gravityX
        val accelY = event.values[1] - gravityY

        // Create a synthetic event for linear acceleration
        val syntheticEvent = SensorEvent::class.java.getDeclaredConstructor(Int::class.java)
            .apply { isAccessible = true }
            .newInstance(3)

        syntheticEvent.values[0] = accelX
        syntheticEvent.values[1] = accelY
        syntheticEvent.values[2] = event.values[2] - gravityZ
        syntheticEvent.timestamp = event.timestamp
        syntheticEvent.sensor = event.sensor

        handleLinearAcceleration(syntheticEvent)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name} -> $accuracy")
    }

    fun start() {
        if (!isAvailable()) {
            Log.e(TAG, "No motion sensors available!")
            return
        }

        isActive = true
        lastTimestamp = 0
        velocityX = 0f
        velocityY = 0f
        filteredAccelX = 0f
        filteredAccelY = 0f

        // Register primary sensor
        if (linearAccelSensor != null) {
            sensorManager.registerListener(
                this,
                linearAccelSensor,
                SensorManager.SENSOR_DELAY_GAME  // Fast updates (~50Hz)
            )
        } else {
            // Register fallback sensors
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            gravitySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        Log.d(TAG, "Desk Mouse mode started (sensitivity=$sensitivity)")
    }

    fun stop() {
        isActive = false
        sensorManager.unregisterListener(this)
        velocityX = 0f
        velocityY = 0f
        Log.d(TAG, "Desk Mouse mode stopped")
    }

    fun isAvailable(): Boolean {
        return linearAccelSensor != null || (accelerometer != null && gravitySensor != null)
    }

    fun setSensitivity(value: Float) {
        sensitivity = value.coerceIn(500f, 10000f)
        Log.d(TAG, "Sensitivity set to $sensitivity")
    }

    /**
     * Reset velocity to zero - useful for calibration or when switching modes
     */
    fun reset() {
        velocityX = 0f
        velocityY = 0f
        filteredAccelX = 0f
        filteredAccelY = 0f
        lastTimestamp = 0
        Log.d(TAG, "Sensor state reset")
    }

    companion object {
        private const val TAG = "DeskMouseSensor"
    }
}
