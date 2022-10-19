package com.example.demomlkit.view

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.demomlkit.databinding.ActivityCamBinding
import com.example.demomlkit.utils.*
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors

class CamActivity : AppCompatActivity() {
    lateinit var binding: ActivityCamBinding
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var preview : Preview
    private lateinit var cameraSelector : CameraSelector
    private lateinit var imageAnalysis : ImageAnalysis
    private var cameraProvider: ProcessCameraProvider? = null
    var flashOn: Boolean = false
    private var imageProcessor: VisionImageProcessor? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PrefManager.putString("isLoggedIn", "yes")
        initListeners()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun initListeners() {
        binding.rotateCameraIconFront.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.VISIBLE
            binding.rotateCameraIconFront.visibility = View.GONE
            startCamera(CameraSelector.LENS_FACING_FRONT)
        }
        binding.rotateCameraIconBack.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.GONE
            binding.rotateCameraIconFront.visibility = View.VISIBLE
            binding.flashIconOn.visibility = View.GONE
            binding.flashIconOff.visibility = View.VISIBLE
            flashOn = false
            startCamera(CameraSelector.LENS_FACING_BACK)
        }

        binding.flashIconOn.setOnClickListener {
            binding.flashIconOn.visibility = View.GONE
            binding.flashIconOff.visibility = View.VISIBLE
            flashOn = false
            cameraProviderFuture.get().bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis).cameraControl.enableTorch(flashOn)
        }
        binding.flashIconOff.setOnClickListener {
            binding.flashIconOn.visibility = View.VISIBLE
            binding.flashIconOff.visibility = View.GONE
            flashOn = true
            cameraProviderFuture.get().bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis).cameraControl.enableTorch(flashOn)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startCamera(lensFacing: Int) {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetector = PoseDetection.getClient(options)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProvider = cameraProviderFuture.get()

        cameraProviderFuture.addListener({
            bindPreview(lensFacing)
        }, ContextCompat.getMainExecutor(this))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindPreview(lensFacing: Int) {
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.myCameraView.surfaceProvider)
        }

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // try

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
                    isStreamMode = true)
            } catch (e: Exception) {
                Toast.makeText(
                    applicationContext,
                    "Can not create image processor: " + e.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()
                return
            }

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT

                if (rotationDegrees == 0 || rotationDegrees == 180)
                    binding.graphicOverlay.setImageSourceInfo(imageProxy.width, imageProxy.height, isImageFlipped)
                else
                    binding.graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width, isImageFlipped)

                try {
                    imageProcessor?.processImageProxy(imageProxy, binding.graphicOverlay)
                } catch (e: MlKitException) {
                    Log.e("TAG", "Failed to process image. Error: " + e.localizedMessage)
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis)?.cameraControl?.enableTorch(flashOn)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        startCamera(CameraSelector.LENS_FACING_BACK)
    }

    override fun onPause() {
        super.onPause()
        imageProcessor?.run { this.stop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }
}
















// val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

// Passing image to mlkit
//                poseDetector.process(inputImage)
//                    .addOnSuccessListener { obj ->
//                        if (obj.allPoseLandmarks.size>0) {
//                            if (binding.parentLayout.childCount > 3) binding.parentLayout.removeViewAt(3)
//                            val mView = Draw(this, obj, binding.tvAngle)
//
//                            binding.parentLayout.addView(mView)
//                        } else if (binding.parentLayout.childCount > 3) {
//                            binding.parentLayout.removeViewAt(3)
//                            binding.tvAngle.text = ""
//                        }
//                        imageProxy.close()
//                    }.addOnFailureListener {
//                        Log.d("TAGpose00", "onCreate: ${it.message}")
//                        toast(applicationContext, it.message.toString())
//                        imageProxy.close()
//                    }