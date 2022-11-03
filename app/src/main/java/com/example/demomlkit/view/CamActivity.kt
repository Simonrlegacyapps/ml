package com.example.demomlkit.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config.OptionPriority
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.example.demomlkit.databinding.ActivityCamBinding
import com.example.demomlkit.utils.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.Dispatchers
import java.io.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import androidx.camera.core.*
import androidx.camera.core.VideoCapture

class CamActivity : AppCompatActivity() {
    lateinit var binding: ActivityCamBinding
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
  //  private lateinit var preview: Preview
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imageAnalysis: ImageAnalysis
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var flashOn : String
    private lateinit var isLensBack : String
    private lateinit var category : String
    private var imageProcessor: VisionImageProcessor? = null
    private lateinit var videoCapture : VideoCapture //<Recorder>
    lateinit var recorder : Recorder
    lateinit var recording : Recording

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initListeners()

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProvider = cameraProviderFuture.get()
        flashOn = intent.extras?.getString("isFlash")!!
        isLensBack = intent.extras?.getString("isLensBack")!!
        category = intent.extras?.getString("category")!!
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initListeners() {
        binding.stopRecordingButton.setOnClickListener {
            videoCapture.stopRecording()
            finish()
        }

        binding.ivBackBtn.setOnClickListener {
           videoCapture.stopRecording()
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startCamera(lensFacing: Int) {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)  // CPU_GPU //STREAM_MODE
            .build()
        poseDetector = PoseDetection.getClient(options)

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector
        )

        cameraProviderFuture.addListener({
            bindPreview(lensFacing)
        }, ContextCompat.getMainExecutor(this))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("UnsafeOptInUsageError", "RestrictedApi", "SuspiciousIndentation")
    private fun bindPreview(lensFacing: Int) {
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

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
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


        videoCapture = VideoCapture.Builder()
            .setDefaultCaptureConfig(CaptureConfig.defaultEmptyCaptureConfig())
//            .setBackgroundExecutor(Dispatchers.IO as Executor)
           // .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetResolution(Size(binding.myCameraView.measuredWidth, binding.myCameraView.measuredHeight))
            .build()

        //tryVideoRecord()

        try {
            cameraProvider?.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageAnalysis
                , videoCapture
            )?.cameraControl?.enableTorch(flashOn == "yes")
        } catch (e: Exception) {
            toast(applicationContext, "This device is not supported")
            Log.d("TAGtrycatch", "bindPreview: ${e.message.toString()}")
        }

        val file = File(
            filesDir.absolutePath,
            "${category}_Video_${System.currentTimeMillis()}.mp4"
        )

        // camera:core lib
        val options = VideoCapture.OutputFileOptions
            .Builder(file)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) toast(this, "Please allow audio recording permission")

        videoCapture.startRecording(
            options,
            ContextCompat.getMainExecutor(this),
            object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    Log.d("TAGsavedvid01", "onVideoSaved: ${outputFileResults.savedUri}")
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    toast(applicationContext, message.toString())
                    Log.d("TAGsavedvid02", "onVideoSaved: ${videoCaptureError}//${message}")
                }
            })
    }

//    private fun tryVideoRecord() {
//        val selector = QualitySelector
//            .from(
//                Quality.UHD,
//                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
//            )
//
//        recorder = Recorder.Builder()
//            .setQualitySelector(selector)
//            .build()
//        videoCapture = VideoCapture.withOutput(recorder)
//
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW-Video")
//            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
//        }
//
//        val mediaStoreOutputOptions = MediaStoreOutputOptions
//            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//            .setContentValues(contentValues)
//            .build()
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) return
//
//        recording = videoCapture.output
//            .prepareRecording(this, mediaStoreOutputOptions)
//            .withAudioEnabled()
//            .start(ContextCompat.getMainExecutor(this), recordingListener)
//    }
//
//    val recordingListener = Consumer<VideoRecordEvent> { event ->
//        when(event) {
//            is VideoRecordEvent.Start -> {
//                Toast.makeText(applicationContext, "started", Toast.LENGTH_SHORT).show()
//            }
//            is VideoRecordEvent.Finalize -> {
//                val msg = if (!event.hasError()) {
//                    "Video capture succeeded: ${event.outputResults.outputUri}"
//                } else {
//                    // update app state when the capture failed.
//                    recording?.close()
////                    recording = null
//                    "Video capture ends with error: ${event.error}"
//                }
//                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        if (isLensBack == "yes") startCamera(CameraSelector.LENS_FACING_BACK)
        else startCamera(CameraSelector.LENS_FACING_FRONT)
        binding.stopRecordingButton.visibility = View.VISIBLE
    }

    @SuppressLint("RestrictedApi")
    override fun onStop() {
        super.onStop()
        imageProcessor?.run { this.stop() }
        videoCapture.stopRecording()
        //videoCapture
        //  recording.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }
}



//        binding.rotateCameraIconFront.setOnClickListener {
//            binding.rotateCameraIconBack.visibility = View.VISIBLE
//            binding.rotateCameraIconFront.visibility = View.GONE

//            cameraSelector = CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
//                .build()
//
//          //  cameraProvider?.unbindAll()
//            cameraProvider?.bindToLifecycle(
//                this,
//                cameraSelector,
//                videoCapture
//            )
//
//           // startCamera(CameraSelector.LENS_FACING_FRONT)
//        }
//        binding.rotateCameraIconBack.setOnClickListener {
//            binding.rotateCameraIconBack.visibility = View.GONE
//            binding.rotateCameraIconFront.visibility = View.VISIBLE
//            binding.flashIconOn.visibility = View.GONE
//            binding.flashIconOff.visibility = View.VISIBLE
//            flashOn = false
//
//            cameraSelector = CameraSelector.Builder()
//                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
//                .build()
//
//           // cameraProvider?.unbindAll()
//            cameraProvider?.bindToLifecycle(
//                this,
//                cameraSelector,
//                videoCapture
//            )
//
//            //startCamera(CameraSelector.LENS_FACING_BACK)
//        }
//
//        binding.flashIconOn.setOnClickListener {
//            binding.flashIconOn.visibility = View.GONE
//            binding.flashIconOff.visibility = View.VISIBLE
//            flashOn = false
//            cameraProvider?.bindToLifecycle(
//                this as LifecycleOwner,
//                cameraSelector,
//                preview,
//                imageAnalysis
//            )?.cameraControl?.enableTorch(flashOn)
//        }
//        binding.flashIconOff.setOnClickListener {
//            binding.flashIconOn.visibility = View.VISIBLE
//            binding.flashIconOff.visibility = View.GONE
//            flashOn = true
//            cameraProvider?.bindToLifecycle(
//                this as LifecycleOwner,
//                cameraSelector,
//                preview,
//                imageAnalysis
//            )?.cameraControl?.enableTorch(flashOn)
//        }







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