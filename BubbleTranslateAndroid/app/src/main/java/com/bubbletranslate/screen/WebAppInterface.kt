package com.bubbletranslate.screen

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val activity: MainActivity) {

    @JavascriptInterface
    fun startBubbleService(fromLang: String, toLang: String) {
        activity.runOnUiThread {
            activity.startFloatingService(fromLang, toLang)
        }
    }

    @JavascriptInterface
    fun stopBubbleService() {
        activity.runOnUiThread {
            activity.stopFloatingService()
        }
    }

    @JavascriptInterface
    fun isServiceRunning(): Boolean {
        return activity.isServiceActive()
    }

    @JavascriptInterface
    fun requestPermissions() {
        activity.runOnUiThread {
            activity.checkAndRequestAllPermissions()
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
}
