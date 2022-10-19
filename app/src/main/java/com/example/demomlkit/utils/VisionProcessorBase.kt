package com.example.demomlkit.utils

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build.VERSION_CODES
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.GuardedBy
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.*
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer
import java.util.*

abstract class VisionProcessorBase<T> protected constructor(context: Context) : VisionImageProcessor {
    private val activityManager: ActivityManager
    private val fpsTimer = Timer()
    private val executor: ScopedExecutor
    // Whether this processor is already shut down
    private var isShutdown = false
    // Used to calculate latency, running in the same thread, no sync needed.
    private var numRuns = 0
    private var totalRunMs: Long = 0
    private var maxRunMs: Long = 0
    private var minRunMs = Long.MAX_VALUE
    // Frame count that have been processed so far in an one second interval to calculate FPS.
    private var frameProcessedInOneSecondInterval = 0
    private var framesPerSecond = 0

    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private var latestImage: ByteBuffer? = null
    @GuardedBy("this")
    private var latestImageMetaData: FrameMetadata? = null
    // To keep the images and metadata in process.
    @GuardedBy("this")
    private var processingImage: ByteBuffer? = null
    @GuardedBy("this")
    private var processingMetaData: FrameMetadata? = null

    init {
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

        fpsTimer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                        framesPerSecond = frameProcessedInOneSecondInterval
                        frameProcessedInOneSecondInterval = 0
                }
            }, 0, 1000
        )
    }

    // code for processing single still image
    override fun processBitmap(bitmap: Bitmap?, graphicOverlay: GraphicOverlay) {
        bitmap?.let { bit ->
            requestDetectInImage(
                InputImage.fromBitmap(bit, 0),
                graphicOverlay,
                null,
                false
            )
        }
    }

    //Code for processing live preview frame from CameraX API
    @RequiresApi(VERSION_CODES.KITKAT)
    @ExperimentalGetImage
    override fun processImageProxy(image: ImageProxy?, graphicOverlay: GraphicOverlay) {
        var bitmap: Bitmap? = null
        image?.let { im ->
            if (isShutdown) {
                im.close()
                return
            }

            bitmap = getBitmap(im)

            im.image?.let { InputImage.fromMediaImage(it, image.imageInfo.rotationDegrees) }?.let {
                requestDetectInImage(
                    it,
                    graphicOverlay,
                    bitmap,
                    true
                ).addOnCompleteListener { results: Task<T>? -> image.close() }
            }
        }
    }

    //Common processing logic
    private fun requestDetectInImage(
        image: InputImage,
        graphicOverlay: GraphicOverlay,
        @Nullable originalCameraImage: Bitmap?,
        shouldShowFps: Boolean
    ): Task<T> {
        val startMs = SystemClock.elapsedRealtime()
        return detectInImage(image)
            .addOnSuccessListener(executor) { results: T ->
                /**Uncomment this code block below*/
//                val currentLatencyMs : Long = SystemClock.elapsedRealtime() - startMs
//                numRuns++
//                frameProcessedInOneSecondInterval++
//                totalRunMs += currentLatencyMs
//                maxRunMs = Math.max(currentLatencyMs, maxRunMs)
//                minRunMs = Math.min(currentLatencyMs, minRunMs)

                // Only log inference info once, per second. When frameProcessedInOneSecondInterval is
                // equal to 1, it means this is the first frame processed during the current second.
//                if (frameProcessedInOneSecondInterval == 1) {
//                    val mi = ActivityManager.MemoryInfo()
//                    activityManager.getMemoryInfo(mi)
//                    val availableMegs: Long = mi.availMem / 0x100000L
//                    Log.d(TAG, "Memory available in system: $availableMegs MB")
//                }
                graphicOverlay.clear()
                if (originalCameraImage != null)
                    graphicOverlay.add(CameraImageGraphic(graphicOverlay, originalCameraImage))

//                graphicOverlay.add(
//                    InferenceInfoGraphic(
//                        graphicOverlay,
//                        currentLatencyMs,
//                        if (shouldShowFps) framesPerSecond else null
//                    )
//                )
                this@VisionProcessorBase.onSuccess(results, graphicOverlay)
                graphicOverlay.postInvalidate()
            }.addOnFailureListener(executor) { e: Exception ->
                graphicOverlay.clear()
                graphicOverlay.postInvalidate()
                val error = "Failed to process : " + e.localizedMessage
                Toast.makeText(
                    graphicOverlay.context,
                    """
                        $error
                        Cause: ${e.cause}
                        """.trimIndent(),
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, error)
                e.printStackTrace()
                this@VisionProcessorBase.onFailure(e)
            }
    }

    override fun stop() {
        executor.shutdown()
        isShutdown = true
        numRuns = 0
        totalRunMs = 0
        fpsTimer.cancel()
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>
    protected abstract fun onSuccess(results: T, graphicOverlay: GraphicOverlay)
    protected abstract fun onFailure(e: Exception)

    companion object {
        private const val TAG = "VisionProcessorBase"
    }
}
















//    // -----------------Code for processing live preview frame from Camera1 API-----------------------
//    @Synchronized
//    fun processByteBuffer(
//        data: ByteBuffer?, frameMetadata: FrameMetadata?, graphicOverlay: GraphicOverlay
//    ) {
//        latestImage = data
//        latestImageMetaData = frameMetadata
//        if (processingImage == null && processingMetaData == null) {
//            processLatestImage(graphicOverlay)
//        }
//    }
//
//    @Synchronized
//    private fun processLatestImage(graphicOverlay: GraphicOverlay) {
//        processingImage = latestImage
//        processingMetaData = latestImageMetaData
//        latestImage = null
//        latestImageMetaData = null
//        if (processingImage != null && processingMetaData != null && !isShutdown) {
//            processImage(processingImage!!, processingMetaData!!, graphicOverlay)
//        }
//    }
//
//    private fun processImage(
//        data: ByteBuffer, frameMetadata: FrameMetadata, graphicOverlay: GraphicOverlay
//    ) {
//        // If live viewport is on (that is the underneath surface view takes care of the camera preview
//        // drawing), skip the unnecessary bitmap creation that used for the manual preview drawing.
//        val bitmap: Bitmap? =
//            if (PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.context)) null else BitmapUtils.getBitmap(
//                data,
//                frameMetadata
//            )
//        requestDetectInImage(
//            InputImage.fromByteBuffer(
//                data,
//                frameMetadata.width,
//                frameMetadata.height,
//                frameMetadata.rotation,
//                InputImage.IMAGE_FORMAT_NV21
//            ),
//            graphicOverlay,
//            bitmap,  /* shouldShowFps= */
//            true
//        )
//            .addOnSuccessListener(executor) { results: T ->
//                processLatestImage(
//                    graphicOverlay
//                )
//            }
//    }
