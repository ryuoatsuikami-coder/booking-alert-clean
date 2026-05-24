package com.booking.alert

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class BookingNotificationListener : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate() {
        super.onCreate()

        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.language = Locale.ENGLISH
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.0f)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageNameText = sbn.packageName ?: ""
        if (!packageNameText.lowercase().contains("lalamove")) return

        val title = sbn.notification.extras.getString("android.title") ?: ""
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = sbn.notification.extras.getCharSequence("android.bigText")?.toString() ?: ""
        val fullText = "$title $text $bigText"

        val fare = extractFare(fullText)
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val minFare = prefs.getInt("min_fare", 200)

        if (fare == null || fare < minFare) return

        val route = extractRoute(fullText) ?: return
        if (!isPreferredLocation(route.first) || !isPreferredLocation(route.second)) return

        val speechText = "${route.first} to ${route.second}. ${fare.toInt()} pesos."

        vibrateAlert()
        speakNow(speechText)

        Handler(Looper.getMainLooper()).postDelayed({
            openLalamove(sbn)
        }, 2200)
    }

    private fun extractFare(text: String): Double? {
        val regex = Regex("""₱\s*([0-9,]+(?:\.\d{1,2})?)""")
        val match = regex.find(text) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private fun extractRoute(text: String): Pair<String, String>? {
        val cleaned = text
            .replace("[Delivery]", "", ignoreCase = true)
            .replace("immediate", "", ignoreCase = true)
            .replace("Hurry up!", "", ignoreCase = true)
            .replace(Regex("""₱\s*[0-9,]+(?:\.\d{1,2})?"""), "")
            .trim()

        val parts = cleaned.split(">").map { it.trim() }

        return if (parts.size >= 2) {
            Pair(parts[0].take(45), parts[1].take(45))
        } else {
            null
        }
    }

    private fun isPreferredLocation(locationText: String): Boolean {
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val locations = prefs.getStringSet("locations", emptySet()) ?: emptySet()

        val text = locationText.lowercase()

        for (location in locations) {
            val enabled = prefs.getBoolean("loc_$location", true)
            if (enabled && text.contains(location.lowercase())) {
                return true
            }
        }

        return false
    }

    private fun speakNow(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "booking_alert")
        }
    }

    private fun vibrateAlert() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun openLalamove(sbn: StatusBarNotification) {
        try {
            val pendingIntent: PendingIntent? = sbn.notification.contentIntent
            pendingIntent?.send()
        } catch (e: Exception) {
            try {
                val launchIntent: Intent? =
                    packageManager.getLaunchIntentForPackage(sbn.packageName)

                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
