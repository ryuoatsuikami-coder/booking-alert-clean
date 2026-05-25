package com.booking.alert

import android.app.*
import android.content.Intent
import android.os.*
import android.speech.tts.TextToSpeech
import java.util.Locale

class BookingVoiceService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra("speak_text")
        if (!text.isNullOrBlank()) {
            speakNow(text)
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "PH")
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
            ready = true

            pendingText?.let {
                speakNow(it)
                pendingText = null
            }
        }
    }

    private fun speakNow(text: String) {
        if (!ready) {
            pendingText = text
            return
        }

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "booking_voice"
        )
    }

    private fun startForegroundServiceNotification() {
        val channelId = "booking_voice_service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Booking Alert Running",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Booking Alert is running")
            .setContentText("Listening for preferred bookings")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
