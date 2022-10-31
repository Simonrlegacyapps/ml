package com.example.demomlkit.view

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.example.demomlkit.databinding.ActivityHomeBinding
import com.example.demomlkit.view.adapter.CategoriesAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.*
import kotlin.collections.ArrayList

class HomeActivity : AppCompatActivity(), CategoriesAdapter.OnCatClickInterface {
    lateinit var binding : ActivityHomeBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setCategories()
        binding.startRecordingButton.setOnClickListener {
            startActivity(Intent(this, CamActivity::class.java))
        }
        binding.goToListButton.setOnClickListener {
            startActivity(Intent(this, RecordedVideoListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }

    private fun startCamera() {
        binding.myCameraView.setLifecycleOwner(this)
        binding.myCameraView
    }

    private fun bindPreview() {
//        val preview = Preview.Builder().build().also {
//            it.setSurfaceProvider(binding.myCameraView.surfaceProvider)
//        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

//        val quality = Quality.HD
//        val qualitySelector = QualitySelector.from(quality)
//        val recorderBuilder = Recorder.Builder()
//        recorderBuilder.setQualitySelector(qualitySelector)
//        val recorder = recorderBuilder.build()
//
//        videoCapture = VideoCapture.withOutput(recorder)

        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(this as LifecycleOwner, cameraSelector)

    //    startVideoRecording()
    }

//    private fun startVideoRecording() {
//        // create MediaStoreOutputOptions for our recorder,, resulting the recording..
//        val contentValues = ContentValues().apply {
//            put(MediaStore.Video.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.mp4")
//        }
//
//        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
//            this.contentResolver,
//            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//            .setContentValues(contentValues)
//            .build()
//
//
//        currentRecording = videoCapture.output
//            .prepareRecording(this, mediaStoreOutput)
//           // .apply { if (audioEnabled) withAudioEnabled() }
//            .start(ContextCompat.getMainExecutor(this), captureListener)
//    }
//
//    private val captureListener = Consumer<VideoRecordEvent> { event ->
//        // listen when video get stopped...
//        val durationInNanos = event.recordingStats.recordedDurationNanos
//        val durationInSeconds = durationInNanos/1000/1000/1000.0
//
//        if (event is VideoRecordEvent.Finalize) {
//            // display the captured video
//            Log.d("TAGdisplayvid01", "${event.outputResults.outputUri}")
//            Log.d("TAGdisplayvid02", "${durationInSeconds}")
//        }
//    }

    private fun setCategories() {
        val catList = ArrayList<String>()
        catList.add("Cat1")
        catList.add("Cat2")
        catList.add("Cat3")
        catList.add("Cat4")
        catList.add("Cat5")
        catList.add("Cat6")
        catList.add("Cat7")
        catList.add("Cat8")
        catList.add("Cat9")
        catList.add("Cat10")
        val cAdapter = CategoriesAdapter(catList, this)
        binding.rvCategories.adapter = cAdapter
        cAdapter.notifyDataSetChanged()
    }

    override fun onClick(position: Int) {

    }

}