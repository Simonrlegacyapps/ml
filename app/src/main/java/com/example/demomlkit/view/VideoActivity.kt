package com.example.demomlkit.view

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.demomlkit.recorded_video_player.MoviePlayer
import com.example.demomlkit.recorded_video_player.SpeedControlCallback
import com.example.demomlkit.databinding.ActivityVideoBinding
import com.example.demomlkit.utils.Draw
import com.example.demomlkit.utils.PoseDetectorProcessor
import com.example.demomlkit.utils.VisionImageProcessor
import com.example.demomlkit.utils.toast
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.android.synthetic.main.activity_cam.*
import java.io.File
import java.io.IOException

class VideoActivity : AppCompatActivity(), TextureView.SurfaceTextureListener, MoviePlayer.PlayerFeedback {
    private lateinit var poseDetector: PoseDetector
    lateinit var binding: ActivityVideoBinding
    private var imageProcessor: VisionImageProcessor? = null

    private var mPlayTask: MoviePlayer.PlayTask? = null
    private lateinit var uri: Uri
    private var mSurfaceTextureReady = false


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mTextureView.surfaceTextureListener = this

        setUpPoseDetect()

        val mUri = Uri.parse(intent.extras?.getString("file_uri"))

        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(this, mUri)
        binding.imgView.setImageBitmap(mediaMetadataRetriever.getFrameAtIndex(0))

        binding.ivBackBtn.setOnClickListener {
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun play() {
        uri = Uri.parse(intent.extras?.getString("file_uri"))
        val path = intent.extras?.getString("file_path")

        val callback = SpeedControlCallback()
        //callback.setFixedPlaybackRate(30)

        val st: SurfaceTexture? = binding.mTextureView.surfaceTexture
        val surface = Surface(st)
        var player: MoviePlayer? = null

        if (uri != null) {
            try {
                player = MoviePlayer(File(path), surface, callback)
            } catch (io: IOException) {
                toast(this, io.message.toString())
                surface.release()
                return
            }
            adjustAspectRatio(player?.videoWidth, player?.videoHeight)
            mPlayTask = MoviePlayer.PlayTask(player, this)
            mPlayTask!!.execute()
        }
    }

    private fun adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
        val viewWidth = binding.mTextureView.width
        val viewHeight = binding.mTextureView.height
        val aspectRatio = videoHeight.toDouble() / videoWidth
        val newWidth: Int
        val newHeight: Int
        if (viewHeight > (viewWidth * aspectRatio).toInt()) {
            // limited by narrow width; restrict height
            newWidth = viewWidth
            newHeight = (viewWidth * aspectRatio).toInt()
        } else {
            // limited by short height; restrict width
            newWidth = (viewHeight / aspectRatio).toInt()
            newHeight = viewHeight
        }
        val xoff = (viewWidth - newWidth) / 2
        val yoff = (viewHeight - newHeight) / 2
        val txform = Matrix()
        binding.mTextureView.getTransform(txform)
        txform.setScale(
            newWidth.toFloat() / viewWidth,
            newHeight.toFloat() / viewHeight
        )
        txform.postTranslate(xoff.toFloat(), yoff.toFloat())
        binding.mTextureView.setTransform(txform)
    }

    private fun setUpPoseDetect() {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE) //CPU_GPU //STREAM_MODE
            .build()
        poseDetector = PoseDetection.getClient(options)

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
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
        mSurfaceTextureReady = true
        val bm = binding.mTextureView.bitmap
        binding.imgView.setImageBitmap(bm)
        play()
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
//        val bm = binding.mTextureView.bitmap
//        binding.imgView.setImageBitmap(bm)
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        mSurfaceTextureReady = false
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        val bm = binding.mTextureView.bitmap
        if (bm != null) {
            binding.imgView.setImageBitmap(bm)
            binding.graphicOverlay.setImageSourceInfo(
                bm.width,
                bm.height,
                false
            )

            try {
                imageProcessor?.processBitmap(bm, binding.graphicOverlay)
            } catch (e: MlKitException) {
                Log.e("TAG", "Failed to process image. Error: " + e.localizedMessage)
                Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }  else Log.d("LOG:", "null")
    }

    override fun playbackStopped() {
        if (mPlayTask != null) {
            stopPlayback()
            mPlayTask!!.waitForStop()
            toast(applicationContext, "Video finished")
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mPlayTask != null) {
            stopPlayback()
            mPlayTask!!.waitForStop()
        }
        imageProcessor?.run { this.stop() }
    }

    private fun stopPlayback() {
        mPlayTask?.requestStop()
    }
}
