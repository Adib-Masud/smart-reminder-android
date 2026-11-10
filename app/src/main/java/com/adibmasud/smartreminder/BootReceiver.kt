package com.adibmasud.smartreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val prefs = context.getSharedPreferences(
            "SmartReminderData",
            Context.MODE_PRIVATE
        )

        val reminders = try {
            JSONArray(prefs.getString("reminders", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }

        val alarmManager = context.getSystemService(
            AlarmManager::class.java
        )

        for (i in 0 until reminders.length()) {

            try {
                val item = reminders.getJSONObject(i)

                val id = item.getInt("id")
                val title = item.getString("title")
                val details = item.optString("details", "")
                val repeat = item.optString("repeat", "none")
                val savedTime = item.getLong("time")

                val nextTime = calculateNextTime(
                    savedTime,
                    repeat
                ) ?: continue

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

                if (Build.VERSION.SDK_INT < 31 ||
                    alarmManager.canScheduleExactAlarms()
                ) {

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )

                } else {

                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateNextTime(
        savedTime: Long,
        repeat: String
    ): Long? {

        val now = System.currentTimeMillis()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = savedTime

        if (savedTime > now) {
            return savedTime
        }

        when (repeat) {

            "none" -> return null

            "daily" -> {
                val days = (
                    (now - savedTime) / 86400000L
                ) + 1

                calendar.add(
                    Calendar.DAY_OF_MONTH,
                    days.toInt()
                )
            }

            "weekly" -> {
                val weeks = (
                    (now - savedTime) / 604800000L
                ) + 1

                calendar.add(
                    Calendar.WEEK_OF_YEAR,
                    weeks.toInt()
                )
            }

            "monthly" -> {

                val originalDay = calendar.get(
                    Calendar.DAY_OF_MONTH
                )

                while (calendar.timeInMillis <= now) {

                    calendar.set(Calendar.DAY_OF_MONTH, 1)

                    calendar.add(
                        Calendar.MONTH,
                        1
                    )

                    val maxDay = calendar.getActualMaximum(
                        Calendar.DAY_OF_MONTH
                    )

                    calendar.set(
                        Calendar.DAY_OF_MONTH,
                        minOf(originalDay, maxDay)
                    )
                }
            }

            else -> return null
        }

        return calendar.timeInMillis
    }
}
