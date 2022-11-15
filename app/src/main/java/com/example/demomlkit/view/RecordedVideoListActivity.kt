package com.example.demomlkit.view

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.demomlkit.BuildConfig
import com.example.demomlkit.R
import com.example.demomlkit.databinding.ActivityCamBinding
import com.example.demomlkit.databinding.ActivityRecordedVideoListBinding
import com.example.demomlkit.view.adapter.RecordedVideoAdapter
import java.io.File
import java.net.URLConnection

class RecordedVideoListActivity : AppCompatActivity(), RecordedVideoAdapter.OnVideoClickInterface {
    lateinit var binding: ActivityRecordedVideoListBinding
    lateinit var recordedVideoFile : ArrayList<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordedVideoListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getStoredFiles()
        setupRecyclerview()
        binding.ivBackBtn.setOnClickListener {
            finish()
        }
    }

    private fun getStoredFiles() {
        recordedVideoFile = ArrayList()
        val path = filesDir.absolutePath.toString() //"/storage/emulated/0/MLVideos/"  //filesDir.absolutePath.toString() //getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath
        //val path = File(getExternalFilesDir(Environment.DIRECTORY_RECORDINGS), "MLKIT").absolutePath.toString()  //"/storage/emulated/0/MLVideos/"

//        val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
//            File(getExternalFilesDir(Environment.DIRECTORY_RECORDINGS), "MLKIT_VID").absolutePath
//         else
//            "/storage/emulated/0/MLVideos/"


        val f = File(path)

        val file: Array<File>? = f.listFiles() as Array<File>?
        if (file.isNullOrEmpty()) {
            binding.tvNoVidText.visibility = View.VISIBLE
            binding.rvVideos.visibility = View.GONE
        } else {
            for (i in file.indices) {
                recordedVideoFile.add(file[i]) //here is our recorded videos list
            }
        }
    }

    private fun setupRecyclerview() {
        if (recordedVideoFile.isNullOrEmpty()) {
            binding.tvNoVidText.visibility = View.VISIBLE
            binding.rvVideos.visibility = View.GONE
        } else {
            binding.tvNoVidText.visibility = View.GONE
            binding.rvVideos.visibility = View.VISIBLE
            val recordedVideoAdapter = RecordedVideoAdapter(recordedVideoFile, this)
            binding.rvVideos.adapter = recordedVideoAdapter
            binding.rvVideos.addItemDecoration(
                DividerItemDecoration(
                    this,
                    LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false).orientation
                )
            )
            recordedVideoAdapter.notifyDataSetChanged()
        }
    }

    override fun onClick(position: Int) {
        startActivity(Intent(this, VideoActivity::class.java)
            .putExtra("file_uri", Uri.fromFile(recordedVideoFile[position]).toString())
            .putExtra("file_path", recordedVideoFile[position].absolutePath.toString()))
    }
}