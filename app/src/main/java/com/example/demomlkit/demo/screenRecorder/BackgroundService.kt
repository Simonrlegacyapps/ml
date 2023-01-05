package com.google.mlkit.vision.demo.screenRecorder

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.demo.record.RecordActivity

class BackgroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /*val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification =
            NotificationCompat.Builder(this, applicationContext.packageName)
                .setContentTitle("Recording Service")
                .setContentText("Running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(1, notification)
        return START_NOT_STICKY*/

        val builder = NotificationCompat.Builder(this, "CHANNEL_ID")
        val nfIntent = Intent(this, RecordActivity::class.java)

        val pIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.getActivity(this, 0, nfIntent, PendingIntent.FLAG_IMMUTABLE)
        else PendingIntent.getActivity(this, 0, nfIntent, PendingIntent.FLAG_ONE_SHOT)

        builder.setContentIntent(pIntent)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    this.resources, R.mipmap.ic_launcher
                )
            ).setSmallIcon(R.mipmap.ic_launcher)
            .setContentText("is running......")
            .setWhen(System.currentTimeMillis())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setChannelId("CHANNEL_ID")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "CHANNEL_ID",
                "CHANNEL_ID",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = builder.build()
        notification.defaults = Notification.DEFAULT_SOUND
        startForeground(110, notification)
        return START_NOT_STICKY
    }
}