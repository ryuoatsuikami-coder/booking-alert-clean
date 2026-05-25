package com.booking.alert

import android.app.Notification
import android.content.Intent
import android.os.*
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class BookingNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        if (!sbn.packageName.lowercase().contains("lalamove")) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullText = "$title $text $bigText"

        val fare = extractFare(fullText)
        val route = extractRoute(fullText)

        val speechText = if (route != null && fare > 0) {
            "${route.first} to ${route.second}. Fare $fare pesos."
        } else {
            "Lalamove booking notification received."
        }

        vibrate()
        speakWithVoiceService(speechText)

        Handler(Looper.getMainLooper()).postDelayed({
            openLalamove(sbn)
        }, 2500)
    }

    private fun speakWithVoiceService(text: String) {
        val intent = Intent(this, BookingVoiceService::class.java)
        intent.putExtra("speak_text", text)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun extractFare(text: String): Int {
        val regex = Regex(
            """(?:₱|php|p)\s?([0-9,]+)(?:\.\d{1,2})?""",
            RegexOption.IGNORE_CASE
        )

        val match = regex.find(text) ?: return 0
        return match.groupValues[1]
            .replace(",", "")
            .toIntOrNull() ?: 0
    }

    private fun extractRoute(text: String): Pair<String, String>? {
        val cleaned = text
            .replace("[Delivery]", "", ignoreCase = true)
            .replace("immediate", "", ignoreCase = true)
            .replace("Hurry up!", "", ignoreCase = true)
            .replace(
                Regex("""(?:₱|php|p)\s?[0-9,]+(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE),
                ""
            )
            .trim()

        val parts = cleaned.split(">").map { it.trim() }

        if (parts.size < 2) return null

        return Pair(
            parts[0].take(45),
            parts[1].take(45)
        )
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 300, 150, 300),
                    -1
                )
            )
        } else {
            vibrator.vibrate(longArrayOf(0, 300, 150, 300), -1)
        }
    }

    private fun openLalamove(sbn: StatusBarNotification) {
        try {
            sbn.notification.contentIntent?.send()
            return
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
}
