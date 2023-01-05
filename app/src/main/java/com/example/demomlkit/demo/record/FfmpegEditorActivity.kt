package com.google.mlkit.vision.demo.record

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
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

class FfmpegEditorActivity : AppCompatActivity() {

    private lateinit var videoView: StyledPlayerView
    private lateinit var simpleExoPlayer: ExoPlayer

    var dialog: BottomSheetDialog? = null
    private lateinit var btnUpload: TextView
    lateinit var vFile: File
   // lateinit var vFileHuman: File
    lateinit var vFileHumanNew: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ffmpeg_editor)

        btnUpload = findViewById(R.id.btnUpload)
        videoView = findViewById(R.id.playerView)
        vFile = File(intent.getStringExtra("SCREEN_VIDEO_VIEW").toString())
       // vFileHuman = File(intent.getStringExtra("VIDEO_VIEW").toString())
        vFileHumanNew = File(intent.getStringExtra("SCREEN_VIDEO_VIEW_HUMAN").toString())
       // playRecordedVideo(Uri.parse(intent.getStringExtra("SCREEN_VIDEO_VIEW").toString()))

        btnUpload.setOnClickListener {
            showDialog()
            uploadVideo()
        }
    }


    /*private fun playRecordedVideo(mUri: Uri) {
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
            Toast.makeText(this, "Video finished",Toast.LENGTH_SHORT).show()
            //videoView.stopPlayback()
            // finish()
        }
    }*/

    private fun showDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.vid_upload, null, false)
        dialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        dialog?.setContentView(view)
        /*dialog?.percentage?.text = "0%"
        dialog?.percentProgressBar?.progress = 0*/
        dialog?.show()
        dialog?.setCancelable(false)
    }

    private fun uploadVideo() {
        ApiInterface.createApi().uploadVideo(
            MultipartBody.Part.createFormData(
                "video",
                vFile.name,
               // vFile.name,
                UploadRequestBody(vFile, "video", object : UploadRequestBody.UploadCallback {
                    override fun onProgressUpdate(percentage: Int) {
                        /*dialog?.percentage?.text = "${percentage}%"
                        dialog?.percentProgressBar?.progress = percentage*/
                    }
                })
            )
        ).enqueue(
            object : Callback<BaseBean> {
                override fun onFailure(call: Call<BaseBean>, t: Throwable) {
                    toast(this@FfmpegEditorActivity, t.message.toString())
                    /*dialog?.percentage?.text = "0%"
                    dialog?.percentProgressBar?.progress = 0*/
                    dialog?.dismiss()
                    dialog = null
                    finish()
                }

                override fun onResponse(call: Call<BaseBean>, response: Response<BaseBean>) {
                    if (response.isSuccessful) {
                        /*dialog?.percentage?.text = "0%"
                        dialog?.percentProgressBar?.progress = 0*/

                        dialog?.dismiss()
                        dialog = null
                        showDialogForHumanVideo()
                        uploadHumanVideo()

                      //  finish()
                    } else {
                        response.body()?.let {
                            //dialog?.percentage?.text = "Uploading Failed"
                            toast(this@FfmpegEditorActivity, it.message)
                            /*dialog?.percentage?.text = "0%"
                            dialog?.percentProgressBar?.progress = 0*/
                            dialog?.dismiss()
                            dialog = null
                            finish()
                        }
                    }
                }
            }
        )
    }

    private fun showDialogForHumanVideo() {
        val view = LayoutInflater.from(this).inflate(R.layout.vid_upload, null, false)
        dialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        dialog?.setContentView(view)
        /*dialog?.percentage?.text = "0%"
        dialog?.percentProgressBar?.progress = 0*/
        dialog?.show()
        dialog?.setCancelable(false)
    }

    private fun uploadHumanVideo() {
        println("fddfgssggsg $vFileHumanNew")
        ApiInterface.createApi().uploadVideo(
            MultipartBody.Part.createFormData(
                "video",
                 vFileHumanNew.name,
                UploadRequestBody(vFileHumanNew, "video", object : UploadRequestBody.UploadCallback {
                    override fun onProgressUpdate(percentage: Int) {
                        /*dialog?.percentage?.text = "${percentage}%"
                        dialog?.percentProgressBar?.progress = percentage*/
                    }
                })
            )
        ).enqueue(
            object : Callback<BaseBean> {
                override fun onFailure(call: Call<BaseBean>, t: Throwable) {
                    toast(this@FfmpegEditorActivity, t.message.toString())
                    /*dialog?.percentage?.text = "0%"
                    dialog?.percentProgressBar?.progress = 0*/
                    dialog?.dismiss()
                    dialog = null
                    finish()
                }

                override fun onResponse(call: Call<BaseBean>, response: Response<BaseBean>) {
                    if (response.isSuccessful) {
                        /*dialog?.percentage?.text = "0%"
                        dialog?.percentProgressBar?.progress = 0*/
                        dialog?.dismiss()
                        dialog = null
                        finish()
                    } else {
                        response.body()?.let {
                            //dialog?.percentage?.text = "Uploading Failed"
                            toast(this@FfmpegEditorActivity, it.message)
                            /*dialog?.percentage?.text = "0%"
                            dialog?.percentProgressBar?.progress = 0*/
                            dialog?.dismiss()
                            dialog = null
                            finish()
                        }
                    }
                }
            }
        )
    }

    fun toast(ctx : Context, msg :String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    private fun initializePlayer() {
        val mediaDataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(this)

        val mediaSource = ProgressiveMediaSource.Factory(mediaDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(intent.getStringExtra("SCREEN_VIDEO_VIEW").toString()))

        val mediaSourceFactory = DefaultMediaSourceFactory(mediaDataSourceFactory)

        simpleExoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        simpleExoPlayer.addMediaSource(mediaSource)

        simpleExoPlayer.playWhenReady = true
        videoView.player = simpleExoPlayer
        videoView.requestFocus()
    }

    private fun releasePlayer() {
        simpleExoPlayer.release()
    }

    public override fun onStart() {
        super.onStart()

        if (Util.SDK_INT > 23) initializePlayer()
    }

    public override fun onResume() {
        super.onResume()

        if (Util.SDK_INT <= 23) initializePlayer()
    }

    public override fun onPause() {
        super.onPause()

        if (Util.SDK_INT <= 23) releasePlayer()
    }

    public override fun onStop() {
        super.onStop()

        if (Util.SDK_INT > 23) releasePlayer()
    }
}