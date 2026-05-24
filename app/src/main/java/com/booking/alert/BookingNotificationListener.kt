package com.booking.alert

import android.app.*
import android.content.*
import android.os.*
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.AudioManager
import android.net.Uri

class BookingNotificationListener : NotificationListenerService() {

    private val minFare = 200

    private val allowedPlaces = listOf(
        "gen trias",
        "general trias",
        "tanza",
        "dasmarinas",
        "dasma",
        "imus",
        "kawit",
        "noveleta",
        "bacoor",
        "trece martires",
        "naic",
        "tagaytay"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName.lowercase()
        if (!packageName.contains("lalamove")) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullText = "$title $text $bigText".lowercase()

        val fare = extractFare(fullText)
        if (fare < minFare) return

        if (!hasAllowedRoute(fullText)) return

        vibrate()
        openLalamove()

        val speech = makeSpeech(fullText, fare)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent("com.booking.alert.SPEAK")
            intent.putExtra("text", speech)
            sendBroadcast(intent)
        }, 1200)
    }

    private fun extractFare(text: String): Int {
        val regex = Regex("""(?:₱|php|p)\s?(\d{2,5})""")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun hasAllowedRoute(text: String): Boolean {
        val found = allowedPlaces.filter { text.contains(it) }
        return found.size >= 2
    }

    private fun makeSpeech(text: String, fare: Int): String {
        val pickup = allowedPlaces.firstOrNull { text.contains(it) } ?: "pickup"
        val dropoff = allowedPlaces.lastOrNull { text.contains(it) } ?: "drop off"

        return "Booking. $pickup to $dropoff. Fare $fare pesos."
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 250, 150, 250),
                    -1
                )
            )
        } else {
            vibrator.vibrate(longArrayOf(0, 250, 150, 250), -1)
        }
    }

    private fun openLalamove() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.lalamove.huolala.driver")
            ?: packageManager.getLaunchIntentForPackage("com.lalamove.client.driver")

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
    }
}
