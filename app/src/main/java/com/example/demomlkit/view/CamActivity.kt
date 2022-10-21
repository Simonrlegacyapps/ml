package com.example.demomlkit.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
//import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.camera.video.*
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.camera.core.*
import androidx.camera.video.VideoCapture
import java.util.concurrent.Executors

class CamActivity : AppCompatActivity() {
    lateinit var binding: ActivityCamBinding
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var preview : Preview
    private lateinit var cameraSelector : CameraSelector
    private lateinit var imageAnalysis : ImageAnalysis
    private var cameraProvider: ProcessCameraProvider? = null
    var flashOn: Boolean = false
    private var imageProcessor: VisionImageProcessor? = null
    private lateinit var currentRecording : Recording
    private lateinit var videoCapture: VideoCapture<Recorder>

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PrefManager.putString("isLoggedIn", "yes")
        initListeners()
    }

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
            cameraProviderFuture.get().bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis).cameraControl.enableTorch(flashOn)
        }
        binding.flashIconOff.setOnClickListener {
            binding.flashIconOn.visibility = View.VISIBLE
            binding.flashIconOff.visibility = View.GONE
            flashOn = true
            cameraProviderFuture.get().bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis).cameraControl.enableTorch(flashOn)
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

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val qualitySelector = QualitySelector.from(Quality.HD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD))
        val recorder = Recorder.Builder()
        .setQualitySelector(qualitySelector)
        .build()

        videoCapture = VideoCapture.withOutput(recorder)

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
                    isStreamMode = true)
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
            val mediaImage = imageProxy.image

            Log.d("TAG_Image_Proxy", "bindPreview: ${getBitmap(imageProxy)}")

            if (mediaImage != null) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT

                if (rotationDegrees == 0 || rotationDegrees == 180)
                    binding.graphicOverlay.setImageSourceInfo(imageProxy.width, imageProxy.height, isImageFlipped)
                else
                    binding.graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width, isImageFlipped)

                try {
                    imageProcessor?.processImageProxy(imageProxy, binding.graphicOverlay)
                } catch (e: MlKitException) {
                    Log.e("TAG", "Failed to process image. Error: " + e.localizedMessage)
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        cameraProvider?.unbindAll() //videoCapture
        cameraProvider?.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)?.cameraControl?.enableTorch(flashOn)

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

        currentRecording = videoCapture.output
            .prepareRecording(this, mediaStoreOutput)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(this), captureListener)
    }

    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // listen when video get stopped...
        val durationInNanos = event.recordingStats.recordedDurationNanos
        val durationInSeconds = durationInNanos/1000/1000/1000.0

        when(event) {
            is VideoRecordEvent.Start -> {
                Toast.makeText(applicationContext, "Capture Started", Toast.LENGTH_SHORT).show()
                // update app internal recording state
            }
            is VideoRecordEvent.Finalize -> {
                if (!event.hasError()) {
                    Toast.makeText(applicationContext, "Video capture succeeded: ${event.outputResults.outputUri}", Toast.LENGTH_SHORT).show()
                } else {
                    // update app state when the capture failed.
//                    recording?.close()
//                    recording = null
                    Toast.makeText(applicationContext, "Video capture ends with error: ${event.error}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        startCamera(CameraSelector.LENS_FACING_BACK)
    }

    override fun onPause() {
        super.onPause()
        imageProcessor?.run { this.stop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }

    @SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
    override fun onStop() {
        super.onStop()
//        videoCapture.stopRecording()
    currentRecording.stop()
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