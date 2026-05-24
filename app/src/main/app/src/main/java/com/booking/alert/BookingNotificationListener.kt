package com.booking.alert

import android.app.Notification
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class BookingNotificationListener : NotificationListenerService() {

    private val pickupKeywords = listOf("gen trias", "general trias")

    private val dropoffKeywords = listOf(
        "imus", "noveleta", "kawit", "dasmarinas", "dasmariñas",
        "bacoor", "naic", "tanza", "cavite city", "trece",
        "trece martires", "amadeo", "silang", "tagaytay"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!sbn.packageName.lowercase().contains("lalamove")) return

        val extras = sbn.notification.extras
        val content = (
            extras.getString(Notification.EXTRA_TITLE).orEmpty() + " " +
            extras.getCharSequence(Notification.EXTRA_TEXT).orEmpty() + " " +
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT).orEmpty()
        ).lowercase()

        val hasPickup = pickupKeywords.any { content.contains(it) }
        val hasDropoff = dropoffKeywords.any { content.contains(it) }
        val fare = extractFare(content)

        if (hasPickup && hasDropoff && fare >= 300) {
            alertDriver()
        }
    }

    private fun extractFare(text: String): Int {
        val regex = Regex("""₱\s?(\d{2,5})|php\s?(\d{2,5})""")
        val match = regex.find(text) ?: return 0
        return match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.toIntOrNull() ?: 0
    }

    private fun alertDriver() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 300, 1000), -1))

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(applicationContext, sound).play()
    }
}
