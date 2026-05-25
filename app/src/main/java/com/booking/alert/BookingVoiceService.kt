package com.booking.alert

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.os.*
import android.speech.tts.TextToSpeech
import java.util.Locale

class BookingVoiceService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        tts = TextToSpeech(applicationContext, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()

        val text = intent?.getStringExtra("speak_text")
        if (!text.isNullOrBlank()) {
            pendingText = text
            speakNow(text)
        }

        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            tts?.language = Locale("en", "PH")
            tts?.setSpeechRate(0.95f)
            tts?.setPitch(1.0f)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            }

            pendingText?.let {
                speakNow(it)
            }
        }
    }

    private fun speakNow(text: String) {
        if (!ready || tts == null) {
            pendingText = text
            return
        }

        tts?.stop()

        Handler(Looper.getMainLooper()).postDelayed({
            tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "booking_voice_${System.currentTimeMillis()}"
            )
        }, 250)
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
            .setContentText("Voice alert is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
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
