package com.pocketsignal.scanner

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val captureRequest = 7001
    private lateinit var assetSpinner: Spinner
    private lateinit var durationSpinner: Spinner
    private lateinit var urlInput: EditText

    private val assets = arrayOf(
        "GBP/USD OTC", "EUR/USD OTC", "GBP/JPY OTC", "AUD/USD OTC",
        "USD/JPY OTC", "USD/CAD OTC", "USD/CHF OTC", "EUR/JPY OTC",
        "AUD/JPY OTC", "NZD/USD OTC"
    )
    private val durations = arrayOf("30 seconds", "1 minute")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(0xFF0B1020.toInt())
        }

        fun label(t: String) = TextView(this).apply {
            text = t
            textSize = 13f
            setTextColor(0xFFAAB3CC.toInt())
            setPadding(0, 8, 0, 6)
        }

        val title = TextView(this).apply {
            text = "📡 Pocket Option Live Scanner"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, 8)
        }
        root.addView(title)

        val info = TextView(this).apply {
            text = "Ekranı paylaş → Pocket Option-a keç → scanner chartı analiz etsin."
            textSize = 14f
            setTextColor(0xFFB9C2D8.toInt())
            setPadding(0, 0, 0, 18)
        }
        root.addView(info)

        root.addView(label("Render API URL"))
        urlInput = EditText(this).apply {
            setSingleLine(true)
            setText("https://pocket-signal-12.onrender.com/analyze-frame")
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF6F7890.toInt())
            setHint("https://.../analyze-frame")
        }
        root.addView(urlInput)

        root.addView(label("Aktiv"))
        assetSpinner = Spinner(this)
        assetSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, assets)
        root.addView(assetSpinner)

        root.addView(label("Müddət"))
        durationSpinner = Spinner(this)
        durationSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, durations)
        root.addView(durationSpinner)

        val start = Button(this).apply {
            text = "▶ Ekranı paylaş və scanneri başlat"
            setOnClickListener { requestCapture() }
        }
        root.addView(start)

        val stop = Button(this).apply {
            text = "■ Scanneri dayandır"
            setOnClickListener {
                stopService(Intent(this@MainActivity, ScreenCaptureService::class.java))
                Toast.makeText(this@MainActivity, "Scanner dayandırıldı", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(stop)

        val note = TextView(this).apply {
            text = "\nQeyd: Android sisteminin ekran paylaşımı icazəsi çıxacaq. İcazə verdikdən sonra Pocket Option-a keçə bilərsən. Scanner avtomatik olaraq görüntüləri Render serverinə göndərir."
            textSize = 13f
            setTextColor(0xFF8994B0.toInt())
        }
        root.addView(note)

        setContentView(root)
    }

    private fun requestCapture() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), captureRequest)
    }

    @Deprecated("Activity result API kept simple for this standalone project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != captureRequest || resultCode != Activity.RESULT_OK || data == null) {
            Toast.makeText(this, "Ekran paylaşımı icazə verilmədi.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
            putExtra("asset", assetSpinner.selectedItem.toString())
            putExtra("duration", durationSpinner.selectedItem.toString())
            putExtra("endpoint", urlInput.text.toString().trim())
        }
        startForegroundService(intent)
        Toast.makeText(this, "Scanner başladı. İndi Pocket Option-a keç.", Toast.LENGTH_LONG).show()
    }
}
