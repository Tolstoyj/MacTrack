package com.dps.droidpadmacos

import android.app.Application
import com.dps.droidpadmacos.util.CrashReporter
import com.dps.droidpadmacos.util.Logger

/**
 * Application class used for global initialization such as crash reporting.
 */
class DroidPadApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize crash reporter (logs uncaught exceptions)
        CrashReporter.init(this)
        Logger.d("DroidPadApplication", "Application started")
    }
}

