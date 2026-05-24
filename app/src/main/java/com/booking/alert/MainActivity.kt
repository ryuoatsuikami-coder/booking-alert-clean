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

    private val speakReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val text = intent?.getStringExtra("text") ?: return
            speakNow(text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)

        val title = TextView(this)
        title.text = "Booking Alert"
        title.textSize = 26f

        val desc = TextView(this)
        desc.text = "Reads selected Lalamove bookings aloud and opens Lalamove."
        desc.textSize = 16f

        val btnNotif = Button(this)
        btnNotif.text = "Open Notification Access"
        btnNotif.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        val btnTest = Button(this)
        btnTest.text = "Test Voice"
        btnTest.setOnClickListener {
            speakNow("Test booking. General Trias to Dasmarinas. Fare 250 pesos.")
        }

        layout.addView(title)
        layout.addView(desc)
        layout.addView(btnNotif)
        layout.addView(btnTest)

        setContentView(layout)

        registerReceiver(speakReceiver, IntentFilter("com.booking.alert.SPEAK"))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("en", "PH")
            tts?.setSpeechRate(1.15f)
            tts?.setPitch(1.0f)
            ready = true
        }
    }

    private fun speakNow(text: String) {
        if (!ready) {
            Handler(Looper.getMainLooper()).postDelayed({
                speakNow(text)
            }, 800)
            return
        }

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "booking_alert_voice"
        )
    }

    override fun onDestroy() {
        unregisterReceiver(speakReceiver)
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
