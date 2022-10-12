package com.example.demomlkit.view

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.demomlkit.databinding.ActivityCamBinding
import com.example.demomlkit.utils.getCoordinates
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors

class CamActivity : AppCompatActivity() { //, TextureView.SurfaceTextureListener {
    lateinit var binding: ActivityCamBinding
    private lateinit var poseDetector: PoseDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var preview : Preview
    private lateinit var cameraSelector : CameraSelector
    private lateinit var imageAnalysis : ImageAnalysis
    var flashOn: Boolean = false

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera(CameraSelector.LENS_FACING_FRONT)

        initListeners()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun initListeners() {
        binding.rotateCameraIconFront.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.VISIBLE
            binding.rotateCameraIconFront.visibility = View.GONE
            startCamera(CameraSelector.LENS_FACING_BACK)
        }
        binding.rotateCameraIconBack.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.GONE
            binding.rotateCameraIconFront.visibility = View.VISIBLE
            binding.flashIconOn.visibility = View.GONE
            binding.flashIconOff.visibility = View.VISIBLE
            flashOn = false
            startCamera(CameraSelector.LENS_FACING_FRONT)
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

        cameraProviderFuture.addListener({
            bindPreview(cameraProvider = cameraProviderFuture.get(), lensFacing)
        }, ContextCompat.getMainExecutor(this))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider, lensFacing: Int) {
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.myCameraView.surfaceProvider)
        }

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                // Passing image to mlkit
                poseDetector.process(inputImage)
                    .addOnSuccessListener { obj ->
                        if (obj.allPoseLandmarks.size>0) getCoordinates(obj, binding.tvAngle)
                        imageProxy.close()
                    }.addOnFailureListener {
                        Log.d("TAGpose00", "onCreate: ${it.message}")
                    }
            }
        }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageAnalysis).cameraControl.enableTorch(flashOn)
    }

}

























//                            Log.d("TAGchildcount", "bindPreview: ${parentLayout.childCount}")
////                            binding.parentLayout.addView()
//                            val poseLandmark = obj.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
//                            Log.d("TAGpose01", "onCreate: ${obj.allPoseLandmarks[0].position3D.x}")
//                            Log.d("TAGpose02", "onCreate: ${obj.allPoseLandmarks[0].position}")
//                            Log.d("TAGpose03", "onCreate: ${obj.allPoseLandmarks[0].inFrameLikelihood}")
//                            Log.d("TAGpose04", "onCreate: ${obj.allPoseLandmarks[0].landmarkType}")
//
//                            val rightHipAngle = getAngle(
//                                obj.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)!!,
//                                obj.getPoseLandmark(PoseLandmark.RIGHT_HIP)!!,
//                                obj.getPoseLandmark(PoseLandmark.RIGHT_KNEE)!!)
//                            Log.d("TAGpose05", "onCreate: ${rightHipAngle}")





//  override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
//    }
//
//    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
//    }
//
//    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//        return false
//    }
//
//    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//    }