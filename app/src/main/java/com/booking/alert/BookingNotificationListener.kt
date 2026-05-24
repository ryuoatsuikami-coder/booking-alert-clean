package com.booking.alert

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class BookingNotificationListener : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var pendingSpeakText: String? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH
            tts?.setSpeechRate(1.35f)
            pendingSpeakText?.let {
                speakBrief(it)
                pendingSpeakText = null
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageNameText = sbn.packageName ?: ""
        val title = sbn.notification.extras.getString("android.title") ?: ""
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = sbn.notification.extras.getCharSequence("android.bigText")?.toString() ?: ""
        val fullText = "$title $text $bigText"

        if (packageNameText.lowercase().contains("lalamove")) {
            val fare = extractFare(fullText)

            if (fare != null && fare >= 200) {
                val route = extractRoute(fullText)
                val speakText = if (route != null) {
                    "${route.first} to ${route.second}, fare ${fare.toInt()} pesos"
                } else {
                    "Lalamove booking, fare ${fare.toInt()} pesos"
                }

                vibrateAlert()
                speakBrief(speakText)
                openLalamove(sbn)
            }
        }
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

        if (parts.size >= 2) {
            val pickup = parts[0].take(25)
            val dropoff = parts[1].take(25)
            return Pair(pickup, dropoff)
        }

        return null
    }

    private fun speakBrief(text: String) {
        val engine = tts

        if (engine == null) {
            pendingSpeakText = text
            return
        }

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "booking_alert")
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
                val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(sbn.packageName)
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
