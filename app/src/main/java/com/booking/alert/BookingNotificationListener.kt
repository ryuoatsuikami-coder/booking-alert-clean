package com.booking.alert

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class BookingNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageNameText = sbn.packageName ?: ""
        val title = sbn.notification.extras.getString("android.title") ?: ""
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = sbn.notification.extras.getCharSequence("android.bigText")?.toString() ?: ""
        val fullText = "$title $text $bigText"

        if (
            packageNameText.lowercase().contains("lalamove") &&
            isFare200Up(fullText)
        ) {
            vibrateAlert()
            openLalamove(sbn)
        }
    }

    private fun isFare200Up(text: String): Boolean {
        val regex = Regex("""₱\s*([0-9,]+(?:\.\d{1,2})?)""")
        val match = regex.find(text) ?: return false
        val fare = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return false
        return fare >= 200
    }

    private fun vibrateAlert() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            vibrator.vibrate(700)
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
}
