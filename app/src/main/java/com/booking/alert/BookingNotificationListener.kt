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
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val fullText = "$title $text $bigText $subText".trim()
        if (fullText.isBlank()) return

        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val minFare = prefs.getInt("min_fare", 200)

        val fare = extractFare(fullText)
        if (fare < minFare) return

        val route = extractRoute(fullText) ?: return
        val matchedRoute = matchPreferredRoute(route.first, route.second) ?: return

        val speechText =
            "Preferred booking. ${matchedRoute.first} to ${matchedRoute.second}. Fare $fare pesos."

        vibrate()
        speakWithVoiceService(speechText)

        Handler(Looper.getMainLooper()).postDelayed({
            openLalamove(sbn)
        }, 800)
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

    private fun matchPreferredRoute(pickupText: String, dropoffText: String): Pair<String, String>? {
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val routes = prefs.getStringSet("routes", emptySet()) ?: emptySet()

        val pickupLower = normalize(pickupText)
        val dropoffLower = normalize(dropoffText)

        for (route in routes) {
            val enabled = prefs.getBoolean("route_$route", true)
            if (!enabled) continue

            val parts = route.split(">").map { it.trim() }
            if (parts.size < 2) continue

            val pickupRoute = normalize(parts[0])
            val dropoffRoute = normalize(parts[1])

            val pickupMatched =
                pickupLower.contains(pickupRoute) || pickupRoute.contains(pickupLower)

            val dropoffMatched =
                dropoffLower.contains(dropoffRoute) || dropoffRoute.contains(dropoffLower)

            if (pickupMatched && dropoffMatched) {
                return Pair(parts[0], parts[1])
            }
        }

        return null
    }

    private fun extractFare(text: String): Int {
        val patterns = listOf(
            Regex("""(?:₱|php|p)\s?([0-9,]+)(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE),
            Regex("""fare\s*(?:₱|php|p)?\s?([0-9,]+)""", RegexOption.IGNORE_CASE),
            Regex("""([0-9,]+)\s*pesos""", RegexOption.IGNORE_CASE)
        )

        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                return match.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            }
        }

        return 0
    }

    private fun extractRoute(text: String): Pair<String, String>? {
        val cleaned = text
            .replace("[Delivery]", "", ignoreCase = true)
            .replace("immediate", "", ignoreCase = true)
            .replace("Hurry up!", "", ignoreCase = true)
            .replace("Pickup", "", ignoreCase = true)
            .replace("Dropoff", "", ignoreCase = true)
            .replace("Drop-off", "", ignoreCase = true)
            .replace(Regex("""(?:₱|php|p)\s?[0-9,]+(?:\.\d{1,2})?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""fare\s*(?:₱|php|p)?\s?[0-9,]+""", RegexOption.IGNORE_CASE), "")
            .trim()

        val separators = listOf(">", " to ", " To ", " TO ", "→", "-")

        for (separator in separators) {
            val parts = cleaned.split(separator).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) {
                return Pair(parts[0], parts[1])
            }
        }

        return null
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace(",", " ")
            .replace(".", " ")
            .replace("-", " ")
            .replace("  ", " ")
            .trim()
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
            val pendingIntent = sbn.notification.contentIntent
            if (pendingIntent != null) {
                pendingIntent.send()
                return
            }
        } catch (_: Exception) {}

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(sbn.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                startActivity(launchIntent)
                return
            }
        } catch (_: Exception) {}

        val packages = listOf(
            "com.lalamove.huolala.driver",
            "com.lalamove.client.driver",
            "com.lalamove.global.driver",
            "com.lalamove.driver"
        )

        for (pkg in packages) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }
    }
}
