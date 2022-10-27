package com.example.demomlkit.view

//import androidx.camera.core.VideoCapture
import android.Manifest
import android.R.attr.data
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
//import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.example.demomlkit.databinding.ActivityCamBinding
import com.example.demomlkit.utils.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.controls.VideoCodec
import com.otaliastudios.cameraview.size.SizeSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors


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
//    lateinit var bitmapArray : ArrayList<Bitmap>
    // lateinit var saveVidDialog : AlertDialog

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PrefManager.putString("isLoggedIn", "yes")

//        binding.myCameraView.setLifecycleOwner(this)

//        val file = File(externalMediaDirs.first(),
//            "${System.currentTimeMillis()}.mp4")
//
//        binding.myCameraView.mode = Mode.VIDEO
//        binding.myCameraView.takeVideo(file)
//        bitmapArray = ArrayList()
        initListeners()
//val photoFile = File(
//            this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
//            "${System.currentTimeMillis()}.jpg"
//        )
//
//        binding.myCameraView.addCameraListener(object : CameraListener() {
//            override fun onVideoTaken(result: VideoResult) {
//                binding.myCameraView.flash = Flash.OFF
//
//                val fOut = openFileOutput(File(externalMediaDirs.first(), "${System.currentTimeMillis()}.mp4").name, MODE_WORLD_READABLE)
//                fOut.write(result.file.readBytes())
//                fOut.close()
//
//                val newfile: File
//
//                try {
//                    val currentFile = File(result.file.path)
//                    val fileName = currentFile.name
//                   // val directory = ContextWrapper(this@CamActivity).getDir("videoDir", Context.MODE_PRIVATE)
//                    newfile = File(externalMediaDirs.first(), fileName)
//                    // File(directory, fileName)
//                    if (currentFile.exists()) {
//                        val inputStream: InputStream = FileInputStream(currentFile)
//                        val outStream: OutputStream = FileOutputStream(newfile)
//
//                        // Copy the bits from instream to outstream
//                        val buf = ByteArray(1024)
//                        var len: Int
//                        while (inputStream.read(buf).also { len = it } > 0) {
//                            outStream.write(buf, 0, len)
//                        }
//                        inputStream.close()
//                        outStream.close()
//                        Log.v("", "Video file saved successfully.")
//                    } else {
//                        Log.v("", "Video saving failed. Source file missing.")
//                    }
//                } catch (e: java.lang.Exception) {
//                    e.printStackTrace()
//                }
//            }
//        })
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
//        preview = Preview.Builder().build().also {
//            it.setSurfaceProvider(binding.myCameraView.surfaceProvider)
//        }

//        cameraSelector = CameraSelector.Builder()
//            .requireLensFacing(lensFacing)
//            .build()

//        val qualitySelector =
//            QualitySelector.from(Quality.HD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD))
//        val recorder = Recorder.Builder()
//            .setQualitySelector(qualitySelector)
//            .build()
//
//        val videoCapture = VideoCapture.withOutput(recorder)

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

        val videoCapture = VideoCapture.Builder()
            .setDefaultCaptureConfig(CaptureConfig.defaultEmptyCaptureConfig())
            .setBackgroundExecutor(Dispatchers.IO as Executor)
//           .setTargetAspectRatio(AspectRatio.Rati)
//            .setVideoFrameRate(60)
            //.setTargetResolution(Size(1080, 1920))  //Point().x,Point().y))
              .setTargetResolution(Size(binding.myCameraView.measuredWidth, binding.myCameraView.measuredHeight))
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
            //  bitmapArray.add(getBitmap(imageProxy)!!)

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

        // cameraProvider?.unbindAll() //videoCapture
        try {
            cameraProvider?.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageAnalysis,
                //     preview,
                videoCapture
            )?.cameraControl?.enableTorch(flashOn)
        } catch (e: Exception) {
           toast(applicationContext, "This device is not supported")
            Log.d("TAGtrycatch", "bindPreview: ${e.message.toString()}")
        }

        //  startVideoRecording(videoCapture)

        val file = File(
            externalMediaDirs.first(),
            "mlkit_video_${System.currentTimeMillis()}.mp4"
        )
// camera:core lib
        val outputFileOptions = VideoCapture.OutputFileOptions
            .Builder(file)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        videoCapture.startRecording(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    Log.d("TAGsavedvid01", "onVideoSaved: ${outputFileResults.savedUri}")
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    Log.d("TAGsavedvid02", "onVideoSaved: ${videoCaptureError}//${message}")
                }
            })
    }

//    private fun startVideoRecording(videoCapture: VideoCapture<Recorder>) {
//        // create MediaStoreOutputOptions for our recorder,, resulting the recording..
//        val contentValues = ContentValues().apply {
//            put(MediaStore.Video.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.mp4")
//        }
//
//        val mediaStoreOutput = MediaStoreOutputOptions
//            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//            .setContentValues(contentValues)
//            .build()
//
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            toast(this, "Give Audio Permission")
//            return
//        }
//
//        val currentRecording : Recording = videoCapture.output
//            .prepareRecording(this, mediaStoreOutput)
//            .withAudioEnabled()
//            .start(ContextCompat.getMainExecutor(this), captureListener)
//
//    }
//
//    private val captureListener = Consumer<VideoRecordEvent> { event ->
//        // listen when video get stopped...
//        val durationInNanos = event.recordingStats.recordedDurationNanos
//        val durationInSeconds = durationInNanos / 1000 / 1000 / 1000.0
//
//        when (event) {
//            is VideoRecordEvent.Start -> {
//                Toast.makeText(applicationContext, "Capture Started", Toast.LENGTH_SHORT).show()
//                // update app internal recording state
//            }
//            is VideoRecordEvent.Finalize -> {
//                if (!event.hasError()) {
//                    Toast.makeText(
//                        applicationContext,
//                        "Video capture succeeded: ${event.outputResults.outputUri}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                } else {
//                    // update app state when the capture failed.
////                    recording?.close()
////                    recording = null
//                    Toast.makeText(
//                        applicationContext,
//                        "Video capture ends with error: ${event.error}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }

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