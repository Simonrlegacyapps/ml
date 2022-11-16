package com.example.demomlkit.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
//                val notificationIntent = Intent(this, MainActivity::class.java)
//
//                val pIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
//                    PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
//                else PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT)
//
//
//                val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
//                    .setContentTitle("MLKit Recording")
//                    .setContentText("Running...")
//                    .setSmallIcon(R.drawable.ic_launcher_foreground)
//                    .setContentIntent(pIntent)
//                    .build()
//                startForeground(1, notification)
//                return START_NOT_STICKY

                val builder = NotificationCompat.Builder(this, "CHANNEL_ID")
                val nfIntent = Intent(this, CamActivity::class.java)

                val pIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    PendingIntent.getActivity(this, 0, nfIntent, PendingIntent.FLAG_IMMUTABLE)
                else PendingIntent.getActivity(this, 0, nfIntent, PendingIntent.FLAG_ONE_SHOT)

                builder.setContentIntent(pIntent)
                    .setLargeIcon(
                        BitmapFactory.decodeResource(
                            this.resources,
                            R.mipmap.ic_launcher
                        )
                    ).setSmallIcon(R.mipmap.ic_launcher)
                    .setContentText("is running......")
                    .setWhen(System.currentTimeMillis())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setChannelId("CHANNEL_ID")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val notificationManager =
                        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    val channel = NotificationChannel(
                        "CHANNEL_ID",
                        "CHANNEL_ID",
                        NotificationManager.IMPORTANCE_LOW
                    )
                    notificationManager.createNotificationChannel(channel)
                }
                val notification = builder.build()
                notification.defaults = Notification.DEFAULT_SOUND
                startForeground(110, notification)
                return START_NOT_STICKY
            }

//            override fun stopService(name: Intent?): Boolean {
//                return super.stopService(name)
//            }
        }

        ///
        private const val TAG = "MainActivity" //720//1412
        private const val REQUEST_CODE = 1000
        private const val DISPLAY_WIDTH = 720
        private const val DISPLAY_HEIGHT = 1400
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
        hideStatusBar()

        flashOn = intent.extras?.getString("isFlash")!!
        isLensBack = intent.extras?.getString("isLensBack")!!
        category = intent.extras?.getString("category")!!

        checkAllPermissions()
        initListeners()

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProvider = cameraProviderFuture.get()

        // screen record
        startRecordingScreen()
    }

    private fun hideStatusBar() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startRecordingScreen() {
        startForegroundService(Intent(this, BackGround::class.java))
        mScreenDensity = Resources.getSystem().displayMetrics.densityDpi
        mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        initRecorder()
    }

    private fun initRecorder() {
        mMediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this)
        else MediaRecorder()
        val file =
            File(filesDir.absolutePath, "${category}_Video_${System.currentTimeMillis()}.mp4")
        videoFile = file.absolutePath

//        val file = File("/storage/emulated/0/MLVideos/")
//        if (!file.exists()) file.mkdirs()
//        videoFile = "/storage/emulated/0/MLVideos/$category-" + System.currentTimeMillis() + ".mp4"
//        val file1 = File(videoFile)
//        val fileWriter = FileWriter(file1)
//        fileWriter.append("")
//        fileWriter.flush()
//        fileWriter.close()

        try {
            mMediaRecorder!!.reset()
            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)  //MPEG_4_SP

            Log.d(
                "TAGw*h",
                "w*h: ${Resources.getSystem().displayMetrics.widthPixels}//${Resources.getSystem().displayMetrics.heightPixels}///${(Resources.getSystem().displayMetrics.heightPixels) - (Resources.getSystem().displayMetrics.heightPixels * 12 / 100)}"
            )

            mMediaRecorder!!.setVideoSize(DISPLAY_WIDTH, 1220)

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // v11/ v12
//                mMediaRecorder!!.setVideoSize(DISPLAY_WIDTH, 1200)
//            } else { // ...v10
//                mMediaRecorder!!.setVideoSize(DISPLAY_WIDTH, 1200)
//            }

//            mMediaRecorder!!.setVideoSize(
//                Resources.getSystem().displayMetrics.widthPixels,
//                (Resources.getSystem().displayMetrics.heightPixels) - (Resources.getSystem().displayMetrics.heightPixels * 12 / 100))


//            mMediaRecorder!!.setVideoFrameRate(30)
//            mMediaRecorder!!.setVideoEncodingBitRate(512 * 1000)

            //mMediaRecorder!!.setVideoEncodingBitRate(3000000)

            mMediaRecorder!!.setOutputFile(videoFile)

            val rotation =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.rotation  //android 9/p
                else windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS.get(rotation?.plus(90) ?: 0)
            mMediaRecorder!!.setOrientationHint(orientation)
            mMediaRecorder!!.prepare()

            if (mMediaProjection == null) {
                startActivityForResult(
                    mProjectionManager!!.createScreenCaptureIntent(),
                    REQUEST_CODE
                )
                return
            } else {
                mVirtualDisplay = createVirtualDisplay()
                mMediaRecorder!!.start()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            toast(this, e.message.toString())
        }

        mMediaRecorder!!.setOnErrorListener { mr, what, extra ->
            Log.d("TAG-mr-error", "initRecorder: ${mr}, ${what}// ${extra}")
        }

        mMediaRecorder!!.setOnInfoListener { mr, what, extra ->
            Log.d("TAG-mr-info", "initRecorder: ${mr}, ${what}// ${extra}")
        }
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            stopScreenSharing()
        }
    }

    private fun stopScreenSharing() {
        mMediaRecorder!!.stop()
        mMediaRecorder!!.reset()
        mMediaRecorder!!.release()
        mMediaProjection = null

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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)   // <v10
                stopScreenSharing()
            stopService(Intent(this, BackGround::class.java))
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
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT)
                        .show()
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
    }

    override fun onPause() {
        super.onPause()
        imageProcessor?.run { this.stop() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: $requestCode")
            finish()
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(
                this,
                "Screen Record Permission Denied", Toast.LENGTH_SHORT
            ).show()

            Toast.makeText(
                this,
                "Please allow recording, so that ML can record video properly", Toast.LENGTH_LONG
            ).show()
            finish()
        }
        mMediaProjectionCallback = MediaProjectionCallback()
        mMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data!!)
        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)

        try {
            mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
                "CamActivity",
                DISPLAY_WIDTH,
                DISPLAY_HEIGHT,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder!!.surface,
                null,
                null
            )
        } catch (e: Exception) {
            toast(this, e.message.toString())
        }
        mMediaRecorder!!.start()
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay(
            "CamActivity",
            DISPLAY_WIDTH,
            DISPLAY_HEIGHT,
            mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder!!.surface, null, null
        )
    }

}