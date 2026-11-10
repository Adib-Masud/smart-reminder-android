package com.adibmasud.smartreminder

import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val prefsName = "SmartReminderData"
    private val reminders = mutableListOf<JSONObject>()

    private lateinit var titleInput: EditText
    private lateinit var detailsInput: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var repeatSpinner: Spinner
    private lateinit var reminderList: LinearLayout

    private var selectedCalendar = Calendar.getInstance()

    private val brown = Color.rgb(35, 32, 30)
    private val gold = Color.rgb(212, 175, 55)
    private val white = Color.WHITE
    private val muted = Color.rgb(180, 175, 165)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = brown
        window.navigationBarColor = brown
        window.decorView.setBackgroundColor(brown)

        loadReminders()
        buildInterface()
        requestNotificationPermission()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 20)
        }

        val scroll = ScrollView(this)
        scroll.addView(root)

        val heading = TextView(this).apply {
            text = "SMART REMINDER"
            textSize = 25f
            setTextColor(gold)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 5)
        }
        root.addView(heading)

        val subtitle = TextView(this).apply {
            text = "আপনার সময়, আপনার নিয়ন্ত্রণে"
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(subtitle)

        addSectionTitle(root, "নতুন রিমাইন্ডার")

        titleInput = createInput("রিমাইন্ডারের শিরোনাম")
        root.addView(titleInput)

        detailsInput = createInput("বিস্তারিত লিখুন (ঐচ্ছিক)").apply {
            minLines = 2
            gravity = Gravity.TOP
        }
        root.addView(detailsInput)

        dateButton = createButton("তারিখ নির্বাচন করুন")
        dateButton.setOnClickListener {
            showDatePicker()
        }
        root.addView(dateButton)

        timeButton = createButton("সময় নির্বাচন করুন")
        timeButton.setOnClickListener {
            showTimePicker()
        }
        root.addView(timeButton)

        val repeatLabel = TextView(this).apply {
            text = "পুনরাবৃত্তি"
            textSize = 14f
            setTextColor(gold)
            setPadding(4, 14, 4, 6)
        }
        root.addView(repeatLabel)

        repeatSpinner = Spinner(this)
        val repeatOptions = listOf(
            "একবার",
            "প্রতিদিন",
            "প্রতি সপ্তাহে",
            "প্রতি মাসে"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            repeatOptions
        )
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        repeatSpinner.adapter = adapter
        root.addView(repeatSpinner)

        val saveButton = createButton("রিমাইন্ডার সংরক্ষণ করুন")
        saveButton.setBackgroundColor(gold)
        saveButton.setTextColor(Color.BLACK)
        saveButton.setOnClickListener {
            saveReminder()
        }
        root.addView(saveButton)

        addSectionTitle(root, "সংরক্ষিত রিমাইন্ডার")

        reminderList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(reminderList)

        val footer = TextView(this).apply {
            text = "SMART REMINDER  •  OFFLINE"
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 10)
        }
        root.addView(footer)

        setContentView(scroll)

        updateDateButton()
        updateTimeButton()
        refreshReminderList()
    }

    private fun addSectionTitle(parent: LinearLayout, text: String) {
        val view = TextView(this).apply {
            this.text = text
            textSize = 17f
            setTextColor(gold)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 18, 0, 12)
        }
        parent.addView(view)
    }

    private fun createInput(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 15f
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(15, 12, 15, 12)
            background = roundedBackground(
                Color.rgb(55, 50, 46),
                12
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }
    }

    private fun createButton(textValue: String): Button {
        return Button(this).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(white)
            background = roundedBackground(
                Color.rgb(75, 62, 48),
                12
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
                topMargin = 5
            }
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedCalendar.set(
                    Calendar.YEAR,
                    year
                )
                selectedCalendar.set(
                    Calendar.MONTH,
                    month
                )
                selectedCalendar.set(
                    Calendar.DAY_OF_MONTH,
                    day
                )
                updateDateButton()
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                selectedCalendar.set(
                    Calendar.HOUR_OF_DAY,
                    hour
                )
                selectedCalendar.set(
                    Calendar.MINUTE,
                    minute
                )
                selectedCalendar.set(
                    Calendar.SECOND,
                    0
                )
                updateTimeButton()
            },
            selectedCalendar.get(Calendar.HOUR_OF_DAY),
            selectedCalendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateDateButton() {
        val format = SimpleDateFormat(
            "dd MMMM yyyy",
            Locale.getDefault()
        )
        dateButton.text = format.format(
            selectedCalendar.time
        )
    }

    private fun updateTimeButton() {
        val format = SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        )
        timeButton.text = format.format(
            selectedCalendar.time
        )
    }

    private fun saveReminder() {
        val title = titleInput.text.toString().trim()

        if (title.isEmpty()) {
            titleInput.error = "শিরোনাম লিখুন"
            return
        }

        val alarmTime = selectedCalendar.timeInMillis

        if (alarmTime <= System.currentTimeMillis()) {
            Toast.makeText(
                this,
                "ভবিষ্যতের তারিখ ও সময় নির্বাচন করুন",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val repeat = when (repeatSpinner.selectedItemPosition) {
            1 -> "daily"
            2 -> "weekly"
            3 -> "monthly"
            else -> "once"
        }

        val reminder = JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("title", title)
            put("details", detailsInput.text.toString().trim())
            put("time", alarmTime)
            put("repeat", repeat)
        }

        reminders.add(reminder)
        saveRemindersToStorage()
        scheduleAlarm(reminder)

        titleInput.text.clear()
        detailsInput.text.clear()

        selectedCalendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
        }

        updateDateButton()
        updateTimeButton()
        repeatSpinner.setSelection(0)
        refreshReminderList()

        Toast.makeText(
            this,
            "রিমাইন্ডার সংরক্ষণ করা হয়েছে",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun scheduleAlarm(reminder: JSONObject) {
        val alarmManager = getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val id = reminder.optLong("id")
        val title = reminder.optString("title")
        val details = reminder.optString("details")
        val time = reminder.optLong("time")

        val intent = Intent(
            this,
            AlarmReceiver::class.java
        ).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("details", details)
            putExtra("repeat", reminder.optString("repeat"))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    )
                )
                Toast.makeText(
                    this,
                    "নির্ভুল অ্যালার্মের অনুমতি চালু করুন",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )

        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "অ্যালার্ম সেট করা যায়নি। অনুমতি পরীক্ষা করুন।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun loadReminders() {
        reminders.clear()

        val json = getSharedPreferences(
            prefsName,
            Context.MODE_PRIVATE
        ).getString("reminders", "[]")

        try {
            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                reminders.add(array.getJSONObject(i))
            }
        } catch (_: Exception) {
            reminders.clear()
        }
    }

    private fun saveRemindersToStorage() {
        val array = JSONArray()

        reminders.forEach {
            array.put(it)
        }

        getSharedPreferences(
            prefsName,
            Context.MODE_PRIVATE
        ).edit()
            .putString("reminders", array.toString())
            .apply()
    }

    private fun refreshReminderList() {
        reminderList.removeAllViews()

        if (reminders.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "এখনো কোনো রিমাইন্ডার সংরক্ষণ করা হয়নি"
                setTextColor(muted)
                textSize = 14f
                setPadding(5, 12, 5, 12)
            }
            reminderList.addView(emptyText)
            return
        }

        reminders.sortedBy {
            it.optLong("time")
        }.forEach { reminder ->

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(15, 13, 15, 13)
                background = roundedBackground(
                    Color.rgb(55, 50, 46),
                    12
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
            }

            val title = TextView(this).apply {
                text = reminder.optString("title")
                textSize = 17f
                setTextColor(gold)
                typeface = Typeface.DEFAULT_BOLD
            }
            card.addView(title)

            val timeFormat = SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            )

            val timeText = TextView(this).apply {
                text = timeFormat.format(
                    Date(reminder.optLong("time"))
                )
                textSize = 13f
                setTextColor(white)
                setPadding(0, 5, 0, 5)
            }
            card.addView(timeText)

            val repeatText = when (
                reminder.optString("repeat")
            ) {
                "daily" -> "প্রতিদিন"
                "weekly" -> "প্রতি সপ্তাহে"
                "monthly" -> "প্রতি মাসে"
                else -> "একবার"
            }

            val repeatView = TextView(this).apply {
                text = "পুনরাবৃত্তি: $repeatText"
                textSize = 12f
                setTextColor(muted)
            }
            card.addView(repeatView)

            val deleteButton = Button(this).apply {
                text = "মুছে ফেলুন"
                isAllCaps = false
                setTextColor(white)
                setOnClickListener {
                    deleteReminder(
                        reminder.optLong("id")
                    )
                }
            }
            card.addView(deleteButton)

            reminderList.addView(card)
        }
    }

    private fun deleteReminder(id: Long) {
        val alarmManager = getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val intent = Intent(
            this,
            AlarmReceiver::class.java
        )

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        reminders.removeAll {
            it.optLong("id") == id
        }

        saveRemindersToStorage()
        refreshReminderList()

        Toast.makeText(
            this,
            "রিমাইন্ডার মুছে ফেলা হয়েছে",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ),
                    100
                )
            }
        }
    }
}
