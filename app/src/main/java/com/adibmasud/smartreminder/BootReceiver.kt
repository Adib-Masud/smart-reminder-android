
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
            JSONArray(
                prefs.getString("reminders", "[]")
            )
        } catch (_: Exception) {
            JSONArray()
        }

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        for (i in 0 until reminders.length()) {

            try {
                val item = reminders.getJSONObject(i)

                val id = item.optLong("id", 0L)
                val title = item.optString(
                    "title",
                    "Smart Reminder"
                )
                val details = item.optString("details", "")
                val repeat = item.optString("repeat", "once")
                val savedTime = item.optLong("time", 0L)

                if (id == 0L || savedTime <= 0L) {
                    continue
                }

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
                    id.toInt(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
                )

                try {

                    if (
                        Build.VERSION.SDK_INT < 31 ||
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

                } catch (_: SecurityException) {
                    // অনুমতি না থাকলে অ্যাপ ক্র্যাশ করবে না।
                }

            } catch (_: Exception) {
                // একটি রিমাইন্ডারে সমস্যা হলেও অন্যগুলো সেট হবে।
            }
        }
    }

    private fun calculateNextTime(
        savedTime: Long,
        repeat: String
    ): Long? {

        val now = System.currentTimeMillis()

        if (savedTime > now) {
            return savedTime
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = savedTime
        }

        when (repeat) {

            "once", "none" -> {
                return null
            }

            "daily" -> {

                while (calendar.timeInMillis <= now) {
                    calendar.add(
                        Calendar.DAY_OF_MONTH,
                        1
                    )
                }
            }

            "weekly" -> {

                while (calendar.timeInMillis <= now) {
                    calendar.add(
                        Calendar.WEEK_OF_YEAR,
                        1
                    )
                }
            }

            "monthly" -> {

                val originalDay = calendar.get(
                    Calendar.DAY_OF_MONTH
                )

                while (calendar.timeInMillis <= now) {

                    calendar.set(
                        Calendar.DAY_OF_MONTH,
                        1
                    )

                    calendar.add(
                        Calendar.MONTH,
                        1
                    )

                    val maximumDay =
                        calendar.getActualMaximum(
                            Calendar.DAY_OF_MONTH
                        )

                    calendar.set(
                        Calendar.DAY_OF_MONTH,
                        minOf(originalDay, maximumDay)
                    )
                }
            }

            else -> {
                return null
            }
        }

        return calendar.timeInMillis
    }
}
