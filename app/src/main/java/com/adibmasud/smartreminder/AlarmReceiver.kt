package com.adibmasud.smartreminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title") ?: "Smart Reminder"
        val details = intent.getStringExtra("details") ?: ""
        val repeat = intent.getStringExtra("repeat") ?: "none"

        createNotificationChannel(context)

        val openIntent = Intent(
            context,
            MainActivity::class.java
        )

        val openPendingIntent = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            "reminder_alarm"
        )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(
                if (details.isNotBlank()) details
                else "আপনার নির্ধারিত কাজের সময় হয়েছে।"
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        if (details.isNotBlank()) details
                        else "আপনার নির্ধারিত কাজের সময় হয়েছে।"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = context.getSystemService(
            NotificationManager::class.java
        )

        manager.notify(id, notification)

        if (repeat != "none") {
            scheduleNextAlarm(
                context,
                id,
                title,
                details,
                repeat
            )
        }
    }

    private fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val soundUri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                "reminder_alarm",
                "Reminder Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart Reminder alarm notifications"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            context.getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }
    }

    private fun scheduleNextAlarm(
        context: Context,
        id: Int,
        title: String,
        details: String,
        repeat: String
    ) {

        val next = Calendar.getInstance()

        when (repeat) {
            "daily" -> next.add(Calendar.DAY_OF_MONTH, 1)

            "weekly" -> next.add(Calendar.WEEK_OF_YEAR, 1)

            "monthly" -> {
                val day = next.get(Calendar.DAY_OF_MONTH)
                next.set(Calendar.DAY_OF_MONTH, 1)
                next.add(Calendar.MONTH, 1)

                val maxDay = next.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )

                next.set(
                    Calendar.DAY_OF_MONTH,
                    minOf(day, maxDay)
                )
            }

            else -> return
        }

        val alarmIntent = Intent(
            context,
            AlarmReceiver::class.java
        ).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("details", details)
            putExtra("repeat", repeat)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(
            AlarmManager::class.java
        )

        if (Build.VERSION.SDK_INT < 31 ||
            alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.timeInMillis,
                pendingIntent
            )
        }
    }
}
