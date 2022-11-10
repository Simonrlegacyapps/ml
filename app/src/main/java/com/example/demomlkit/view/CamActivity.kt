package com.example.demomlkit.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.VideoCapture
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.lifecycle.LifecycleOwner
import com.example.demomlkit.R
import com.example.demomlkit.databinding.ActivityCamBinding
import com.example.demomlkit.utils.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


class CamActivity : AppCompatActivity() {
    lateinit var binding: ActivityCamBinding
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imageAnalysis: ImageAnalysis
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var flashOn: String
    private lateinit var isLensBack: String
    private lateinit var category: String
    private var imageProcessor: VisionImageProcessor? = null
    private lateinit var videoCapture: VideoCapture


    //////
    private var mScreenDensity = 0
    private var mProjectionManager: MediaProjectionManager? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionCallback: MediaProjectionCallback? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var videoFile = ""

    companion object {
        class BackGround : Service() {
            override fun onBind(intent: Intent): IBinder? {
                return null
            }

            override fun onCreate() {
                super.onCreate()
            }

            override fun onDestroy() {
                super.onDestroy()
            }

            override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
                val notificationIntent = Intent(this, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

                val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
                    .setContentTitle("Recording Service")
                    .setContentText("Running")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .build()
                startForeground(1, notification)
                return START_NOT_STICKY
            }
        }
        ///
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE = 1000
        private const val DISPLAY_WIDTH = 720
        private const val DISPLAY_HEIGHT = 1280
        private val ORIENTATIONS = SparseIntArray()

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        checkAllPermissions()
        initListeners()

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProvider = cameraProviderFuture.get()
        flashOn = intent.extras?.getString("isFlash")!!
        isLensBack = intent.extras?.getString("isLensBack")!!
        category = intent.extras?.getString("category")!!

        // screen record
        screenShare()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun screenShare() {
        startForegroundService(Intent(this, BackGround::class.java))
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mScreenDensity = metrics.densityDpi
        mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        initRecorder()
        Handler().postDelayed({
            shareScreen()
        }, 3000)
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
            mMediaRecorder!!.surface, null, null
        )
    }

    private fun initRecorder() {
        mMediaRecorder = MediaRecorder()
        try {
            val file = File("/storage/emulated/0/MLVideos/")
            if (!file.exists()) file.mkdirs()

            videoFile = "/storage/emulated/0/MLVideos/$category-" + System.currentTimeMillis() + ".mp4"

            val file1 = File(videoFile)

            val fileWriter = FileWriter(file1)
            fileWriter.append("")
            fileWriter.flush()
            fileWriter.close()

            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mMediaRecorder!!.setVideoSize(DISPLAY_WIDTH, 1125)
            mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//            mMediaRecorder!!.setVideoEncodingBitRate(512 * 1000)
//            mMediaRecorder!!.setVideoFrameRate(30)
//            mMediaRecorder!!.setVideoEncodingBitRate(3000000)
            val rotation = windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS.get(rotation + 90)
            mMediaRecorder!!.setOrientationHint(orientation)
            mMediaRecorder!!.setOutputFile(videoFile)
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
        destroyMediaProjection()

        MediaScannerConnection.scanFile(
            this, arrayOf(videoFile), null
        ) { path, uri ->
            Log.i("External", "scanned$path:")
            Log.i("External", "-> uri=$uri")
        }
    }


    private fun destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection!!.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
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
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
            mMediaProjection = null
            stopScreenSharing()

            stopService(Intent(this, BackGround::class.java))
            destroyMediaProjection()
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

//        videoCapture = VideoCapture.Builder()
//            .setDefaultCaptureConfig(CaptureConfig.defaultEmptyCaptureConfig())
//            .setTargetResolution(
//                Size(
//                    binding.myCameraView.measuredWidth,
//                    binding.myCameraView.measuredHeight
//                )
//            )
//            .build()

        try {
            cameraProvider?.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageAnalysis
                //, videoCapture
            )?.cameraControl?.enableTorch(flashOn == "yes")
        } catch (e: Exception) {
            toast(applicationContext, "This device is not supported")
            Log.d("TAGtrycatch", "bindPreview: ${e.message.toString()}")
        }

//        val file = File(
//            filesDir.absolutePath,
//            "${category}_Video_${System.currentTimeMillis()}.mp4"
//        )
//
//        // camera:core lib
//        val options = VideoCapture.OutputFileOptions
//            .Builder(file)
//            .build()
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) toast(this, "Please allow audio recording permission")
//
//        videoCapture.startRecording(
//            options,
//            ContextCompat.getMainExecutor(this),
//            object : VideoCapture.OnVideoSavedCallback {
//                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
//                    Log.d("TAGsavedvid01", "onVideoSaved: ${outputFileResults.savedUri}")
//                }
//
//                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
//                    toast(applicationContext, message.toString())
//                    Log.d("TAGsavedvid02", "onVideoSaved: ${videoCaptureError}//${message}")
//                }
//            })
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

    override fun onPause() {
        super.onPause()
        imageProcessor?.run { this.stop() }
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

        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            "MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder!!.surface, null, null
        )

        mMediaRecorder!!.start()
    }




}