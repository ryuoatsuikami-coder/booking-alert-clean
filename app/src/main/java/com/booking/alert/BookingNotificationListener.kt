package com.booking.alert

import android.app.*
import android.content.*
import android.os.*
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class BookingNotificationListener : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null

    override fun onCreate() {
        super.onCreate()

        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.language = Locale("en", "PH")
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.0f)

                pendingSpeech?.let {
                    speakNow(it)
                    pendingSpeech = null
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (!sbn.packageName.lowercase().contains("lalamove")) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val fullText = "$title $text $bigText"

        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val minFare = prefs.getInt("min_fare", 200)
        val fare = extractFare(fullText)

        if (fare < minFare) return

        val route = extractRoute(fullText) ?: return
        if (!isPreferred(route.first) || !isPreferred(route.second)) return

        val speechText = "${route.first} to ${route.second}. Fare $fare pesos."

        vibrate()
        speakNow(speechText)

        Handler(Looper.getMainLooper()).postDelayed({
            openLalamove(sbn)
        }, 1800)
    }

    private fun extractFare(text: String): Int {
        val regex = Regex("""(?:₱|php|p)\s?([0-9,]+)(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE)
        val match = regex.find(text) ?: return 0
        return match.groupValues[1].replace(",", "").toIntOrNull() ?: 0
    }

    private fun extractRoute(text: String): Pair<String, String>? {
        val cleaned = text
            .replace("[Delivery]", "", ignoreCase = true)
            .replace("immediate", "", ignoreCase = true)
            .replace("Hurry up!", "", ignoreCase = true)
            .replace(Regex("""(?:₱|php|p)\s?[0-9,]+(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE), "")
            .trim()

        val parts = cleaned.split(">").map { it.trim() }
        if (parts.size < 2) return null

        return Pair(parts[0].take(45), parts[1].take(45))
    }

    private fun isPreferred(locationText: String): Boolean {
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val locations = prefs.getStringSet("locations", emptySet()) ?: emptySet()
        val text = locationText.lowercase()

        for (place in locations) {
            val enabled = prefs.getBoolean("loc_$place", true)
            if (enabled && text.contains(place.lowercase())) return true
        }
        return false
    }

    private fun speakNow(text: String) {
        if (!ttsReady || tts == null) {
            pendingSpeech = text
            return
        }

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "booking_alert_voice"
        )
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250), -1))
        } else {
            vibrator.vibrate(longArrayOf(0, 250, 150, 250), -1)
        }
    }

    private fun openLalamove(sbn: StatusBarNotification) {
        try {
            val clickIntent = sbn.notification.contentIntent
            if (clickIntent != null) {
                clickIntent.send()
                return
            }
        } catch (_: Exception) {}

        val packages = listOf(
            sbn.packageName,
            "com.lalamove.huolala.driver",
            "com.lalamove.client.driver",
            "com.lalamove.global.driver",
            "com.lalamove.driver"
        )

        for (pkg in packages) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                    return
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
