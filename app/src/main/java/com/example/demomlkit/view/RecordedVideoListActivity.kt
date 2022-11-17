package com.example.demomlkit.view

import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.demomlkit.databinding.ActivityRecordedVideoListBinding
import com.example.demomlkit.view.adapter.RecordedVideoAdapter
import java.io.File


class RecordedVideoListActivity : AppCompatActivity(), RecordedVideoAdapter.OnVideoClickInterface {
    lateinit var binding: ActivityRecordedVideoListBinding
    lateinit var recordedVideoFile : ArrayList<File>

    @RequiresApi(Build.VERSION_CODES.R)
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
        //val path = "/storage/emulated/0/MLVideos/"
        val f = File(filesDir.absolutePath.toString())

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