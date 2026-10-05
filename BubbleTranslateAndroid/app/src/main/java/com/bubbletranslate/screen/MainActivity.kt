package com.bubbletranslate.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var projectionManager: MediaProjectionManager
    private var isServiceRunning = false

    private var pendingFromLang = "EN"
    private var pendingToLang = "VI"

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (checkOverlayPermission()) {
            requestMediaProjectionPermission()
        } else {
            Toast.makeText(this, "Cần cấp quyền 'Xuất hiện trên cùng' để hiển thị bong bóng dịch!", Toast.LENGTH_LONG).show()
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureManager.init(this, result.resultCode, result.data!!)
            launchFloatingBubbleService(pendingFromLang, pendingToLang)
        } else {
            Toast.makeText(this, "Không có quyền chụp màn hình. Dịch trực tiếp không thể hoạt động!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        setupWebView()
    }

    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Thông báo cho Web biết đang chạy trên app Android Native
                webView.evaluateJavascript("window.IS_NATIVE_ANDROID = true; if(window.onNativeReady) window.onNativeReady();", null)
            }
        }

        webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")
        webView.loadUrl("file:///android_asset/app.html")
    }

    fun isServiceActive(): Boolean = isServiceRunning

    fun checkAndRequestAllPermissions() {
        if (!checkOverlayPermission()) {
            requestOverlayPermission()
        } else if (!ScreenCaptureManager.isReady()) {
            requestMediaProjectionPermission()
        }
    }

    fun startFloatingService(fromLang: String, toLang: String) {
        pendingFromLang = fromLang
        pendingToLang = toLang

        if (!checkOverlayPermission()) {
            requestOverlayPermission()
            return
        }

        if (!ScreenCaptureManager.isReady()) {
            requestMediaProjectionPermission()
            return
        }

        launchFloatingBubbleService(fromLang, toLang)
    }

    private fun launchFloatingBubbleService(from: String, to: String) {
        val intent = Intent(this, FloatingBubbleService::class.java).apply {
            putExtra("FROM_LANG", from)
            putExtra("TO_LANG", to)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isServiceRunning = true
        Toast.makeText(this, "Bong bóng dịch đã bật! Hãy chuyển sang app khác để dịch trực tiếp.", Toast.LENGTH_LONG).show()
        
        // Cập nhật trạng thái cho Web UI
        webView.evaluateJavascript("if(window.onServiceStateChanged) window.onServiceStateChanged(true);", null)
    }

    fun stopFloatingService() {
        val intent = Intent(this, FloatingBubbleService::class.java)
        stopService(intent)
        isServiceRunning = false
        webView.evaluateJavascript("if(window.onServiceStateChanged) window.onServiceStateChanged(false);", null)
        Toast.makeText(this, "Đã tắt bong bóng dịch màn hình.", Toast.LENGTH_SHORT).show()
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(this, "Vui lòng cho phép quyền 'Xuất hiện trên cùng' cho Bubble Translate", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun requestMediaProjectionPermission() {
        val intent = projectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
