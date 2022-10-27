package com.example.demomlkit.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
        recordedVideoFile = ArrayList()
        val path = externalMediaDirs.first().toString()
        val f = File(path)
        val file: Array<File> = f.listFiles()
        Log.d("Files", "Size: " + file.size)
        for (i in file.indices) {
            //here is our recorded videos list
            Log.d("Files", "FileName:" + file[i].name)
            recordedVideoFile.add(file[i])
        }

        setupRecyclerview()
    }

    private fun setupRecyclerview() {
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

    override fun onClick(position: Int) {
        startActivity(Intent(this, VideoActivity::class.java)
            .putExtra("file_path", recordedVideoFile[position].path))
       // val fileUri = Uri.fromFile(File(recordedVideoFile[position].path))
//        val fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", recordedVideoFile[position])
//        val intent = Intent()
//        intent.action = Intent.ACTION_VIEW
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        intent.setDataAndType(fileUri, "video/mp4") //URLConnection.guessContentTypeFromName(fileUri.toString()))
//        startActivity(intent)
    }
}