package com.google.mlkit.vision.demo.record

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.demo.remote.ApiInterface
import com.google.mlkit.vision.demo.remote.BaseBean
import com.google.mlkit.vision.demo.remote.UploadRequestBody
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class HumanPreviewActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_human_preview)

        videoView = findViewById(R.id.videoView)
        playRecordedVideo(Uri.parse(intent.getStringExtra("VIDEO_VIEW").toString()))
    }

    private fun playRecordedVideo(mUri: Uri) {
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        mediaController.setMediaPlayer(videoView)
        videoView.setMediaController(mediaController)

        videoView.setVideoURI(mUri)
        videoView.requestFocus()

        videoView.setOnPreparedListener { mp ->
            videoView.scaleX = 1.06f
            videoView.scaleY = 1.01f
            videoView.start()
        }

        videoView.setOnCompletionListener {
            Toast.makeText(this, "Video finished", Toast.LENGTH_SHORT).show()
            //videoView.stopPlayback()
            // finish()
        }
    }

}