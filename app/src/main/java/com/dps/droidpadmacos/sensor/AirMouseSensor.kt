package com.dps.droidpadmacos.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class AirMouseSensor(
    context: Context,
    private val onMove: (deltaX: Int, deltaY: Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    init {
        if (gyroscope != null) {
            Log.d(TAG, "Gyroscope sensor found: ${gyroscope.name}")
        } else {
            Log.e(TAG, "Gyroscope sensor NOT available on this device!")
        }
    }

    private var isActive = false
    private var sensitivity = 30f  // Increased for more responsive movement

    // Rotation rates
    private var rotationX = 0f
    private var rotationY = 0f
    private var rotationZ = 0f

    private var sampleCount = 0

    override fun onSensorChanged(event: SensorEvent) {
        if (!isActive) return

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                // Gyroscope values: rotation rate around each axis (radians/second)
                // event.values[0] = rotation around X axis (pitch)
                // event.values[1] = rotation around Y axis (roll)
                // event.values[2] = rotation around Z axis (yaw)

                rotationX = event.values[0]
                rotationY = event.values[1]
                rotationZ = event.values[2]

                // Convert rotation to cursor movement
                // Tilt phone left/right = move cursor left/right
                // Tilt phone up/down = move cursor up/down

                val deltaX = (-rotationY * sensitivity).toInt()
                val deltaY = (rotationX * sensitivity).toInt()

                // Log every 30 samples (about once per second)
                if (sampleCount++ % 30 == 0) {
                    Log.d(TAG, "Sensor: X=$rotationX, Y=$rotationY, Z=$rotationZ -> Move: deltaX=$deltaX, deltaY=$deltaY")
                }

                if (deltaX != 0 || deltaY != 0) {
                    onMove(deltaX, deltaY)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for gyroscope
    }

    fun start() {
        if (gyroscope == null) {
            Log.e(TAG, "Gyroscope sensor not available!")
            return
        }

        isActive = true
        sensorManager.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_GAME  // Fast updates for smooth movement
        )
        Log.d(TAG, "Air Mouse mode started")
    }

    fun stop() {
        isActive = false
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Air Mouse mode stopped")
    }

    fun isAvailable(): Boolean {
        return gyroscope != null
    }

    fun setSensitivity(value: Float) {
        sensitivity = value.coerceIn(5f, 30f)
    }

    companion object {
        private const val TAG = "AirMouseSensor"
    }
}
