package com.example.demomlkit.view

//import androidx.camera.core.VideoCapture

import android.Manifest
import android.annotation.SuppressLint

import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import com.example.demomlkit.databinding.ActivityCamBinding
import com.example.demomlkit.utils.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import org.jcodec.api.awt.AWTSequenceEncoder
import org.jcodec.common.AndroidUtil
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.ColorSpace.RGB
import org.jcodec.common.model.Picture
import org.jcodec.common.model.Rational
import java.io.File
import java.lang.System.out
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class CamActivity : AppCompatActivity() {

    lateinit var binding: ActivityCamBinding
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var preview: Preview
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imageAnalysis: ImageAnalysis
    private var cameraProvider: ProcessCameraProvider? = null
    var flashOn: Boolean = false
    private var imageProcessor: VisionImageProcessor? = null
    lateinit var bitmapArray : ArrayList<Bitmap>
   // lateinit var saveVidDialog : AlertDialog

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PrefManager.putString("isLoggedIn", "yes")
        bitmapArray = ArrayList()
        initListeners()
    }

    fun creteRootPath(): File? {
        var file: File? = null
        try {
            file = File(getExternalFilesDir(null).toString() + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date()) +"-mlkit.mp4")
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                file =  File(
//                    applicationContext
//                    .getExternalFilesDir("")
//                    .toString() + File.separator + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date()) +"-mlkit.mp4")
//            } else {
//                file = File(Environment
//                    .getExternalStorageDirectory()
//                    .absolutePath.toString()
//                        + File.separator + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date()) + "-mlkit.mp4")
//            }
//            if (file != null && file.exists()) file.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
            file // it will return null
        }
        return file
    }

    fun convertImagesToVideo() {
        try {
            //Rational(1, 1). SequenceEncoder
            val output = creteRootPath()
            val enc = AWTSequenceEncoder.createWithFps(
                NIOUtils.writableChannel(output), Rational.R(8, 2)
            )
            for (bitmap in bitmapArray) {
                enc.encodeNativeFrame(fromBitmaps(bitmap))
            }
            enc.finish()
        } finally {
            NIOUtils.closeQuietly(out)
           // saveVidDialog.dismiss()
            toast(this@CamActivity, "Saved")
            finish()
        }
    }

    fun fromBitmaps(src: Bitmap): Picture {
        val dst = Picture.create(src.width, src.height, RGB)
        AndroidUtil.fromBitmap(src, dst)
        return dst
    }

    override fun onBackPressed() {
      //  super.onBackPressed()
//        val dialogBuilder = AlertDialog.Builder(this@CamActivity)
//        saveVidDialog = dialogBuilder.create()
//        saveVidDialog.setTitle("Saving video..")
//        saveVidDialog.show()
        toast(this@CamActivity, "Saving video..")
        convertImagesToVideo()
    }

//    private fun recordScreen() {
//        mMediaRecorder = MediaRecorder()
//        mDisplayMetrics = DisplayMetrics()
//
//        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        windowManager.defaultDisplay.getMetrics(mDisplayMetrics)
//
//        mMediaProjectionCallback = MediaProjectionCallback()
//
//        val width = mDisplayMetrics!!.widthPixels
//        val height = mDisplayMetrics!!.heightPixels
//
//        val directory =
//            "${getExternalFilesDir(null)}" + "${File.separator}Recordings"
//        if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) {
//            Toast.makeText(this, "Failed to get External Storage", Toast.LENGTH_SHORT).show()
//            return
//        }
//        val folder = File(directory)
//        var success = true
//        if (!folder.exists()) {
//            success = folder.mkdir()
//        }
//        val filePath= if (success) {
//            val videoName = "capture_" + getCurSysDate().toString() + ".mp4"
//            directory + File.separator.toString() + videoName
//        } else {
//            Toast.makeText(this, "Failed to create Recordings directory", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
//        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
//        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//        mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//        mMediaRecorder!!.setAudioSamplingRate(16000)
//        mMediaRecorder!!.setVideoEncodingBitRate(512 * 1000)
//        mMediaRecorder!!.setVideoFrameRate(30)
//        mMediaRecorder!!.setVideoSize(width, height)
//        mMediaRecorder!!.setOutputFile(filePath)
//
//        try {
//            mMediaRecorder!!.prepare()
//        } catch (e: java.lang.Exception) {
//            toast(applicationContext, e.message.toString())
//            e.printStackTrace()
//            return
//        }
//
//
//        // starting recording
//        // If mMediaProjection is null that means we didn't get a context, lets ask the user
//        if (mMediaProjection == null) {
//            // This asks for user permissions to capture the screen
//            startActivityForResult(mProjectionManager!!.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
//            return
//        }
//        mVirtualDisplay = getVirtualDisplay()
//        mMediaRecorder!!.start()
//    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode != CAST_PERMISSION_CODE) {
//            Log.w("TAG", "Unknown request code: $requestCode")
//            return
//        }
//        if (resultCode != RESULT_OK) {
//            Toast.makeText(this, "Screen Cast Permission Denied :(", Toast.LENGTH_SHORT).show()
//            return
//        }
//        mMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data!!)
//        // TODO Register a callback that will listen onStop and release & prepare the recorder for next recording
//        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null);
//        mVirtualDisplay = getVirtualDisplay()
//        mMediaRecorder!!.start()
//    }

//    private fun getVirtualDisplay(): VirtualDisplay? {
//        val screenDensity = mDisplayMetrics!!.densityDpi
//        val width = mDisplayMetrics!!.widthPixels
//        val height = mDisplayMetrics!!.heightPixels
//        return mMediaProjection!!.createVirtualDisplay(
//            this.javaClass.simpleName,
//            width, height, screenDensity,
//            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//            mMediaRecorder!!.surface, null /*Callbacks*/, null /*Handler*/
//        )
//    }
//
//    fun getCurSysDate(): String? {
//        return SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
//    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun initListeners() {
        binding.rotateCameraIconFront.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.VISIBLE
            binding.rotateCameraIconFront.visibility = View.GONE
            startCamera(CameraSelector.LENS_FACING_FRONT)
        }
        binding.rotateCameraIconBack.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.GONE
            binding.rotateCameraIconFront.visibility = View.VISIBLE
            binding.flashIconOn.visibility = View.GONE
            binding.flashIconOff.visibility = View.VISIBLE
            flashOn = false
            startCamera(CameraSelector.LENS_FACING_BACK)
        }

        binding.flashIconOn.setOnClickListener {
            binding.flashIconOn.visibility = View.GONE
            binding.flashIconOff.visibility = View.VISIBLE
            flashOn = false
            cameraProviderFuture.get().bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            ).cameraControl.enableTorch(flashOn)
        }
        binding.flashIconOff.setOnClickListener {
            binding.flashIconOn.visibility = View.VISIBLE
            binding.flashIconOff.visibility = View.GONE
            flashOn = true
            cameraProviderFuture.get().bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            ).cameraControl.enableTorch(flashOn)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startCamera(lensFacing: Int) {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetector = PoseDetection.getClient(options)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProvider = cameraProviderFuture.get()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        cameraProvider?.unbindAll() //videoCapture
        cameraProvider?.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector
        )

        cameraProviderFuture.addListener({
            bindPreview(lensFacing)
        }, ContextCompat.getMainExecutor(this))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
    private fun bindPreview(lensFacing: Int) {
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.myCameraView.surfaceProvider)
        }

//        cameraSelector = CameraSelector.Builder()
//            .requireLensFacing(lensFacing)
//            .build()

//        val qualitySelector =
//            QualitySelector.from(Quality.HD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD))
//        val recorder = Recorder.Builder()
//            .setQualitySelector(qualitySelector)
//            .build()
//
//        videoCapture = VideoCapture.withOutput(recorder)

        if (imageProcessor != null) imageProcessor!!.stop()

        imageProcessor =
            try {
                PoseDetectorProcessor(
                    this,
                    poseDetector,
                    showInFrameLikelihood = true,
                    visualizeZ = false,
                    rescaleZForVisualization = false,
                    runClassification = true,
                    isStreamMode = true
                )
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    "Can not create image processor: " + e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()
                return
            }

//        videoCapture = VideoCapture.Builder()
//            .setTargetResolution(Size(Point().x,Point().y))
//            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
            bitmapArray.add(getBitmap(imageProxy)!!)

            if (imageProxy.image != null) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT

                if (rotationDegrees == 0 || rotationDegrees == 180)
                    binding.graphicOverlay.setImageSourceInfo(
                        imageProxy.width,
                        imageProxy.height,
                        isImageFlipped
                    )
                else
                    binding.graphicOverlay.setImageSourceInfo(
                        imageProxy.height,
                        imageProxy.width,
                        isImageFlipped
                    )

                try {
                    imageProcessor?.processImageProxy(imageProxy, binding.graphicOverlay)
                } catch (e: MlKitException) {
                    Log.e("TAG", "Failed to process image. Error: " + e.localizedMessage)
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

       // cameraProvider?.unbindAll() //videoCapture
        cameraProvider?.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis,
            preview
        )?.cameraControl?.enableTorch(flashOn)

        // startVideoRecording()

        ///
//        val file = File(externalMediaDirs.first(),
//            "${System.currentTimeMillis()}.mp4")

//        val outputFileOptions =  VideoCapture.OutputFileOptions.Builder(file).build()

//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) return
//
//        videoCapture.startRecording(outputFileOptions,ContextCompat.getMainExecutor(this),object: VideoCapture.OnVideoSavedCallback{
//            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
//                Log.d("TAGsavedvid01", "onVideoSaved: ${outputFileResults.savedUri}")
//            }
//
//            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
//                Log.d("TAGsavedvid02", "onVideoSaved: ${videoCaptureError}//${message}")
//            }
//        })
    }


    private fun startVideoRecording() {
        // create MediaStoreOutputOptions for our recorder,, resulting the recording..
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.mp4")
        }

        val mediaStoreOutput = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            toast(this, "Give Audio Permission")
            return
        }

//        currentRecording = videoCapture.output
//            .prepareRecording(this, mediaStoreOutput)
//            .withAudioEnabled()
//            .start(ContextCompat.getMainExecutor(this), captureListener)
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // listen when video get stopped...
        val durationInNanos = event.recordingStats.recordedDurationNanos
        val durationInSeconds = durationInNanos / 1000 / 1000 / 1000.0

        when (event) {
            is VideoRecordEvent.Start -> {
                Toast.makeText(applicationContext, "Capture Started", Toast.LENGTH_SHORT).show()
                // update app internal recording state
            }
            is VideoRecordEvent.Finalize -> {
                if (!event.hasError()) {
                    Toast.makeText(
                        applicationContext,
                        "Video capture succeeded: ${event.outputResults.outputUri}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // update app state when the capture failed.
//                    recording?.close()
//                    recording = null
                    Toast.makeText(
                        applicationContext,
                        "Video capture ends with error: ${event.error}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        startCamera(CameraSelector.LENS_FACING_BACK)
    }

//    override fun onPause() {
//        super.onPause()
//        imageProcessor?.run { this.stop() }
//    }

    override fun onStop() {
        super.onStop()
        imageProcessor?.run { this.stop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }
}


// val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

// Passing image to mlkit
//                poseDetector.process(inputImage)
//                    .addOnSuccessListener { obj ->
//                        if (obj.allPoseLandmarks.size>0) {
//                            if (binding.parentLayout.childCount > 3) binding.parentLayout.removeViewAt(3)
//                            val mView = Draw(this, obj, binding.tvAngle)
//
//                            binding.parentLayout.addView(mView)
//                        } else if (binding.parentLayout.childCount > 3) {
//                            binding.parentLayout.removeViewAt(3)
//                            binding.tvAngle.text = ""
//                        }
//                        imageProxy.close()
//                    }.addOnFailureListener {
//                        Log.d("TAGpose00", "onCreate: ${it.message}")
//                        toast(applicationContext, it.message.toString())
//                        imageProxy.close()
//                    }