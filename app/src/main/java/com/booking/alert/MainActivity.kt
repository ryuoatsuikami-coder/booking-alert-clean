package com.booking.alert

import android.app.*
import android.content.*
import android.os.*
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.*
import java.util.*

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false

    private val locations = listOf(
        "General Trias", "Tanza", "Dasmarinas", "Imus", "Kawit",
        "Noveleta", "Bacoor", "Trece Martires", "Naic", "Tagaytay"
    )

    private val locationBoxes = mutableListOf<CheckBox>()
    private val routeBoxes = mutableListOf<CheckBox>()

    private val speakReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val text = intent?.getStringExtra("text") ?: return
            speakNow(text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)

        val scroll = ScrollView(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)

        val title = TextView(this)
        title.text = "Booking Alert"
        title.textSize = 26f
        layout.addView(title)

        val locTitle = TextView(this)
        locTitle.text = "Preferred Locations"
        locTitle.textSize = 18f
        layout.addView(locTitle)

        locations.forEach { location ->
            val cb = CheckBox(this)
            cb.text = location
            cb.textSize = 16f
            cb.isChecked = prefs.getBoolean("loc_$location", true)
            locationBoxes.add(cb)
            layout.addView(cb)
        }

        val routeTitle = TextView(this)
        routeTitle.text = "Preferred Routes"
        routeTitle.textSize = 18f
        routeTitle.setPadding(0, 25, 0, 0)
        layout.addView(routeTitle)

        val routes = mutableListOf<String>()
        for (i in locations.indices) {
            for (j in i until locations.size) {
                routes.add("${locations[i]} ↔ ${locations[j]}")
            }
        }

        routes.forEach { route ->
            val cb = CheckBox(this)
            cb.text = route
            cb.textSize = 15f
            cb.isChecked = prefs.getBoolean("route_$route", true)
            routeBoxes.add(cb)
            layout.addView(cb)
        }

        val btnSave = Button(this)
        btnSave.text = "Save Settings"
        btnSave.setOnClickListener {
            val editor = prefs.edit()

            locationBoxes.forEach {
                editor.putBoolean("loc_${it.text}", it.isChecked)
            }

            routeBoxes.forEach {
                editor.putBoolean("route_${it.text}", it.isChecked)
            }

            editor.apply()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        val btnNotif = Button(this)
        btnNotif.text = "Open Notification Access"
        btnNotif.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        val btnTest = Button(this)
        btnTest.text = "Test Voice"
        btnTest.setOnClickListener {
            speakNow("Booking. General Trias to Dasmarinas. Fare 250 pesos.")
        }

        layout.addView(btnSave)
        layout.addView(btnNotif)
        layout.addView(btnTest)

        scroll.addView(layout)
        setContentView(scroll)

        registerReceiver(speakReceiver, IntentFilter("com.booking.alert.SPEAK"))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "PH")
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
            ready = true
        }
    }

    private fun speakNow(text: String) {
        if (!ready) {
            Handler(Looper.getMainLooper()).postDelayed({ speakNow(text) }, 700)
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "booking_alert_voice")
    }

    override fun onDestroy() {
        unregisterReceiver(speakReceiver)
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
