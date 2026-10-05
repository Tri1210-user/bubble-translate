package com.bubbletranslate.screen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingBubbleService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var overlayManager: OverlayViewManager

    private var fromLang: String = "EN"
    private var toLang: String = "VI"
    private var isTranslating = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayManager = OverlayViewManager(this)
        startForegroundServiceNotification()
        createFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            fromLang = it.getStringExtra("FROM_LANG") ?: "EN"
            toLang = it.getStringExtra("TO_LANG") ?: "VI"
            updateBubbleBadge()
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "bubble_translate_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bubble Translate Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bubble Translate đang hoạt động")
            .setContentText("Chạm vào bong bóng nổi để dịch màn hình ngay lập tức")
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1001, notification)
    }

    private fun createFloatingBubble() {
        val inflater = LayoutInflater.from(this)
        bubbleView = inflater.inflate(R.layout.layout_floating_bubble, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        bubbleParams.gravity = Gravity.TOP or Gravity.START
        bubbleParams.x = 20
        bubbleParams.y = 300

        setupTouchListener()
        updateBubbleBadge()

        try {
            windowManager.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBubbleBadge() {
        bubbleView?.findViewById<TextView>(R.id.tvBubbleBadge)?.text = toLang
    }

    private fun setupTouchListener() {
        val view = bubbleView ?: return
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        hasMoved = true
                    }
                    bubbleParams.x = initialX + dx
                    bubbleParams.y = initialY + dy
                    windowManager.updateViewLayout(bubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved) {
                        // Người dùng chạm vào bong bóng -> Thực hiện dịch màn hình trực tiếp!
                        onBubbleClicked()
                    } else {
                        // Tự động hít vào mép viền trái hoặc phải
                        snapBubbleToEdge()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapBubbleToEdge() {
        val displaySize = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(displaySize)
        val screenWidth = displaySize.x
        val middle = screenWidth / 2

        bubbleParams.x = if (bubbleParams.x + (bubbleView?.width ?: 0) / 2 < middle) {
            16
        } else {
            screenWidth - (bubbleView?.width ?: 140) - 16
        }
        windowManager.updateViewLayout(bubbleView, bubbleParams)
    }

    /**
     * DỊCH MÀN HÌNH TRỰC TIẾP TRONG 1 CHẠM:
     * 1. Chụp màn hình máy hiện tại qua MediaProjection
     * 2. Nhận diện chữ bằng Google ML Kit
     * 3. Dịch sang tiếng Việt
     * 4. Vẽ bản dịch đè trực tiếp lên màn hình
     */
    private fun onBubbleClicked() {
        if (isTranslating) return
        isTranslating = true

        val spinner = bubbleView?.findViewById<ProgressBar>(R.id.bubbleSpinner)
        val icon = bubbleView?.findViewById<View>(R.id.ivBubbleIcon)
        spinner?.visibility = View.VISIBLE
        icon?.visibility = View.INVISIBLE

        serviceScope.launch {
            try {
                if (!ScreenCaptureManager.isReady()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            "Vui lòng mở ứng dụng Bubble Translate và cấp quyền chụp màn hình!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                // 1. Chụp màn hình trực tiếp
                val bitmap = ScreenCaptureManager.captureScreen()
                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Không thể chụp màn hình. Hãy thử lại!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 2. Nhận diện chữ qua OCR ML Kit
                val blocks = OcrEngine.recognizeText(bitmap, fromLang)
                if (blocks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Không tìm thấy chữ nào trên màn hình này.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 3. Dịch song song các câu chữ
                val deferredTranslations = blocks.map { block ->
                    async(Dispatchers.IO) {
                        val trans = TranslationEngine.translate(block.originalText, fromLang, toLang)
                        block.translatedText = trans
                    }
                }
                deferredTranslations.awaitAll()

                // 4. Hiển thị lớp phủ đè trực tiếp lên màn hình máy
                withContext(Dispatchers.Main) {
                    overlayManager.showTranslationOverlay(blocks, toLang)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Lỗi dịch màn hình: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isTranslating = false
                withContext(Dispatchers.Main) {
                    spinner?.visibility = View.GONE
                    icon?.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        overlayManager.destroy()
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
