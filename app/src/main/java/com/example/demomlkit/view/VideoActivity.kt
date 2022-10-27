package com.example.demomlkit.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.demomlkit.BuildConfig
import com.example.demomlkit.R
import com.example.demomlkit.databinding.ActivityRecordedVideoListBinding
import com.example.demomlkit.databinding.ActivityVideoBinding
import java.io.File
import java.util.*

class VideoActivity : AppCompatActivity() {
    lateinit var binding: ActivityVideoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        binding.videoView.setVideoURI(
//            Uri.parse("android.resource://"+packageName+ intent.extras?.getString("file_path"))
//        )
       val rr =  FileProvider.getUriForFile(
            Objects.requireNonNull(applicationContext),
            BuildConfig.APPLICATION_ID + ".provider", File(intent.extras?.getString("file_path")))

        binding.videoView.setVideoURI(rr)
        //binding.videoView.setVideoURI(FileProvider.getUriForFile(this, packageName, File(intent.extras?.getString("file_path"))))
        binding.videoView .requestFocus()
        binding.videoView.start()
    }
}