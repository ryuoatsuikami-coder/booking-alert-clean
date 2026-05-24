package com.booking.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class BookingNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageNameText = sbn.packageName ?: ""
        if (!packageNameText.lowercase().contains("lalamove")) return

        val title = sbn.notification.extras.getString("android.title") ?: ""
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = sbn.notification.extras.getCharSequence("android.bigText")?.toString() ?: ""
        val fullText = "$title $text $bigText"

        val fare = extractFare(fullText)
        if (fare == null || fare < 200) return

        val route = extractRoute(fullText)
        val message = if (route != null) {
            "${route.first} to ${route.second}. ${fare.toInt()} pesos."
        } else {
            "Lalamove booking. ${fare.toInt()} pesos."
        }

        vibrateAlert()
        showAlertNotification(message)
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
            Pair(parts[0].take(30), parts[1].take(30))
        } else {
            null
        }
    }

    private fun vibrateAlert() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(600)
        }
    }

    private fun showAlertNotification(message: String) {
        val channelId = "booking_alert_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Booking Alert",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Preferred Booking")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }
}
