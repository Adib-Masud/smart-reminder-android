package com.adibmasud.smartreminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "reminder_alarm"

        const val ACTION_SNOOZE =
            "com.adibmasud.smartreminder.SNOOZE"

        const val ACTION_STOP =
            "com.adibmasud.smartreminder.STOP"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getLongExtra("id", 0L)
        val title = intent.getStringExtra("title")
            ?: "Smart Reminder"
        val details = intent.getStringExtra("details") ?: ""
        val repeat = intent.getStringExtra("repeat") ?: "once"

        if (id == 0L) return

        when (intent.action) {

            ACTION_STOP -> {
                cancelNotification(context, id)
                return
            }

            ACTION_SNOOZE -> {
                scheduleSnooze(
                    context,
                    id,
                    title,
                    details
                )
                cancelNotification(context, id)
                return
            }
        }

        createNotificationChannel(context)

        val alarmIntent = Intent(
            context,
            AlarmActivity::class.java
        ).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("details", details)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }

        val fullScreenPendingIntent =
            PendingIntent.getActivity(
                context,
                id.toInt(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_lock_idle_alarm
            )
            .setContentTitle("⏰ $title")
            .setContentText(
                if (details.isNotBlank()) {
                    details
                } else {
                    "আপনার নির্ধারিত কাজের সময় হয়েছে।"
                }
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        if (details.isNotBlank()) {
                            details
                        } else {
                            "আপনার নির্ধারিত কাজের সময় হয়েছে।"
                        }
                    )
            )
            .setPriority(
                NotificationCompat.PRIORITY_MAX
            )
            .setCategory(
                NotificationCompat.CATEGORY_ALARM
            )
            .setVisibility(
                NotificationCompat.VISIBILITY_PUBLIC
            )
            .setFullScreenIntent(
                fullScreenPendingIntent,
                true
            )
            .setContentIntent(
                fullScreenPendingIntent
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        val manager = context.getSystemService(
            NotificationManager::class.java
        )

        manager.notify(id.toInt(), notification)

        if (repeat != "once" && repeat != "none") {
            scheduleNextAlarm(
                context,
                id,
                title,
                details,
                repeat
            )
        }
    }

    private fun createNotificationChannel(
        context: Context
    ) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminder Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Smart Reminder alarm notifications"

                enableVibration(true)

                // অ্যালার্মের শব্দ AlarmActivity বাজাবে।
                setSound(null, null)

                lockscreenVisibility =
                    android.app.Notification.VISIBILITY_PUBLIC
            }

            val manager = context.getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleNextAlarm(
        context: Context,
        id: Long,
        title: String,
        details: String,
        repeat: String
    ) {

        val next = Calendar.getInstance()

        when (repeat) {

            "daily" -> {
                next.add(Calendar.DAY_OF_MONTH, 1)
            }

            "weekly" -> {
                next.add(Calendar.WEEK_OF_YEAR, 1)
            }

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
            id.toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(
            AlarmManager::class.java
        )

        try {
            if (
                Build.VERSION.SDK_INT < 31 ||
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

        } catch (_: SecurityException) {
            // অনুমতি না থাকলে অ্যাপ ক্র্যাশ করবে না।
        }
    }

    private fun scheduleSnooze(
        context: Context,
        id: Long,
        title: String,
        details: String
    ) {

        val alarmManager = context.getSystemService(
            AlarmManager::class.java
        )

        val snoozeIntent = Intent(
            context,
            AlarmReceiver::class.java
        ).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("details", details)
            putExtra("repeat", "once")
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt() xor 0x40000000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime =
            System.currentTimeMillis() + 5 * 60 * 1000L

        try {
            if (
                Build.VERSION.SDK_INT < 31 ||
                alarmManager.canScheduleExactAlarms()
            ) {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    snoozePendingIntent
                )

            } else {

                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    snoozePendingIntent
                )
            }

        } catch (_: SecurityException) {
            // অনুমতি না থাকলে ক্র্যাশ প্রতিরোধ।
        }
    }

    private fun cancelNotification(
        context: Context,
        id: Long
    ) {

        val manager = context.getSystemService(
            NotificationManager::class.java
        )

        manager.cancel(id.toInt())
    }
}
