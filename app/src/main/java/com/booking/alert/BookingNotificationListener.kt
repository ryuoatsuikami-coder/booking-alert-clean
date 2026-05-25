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

        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val minFare = prefs.getInt("min_fare", 200)
        val fare = extractFare(fullText)

        if (fare < minFare) return

        val route = extractRoute(fullText) ?: return
        val matchedRoute = getPreferredRoute(route.first, route.second) ?: return

        val speechText = "${matchedRoute.first} to ${matchedRoute.second}. Fare $fare pesos."

        vibrate()
        openBookingAlertAndSpeak(speechText)

        Handler(Looper.getMainLooper()).postDelayed({
            openLalamove(sbn)
        }, 3000)
    }

    private fun openBookingAlertAndSpeak(text: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        intent.putExtra("speak_text", text)
        startActivity(intent)
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

        return Pair(parts[0], parts[1])
    }

    private fun getPreferredRoute(pickupText: String, dropoffText: String): Pair<String, String>? {
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val locations = prefs.getStringSet("locations", emptySet()) ?: emptySet()

        val pickupPlace = locations.firstOrNull {
            prefs.getBoolean("loc_$it", true) &&
            pickupText.lowercase().contains(it.lowercase())
        } ?: return null

        val dropoffPlace = locations.firstOrNull {
            prefs.getBoolean("loc_$it", true) &&
            dropoffText.lowercase().contains(it.lowercase())
        } ?: return null

        val route1 = "$pickupPlace ↔ $dropoffPlace"
        val route2 = "$dropoffPlace ↔ $pickupPlace"

        val routeAllowed =
            prefs.getBoolean("route_$route1", true) ||
            prefs.getBoolean("route_$route2", true)

        if (!routeAllowed) return null

        return Pair(pickupPlace, dropoffPlace)
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        } else {
            vibrator.vibrate(longArrayOf(0, 300, 150, 300), -1)
        }
    }

    private fun openLalamove(sbn: StatusBarNotification) {
        try {
            sbn.notification.contentIntent?.send()
            return
        } catch (_: Exception) {}

        val launchIntent = packageManager.getLaunchIntentForPackage(sbn.packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (launchIntent != null) startActivity(launchIntent)
    }
}
