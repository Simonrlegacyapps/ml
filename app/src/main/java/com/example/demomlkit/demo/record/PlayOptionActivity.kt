package com.google.mlkit.vision.demo.record

import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
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
import java.text.SimpleDateFormat
import java.util.*


class PlayOptionActivity : AppCompatActivity() {

    private var exeCmd: MutableList<String>? = null
    private var exeCmd2: MutableList<String>? = null

    var dialog: BottomSheetDialog? = null

    val COLOR = "0x000000"
    val COLORKEY = "0.2:0.3"
    var imputvideoPath = ""

    var skelotonPath = ""

    lateinit var vFile: File
    lateinit var vFileHumanNew: File
    private var calendar = Calendar.getInstance()

    //1080*608
    //292*541

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_option)
        showDialog()
        imputvideoPath  = intent.getStringExtra("SCREEN_VIDEO_VIEW").toString()
        //humanPath = intent.getStringExtra("VIDEO_VIEW").toString()
        exeCmd = ArrayList()
        executeRemoveBackgroundCommand()
        initViews()
        /*Handler(Looper.getMainLooper()).postDelayed({

        }, 1000)*/

    }

    private fun showDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.vid_processing, null, false)
        dialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        dialog?.setContentView(view)
        /*dialog?.percentage?.text = "0%"
        dialog?.percentProgressBar?.progress = 0*/
        dialog?.show()
        dialog?.setCancelable(false)
    }

    private fun initViews() {
        findViewById<TextView>(R.id.skeloton_video).setOnClickListener {
            val intent = Intent(this, FfmpegEditorActivity::class.java)
            intent.putExtra("SCREEN_VIDEO_VIEW", skelotonPath)
            intent.putExtra("SCREEN_VIDEO_VIEW_HUMAN", imputvideoPath)
            startActivity(intent)

        }
        findViewById<TextView>(R.id.human_video).setOnClickListener {
            val intent = Intent(this@PlayOptionActivity, HumanPreviewActivity::class.java)
            intent.putExtra("VIDEO_VIEW",imputvideoPath)
            startActivity(intent)
        }
    }

    // ffmpeg -i bg.jpg -i input.mp4 -filter_complex "[1:v]colorkey=0x3BBD1E:0.3:0.2[ckout];[0:v][ckout]overlay[out]" -map "[out]" output.mp4
    // ffmpeg -i bg.jpg -i input.mp4 -filter_complex "[1:v]chromakey=0x3BBD1E:0.1:0.2[ckout];[0:v][ckout]overlay[o]" -map [o] -map 1:a output.mp4


    fun executeRemoveBackgroundCommand() {
        calendar = Calendar.getInstance()
        val outerMediaPath = Environment.getExternalStorageDirectory().toString()
        //cacheDir.absolutePath
        val dir = File(outerMediaPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val path = outerMediaPath + "/DCIM/" + SimpleDateFormat("ddMMYYYY_hhmmss").format(
            calendar.time
        )+"_"+Utility.category +"_skeleton"+ ".mp4"
        val output = File(path)
        skelotonPath = output.absolutePath
       /* val dir = File(getExternalFilesDir(Environment.DIRECTORY_DCIM).toString())
        val file = File(dir.path, "${System.currentTimeMillis()}.mp4")
        skelotonPath = file.absolutePath*/

        val imagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/" + "r" + ".jpg"

        val mergeCommand = mutableListOf<String>(
            "-i",
            imagePath,
            "-i",
            imputvideoPath,
            "-filter_complex",
            "[1:v]chromakey=$COLOR:$COLORKEY[ckout];[0:v][ckout]overlay[out]",
            "-map",
            "-[out]",
            skelotonPath
        )

        executeCommand(getCommand(mergeCommand)!!)
    }

    fun getCommand(args: MutableList<String>?): String? {
        val sb = StringBuffer()
        for (i in args!!.indices) {
            if (i == args.size - 1) {
                sb.append("\"")
                sb.append(args[i])
                sb.append("\"")
            } else {
                sb.append("\"")
                sb.append(args[i])
                sb.append("\" ")
            }
        }
        val str = sb.toString()
        println(str)
        return str
    }

    fun executeCommand(command: String) {
        FFmpeg.executeAsync(command) { executionId, returnCode ->
            when (returnCode) {
                Config.RETURN_CODE_SUCCESS -> {
                    dialog?.dismiss()
                    dialog = null
                    //playRecordedVideo(Uri.parse(absolutePath))
                    Log.d("CommandExecute", "onSuccess  ${Config.RETURN_CODE_SUCCESS}")
                   // Toast.makeText(applicationContext, "Sucess $absolutePath", Toast.LENGTH_SHORT).show()
                    uploadVideoVideos()
                }
                Config.RETURN_CODE_CANCEL -> {
                    Log.d("CommandExecute", "onCancel  ${Config.RETURN_CODE_CANCEL}")
                    Toast.makeText(applicationContext, "onCancel", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Log.d("CommandExecute", "onError  $returnCode")
                    Toast.makeText(applicationContext, "onError", Toast.LENGTH_SHORT).show()
                    Config.printLastCommandOutput(Log.INFO)
                }
            }
        }
    }

    private fun uploadVideoVideos() {
        vFile = File(skelotonPath)
        showUploadDialog()
        uploadVideo()
    }

    private fun showUploadDialog() {
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
                    toast(this@PlayOptionActivity, t.message.toString())
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
                        vFileHumanNew = File(imputvideoPath)
                        showDialogForHumanVideo()
                        uploadHumanVideo()

                        //  finish()
                    } else {
                        response.body()?.let {
                            //dialog?.percentage?.text = "Uploading Failed"
                            toast(this@PlayOptionActivity, it.message)
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
                    toast(this@PlayOptionActivity, t.message.toString())
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
                        //finish()
                    } else {
                        response.body()?.let {
                            //dialog?.percentage?.text = "Uploading Failed"
                            toast(this@PlayOptionActivity, it.message)
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

}