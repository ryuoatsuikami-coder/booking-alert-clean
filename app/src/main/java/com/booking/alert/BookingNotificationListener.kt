package com.booking.alert

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.MediaPlayer
import android.provider.Settings

class BookingNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageNameText = sbn.packageName ?: ""
        val title = sbn.notification.extras.getString("android.title") ?: ""
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
        val fullText = "$title $text"

        if (
            packageNameText.lowercase().contains("lalamove") &&
            fullText.contains("₱") &&
            fullText.contains("200")
        ) {
            playAlert()
        }
    }

    private fun playAlert() {
        val player = MediaPlayer.create(this, Settings.System.DEFAULT_ALARM_ALERT_URI)
        player?.start()
    }
}
