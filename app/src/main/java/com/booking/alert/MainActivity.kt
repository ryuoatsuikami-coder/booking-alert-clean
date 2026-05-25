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
    private var pendingSpeak: String? = null

    private val defaultLocations = listOf(
        "General Trias", "Tanza", "Dasmarinas", "Imus", "Kawit",
        "Noveleta", "Bacoor", "Trece Martires", "Naic", "Tagaytay"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val text = intent.getStringExtra("speak_text")
        if (!text.isNullOrBlank()) {
            pendingSpeak = text
            speakNow(text)
        }
    }

    private fun buildUi() {
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val savedLocations = prefs.getStringSet("locations", defaultLocations.toSet())!!.toMutableSet()

        val scroll = ScrollView(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)

        val title = TextView(this)
        title.text = "Booking Alert"
        title.textSize = 26f
        layout.addView(title)

        val fareInput = EditText(this)
        fareInput.hint = "Minimum fare"
        fareInput.setText(prefs.getInt("min_fare", 200).toString())
        fareInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(fareInput)

        val addInput = EditText(this)
        addInput.hint = "Add place, example Silang"
        layout.addView(addInput)

        val addBtn = Button(this)
        addBtn.text = "Add Place"
        layout.addView(addBtn)

        val placeTitle = TextView(this)
        placeTitle.text = "Preferred Places"
        placeTitle.textSize = 18f
        layout.addView(placeTitle)

        val placeContainer = LinearLayout(this)
        placeContainer.orientation = LinearLayout.VERTICAL
        layout.addView(placeContainer)

        val routeTitle = TextView(this)
        routeTitle.text = "Preferred Routes"
        routeTitle.textSize = 18f
        routeTitle.setPadding(0, 25, 0, 0)
        layout.addView(routeTitle)

        val routeContainer = LinearLayout(this)
        routeContainer.orientation = LinearLayout.VERTICAL
        layout.addView(routeContainer)

        fun renderAll() {
            placeContainer.removeAllViews()
            routeContainer.removeAllViews()

            savedLocations.sorted().forEach { place ->
                val cb = CheckBox(this)
                cb.text = place
                cb.textSize = 16f
                cb.isChecked = prefs.getBoolean("loc_$place", true)
                placeContainer.addView(cb)
            }

            val list = savedLocations.sorted()
            for (i in list.indices) {
                for (j in i until list.size) {
                    val route = "${list[i]} ↔ ${list[j]}"
                    val cb = CheckBox(this)
                    cb.text = route
                    cb.textSize = 15f
                    cb.isChecked = prefs.getBoolean("route_$route", true)
                    routeContainer.addView(cb)
                }
            }
        }

        renderAll()

        addBtn.setOnClickListener {
            val newPlace = addInput.text.toString().trim()
            if (newPlace.isNotEmpty()) {
                savedLocations.add(newPlace)
                prefs.edit()
                    .putStringSet("locations", savedLocations)
                    .putBoolean("loc_$newPlace", true)
                    .apply()
                addInput.setText("")
                renderAll()
            }
        }

        val saveBtn = Button(this)
        saveBtn.text = "Save Settings"
        layout.addView(saveBtn)

        val notifBtn = Button(this)
        notifBtn.text = "Open Notification Access"
        layout.addView(notifBtn)

        val testBtn = Button(this)
        testBtn.text = "Test Voice"
        layout.addView(testBtn)

        saveBtn.setOnClickListener {
            val editor = prefs.edit()
            editor.putInt("min_fare", fareInput.text.toString().toIntOrNull() ?: 200)
            editor.putStringSet("locations", savedLocations)

            for (i in 0 until placeContainer.childCount) {
                val cb = placeContainer.getChildAt(i) as CheckBox
                editor.putBoolean("loc_${cb.text}", cb.isChecked)
            }

            for (i in 0 until routeContainer.childCount) {
                val cb = routeContainer.getChildAt(i) as CheckBox
                editor.putBoolean("route_${cb.text}", cb.isChecked)
            }

            editor.apply()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        notifBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        testBtn.setOnClickListener {
            speakNow("Booking. General Trias to Dasmarinas. Fare 250 pesos.")
        }

        scroll.addView(layout)
        setContentView(scroll)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "PH")
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
            ready = true

            pendingSpeak?.let {
                speakNow(it)
                pendingSpeak = null
            }
        }
    }

    private fun speakNow(text: String) {
        if (!ready) {
            pendingSpeak = text
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "booking_alert_voice")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
