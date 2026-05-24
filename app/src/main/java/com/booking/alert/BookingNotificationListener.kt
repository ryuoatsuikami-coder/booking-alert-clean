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
        if (!isSelectedRoute(route.first, route.second)) return

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

    private fun isSelectedRoute(pickup: String, dropoff: String): Boolean {
        val p = normalizeLocation(pickup)
        val d = normalizeLocation(dropoff)

        val routeKey = routeKey(p, d) ?: return false

        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        return prefs.getBoolean(routeKey, true)
    }

    private fun routeKey(pickup: String, dropoff: String): String? {
        val p = normalizeLocation(pickup)
        val d = normalizeLocation(dropoff)

        return when {
            isPair(p, d, "gen trias", "gen trias") -> "Gen Trias ↔ Gen Trias"
            isPair(p, d, "gen trias", "tanza") -> "Gen Trias ↔ Tanza"
            isPair(p, d, "gen trias", "dasmarinas") -> "Gen Trias ↔ Dasmarinas"
            isPair(p, d, "gen trias", "imus") -> "Gen Trias ↔ Imus"
            isPair(p, d, "gen trias", "kawit") -> "Gen Trias ↔ Kawit"
            isPair(p, d, "gen trias", "noveleta") -> "Gen Trias ↔ Noveleta"
            isPair(p, d, "gen trias", "bacoor") -> "Gen Trias ↔ Bacoor"
            isPair(p, d, "gen trias", "trece martires") -> "Gen Trias ↔ Trece Martires"
            isPair(p, d, "gen trias", "naic") -> "Gen Trias ↔ Naic"
            isPair(p, d, "gen trias", "tagaytay") -> "Gen Trias ↔ Tagaytay"
            else -> null
        }
    }

    private fun isPair(a: String, b: String, x: String, y: String): Boolean {
        return (a == x && b == y) || (a == y && b == x)
    }

    private fun normalizeLocation(location: String): String {
        val l = location.lowercase()

        return when {
            l.contains("general trias") ||
            l.contains("gen trias") ||
            l.contains("gen. trias") -> "gen trias"

            l.contains("tanza") -> "tanza"

            l.contains("dasmarinas") ||
            l.contains("dasmariñas") ||
            l.contains("dasma") -> "dasmarinas"

            l.contains("imus") -> "imus"
            l.contains("kawit") -> "kawit"
            l.contains("noveleta") -> "noveleta"
            l.contains("bacoor") -> "bacoor"

            l.contains("trece martires") ||
            l.contains("trece") -> "trece martires"

            l.contains("naic") -> "naic"
            l.contains("tagaytay") -> "tagaytay"

            else -> l
        }
    }

    private fun speakNow(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "booking_alert")
        }
    }

    private fun vibrateAlert() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    500,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
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
