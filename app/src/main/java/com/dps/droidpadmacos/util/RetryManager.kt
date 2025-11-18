package com.dps.droidpadmacos.util

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages retry logic with exponential backoff
 */
class RetryManager(
    private val maxRetries: Int = 5,
    private val initialDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 32000L,
    private val backoffMultiplier: Double = 2.0
) {
    private var currentRetry = 0
    private var currentDelayMs = initialDelayMs

    /**
     * Reset retry state
     */
    fun reset() {
        currentRetry = 0
        currentDelayMs = initialDelayMs
    }

    /**
     * Check if we can retry
     */
    fun canRetry(): Boolean {
        return currentRetry < maxRetries
    }

    /**
     * Get current retry attempt number
     */
    fun getCurrentRetry(): Int = currentRetry

    /**
     * Wait for exponential backoff delay
     * Returns true if we can continue retrying, false if max retries reached
     */
    suspend fun waitForRetry(): Boolean {
        if (!canRetry()) {
            return false
        }

        // Calculate delay with exponential backoff
        val delay = min(
            currentDelayMs,
            maxDelayMs
        )

        Logger.d("RetryManager", "Waiting ${delay}ms before retry attempt ${currentRetry + 1}/$maxRetries")

        delay(delay)

        // Increment retry counter and calculate next delay
        currentRetry++
        currentDelayMs = min(
            (currentDelayMs * backoffMultiplier).toLong(),
            maxDelayMs
        )

        return true
    }

    /**
     * Get human-readable retry status
     */
    fun getRetryStatus(): String {
        return "Retry ${currentRetry}/$maxRetries (next delay: ${currentDelayMs}ms)"
    }
}
