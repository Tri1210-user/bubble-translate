package com.bubbletranslate.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ScreenCaptureManager {

    private var mediaProjection: MediaProjection? = null
    private var densityDpi: Int = DisplayMetrics.DENSITY_DEFAULT
    private var screenWidth: Int = 1080
    private var screenHeight: Int = 1920

    fun init(context: Context, resultCode: Int, data: Intent) {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        densityDpi = metrics.densityDpi
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    fun isReady(): Boolean = mediaProjection != null

    suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.IO) {
        val projection = mediaProjection ?: return@withContext null

        val imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )

        val virtualDisplay: VirtualDisplay = projection.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

        // Chờ một nhịp nhỏ để frame kịp kết xuất vào ImageReader
        delay(60)

        var image: Image? = null
        try {
            image = imageReader.acquireLatestImage()
            if (image == null) {
                // Thử lại lần 2
                delay(40)
                image = imageReader.acquireLatestImage()
            }

            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth

                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                // Cắt bỏ phần padding nếu có
                val cleanBitmap = if (rowPadding > 0) {
                    Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                } else {
                    bitmap
                }
                return@withContext cleanBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image?.close()
            virtualDisplay.release()
            imageReader.close()
        }

        return@withContext null
    }

    fun release() {
        mediaProjection?.stop()
        mediaProjection = null
    }
}
