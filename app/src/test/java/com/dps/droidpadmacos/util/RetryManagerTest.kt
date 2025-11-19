package com.dps.droidpadmacos.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryManagerTest {

    @Test
    fun `initial state is sane`() {
        val retryManager = RetryManager(
            maxRetries = 3,
            initialDelayMs = 1000L,
            maxDelayMs = 32000L
        )

        assertEquals(0, retryManager.getCurrentRetry())
        assertTrue(retryManager.canRetry())
        assertEquals(
            "Retry 0/3 (next delay: 1000ms)",
            retryManager.getRetryStatus()
        )
    }

    @Test
    fun `reset clears retry state`() {
        val retryManager = RetryManager(maxRetries = 3, initialDelayMs = 1000L, maxDelayMs = 32000L)

        // After calling reset, state should remain at initial values
        retryManager.reset()
        assertEquals(0, retryManager.getCurrentRetry())
        assertTrue(retryManager.canRetry())
    }
}

