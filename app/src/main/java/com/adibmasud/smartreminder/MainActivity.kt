package com.adibmasud.smartreminder

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val gold = Color.rgb(217, 180, 107)
    private val bg = Color.rgb(23, 25, 35)
    private val card = Color.rgb(34, 38, 52)
    private val white = Color.WHITE
    private val muted = Color.rgb(165, 169, 181)

    private lateinit var taskInput: EditText
    private lateinit var detailsInput: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var repeatSpinner: Spinner
    private lateinit var reminderList: LinearLayout

    private val selectedDate = Calendar.getInstance()
    private val prefs by lazy {
        getSharedPreferences("SmartReminderData", MODE_PRIVATE)
    }

    private val dateFormat =
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    private val timeFormat =
        SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        createInterface()
        updateDateTimeButtons()
        loadReminders()
    }

    private fun createInterface() {

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 25, 20, 35)

        scroll.addView(root)
        setContentView(scroll)

        val heading = TextView(this).apply {
            text = "Smart Reminder"
            textSize = 27f
            setTextColor(gold)
            typeface = Typeface.DEFAULT_BOLD
        }

        root.addView(heading)

        val subtitle = TextView(this).apply {
            text = "আপনার প্রতিদিনের কাজের সঙ্গী"
            textSize = 13f
            setTextColor(muted)
        }

        root.addView(subtitle)

        val clockCard = createCard()
        clockCard.setPadding(18, 20, 18, 20)

        val clockTitle = TextView(this).apply {
            text = "আপনার সময়"
            textSize = 14f
            setTextColor(gold)
        }

        val clock = TextView(this).apply {
            text = SimpleDateFormat(
                "hh:mm:ss a", Locale.getDefault()
            ).format(Date())
            textSize = 30f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }

        clockCard.addView(clockTitle)
        clockCard.addView(clock)
        root.addView(clockCard, marginTop(22))

        val handler = android.os.Handler(mainLooper)
        val clockUpdater = object : Runnable {
            override fun run() {
                clock.text = SimpleDateFormat(
                    "hh:mm:ss a", Locale.getDefault()
                ).format(Date())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(clockUpdater)

        addHeading(root, "নতুন রিমাইন্ডার")

        val form = createCard()
        form.setPadding(16, 18, 16, 18)

        taskInput = createInput("কাজের নাম লিখুন")
        detailsInput = createInput("কাজের বিস্তারিত (ঐচ্ছিক)")

        form.addView(label("কাজের নাম"))
        form.addView(taskInput)

        form.addView(label("বিস্তারিত"))
        form.addView(detailsInput)

        form.addView(label("তারিখ"))
        dateButton = createButton("")
        dateButton.setOnClickListener { chooseDate() }
        form.addView(dateButton, marginBottom(15))

        form.addView(label("সময়"))
        timeButton = createButton("")
        timeButton.setOnClickListener { chooseTime() }
        form.addView(timeButton, marginBottom(15))

        form.addView(label("পুনরাবৃত্তি"))

        repeatSpinner = Spinner(this)

        val repeatOptions = arrayOf(
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
        form.addView(repeatSpinner, marginBottom(18))

        val saveButton = createButton("＋  রিমাইন্ডার সংরক্ষণ করুন")
        saveButton.setOnClickListener {
            saveReminder()
        }

        form.addView(saveButton)

        root.addView(form)

        addHeading(root, "আমার রিমাইন্ডার")

        reminderList = LinearLayout(this)
        reminderList.orientation = LinearLayout.VERTICAL
        root.addView(reminderList)
    }

    private fun createCard(): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val shape = GradientDrawable()
        shape.setColor(card)
        shape.cornerRadius = 22f
        shape.setStroke(1, Color.rgb(53, 59, 75))

        layout.background = shape
        return layout
    }

    private fun label(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 8, 0, 7)
        }
    }

    private fun createInput(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 14f
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(14, 10, 14, 10)
            background = roundedBackground(
                Color.rgb(23, 27, 39)
            )
        }
    }

    private fun createButton(textValue: String): Button {
        return Button(this).apply {
            text = textValue
            textSize = 14f
            setTextColor(Color.rgb(35, 28, 20))
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBackground(gold)
        }
    }

    private fun roundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = 13f
        }
    }

    private fun addHeading(parent: LinearLayout, title: String) {
        val heading = TextView(this).apply {
            text = title
            textSize = 18f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 24, 0, 13)
        }
        parent.addView(heading)
    }

    private fun marginTop(value: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            -1, -2
        ).apply {
            topMargin = value
        }
    }

    private fun marginBottom(value: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            -1, -2
        ).apply {
            bottomMargin = value
        }
    }

    private fun updateDateTimeButtons() {
        dateButton.text = dateFormat.format(
            selectedDate.time
        )

        timeButton.text = timeFormat.format(
            selectedDate.time
        )
    }

    private fun chooseDate() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(
                    year, month, day
                )
                updateDateTimeButtons()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun chooseTime() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                selectedDate.set(
                    Calendar.HOUR_OF_DAY, hour
                )
                selectedDate.set(
                    Calendar.MINUTE, minute
                )
                selectedDate.set(
                    Calendar.SECOND, 0
                )
                updateDateTimeButtons()
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun saveReminder() {

        val title = taskInput.text.toString().trim()
        val details = detailsInput.text.toString().trim()

        if (title.isEmpty()) {
            taskInput.error = "কাজের নাম লিখুন"
            return
        }

        if (selectedDate.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(
                this,
                "ভবিষ্যতের তারিখ ও সময় নির্বাচন করুন",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (Build.VERSION.SDK_INT >= 31 &&
            !getSystemService(AlarmManager::class.java)
                .canScheduleExactAlarms()
        ) {
            Toast.makeText(
                this,
                "অ্যালার্মের অনুমতি চালু করে আবার সংরক্ষণ করুন",
                Toast.LENGTH_LONG
            ).show()

            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            )
            return
        }

        val repeat = when (repeatSpinner.selectedItemPosition) {
            1 -> "daily"
            2 -> "weekly"
            3 -> "monthly"
            else -> "none"
        }

        val id = prefs.getInt("next_id", 1)

        val reminder = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("details", details)
            put("time", selectedDate.timeInMillis)
            put("repeat", repeat)
        }

        val reminders = getSavedReminders()
        reminders.put(reminder)

        prefs.edit()
            .putString("reminders", reminders.toString())
            .putInt("next_id", id + 1)
            .apply()

        scheduleAlarm(id, title, details,
            selectedDate.timeInMillis, repeat)

        taskInput.text.clear()
        detailsInput.text.clear()

        loadReminders()

        Toast.makeText(
            this,
            "রিমাইন্ডার সংরক্ষণ করা হয়েছে",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun scheduleAlarm(
        id: Int,
        title: String,
        details: String,
        time: Long,
        repeat: String
    ) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("details", details)
            putExtra("repeat", repeat)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(
            AlarmManager::class.java
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
    }

    private fun getSavedReminders(): JSONArray {
        return try {
            JSONArray(
                prefs.getString("reminders", "[]")
            )
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun loadReminders() {
        reminderList.removeAllViews()

        val reminders = getSavedReminders()

        if (reminders.length() == 0) {
            val empty = TextView(this).apply {
                text = "এখনো কোনো রিমাইন্ডার যোগ করা হয়নি।"
                setTextColor(muted)
                gravity = Gravity.CENTER
                setPadding(10, 30, 10, 30)
            }
            reminderList.addView(empty)
            return
        }

        for (i in 0 until reminders.length()) {
            val item = reminders.getJSONObject(i)

            val id = item.getInt("id")
            val title = item.getString("title")
            val time = item.getLong("time")
            val repeat = item.optString("repeat", "none")

            val row = createCard()
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(14, 14, 14, 14)

            val info = LinearLayout(this)
            info.orientation = LinearLayout.VERTICAL

            val name = TextView(this).apply {
                text = title
                textSize = 15f
                setTextColor(white)
                typeface = Typeface.DEFAULT_BOLD
            }

            val whenText = TextView(this).apply {
                text = dateFormat.format(Date(time)) +
                    " · " + timeFormat.format(Date(time))
                textSize = 12f
                setTextColor(muted)
            }

            val repeatText = TextView(this).apply {
                text = when (repeat) {
                    "daily" -> "প্রতিদিন"
                    "weekly" -> "প্রতি সপ্তাহে"
                    "monthly" -> "প্রতি মাসে"
                    else -> "একবার"
                }
                textSize = 11f
                setTextColor(gold)
            }

            info.addView(name)
            info.addView(whenText)
            info.addView(repeatText)

            row.addView(
                info,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            val delete = Button(this).apply {
                text = "মুছুন"
                textSize = 11f
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = roundedBackground(
                    Color.rgb(110, 55, 65)
                )
                setOnClickListener {
                    deleteReminder(id)
                }
            }

            row.addView(delete)

            reminderList.addView(row, marginBottom(10))
        }
    }

    private fun deleteReminder(id: Int) {
        val reminders = getSavedReminders()
        val updated = JSONArray()

        for (i in 0 until reminders.length()) {
            val item = reminders.getJSONObject(i)

            if (item.getInt("id") != id) {
                updated.put(item)
            }
        }

        prefs.edit()
            .putString("reminders", updated.toString())
            .apply()

        val intent = Intent(this, AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent)

        loadReminders()

        Toast.makeText(
            this,
            "রিমাইন্ডার মুছে ফেলা হয়েছে",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
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
