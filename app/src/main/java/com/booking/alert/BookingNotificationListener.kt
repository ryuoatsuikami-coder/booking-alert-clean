package com.booking.alert

import android.app.*
import android.content.*
import android.os.*
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class BookingNotificationListener : NotificationListenerService() {

    private val minFare = 200

    private val locations = listOf(
        "General Trias", "Tanza", "Dasmarinas", "Imus", "Kawit",
        "Noveleta", "Bacoor", "Trece Martires", "Naic", "Tagaytay"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (!sbn.packageName.lowercase().contains("lalamove")) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val fullText = "$title $text $bigText"

        val fare = extractFare(fullText)
        if (fare < minFare) return

        val route = extractRoute(fullText) ?: return
        if (!isSelectedRoute(route.first, route.second)) return

        vibrate()
        speak("${route.first} to ${route.second}. Fare $fare pesos.")
        openLalamove(sbn)
    }

    private fun extractFare(text: String): Int {
        val regex = Regex("""(?:₱|php|p)\s?(\d{2,5})""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun extractRoute(text: String): Pair<String, String>? {
        val parts = text.split(">").map { it.trim() }
        if (parts.size < 2) return null

        val pickup = cleanLocation(parts[0])
        val dropoff = cleanLocation(parts[1])

        return Pair(pickup, dropoff)
    }

    private fun cleanLocation(text: String): String {
        return text
            .replace("[Delivery]", "", ignoreCase = true)
            .replace("immediate", "", ignoreCase = true)
            .replace("Hurry up!", "", ignoreCase = true)
            .replace(Regex("""₱\s*\d+"""), "")
            .trim()
    }

    private fun isSelectedRoute(pickup: String, dropoff: String): Boolean {
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)

        val p = matchLocation(pickup) ?: return false
        val d = matchLocation(dropoff) ?: return false

        val pEnabled = prefs.getBoolean("loc_$p", true)
        val dEnabled = prefs.getBoolean("loc_$d", true)

        val route1 = "$p ↔ $d"
        val route2 = "$d ↔ $p"

        val routeEnabled =
            prefs.getBoolean("route_$route1", true) ||
            prefs.getBoolean("route_$route2", true)

        return pEnabled && dEnabled && routeEnabled
    }

    private fun matchLocation(text: String): String? {
        val lower = text.lowercase()

        return locations.firstOrNull { loc ->
            val locLower = loc.lowercase()
            lower.contains(locLower) ||
            lower.contains(locLower.replace("general trias", "gen trias")) ||
            lower.contains(locLower.replace("dasmarinas", "dasma"))
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250), -1))
        } else {
            vibrator.vibrate(longArrayOf(0, 250, 150, 250), -1)
        }
    }

    private fun speak(text: String) {
        val intent = Intent("com.booking.alert.SPEAK")
        intent.putExtra("text", text)
        sendBroadcast(intent)
    }

    private fun openLalamove(sbn: StatusBarNotification) {
        try {
            sbn.notification.contentIntent?.send()
        } catch (e: Exception) {
            val launchIntent = packageManager.getLaunchIntentForPackage(sbn.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launchIntent != null) startActivity(launchIntent)
        }
    }
}
