package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import java.nio.charset.StandardCharsets

class ContinueWatchingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            val sharedPreferences = applicationContext.getSharedPreferences("StudyMemory", Context.MODE_PRIVATE)
            val historyString = sharedPreferences.getString("history", "") ?: ""
            if (historyString.isBlank()) return Result.success()

            val lastUri = if (historyString.startsWith("[")) {
                try {
                    val jsonArray = org.json.JSONArray(historyString)
                    if (jsonArray.length() > 0) jsonArray.getString(0) else return Result.success()
                } catch (e: Exception) {
                    historyString.split(",").firstOrNull { it.isNotEmpty() } ?: return Result.success()
                }
            } else {
                historyString.split(",").firstOrNull { it.isNotEmpty() } ?: return Result.success()
            }

            val pos = sharedPreferences.getLong("video_$lastUri", 0L)
            if (pos <= 5000L) return Result.success() // Less than 5 seconds played

            val encodedUri = Base64.encodeToString(lastUri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = android.net.Uri.parse("aether://player/$encodedUri?startPosition=$pos")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "continue_watching_channel"
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Continue Watching",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders to finish watching your videos"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Continue Watching")
                .setContentText("Pick up where you left off in your last video.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
