package com.adibmasud.smartreminder

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AlarmActivity : Activity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private var reminderId: Long = 0L
    private var reminderTitle: String = "Smart Reminder"
    private var reminderDetails: String = ""

    private val backgroundColor =
        Color.rgb(35, 32, 30)

    private val goldColor =
        Color.rgb(212, 175, 55)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        reminderId = intent.getLongExtra("id", 0L)
        reminderTitle = intent.getStringExtra("title")
            ?: "Smart Reminder"
        reminderDetails = intent.getStringExtra("details") ?: ""

        buildAlarmScreen()
        startAlarmSound()
        startVibration()
    }

    private fun buildAlarmScreen() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 30, 28, 30)
            setBackgroundColor(backgroundColor)
        }

        val icon = TextView(this).apply {
            text = "⏰"
            textSize = 65f
            gravity = Gravity.CENTER
        }

        root.addView(icon)

        val heading = TextView(this).apply {
            text = "SMART REMINDER"
            textSize = 18f
            setTextColor(goldColor)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 35)
        }

        root.addView(heading)

        val title = TextView(this).apply {
            text = reminderTitle
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        root.addView(title)

        if (reminderDetails.isNotBlank()) {

            val details = TextView(this).apply {
                text = reminderDetails
                textSize = 16f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(0, 15, 0, 20)
            }

            root.addView(details)
        }

        val message = TextView(this).apply {
            text = "আপনার নির্ধারিত কাজের সময় হয়েছে"
            textSize = 15f
            setTextColor(goldColor)
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 45)
        }

        root.addView(message)

        val snoozeButton = createButton(
            "৫ মিনিট পরে আবার বাজবে",
            Color.rgb(75, 62, 48),
            Color.WHITE
        )

        snoozeButton.setOnClickListener {
            stopAlarmSound()
            stopVibration()

            val snoozeIntent = Intent(
                this,
                AlarmReceiver::class.java
            ).apply {
                action = AlarmReceiver.ACTION_SNOOZE
                putExtra("id", reminderId)
                putExtra("title", reminderTitle)
                putExtra("details", reminderDetails)
            }

            sendBroadcast(snoozeIntent)
            finish()
        }

        root.addView(snoozeButton)

        val stopButton = createButton(
            "অ্যালার্ম বন্ধ করুন",
            goldColor,
            Color.BLACK
        )

        stopButton.setOnClickListener {
            stopAlarmSound()
            stopVibration()

            val stopIntent = Intent(
                this,
                AlarmReceiver::class.java
            ).apply {
                action = AlarmReceiver.ACTION_STOP
                putExtra("id", reminderId)
            }

            sendBroadcast(stopIntent)
            finish()
        }

        root.addView(stopButton)

        setContentView(root)
    }

    private fun createButton(
        text: String,
        background: Int,
        foreground: Int
    ): Button {

        return Button(this).apply {
            this.text = text
            textSize = 16f
            isAllCaps = false
            setTextColor(foreground)

            background = GradientDrawable().apply {
                setColor(background)
                cornerRadius = 18f
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 15
                topMargin = 8
            }

            minHeight = 60
        }
    }

    private fun startAlarmSound() {

        try {

            val soundUri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )

            ringtone = RingtoneManager.getRingtone(
                this,
                soundUri
            )

            ringtone?.let {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    it.isLooping = true
                }

                it.play()
            }

        } catch (_: Exception) {
            ringtone = null
        }
    }

    private fun startVibration() {

        try {

            vibrator = getSystemService(
                Context.VIBRATOR_SERVICE
            ) as Vibrator

            val pattern = longArrayOf(
                0, 500, 300, 500, 300
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        pattern,
                        0
                    )
                )

            } else {

                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }

        } catch (_: Exception) {
            vibrator = null
        }
    }

    private fun stopAlarmSound() {
        try {
            ringtone?.stop()
            ringtone = null
        } catch (_: Exception) {
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        stopAlarmSound()
        stopVibration()
        super.onDestroy()
    }
}
