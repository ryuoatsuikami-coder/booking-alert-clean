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
            if (shouldSpeakBooking(text)) {
                val spokenText = buildMatchedSpeech(text)
                pendingText = spokenText
                speakNow(spokenText)
            }
        }

        return START_STICKY
    }

    private fun shouldSpeakBooking(text: String): Boolean {
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)

        val speakOnlyMatched = prefs.getBoolean("speak_only_matched", true)
        if (!speakOnlyMatched) return true

        val minFare = prefs.getInt("min_fare", 200)
        val fare = extractFare(text)

        if (fare < minFare) return false

        val routes = prefs.getStringSet("routes", emptySet()) ?: emptySet()

        for (route in routes) {
            val enabled = prefs.getBoolean("route_$route", true)
            if (!enabled) continue

            val parts = route.split(">")
            if (parts.size != 2) continue

            val pickup = parts[0].trim()
            val dropoff = parts[1].trim()

            val pickupMatched = text.contains(pickup, ignoreCase = true)
            val dropoffMatched = text.contains(dropoff, ignoreCase = true)

            if (pickupMatched && dropoffMatched) {
                return true
            }
        }

        return false
    }

    private fun extractFare(text: String): Int {
        val patterns = listOf(
            Regex("₱\\s*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("PHP\\s*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("(\\d+)\\s*pesos", RegexOption.IGNORE_CASE),
            Regex("fare\\s*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("fare\\s*₱?\\s*(\\d+)", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].toIntOrNull() ?: 0
            }
        }

        return 0
    }

    private fun buildMatchedSpeech(text: String): String {
        return "Pasok sa preferred route. $text. Pwede itong i-consider."
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
            .setContentText("Only matched preferred-route bookings will be spoken")
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
