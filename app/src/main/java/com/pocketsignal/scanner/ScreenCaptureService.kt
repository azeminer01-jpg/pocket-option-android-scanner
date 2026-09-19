package com.pocketsignal.scanner

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.core.app.NotificationCompat
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val busy = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private var lastSent = 0L
    private lateinit var endpoint: String
    private lateinit var asset: String
    private lateinit var duration: String
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        endpoint = intent?.getStringExtra("endpoint") ?: return START_NOT_STICKY
        asset = intent.getStringExtra("asset") ?: "GBP/USD OTC"
        duration = intent.getStringExtra("duration") ?: "30 seconds"
        val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data = intent.getParcelableExtra<Intent>("data")

        startForeground(
            1001,
            NotificationCompat.Builder(this, "scanner")
                .setContentTitle("Pocket Option Scanner")
                .setContentText("Canlı ekran analizi aktivdir")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build()
        )

        if (data != null && resultCode == Activity.RESULT_OK) {
            startProjection(resultCode, data)
        }
        return START_NOT_STICKY
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(resultCode, data)

        val dm = resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        val density = dm.densityDpi

        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        imageReader!!.setOnImageAvailableListener({ reader ->
            val now = System.currentTimeMillis()
            if (now - lastSent < 2000L || !busy.compareAndSet(false, true)) {
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }

            val image = reader.acquireLatestImage() ?: run {
                busy.set(false)
                return@setOnImageAvailableListener
            }
            lastSent = now

            Thread {
                try {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val bmpWidth = width + rowPadding / pixelStride

                    val bitmap = Bitmap.createBitmap(bmpWidth, height, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(buffer)

                    val cropped = if (bmpWidth != width) {
                        Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    } else bitmap

                    val scaled = if (width > 1280) {
                        val h = (height * 1280f / width).toInt()
                        Bitmap.createScaledBitmap(cropped, 1280, h, true)
                    } else cropped

                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 62, out)
                    sendFrame(out.toByteArray())

                    if (scaled !== cropped) scaled.recycle()
                    if (cropped !== bitmap) cropped.recycle()
                    bitmap.recycle()
                } catch (_: Exception) {
                } finally {
                    image.close()
                    busy.set(false)
                }
            }.start()
        }, handler)

        virtualDisplay = projection!!.createVirtualDisplay(
            "PocketOptionScanner",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, handler
        )
    }

    private fun sendFrame(bytes: ByteArray) {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("asset", asset)
            .addFormDataPart("duration", duration)
            .addFormDataPart(
                "image", "frame.jpg",
                bytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder().url(endpoint).post(body).build()
        try {
            client.newCall(request).execute().use { response ->
                // Result is intentionally not auto-traded.
                // The server response can be viewed in a later UI version.
            }
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                "scanner", "Live Scanner",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }
}
