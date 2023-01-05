package com.google.mlkit.vision.demo.record

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.demo.screenRecorder.BackgroundService
import com.google.mlkit.vision.demo.screenRecorder.Function
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RecordActivity : AppCompatActivity() {
    private lateinit var poseDetector: PoseDetector
    private lateinit var videoCapture: VideoCapture
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var btnStart: TextView
    private lateinit var btnStop: TextView
    private lateinit var previewView: PreviewView
    private lateinit var parentLayout: RelativeLayout

    var path = ""
    var screenRecordingPath = ""




    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission", "RestrictedApi", "UnsafeExperimentalUsageError")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this@RecordActivity, BackgroundService::class.java))
        setContentView(R.layout.activity_record)

        saveImageToInternal()

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        previewView = findViewById(R.id.previewView)
        parentLayout = findViewById(R.id.parentLayout)

        btnStart.setOnClickListener {
            //timer.start()
            btnStop.visibility = View.VISIBLE
            btnStart.visibility = View.GONE
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_DCIM).toString())
            val file = File(dir.path, "${System.currentTimeMillis()}.mp4")

            path = file.absolutePath
            val outputFileOptions =  VideoCapture.OutputFileOptions.Builder(file).build()

            videoCapture.startRecording(outputFileOptions,
                ContextCompat.getMainExecutor(this),object: VideoCapture.OnVideoSavedCallback{
                    override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                        Log.d("Check:","On Video Saved")
                    }

                    override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                        Log.d("Check:","On Video Error" + message)
                    }

                })

        }

        btnStop.setOnClickListener {

        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)

        }, ContextCompat.getMainExecutor(this))

        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()

        poseDetector = PoseDetection.getClient(options)




       /* btn_action = findViewById<View>(R.id.btn_action) as Button

        btn_action.setOnClickListener(View.OnClickListener { onToggleScreenShare() })*/

    }

    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError", "NewApi",
        "UnsafeOptInUsageError"
    )
    private fun bindPreview(cameraProvider: ProcessCameraProvider){

        Log.d("Check:","inside bind preview")

        val preview = Preview.Builder().build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val point = Point()
        val size = display?.getRealSize(point)

        videoCapture = VideoCapture.Builder()
            .setTargetResolution(Size(point.x,point.y))
            .build()



        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(point.x,point.y))
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), ImageAnalysis.Analyzer { imageProxy ->

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image

            if(image!=null){

                val processImage = InputImage.fromMediaImage(image,rotationDegrees)

                poseDetector.process(processImage)
                    .addOnSuccessListener {

                        if(parentLayout.childCount>3){
                            parentLayout.removeViewAt(3)
                        }
                        if(it.allPoseLandmarks.isNotEmpty()){

                            if(parentLayout.childCount>3){
                                parentLayout.removeViewAt(3)
                            }

                            Utility.poses.add(it)
                            //startTimer()
                            val element = Draw(applicationContext,it)
                            parentLayout.addView(element)
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener{


                        imageProxy.close()
                    }
            }
        })
        cameraProvider.unbindAll()
        //,imageAnalysis
        try {
            cameraProvider.bindToLifecycle(this,cameraSelector,preview,videoCapture)
        }
        catch (exception:Exception){
            Log.d("Exception",exception.message.toString())
        }
    }

    private fun saveImageToInternal() {
        val bm = BitmapFactory.decodeResource(resources, R.drawable.r)
        val extStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
        val file = File(extStorageDirectory, "r.jpg")
        if (!file.exists()) {
            try {
                val outStream = FileOutputStream(file)
                bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                outStream.flush()
                outStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

 /*   //screen recording
    @RequiresApi(api = Build.VERSION_CODES.O)
    open fun onToggleScreenShare() {
        if (!isRecording) {
            val PERMISSIONS = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )
            if (!Function.hasPermissions(this, *PERMISSIONS)) {
                ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_PERMISSION_KEY
                )
            } else {
                initRecorder()
                shareScreen()
            }
        } else {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
            stopScreenSharing()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(
                mProjectionManager!!.createScreenCaptureIntent(),
                REQUEST_CODE
            )
            return
        }
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder!!.start()
        isRecording = true
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay(
            "MainActivity",
            DISPLAY_WIDTH,
            DISPLAY_HEIGHT,
            mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder!!.surface,
            null,
            null
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun initRecorder() {
        mMediaRecorder = MediaRecorder()
        try {
            val file = File("/storage/emulated/0/Ezy/")
            if (!file.exists()) {
                file.mkdirs()
            }
            calendar = Calendar.getInstance()
            Log.e("e", "Before")
            videofile =
                "/storage/emulated/0/Ezy/Video_" + SimpleDateFormat("dd_MM_yyyy_hh_mm_ss_a").format(
                    calendar.time
                ) + ".mp4"
            val file1 = File(videofile)
            Log.e("e", "after")
            val fileWriter = FileWriter(file1)
            fileWriter.append("")
            fileWriter.flush()
            fileWriter.close()
            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            Log.e("e", "after")
            mMediaRecorder!!.setVideoSize(
                DISPLAY_WIDTH,
                DISPLAY_HEIGHT
            )
            mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mMediaRecorder!!.setVideoEncodingBitRate(512 * 1000)
            mMediaRecorder!!.setVideoFrameRate(30)
            mMediaRecorder!!.setVideoEncodingBitRate(3000000)
            val rotation = windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS[rotation + 90]
            mMediaRecorder!!.setOrientationHint(orientation)
            mMediaRecorder!!.setOutputFile(videofile)
            mMediaRecorder!!.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopScreenSharing():String {
        if (mVirtualDisplay == null) {
            return ""
        }
        mVirtualDisplay!!.release()
        destroyMediaProjection()
        isRecording = false
        MediaScannerConnection.scanFile(
            this, arrayOf(videofile), null
        ) { path, uri ->
            Log.i("External", "scanned$path:")
            Log.i("External", "-> uri=$uri")
            screenRecordingPath = path
        }
        return screenRecordingPath
    }

    private fun destroyMediaProjection() {
        if (mMediaProjection != null) {
           // mMediaProjection!!.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
        Log.i(TAG, "MediaProjection Stopped")
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: $requestCode")
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
            isRecording = false
            return
        }
       // mMediaProjectionCallback = MediaProjectionCallback()

        mMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data!!)
        // moveTaskToBack(true);
        timerTask = object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    timerTask = object : TimerTask() {
                        override fun run() {
                            runOnUiThread {
                               // mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
                                mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
                                    "MainActivity",
                                    DISPLAY_WIDTH,
                                    DISPLAY_HEIGHT,
                                    mScreenDensity,
                                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                    mMediaRecorder!!.surface,
                                    null,
                                    null
                                )
                                isRecording = true
                                mMediaRecorder!!.start()
                            }
                        }
                    }
                    timer!!.schedule(timerTask, 100 )
                }
            }
        }
        timer!!.schedule(timerTask, 100)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_KEY -> {
                if (grantResults.size > 0 && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    onToggleScreenShare()
                } else {
                    isRecording = false
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Please enable Microphone and Storage permissions.",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction(
                        "ENABLE"
                    ) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        intent.addCategory(Intent.CATEGORY_DEFAULT)
                        intent.data = Uri.parse("package:$packageName")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        startActivity(intent)
                    }.show()
                }
                return
            }
        }
    }


*//*
    private class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            if (isRecording) {
                isRecording = false
                actionBtnReload()
                mMediaRecorder.stop()
                mMediaRecorder.reset()
            }
            mMediaProjection = null
            stopScreenSharing()
        }
    }
*//*
    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this@RecordActivity, BackgroundService::class.java))
        destroyMediaProjection()
    }

    override fun onBackPressed() {
        if (isRecording) {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
            Log.v(TAG, "Stopping Recording")
            stopScreenSharing()
            finish()
            *//*Snackbar.make(
                findViewById(android.R.id.content), "Wanna Stop recording and exit?",
                Snackbar.LENGTH_INDEFINITE
            ).setAction(
                "Stop"
            ) {
                mMediaRecorder!!.stop()
                mMediaRecorder!!.reset()
                Log.v(TAG, "Stopping Recording")
                stopScreenSharing()
                finish()
            }.show()*//*
        }
        else {
            finish()
        }
    }*/
}