package com.example.demomlkit.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.demomlkit.databinding.ActivityCamBinding
import com.example.demomlkit.utils.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.io.*
import java.util.concurrent.Executors


class CamActivity : AppCompatActivity() {
    lateinit var binding: ActivityCamBinding
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imageAnalysis: ImageAnalysis
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var flashOn : String
    private lateinit var isLensBack : String
    private lateinit var category : String
    private var imageProcessor: VisionImageProcessor? = null
    private lateinit var videoCapture : VideoCapture
//    private lateinit var mMediaProjectionManager : MediaProjectionManager
//    private lateinit var mRecorder : MediaRecorder
//    private var mDisplay : VirtualDisplay? = null
//    private var mMediaProjection : MediaProjection? = null

    //////
    private var mScreenDensity = 0
    private var mProjectionManager: MediaProjectionManager? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionCallback: MediaProjectionCallback? = null
    private var mMediaRecorder: MediaRecorder? = null

    companion object {
///
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE = 1000
        private const val DISPLAY_WIDTH = 720
        private const val DISPLAY_HEIGHT = 1280
        private val ORIENTATIONS = SparseIntArray()
        private const val REQUEST_PERMISSIONS = 10

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
////
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.CAMERA)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) return
            else toast(this, "Permissions are not granted")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAllPermissions()
        initListeners()

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProvider = cameraProviderFuture.get()
        flashOn = intent.extras?.getString("isFlash")!!
        isLensBack = intent.extras?.getString("isLensBack")!!
        category = intent.extras?.getString("category")!!

        // screen record
//        val metrics = DisplayMetrics()
//        windowManager.defaultDisplay.getMetrics(metrics)
//        mScreenDensity = metrics.densityDpi
//        mMediaRecorder = MediaRecorder()
//        mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        screenShare()
    }

    private fun screenShare() {
        initRecorder()
        shareScreen()
    }

    private fun shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager!!.createScreenCaptureIntent(), REQUEST_CODE)
            return
        }
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder!!.start()
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay(
            "MainActivity",
            DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder!!.surface, null /*Callbacks*/, null /*Handler*/
        )
    }

    private fun initRecorder() {
        try {
            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mMediaRecorder!!.setOutputFile(
                Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + "/video.mp4"
            )
            mMediaRecorder!!.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mMediaRecorder!!.setVideoEncodingBitRate(512 * 1000)
            mMediaRecorder!!.setVideoFrameRate(30)
            val rotation = windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS[rotation + 90]
            mMediaRecorder!!.setOrientationHint(orientation)
            mMediaRecorder!!.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
            mMediaProjection = null
            stopScreenSharing()
        }
    }

    private fun stopScreenSharing() {
        if (mVirtualDisplay == null) return
        mVirtualDisplay!!.release()
        //mMediaRecorder.release(); //If used: mMediaRecorder object cannot
        // be reused again
        destroyMediaProjection()
    }

    public override fun onDestroy() {
        super.onDestroy()
        destroyMediaProjection()
    }

    private fun destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection!!.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
        Log.i(TAG, "MediaProjection Stopped")
    }

    private fun checkAllPermissions() {
        if (allPermissionsGranted()) return
        else ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initListeners() {
        binding.stopRecordingButton.setOnClickListener {
         //   videoCapture.stopRecording()
            finish()
        }

        binding.ivBackBtn.setOnClickListener {
        //   videoCapture.stopRecording()
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startCamera(lensFacing: Int) {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)  // CPU_GPU
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
            .setTargetResolution(Size(binding.myCameraView.measuredWidth, binding.myCameraView.measuredHeight))
            .build()

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

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        if (isLensBack == "yes") startCamera(CameraSelector.LENS_FACING_BACK)
        else startCamera(CameraSelector.LENS_FACING_FRONT)
        binding.stopRecordingButton.visibility = View.VISIBLE

//        prepareRecording()
//        mMediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), 101)
//        mDisplay = getVirtualDisplay()
//        try {
//            mRecorder.start()
//        } catch (e:Exception) {
//            toast(this, e.message.toString())
//        }
    }

//    private fun prepareRecording() {
//        val moviesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
//        val movieFile = File(moviesFolder, "test.mp4")
//        mRecorder = MediaRecorder()
//        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
//        mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
//        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//        mRecorder.setVideoEncodingBitRate(512 * 1000)
//        mRecorder.setVideoFrameRate(30)
//        mRecorder.setVideoSize(720, 1200)
//        mRecorder.setOutputFile(movieFile.path)
//        try {
//            mRecorder.prepare()
//        } catch (e: java.lang.Exception) {
//            e.printStackTrace()
//            return
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun startScreenRecording(mediaProjection: MediaProjection, file: File): Boolean {
//        mRecorder = MediaRecorder()
//
//      //  mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
//        mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
////        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
////        profile.videoFrameHeight = metrics.heightPixels
////        profile.videoFrameWidth = metrics.widthPixels
//       // mRecorder.setProfile(profile)
//        mRecorder.setOutputFile(file)
//        mRecorder.setVideoSize(720, 1200)
//
//        mDisplay = mediaProjection.createVirtualDisplay(
//            "ScreenRecorder",
//            710,
//            1200,
//            240,
//            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//            mRecorder.surface,
//            null,
//            null)
//
//        try {
//            mRecorder.prepare()
//
////            mRecording = true
////            showNotification()
//        } catch (e:Exception) {
//            e.printStackTrace()
//            toast(this, e.message.toString())
//            return false
//        }
//
//        try {
//            mRecorder.start()
//        } catch (e:Exception) {
//            toast(this, e.message.toString())
//            return false
//        }
//        return true
//    }

    override fun onStop() {
        super.onStop()
        imageProcessor?.run { this.stop() }
        //  videoCapture.stopRecording()
        mMediaRecorder?.stop()
        mVirtualDisplay?.release()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: $requestCode")
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(
                this,
                "Screen Cast Permission Denied", Toast.LENGTH_SHORT
            ).show()

            return
        }
        mMediaProjectionCallback = MediaProjectionCallback()
        mMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data!!)
        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder!!.start()
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 101) {
//            if (resultCode == RESULT_OK) {
//                if (data != null) {
//                    mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data)
////                    startScreenRecording(mMediaProjection, movieFile)
//                    mDisplay = getVirtualDisplay()
//                    try {
//                        mRecorder.start()
//                    } catch (e:Exception) {
//                        toast(this, e.message.toString())
//                    }
//                }
//            }
//        }
//    }

//    private fun getVirtualDisplay(): VirtualDisplay? {
////        val screenDensity: Int = mDisplayMetrics.densityDpi
////        val width: Int = mDisplayMetrics.widthPixels
////        val height: Int = mDisplayMetrics.heightPixels
//        return if (mMediaProjection != null)
//            mMediaProjection!!.createVirtualDisplay(
//                javaClass.simpleName,
//                710,
//                1200,
//                240,
//                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//                mRecorder.surface,
//                null,
//                null
//            ) else return null
//    }

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