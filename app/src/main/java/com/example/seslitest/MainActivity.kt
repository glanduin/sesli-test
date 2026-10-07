package com.example.seslitest

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var sonucYazisi: TextView
    private lateinit var durumYazisi: TextView
    private lateinit var buton: View
    private var kayitAktif = false
    private val birikenMetin = StringBuilder()

    companion object {
        private const val MIC_IZIN_KODU = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ana çerçeve (üst üste yerleştirme)
        val anaLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1a1a2e"))
        }

        // Üst kısımdaki yazılar için dikey düzen
        val ustLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(50, 150, 50, 50)
        }

        durumYazisi = TextView(this).apply {
            text = "Konuşmak için kırmızı butona bas"
            textSize = 18f
            setTextColor(Color.parseColor("#aaaaaa"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }

        sonucYazisi = TextView(this).apply {
            text = ""
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }

        ustLayout.addView(durumYazisi)
        ustLayout.addView(sonucYazisi)

        // Kırmızı yuvarlak buton - ekranın altına sabitlenmiş
        buton = View(this).apply {
            val boyut = 400
            layoutParams = FrameLayout.LayoutParams(boyut, boyut).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 120
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#e53935"))
                setStroke(8, Color.parseColor("#b71c1c"))
            }
            isClickable = true
        }

        anaLayout.addView(ustLayout)
        anaLayout.addView(buton)
        setContentView(anaLayout)

        // İzin kontrolü
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MIC_IZIN_KODU
            )
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val tanimaNiyeti = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 30000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 30000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000L)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                durumYazisi.text = "Dinliyorum... birakinca duracak"
                durumYazisi.setTextColor(Color.parseColor("#4caf50"))
            }

            override fun onBeginningOfSpeech() {
                durumYazisi.text = "Konusun..."
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                durumYazisi.text = "Isleniyor..."
            }

            override fun onError(error: Int) {
                val hataMesaji = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Ses hatasi"
                    SpeechRecognizer.ERROR_CLIENT -> "Istemci hatasi"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni yok"
                    SpeechRecognizer.ERROR_NETWORK -> "Internet hatasi"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Internet zaman asimi"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Ses anlasilamadi"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Taniyici mesgul"
                    SpeechRecognizer.ERROR_SERVER -> "Sunucu hatasi"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Konusma algilanamadi"
                    else -> "Bilinmeyen hata"
                }
                durumYazisi.text = hataMesaji
                durumYazisi.setTextColor(Color.parseColor("#f44336"))
                kayitAktif = false
            }

            override fun onResults(results: Bundle?) {
                val sonuclar = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val metin = sonuclar?.firstOrNull() ?: ""

                if (metin.isNotBlank()) {
                    birikenMetin.append(metin).append(" ")
                    sonucYazisi.text = birikenMetin.toString().trim()
                    durumYazisi.text = "Kaydedildi"
                    durumYazisi.setTextColor(Color.parseColor("#4caf50"))
                }

                kayitAktif = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val sonuclar = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val kismi = sonuclar?.firstOrNull() ?: ""
                if (kismi.isNotBlank()) {
                    sonucYazisi.text = (birikenMetin.toString() + kismi).trim()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        buton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    buton.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
                    kayitAktif = true
                    birikenMetin.clear()
                    sonucYazisi.text = ""
                    durumYazisi.text = "Hazirlaniyor..."
                    durumYazisi.setTextColor(Color.parseColor("#aaaaaa"))

                    try {
                        speechRecognizer.startListening(tanimaNiyeti)
                    } catch (e: Exception) {
                        durumYazisi.text = "Baslatilamadi: " + e.message
                        kayitAktif = false
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    buton.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    if (kayitAktif) {
                        speechRecognizer.stopListening()
                        durumYazisi.text = "Donusturuluyor..."
                    }
                    true
                }

                else -> false
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MIC_IZIN_KODU) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                durumYazisi.text = "Konusmak icin kirmizi butona bas"
            } else {
                durumYazisi.text = "Mikrofon izni verilmedi"
                durumYazisi.setTextColor(Color.parseColor("#f44336"))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
