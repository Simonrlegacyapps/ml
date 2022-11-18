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
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.util.SparseIntArray
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.demomlkit.R
import com.example.demomlkit.databinding.ActivityCamBinding
import com.example.demomlkit.remote.ApiInterface
import com.example.demomlkit.remote.BaseBean
import com.example.demomlkit.remote.UploadRequestBody
import com.example.demomlkit.utils.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.android.synthetic.main.bottom_sheet_permission.*
import kotlinx.android.synthetic.main.vid_upload.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
//    private lateinit var videoCapture: VideoCapture

    //////
    private var mScreenDensity = 0
    private var mProjectionManager: MediaProjectionManager? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionCallback: MediaProjectionCallback? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var videoFile = ""
    lateinit var vFile: File
    lateinit var notificationManager: NotificationManager
    var dialog: BottomSheetDialog? = null

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
//                val pIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
//                    PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
//                else PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT)
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

        private const val TAG = "CamActivity" //720//1412
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

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.CAMERA)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    add(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
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
        hideStatusBar(this)

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        enableDND(notificationManager)

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
        vFile = File(filesDir.absolutePath, "${category}_Video_${System.currentTimeMillis()}.mp4")
        videoFile = vFile.absolutePath

        try {
            mMediaRecorder!!.reset()
            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mMediaRecorder!!.setVideoEncodingBitRate(8000000)
            mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264) //VP8 //MPEG_4_SP
            mMediaRecorder!!.setVideoFrameRate(45)
            mMediaRecorder!!.setVideoSize(DISPLAY_WIDTH, 1220)

//            mMediaRecorder!!.setVideoSize(
//                Resources.getSystem().displayMetrics.widthPixels,
//                (Resources.getSystem().displayMetrics.heightPixels) - (Resources.getSystem().displayMetrics.heightPixels * 12 / 100))

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
    }

    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            //destroyMediaProjection()
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

//        MediaScannerConnection.scanFile(
//            this, arrayOf(videoFile), null
//        ) { path, uri ->
//            Log.i("External", "scanned$path:${uri}")
//        }
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
            //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)   // <v10
            stopScreenSharing()
            imageProcessor?.run { this.stop() }
            disableDND(notificationManager)
            stopService(Intent(this, BackGround::class.java))
            showDialog()
            uploadVideo()
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

//    override fun onStop() {
//        super.onStop()
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: $requestCode")
            finish()
        }
        if (resultCode != RESULT_OK) {
            toast(this, "Screen Record Permission Denied")
            toast(this, "Please allow recording, so that ML can record video properly")
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
                mMediaRecorder!!.surface, null, null
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

    private fun showDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.vid_upload, null, false)
        dialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        dialog?.setContentView(view)
        dialog?.percentage?.text = "0%"
        dialog?.percentProgressBar?.progress = 0
        dialog?.show()
        dialog?.setCancelable(false)
    }

    private fun uploadVideo() {
        ApiInterface.createApi().uploadVideo(
            MultipartBody.Part.createFormData(
                "video",
                vFile.name,
                UploadRequestBody(vFile, "video", object : UploadRequestBody.UploadCallback {
                    override fun onProgressUpdate(percentage: Int) {
                        dialog?.percentage?.text = "${percentage}%"
                        dialog?.percentProgressBar?.progress = percentage
                    }
                })
            )
        ).enqueue(
            object : Callback<BaseBean> {
                override fun onFailure(call: Call<BaseBean>, t: Throwable) {
                    toast(this@CamActivity, t.message.toString())
                    dialog?.percentage?.text = "0%"
                    dialog?.percentProgressBar?.progress = 0
                    dialog?.dismiss()
                    dialog = null
                    finish()
                }

                override fun onResponse(call: Call<BaseBean>, response: Response<BaseBean>) {
                    if (response.isSuccessful) {
                        dialog?.percentage?.text = "0%"
                        dialog?.percentProgressBar?.progress = 0
                        dialog?.dismiss()
                        dialog = null
                        finish()
                    } else {
                        response.body()?.let {
                            dialog?.percentage?.text = "Uploading Failed"
                            toast(this@CamActivity, it.message)
                            dialog?.percentage?.text = "0%"
                            dialog?.percentProgressBar?.progress = 0
                            dialog?.dismiss()
                            dialog = null
                            finish()
                        }
                    }
                }
            }
        )
    }
}


//        val file = File("/storage/emulated/0/MLVideos/")
//        if (!file.exists()) file.mkdirs()
//        videoFile = "/storage/emulated/0/MLVideos/$category-" + System.currentTimeMillis() + ".mp4"
//        val file1 = File(videoFile)
//        val fileWriter = FileWriter(file1)
//        fileWriter.append("")
//        fileWriter.flush()
//        fileWriter.close()