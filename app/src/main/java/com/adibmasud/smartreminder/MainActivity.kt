package com.adibmasud.smartreminder

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
    private lateinit var permissionStatus: TextView

    private var selectedCalendar = Calendar.getInstance()
    private var continueSetupAfterSettings = false

    private val brown = Color.rgb(35, 32, 30)
    private val gold = Color.rgb(212, 175, 55)
    private val white = Color.WHITE
    private val muted = Color.rgb(180, 175, 165)
    private val cardColor = Color.rgb(55, 50, 46)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = brown
        window.navigationBarColor = brown
        window.decorView.setBackgroundColor(brown)

        loadReminders()
        buildInterface()
        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()

        if (::permissionStatus.isInitialized) {
            updatePermissionStatus()
        }

        if (continueSetupAfterSettings) {
            continueSetupAfterSettings = false
            startPermissionSetup()
        }

        schedulePendingReminders()
    }

    private fun buildInterface() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 34, 20, 20)
        }

        val scroll = ScrollView(this)
        scroll.addView(root)

        val heading = TextView(this).apply {
            text = "SMART REMINDER"
            textSize = 25f
            setTextColor(gold)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 14, 0, 5)
        }

        root.addView(heading)

        val subtitle = TextView(this).apply {
            text = "আপনার সময়, আপনার নিয়ন্ত্রণে"
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        root.addView(subtitle)

        addPermissionCard(root)

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

        val saveButton = createButton(
            "রিমাইন্ডার সংরক্ষণ করুন"
        )

        saveButton.background = roundedBackground(gold, 12)
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
        updatePermissionStatus()
    }

    private fun addPermissionCard(parent: LinearLayout) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15, 13, 15, 13)
            background = roundedBackground(cardColor, 14)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }

        val heading = TextView(this).apply {
            text = "অ্যালার্ম সেটআপ"
            textSize = 17f
            setTextColor(gold)
            typeface = Typeface.DEFAULT_BOLD
        }

        card.addView(heading)

        permissionStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(white)
            setLineSpacing(5f, 1f)
            setPadding(0, 9, 0, 12)
        }

        card.addView(permissionStatus)

        val setupButton = createButton(
            "অনুমতি যাচাই ও চালু করুন"
        )

        setupButton.background = roundedBackground(
            Color.rgb(75, 62, 48),
            10
        )

        setupButton.setOnClickListener {
            startPermissionSetup()
        }

        card.addView(setupButton)

        parent.addView(card)
    }

    private fun updatePermissionStatus() {

        if (!::permissionStatus.isInitialized) return

        val notificationGranted =
            Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        val alarmGranted =
            Build.VERSION.SDK_INT < 31 ||
                getSystemService(AlarmManager::class.java)
                    .canScheduleExactAlarms()

        val fullScreenGranted =
            Build.VERSION.SDK_INT < 34 ||
                getSystemService(NotificationManager::class.java)
                    .canUseFullScreenIntent()

        fun status(granted: Boolean): String {
            return if (granted) "✓ চালু" else "✕ বন্ধ"
        }

        permissionStatus.text =
            "নোটিফিকেশন: ${status(notificationGranted)}\n" +
            "নির্ভুল অ্যালার্ম: ${status(alarmGranted)}\n" +
            "ফুল স্ক্রিন অ্যালার্ম: ${status(fullScreenGranted)}"
    }

    private fun startPermissionSetup() {

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ),
                100
            )

            return
        }

        val alarmManager = getSystemService(
            AlarmManager::class.java
        )

        if (
            Build.VERSION.SDK_INT >= 31 &&
            !alarmManager.canScheduleExactAlarms()
        ) {

            showSettingsDialog(
                "নির্ভুল অ্যালার্ম অনুমতি প্রয়োজন",
                "সময়মতো অ্যালার্ম বাজানোর জন্য Alarms & reminders অনুমতি চালু করুন।",
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                )
            )

            return
        }

        if (
            Build.VERSION.SDK_INT >= 34 &&
            !getSystemService(NotificationManager::class.java)
                .canUseFullScreenIntent()
        ) {

            showSettingsDialog(
                "ফুল স্ক্রিন অ্যালার্ম অনুমতি প্রয়োজন",
                "স্ক্রিন বন্ধ বা লক থাকা অবস্থায় অ্যালার্মের আলাদা স্ক্রিন দেখানোর জন্য এই অনুমতি চালু করুন।",
                Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:$packageName")
                )
            )

            return
        }

        updatePermissionStatus()

        Toast.makeText(
            this,
            "সব প্রয়োজনীয় অনুমতি চালু আছে",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showSettingsDialog(
        title: String,
        message: String,
        settingsIntent: Intent
    ) {

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("সেটিংস খুলুন") { _, _ ->

                try {
                    continueSetupAfterSettings = true
                    startActivity(settingsIntent)
                } catch (_: Exception) {
                    continueSetupAfterSettings = false

                    Toast.makeText(
                        this,
                        "সেটিংস খোলা যায়নি",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("পরে করব", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100) {

            updatePermissionStatus()

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startPermissionSetup()
            }
        }
    }

    private fun addSectionTitle(
        parent: LinearLayout,
        text: String
    ) {

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

            background = roundedBackground(cardColor, 12)

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

                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, day)

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

                selectedCalendar.set(Calendar.HOUR_OF_DAY, hour)
                selectedCalendar.set(Calendar.MINUTE, minute)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)

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

        dateButton.text = format.format(selectedCalendar.time)
    }

    private fun updateTimeButton() {

        val format = SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        )

        timeButton.text = format.format(selectedCalendar.time)
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

        val scheduled = scheduleAlarm(reminder)

        titleInput.text.clear()
        detailsInput.text.clear()

        selectedCalendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
        }

        updateDateButton()
        updateTimeButton()
        repeatSpinner.setSelection(0)
        refreshReminderList()

        if (scheduled) {

            Toast.makeText(
                this,
                "রিমাইন্ডার সংরক্ষণ করা হয়েছে",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            Toast.makeText(
                this,
                "রিমাইন্ডার সংরক্ষিত। অ্যালার্ম চালু করতে অনুমতি দিন।",
                Toast.LENGTH_LONG
            ).show()

            startPermissionSetup()
        }
    }

    private fun scheduleAlarm(reminder: JSONObject): Boolean {

        val alarmManager = getSystemService(
            AlarmManager::class.java
        )

        if (
            Build.VERSION.SDK_INT >= 31 &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            return false
        }

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

        return try {

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pendingIntent
            )

            true

        } catch (_: SecurityException) {
            false
        }
    }

    private fun schedulePendingReminders() {

        if (Build.VERSION.SDK_INT >= 31) {

            val alarmManager = getSystemService(
                AlarmManager::class.java
            )

            if (!alarmManager.canScheduleExactAlarms()) {
                return
            }
        }

        val now = System.currentTimeMillis()

        reminders.forEach { reminder ->

            if (reminder.optLong("time") > now) {
                scheduleAlarm(reminder)
            }
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

                background = roundedBackground(cardColor, 12)

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
                    deleteReminder(reminder.optLong("id"))
                }
            }

            card.addView(deleteButton)
            reminderList.addView(card)
        }
    }

    private fun deleteReminder(id: Long) {

        val alarmManager = getSystemService(
            AlarmManager::class.java
        )

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

            if (
                checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
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
