package com.dps.droidpadmacos.util

import android.util.Log

/**
 * Conditional logger that only logs in debug builds
 * Helps improve production performance and reduce log spam
 *
 * In release builds:
 * - Debug (d) and Verbose (v) logs are completely disabled
 * - Info (i) logs are disabled
 * - Only Warning (w) and Error (e) logs are kept for critical issues
 * - ProGuard rules will additionally remove the log calls at compile time
 */
object Logger {

    private const val DEFAULT_TAG = "DroidPad"
    private val isDebug: Boolean by lazy {
        try {
            val clazz = Class.forName("com.dps.droidpadmacos.BuildConfig")
            val debugField = clazz.getField("DEBUG")
            debugField.getBoolean(null)
        } catch (e: Exception) {
            // If anything goes wrong, default to debug enabled to avoid hiding logs during development
            Log.w(DEFAULT_TAG, "Unable to read BuildConfig.DEBUG, defaulting to debug logging", e)
            true
        }
    }

    /**
     * Log debug message (only in debug builds)
     * Completely removed in release builds by both runtime check and ProGuard
     */
    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }

    /**
     * Log debug message with throwable (only in debug builds)
     */
    fun d(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        if (isDebug) {
            Log.d(tag, message, throwable)
        }
    }

    /**
     * Log info message (only in debug builds)
     * Info logs are also disabled in production to reduce log noise
     */
    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (isDebug) {
            Log.i(tag, message)
        }
    }

    /**
     * Log warning message (always logged)
     * Warnings indicate potential issues that should be tracked in production
     */
    fun w(tag: String = DEFAULT_TAG, message: String) {
        Log.w(tag, message)
    }

    /**
     * Log warning message with throwable (always logged)
     */
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
    }

    /**
     * Log error message (always logged)
     * Errors are critical and should always be tracked
     */
    fun e(tag: String = DEFAULT_TAG, message: String) {
        Log.e(tag, message)
    }

    /**
     * Log error message with throwable (always logged)
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }

    /**
     * Log verbose message (only in debug builds)
     * Most verbose logging level, completely removed in production
     */
    fun v(tag: String = DEFAULT_TAG, message: String) {
        if (isDebug) {
            Log.v(tag, message)
        }
    }

    /**
     * Log what a terrible failure (only in debug builds)
     * Used for conditions that should never happen
     */
    fun wtf(tag: String = DEFAULT_TAG, message: String) {
        if (isDebug) {
            Log.wtf(tag, message)
        }
    }

    /**
     * Extension functions for easier usage
     */
    inline fun <reified T> T.logD(message: String) {
        d(T::class.java.simpleName, message)
    }

    inline fun <reified T> T.logI(message: String) {
        i(T::class.java.simpleName, message)
    }

    inline fun <reified T> T.logW(message: String) {
        w(T::class.java.simpleName, message)
    }

    inline fun <reified T> T.logE(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            e(T::class.java.simpleName, message, throwable)
        } else {
            e(T::class.java.simpleName, message)
        }
    }
}
