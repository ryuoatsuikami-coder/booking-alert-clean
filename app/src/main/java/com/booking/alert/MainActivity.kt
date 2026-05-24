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

    private val defaultLocations = listOf(
        "General Trias", "Tanza", "Dasmarinas", "Imus", "Kawit",
        "Noveleta", "Bacoor", "Trece Martires", "Naic", "Tagaytay"
    )

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
        fareInput.hint = "Minimum fare, example 200"
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
        placeTitle.text = "Selected Places"
        placeTitle.textSize = 18f
        layout.addView(placeTitle)

        val boxContainer = LinearLayout(this)
        boxContainer.orientation = LinearLayout.VERTICAL
        layout.addView(boxContainer)

        fun renderPlaces() {
            boxContainer.removeAllViews()
            savedLocations.sorted().forEach { place ->
                val cb = CheckBox(this)
                cb.text = place
                cb.textSize = 16f
                cb.isChecked = prefs.getBoolean("loc_$place", true)
                boxContainer.addView(cb)
            }
        }

        renderPlaces()

        addBtn.setOnClickListener {
            val newPlace = addInput.text.toString().trim()
            if (newPlace.isNotEmpty()) {
                savedLocations.add(newPlace)
                prefs.edit()
                    .putStringSet("locations", savedLocations)
                    .putBoolean("loc_$newPlace", true)
                    .apply()
                addInput.setText("")
                renderPlaces()
                Toast.makeText(this, "Place added", Toast.LENGTH_SHORT).show()
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

            for (i in 0 until boxContainer.childCount) {
                val cb = boxContainer.getChildAt(i) as CheckBox
                editor.putBoolean("loc_${cb.text}", cb.isChecked)
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
