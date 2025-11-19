package com.dps.droidpadmacos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service used to keep the Bluetooth HID connection alive
 * and show a persistent notification while connected.
 */
class ConnectionForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceName = intent?.getStringExtra(EXTRA_DEVICE_NAME).orEmpty()
        val deviceAddress = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS).orEmpty()

        val notification = buildNotification(
            deviceName = if (deviceName.isNotBlank()) deviceName else "Mac",
            deviceAddress = deviceAddress
        )

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(
        deviceName: String,
        deviceAddress: String
    ): Notification {
        val title = "DroidPad connected"
        val content = if (deviceAddress.isNotBlank()) {
            "Connected to $deviceName ($deviceAddress)"
        } else {
            "Connected to $deviceName"
        }

        val tapIntent = Intent(this, FullScreenTrackpadActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DroidPad Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps DroidPad connection active and shows status"
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "droidpad_connection"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_DEVICE_NAME = "extra_device_name"
        private const val EXTRA_DEVICE_ADDRESS = "extra_device_address"

        fun start(
            context: Context,
            deviceName: String,
            deviceAddress: String
        ) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                putExtra(EXTRA_DEVICE_NAME, deviceName)
                putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConnectionForegroundService::class.java))
        }
    }
}

