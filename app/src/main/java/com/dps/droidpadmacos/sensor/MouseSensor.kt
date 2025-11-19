package com.dps.droidpadmacos.sensor

/**
 * Interface for mouse sensors
 *
 * Interface Segregation: Minimal interface that all mouse sensors must implement
 * Liskov Substitution: Any sensor implementing this can be used interchangeably
 * Dependency Inversion: UI depends on this abstraction, not concrete implementations
 */
interface MouseSensor {
    /**
     * Start the sensor and begin sending movement updates
     */
    fun start()

    /**
     * Stop the sensor and cease movement updates
     */
    fun stop()

    /**
     * Check if this sensor type is available on the device
     */
    fun isAvailable(): Boolean

    /**
     * Set the sensitivity/scaling factor
     */
    fun setSensitivity(value: Float)

    /**
     * Get the current sensitivity
     */
    fun getSensitivity(): Float

    /**
     * Reset/calibrate the sensor
     */
    fun reset()
}

/**
 * Factory for creating mouse sensors
 * Open/Closed: Easy to add new sensor types without modifying existing code
 */
object MouseSensorFactory {

    enum class SensorType {
        AIR_MOUSE,
        DESK_MOUSE
    }

    /**
     * Create a mouse sensor of the specified type
     */
    fun createSensor(
        type: SensorType,
        context: android.content.Context,
        onMove: (deltaX: Int, deltaY: Int) -> Unit
    ): MouseSensor {
        return when (type) {
            SensorType.AIR_MOUSE -> AirMouseSensorAdapter(
                AirMouseSensor(context, onMove)
            )
            SensorType.DESK_MOUSE -> DeskMouseSensorAdapter(
                DeskMouseSensor(context, onMove)
            )
        }
    }
}

/**
 * Adapter to make AirMouseSensor conform to MouseSensor interface
 * Adapter Pattern: Wraps existing implementation with new interface
 */
class AirMouseSensorAdapter(
    private val airMouseSensor: AirMouseSensor
) : MouseSensor {

    override fun start() = airMouseSensor.start()

    override fun stop() = airMouseSensor.stop()

    override fun isAvailable() = airMouseSensor.isAvailable()

    override fun setSensitivity(value: Float) = airMouseSensor.setSensitivity(value)

    override fun getSensitivity() = airMouseSensor.sensitivity

    override fun reset() {
        // AirMouseSensor doesn't have a reset method, but we can stop and start
        airMouseSensor.stop()
        airMouseSensor.start()
    }
}

/**
 * Adapter to make DeskMouseSensor conform to MouseSensor interface
 */
class DeskMouseSensorAdapter(
    private val deskMouseSensor: DeskMouseSensor
) : MouseSensor {

    override fun start() = deskMouseSensor.start()

    override fun stop() = deskMouseSensor.stop()

    override fun isAvailable() = deskMouseSensor.isAvailable()

    override fun setSensitivity(value: Float) = deskMouseSensor.setSensitivity(value)

    override fun getSensitivity() = deskMouseSensor.sensitivity

    override fun reset() = deskMouseSensor.reset()
}