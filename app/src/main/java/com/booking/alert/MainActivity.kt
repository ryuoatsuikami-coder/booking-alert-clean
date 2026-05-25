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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        startService(Intent(this, BookingVoiceService::class.java))
        buildUi()
    }

    private fun buildUi() {
        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val savedLocations = prefs.getStringSet("locations", defaultLocations.toSet())!!.toMutableSet()
        val savedRoutes = prefs.getStringSet("routes", emptySet())!!.toMutableSet()

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

        val addPlaceInput = EditText(this)
        addPlaceInput.hint = "Add place, example Silang"
        layout.addView(addPlaceInput)

        val addPlaceBtn = Button(this)
        addPlaceBtn.text = "Add Place"
        layout.addView(addPlaceBtn)

        val addRouteInput = EditText(this)
        addRouteInput.hint = "Add route, example General Trias > Silang"
        layout.addView(addRouteInput)

        val addRouteBtn = Button(this)
        addRouteBtn.text = "Add Route"
        layout.addView(addRouteBtn)

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

            savedRoutes.sorted().forEach { route ->
                val cb = CheckBox(this)
                cb.text = route
                cb.textSize = 15f
                cb.isChecked = prefs.getBoolean("route_$route", true)
                routeContainer.addView(cb)
            }
        }

        if (savedRoutes.isEmpty()) {
            val list = savedLocations.sorted()
            for (i in list.indices) {
                for (j in i until list.size) {
                    savedRoutes.add("${list[i]} > ${list[j]}")
                    savedRoutes.add("${list[j]} > ${list[i]}")
                }
            }
            prefs.edit().putStringSet("routes", savedRoutes).apply()
        }

        renderAll()

        addPlaceBtn.setOnClickListener {
            val newPlace = addPlaceInput.text.toString().trim()
            if (newPlace.isNotEmpty()) {
                savedLocations.add(newPlace)
                prefs.edit()
                    .putStringSet("locations", savedLocations)
                    .putBoolean("loc_$newPlace", true)
                    .apply()
                addPlaceInput.setText("")
                renderAll()
            }
        }

        addRouteBtn.setOnClickListener {
            val route = addRouteInput.text.toString().trim()
            if (route.contains(">")) {
                savedRoutes.add(route)
                prefs.edit()
                    .putStringSet("routes", savedRoutes)
                    .putBoolean("route_$route", true)
                    .apply()
                addRouteInput.setText("")
                renderAll()
            } else {
                Toast.makeText(this, "Use format: Pickup > Dropoff", Toast.LENGTH_SHORT).show()
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
            editor.putStringSet("routes", savedRoutes)

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
            val intent = Intent(this, BookingVoiceService::class.java)
            intent.putExtra("speak_text", "Booking. General Trias to Dasmarinas. Fare 250 pesos.")
            startService(intent)
        }

        scroll.addView(layout)
        setContentView(scroll)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
