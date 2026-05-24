package com.booking.alert

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import android.view.ViewGroup
import android.graphics.Typeface

class MainActivity : Activity() {

    private val routes = listOf(
        "Gen Trias ↔ Gen Trias",
        "Gen Trias ↔ Tanza",
        "Gen Trias ↔ Dasmarinas",
        "Gen Trias ↔ Imus",
        "Gen Trias ↔ Kawit",
        "Gen Trias ↔ Noveleta",
        "Gen Trias ↔ Bacoor",
        "Gen Trias ↔ Trece Martires",
        "Gen Trias ↔ Naic",
        "Gen Trias ↔ Tagaytay"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("booking_prefs", MODE_PRIVATE)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)

        val title = TextView(this)
        title.text = "Booking Alert Settings"
        title.textSize = 22f
        title.setTypeface(null, Typeface.BOLD)
        layout.addView(title)

        val fareLabel = TextView(this)
        fareLabel.text = "Minimum Fare"
        fareLabel.textSize = 16f
        layout.addView(fareLabel)

        val fareInput = EditText(this)
        fareInput.hint = "Example: 200"
        fareInput.setText(prefs.getInt("min_fare", 200).toString())
        fareInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        layout.addView(fareInput)

        val routeLabel = TextView(this)
        routeLabel.text = "Preferred Routes"
        routeLabel.textSize = 16f
        routeLabel.setPadding(0, 24, 0, 8)
        layout.addView(routeLabel)

        val checkboxes = mutableListOf<CheckBox>()

        routes.forEach { route ->
            val cb = CheckBox(this)
            cb.text = route
            cb.textSize = 15f
            cb.isChecked = prefs.getBoolean(route, true)
            checkboxes.add(cb)
            layout.addView(cb)
        }

        val saveBtn = Button(this)
        saveBtn.text = "Save Settings"
        layout.addView(saveBtn)

        val notifBtn = Button(this)
        notifBtn.text = "Open Notification Access"
        layout.addView(notifBtn)

        saveBtn.setOnClickListener {
            val minFare = fareInput.text.toString().toIntOrNull() ?: 200

            val editor = prefs.edit()
            editor.putInt("min_fare", minFare)

            checkboxes.forEach { cb ->
                editor.putBoolean(cb.text.toString(), cb.isChecked)
            }

            editor.apply()

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        notifBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        val scroll = ScrollView(this)
        scroll.addView(layout)

        setContentView(scroll)
    }
}
