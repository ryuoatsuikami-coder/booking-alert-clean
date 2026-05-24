package com.booking.alert

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {

    private val defaultLocations = listOf(
        "Gen Trias",
        "Tanza",
        "Dasmarinas",
        "Imus",
        "Kawit",
        "Noveleta",
        "Bacoor",
        "Trece Martires",
        "Naic",
        "Tagaytay"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)
        val savedLocations = prefs.getStringSet("locations", defaultLocations.toSet())!!.toMutableSet()

        val scroll = ScrollView(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(36, 36, 36, 36)
        scroll.addView(layout)

        val title = TextView(this)
        title.text = "Booking Alert"
        title.textSize = 24f
        title.setTypeface(null, Typeface.BOLD)
        layout.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "Preferred booking monitor"
        subtitle.textSize = 14f
        layout.addView(subtitle)

        val fareLabel = TextView(this)
        fareLabel.text = "Minimum Fare"
        fareLabel.textSize = 16f
        fareLabel.setPadding(0, 28, 0, 4)
        layout.addView(fareLabel)

        val fareInput = EditText(this)
        fareInput.inputType = InputType.TYPE_CLASS_NUMBER
        fareInput.hint = "Example: 200"
        fareInput.setText(prefs.getInt("min_fare", 200).toString())
        layout.addView(fareInput)

        val addLabel = TextView(this)
        addLabel.text = "Add Location"
        addLabel.textSize = 16f
        addLabel.setPadding(0, 28, 0, 4)
        layout.addView(addLabel)

        val addInput = EditText(this)
        addInput.hint = "Example: Silang"
        layout.addView(addInput)

        val addBtn = Button(this)
        addBtn.text = "Add Location"
        layout.addView(addBtn)

        val listLabel = TextView(this)
        listLabel.text = "Selected Pickup / Drop-off Locations"
        listLabel.textSize = 16f
        listLabel.setPadding(0, 28, 0, 8)
        layout.addView(listLabel)

        val checkBoxContainer = LinearLayout(this)
        checkBoxContainer.orientation = LinearLayout.VERTICAL
        layout.addView(checkBoxContainer)

        fun renderCheckboxes() {
            checkBoxContainer.removeAllViews()

            savedLocations.sorted().forEach { location ->
                val cb = CheckBox(this)
                cb.text = location
                cb.textSize = 15f
                cb.isChecked = prefs.getBoolean("loc_$location", true)
                checkBoxContainer.addView(cb)
            }
        }

        renderCheckboxes()

        addBtn.setOnClickListener {
            val newLoc = addInput.text.toString().trim()

            if (newLoc.isNotEmpty()) {
                savedLocations.add(newLoc)
                prefs.edit()
                    .putStringSet("locations", savedLocations)
                    .putBoolean("loc_$newLoc", true)
                    .apply()

                addInput.setText("")
                renderCheckboxes()
                Toast.makeText(this, "Location added", Toast.LENGTH_SHORT).show()
            }
        }

        val saveBtn = Button(this)
        saveBtn.text = "Save Settings"
        layout.addView(saveBtn)

        val notifBtn = Button(this)
        notifBtn.text = "Open Notification Access"
        layout.addView(notifBtn)

        saveBtn.setOnClickListener {
            val editor = prefs.edit()
            val minFare = fareInput.text.toString().toIntOrNull() ?: 200
            editor.putInt("min_fare", minFare)
            editor.putStringSet("locations", savedLocations)

            for (i in 0 until checkBoxContainer.childCount) {
                val cb = checkBoxContainer.getChildAt(i) as CheckBox
                editor.putBoolean("loc_${cb.text}", cb.isChecked)
            }

            editor.apply()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        notifBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        setContentView(scroll)
    }
}
