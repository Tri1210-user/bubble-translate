package com.bubbletranslate.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class OverlayViewManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayRoot: View? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("vi", "VN"))
            isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
        }
    }

    fun showTranslationOverlay(blocks: List<RecognizedBlock>, targetLang: String = "VI") {
        hideOverlay()

        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_translation_overlay, null)
        overlayRoot = view

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START

        val container = view.findViewById<FrameLayout>(R.id.translationBoxesContainer)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatusInfo)
        val btnClose = view.findViewById<View>(R.id.btnCloseOverlay)
        val btnSpeakAll = view.findViewById<View>(R.id.btnSpeakAll)
        val btnCopyAll = view.findViewById<View>(R.id.btnCopyAll)

        tvStatus.text = "Đã dịch ${blocks.size} câu"

        // Vẽ từng hộp chữ đè lên vị trí gốc trên màn hình
        val allTranslatedText = StringBuilder()
        for (block in blocks) {
            if (block.translatedText.isEmpty()) continue
            allTranslatedText.append(block.translatedText).append("\n")

            val textBox = TextView(context).apply {
                text = block.translatedText
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setBackgroundResource(R.drawable.bg_translation_box)
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true

                // Chạm vào ô chữ để nghe phát âm và xem chữ gốc
                setOnClickListener {
                    speak(block.translatedText)
                    Toast.makeText(context, "Gốc: ${block.originalText}", Toast.LENGTH_SHORT).show()
                }
            }

            val boxParams = FrameLayout.LayoutParams(
                Math.max(block.boundingBox.width(), 80),
                Math.max(block.boundingBox.height(), 40)
            ).apply {
                leftMargin = block.boundingBox.left
                topMargin = block.boundingBox.top
            }

            container.addView(textBox, boxParams)
        }

        // Đóng khi bấm nút hoặc chạm ra ngoài
        btnClose.setOnClickListener { hideOverlay() }
        view.setOnClickListener { hideOverlay() }

        // Đọc toàn bộ
        btnSpeakAll.setOnClickListener {
            speak(allTranslatedText.toString())
        }

        // Sao chép toàn bộ bản dịch
        btnCopyAll.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Bản dịch", allTranslatedText.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Đã sao chép toàn bộ bản dịch!", Toast.LENGTH_SHORT).show()
        }

        try {
            windowManager.addView(view, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun speak(text: String) {
        if (isTtsReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
        }
    }

    fun hideOverlay() {
        overlayRoot?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // View might not be attached
            }
            overlayRoot = null
        }
    }

    fun destroy() {
        hideOverlay()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
