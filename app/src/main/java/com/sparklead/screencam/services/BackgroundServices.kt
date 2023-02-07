package com.sparklead.screencam.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.stopForeground
import androidx.core.content.ContextCompat.getSystemService
import com.sparklead.screencam.ui.activities.RecorderActivity


class BackgroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val intent1 = Intent(this, RecorderActivity::class.java)
//        val intent2 = Intent(this, AnotherClassWithError::class.java)
        val pendingIntent1 = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_IMMUTABLE)
//        val pendingIntent2 = PendingIntent.getActivity(this, 0, intent2, 0)
        val notification1: Notification = NotificationCompat.Builder(this, "ScreenRecorder")
            .setContentTitle("yNote studios")
            .setContentText("Filming...")
            .setContentIntent(pendingIntent1).build()
//        val notification2: Notification = NotificationCompat.Builder(this, "ScreenRecorder")
//            .setContentTitle("yNote studios")
//            .setContentText("Filming...")
//            .setContentIntent(pendingIntent1).build()
        startForeground(1, notification1)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ScreenRecorder", "Foreground notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }
}