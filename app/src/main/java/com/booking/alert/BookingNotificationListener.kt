package com.booking.alert

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.MediaPlayer
import android.provider.Settings

class BookingNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageNameText = sbn.packageName ?: ""

        if (packageNameText.lowercase().contains("lalamove")) {
            playAlert()
        }
    }

    private fun playAlert() {
        val player = MediaPlayer.create(this, Settings.System.DEFAULT_ALARM_ALERT_URI)
        player?.start()
    }
}
