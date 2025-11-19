package com.dps.droidpadmacos.util

import android.app.Application
import kotlin.system.exitProcess

/**
 * Minimal crash reporting hook that logs uncaught exceptions.
 *
 * This can later be wired to a real crash reporting backend
 * such as Firebase Crashlytics without changing call sites.
 */
object CrashReporter {

    fun init(application: Application) {
        Logger.d("CrashReporter", "Initializing crash reporter")

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.e(
                "CrashReporter",
                "Uncaught exception in thread ${thread.name}",
                throwable
            )

            // Delegate to original handler so normal crash behavior is preserved
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                // Fallback to terminating the process
                exitProcess(2)
            }
        }
    }

    /**
     * Log non-fatal exceptions that should still be tracked.
     */
    fun logNonFatal(throwable: Throwable, message: String? = null) {
        if (message != null) {
            Logger.e("CrashReporter", message, throwable)
        } else {
            Logger.e("CrashReporter", "Non-fatal exception", throwable)
        }
    }
}

