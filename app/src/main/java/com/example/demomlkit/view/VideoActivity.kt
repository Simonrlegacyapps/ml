package com.example.demomlkit.view

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.MediaController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.demomlkit.databinding.ActivityVideoBinding
import com.example.demomlkit.utils.*
import kotlinx.android.synthetic.main.activity_cam.*

class VideoActivity :
    AppCompatActivity() { //, TextureView.SurfaceTextureListener, MoviePlayer.PlayerFeedback {
    lateinit var binding: ActivityVideoBinding

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                //   Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                //  Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    //add(Manifest.permission.CAMERA)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    // add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) return
            else toast(this, "Permissions are not granted")
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAllPermissions()
        val mUri = Uri.parse(intent.extras?.getString("file_uri"))
        binding.ivBackBtn.setOnClickListener {
            finish()
        }
        playRecordedVideo(mUri)
    }

    private fun playRecordedVideo(mUri: Uri) {
        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)
        mediaController.setMediaPlayer(binding.videoView)
        binding.videoView.setMediaController(mediaController)

        binding.videoView.setVideoURI(mUri)
        binding.videoView.requestFocus()

        binding.videoView.setOnPreparedListener { mp ->
            binding.videoView.scaleX = 1.06f
            binding.videoView.scaleY = 1.01f
            binding.videoView.start()
        }

        binding.videoView.setOnCompletionListener {
            toast(this, "Video finished")
            binding.videoView.stopPlayback()
            finish()
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
}


// scaleX/// scaleY
//            val videoRatio = mp.videoWidth / mp.videoHeight.toFloat()
//            val screenRatio = binding.videoView.width / binding.videoView.height.toFloat()
//            val scaleX = videoRatio / screenRatio
//            if (scaleX >= 1f)
//                binding.videoView.scaleX = scaleX
//            else
//                binding.videoView.scaleY = 1f / scaleX


// so it fits on the screen
//            val videoWidth = binding.videoView.width //videoWidth
//            val videoHeight = binding.videoView.height //videoHeight
//            val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
//            val screenWidth = windowManager.defaultDisplay.width
//            val screenHeight = windowManager.defaultDisplay.height
//            val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
//            val lp = binding.parentLayout.layoutParams
//
//            if (videoProportion > screenProportion) {
//                lp.width = screenWidth
//                lp.height = (screenWidth.toFloat() / videoProportion).toInt()
//            } else {
//                lp.width = (videoProportion * screenHeight.toFloat()).toInt()
//                lp.height = screenHeight
//            }
//            binding.parentLayout.layoutParams = lp


// private lateinit var poseDetector: PoseDetector
//private var imageProcessor: VisionImageProcessor? = null
//    private var mPlayTask: MoviePlayer.PlayTask? = null
// private lateinit var uri: Uri
//    private var mSurfaceTextureReady = false


//    @RequiresApi(Build.VERSION_CODES.P)
//    private fun play() {
//        uri = Uri.parse(intent.extras?.getString("file_uri"))
//        val path = intent.extras?.getString("file_path")
//
//        val callback = SpeedControlCallback()
//        //callback.setFixedPlaybackRate(30)
//
//        val st: SurfaceTexture? = binding.mTextureView.surfaceTexture
//        val surface = Surface(st)
//        var player: MoviePlayer? = null
//
//        if (uri != null) {
//            try {
//                player = MoviePlayer(File(path), surface, callback)
//            } catch (io: IOException) {
//                toast(this, io.message.toString())
//                surface.release()
//                return
//            }
//            adjustAspectRatio(player?.videoWidth, player?.videoHeight)
//            mPlayTask = MoviePlayer.PlayTask(player, this)
//            mPlayTask!!.execute()
//        }
//    }

//    private fun adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
//        val viewWidth = binding.mTextureView.width
//        val viewHeight = binding.mTextureView.height
//        val aspectRatio = videoHeight.toDouble() / videoWidth
//        val newWidth: Int
//        val newHeight: Int
//        if (viewHeight > (viewWidth * aspectRatio).toInt()) {
//            // limited by narrow width; restrict height
//            newWidth = viewWidth
//            newHeight = (viewWidth * aspectRatio).toInt()
//        } else {
//            // limited by short height; restrict width
//            newWidth = (viewHeight / aspectRatio).toInt()
//            newHeight = viewHeight
//        }
//        val xoff = (viewWidth - newWidth) / 2
//        val yoff = (viewHeight - newHeight) / 2
//        val txform = Matrix()
//        binding.mTextureView.getTransform(txform)
//        txform.setScale(
//            newWidth.toFloat() / viewWidth,
//            newHeight.toFloat() / viewHeight
//        )
//        txform.postTranslate(xoff.toFloat(), yoff.toFloat())
//        binding.mTextureView.setTransform(txform)
//    }

//    private fun setUpPoseDetect() {
//        val options = PoseDetectorOptions.Builder()
//            .setDetectorMode(PoseDetectorOptions.STREAM_MODE) //CPU_GPU //STREAM_MODE
//            .build()
//        poseDetector = PoseDetection.getClient(options)
//
//        if (imageProcessor != null) imageProcessor!!.stop()
//
//        imageProcessor =
//            try {
//                PoseDetectorProcessor(
//                    this,
//                    poseDetector,
//                    showInFrameLikelihood = true,
//                    visualizeZ = false,
//                    rescaleZForVisualization = false,
//                    runClassification = true,
//                    isStreamMode = true
//                )
//            } catch (e: Exception) {
//                Toast.makeText(
//                    applicationContext,
//                    "Can not create image processor: " + e.localizedMessage,
//                    Toast.LENGTH_LONG
//                ).show()
//                return
//            }
//    }

//    @RequiresApi(Build.VERSION_CODES.P)
//    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
//        mSurfaceTextureReady = true
//        val bm = binding.mTextureView.bitmap
//        binding.imgView.setImageBitmap(bm)
//        play()
//    }
//
//    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
////        val bm = binding.mTextureView.bitmap
////        binding.imgView.setImageBitmap(bm)
//    }
//
//    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
//        mSurfaceTextureReady = false
//        return true
//    }
//
//    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//        val bm = binding.mTextureView.bitmap
//        if (bm != null) {
//            binding.imgView.setImageBitmap(bm)
//            binding.graphicOverlay.setImageSourceInfo(
//                bm.width,
//                bm.height,
//                false
//            )
//
//            try {
//                imageProcessor?.processBitmap(bm, binding.graphicOverlay)
//            } catch (e: MlKitException) {
//                Log.e("TAG", "Failed to process image. Error: " + e.localizedMessage)
//                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
//            }
//        }  else Log.d("LOG:", "null")
//    }
//
//    override fun playbackStopped() {
//        if (mPlayTask != null) {
//            stopPlayback()
//            mPlayTask!!.waitForStop()
//            toast(applicationContext, "Video finished")
//            finish()
//        }
// }

//    override fun onPause() {
//        super.onPause()
//        if (mPlayTask != null) {
//            stopPlayback()
//            mPlayTask!!.waitForStop()
//        }
//        imageProcessor?.run { this.stop() }
//    }
//
//    private fun stopPlayback() {
//        mPlayTask?.requestStop()
//    }