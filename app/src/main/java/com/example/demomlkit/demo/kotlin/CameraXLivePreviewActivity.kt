/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.kotlin

import android.Manifest
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.util.SparseIntArray
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.annotation.KeepName
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.demo.CameraXViewModel
import com.google.mlkit.vision.demo.GraphicOverlay
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.demo.VisionImageProcessor
import com.google.mlkit.vision.demo.kotlin.barcodescanner.BarcodeScannerProcessor
import com.google.mlkit.vision.demo.kotlin.facedetector.FaceDetectorProcessor
import com.google.mlkit.vision.demo.kotlin.labeldetector.LabelDetectorProcessor
import com.google.mlkit.vision.demo.kotlin.objectdetector.ObjectDetectorProcessor
import com.google.mlkit.vision.demo.kotlin.posedetector.PoseDetectorProcessor
import com.google.mlkit.vision.demo.kotlin.segmenter.SegmenterProcessor
import com.google.mlkit.vision.demo.kotlin.textdetector.TextRecognitionProcessor
import com.google.mlkit.vision.demo.preference.PreferenceUtils
import com.google.mlkit.vision.demo.preference.SettingsActivity
import com.google.mlkit.vision.demo.preference.SettingsActivity.LaunchSource
import com.google.mlkit.vision.demo.record.CatAdapter
import com.google.mlkit.vision.demo.record.PlayOptionActivity
import com.google.mlkit.vision.demo.record.Utility
import com.google.mlkit.vision.demo.screenRecorder.BackgroundService
import com.google.mlkit.vision.demo.screenRecorder.Function
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/** Live preview demo app for ML Kit APIs using CameraX.  */
@KeepName
@RequiresApi(VERSION_CODES.LOLLIPOP)
class CameraXLivePreviewActivity : AppCompatActivity(),
  ActivityCompat.OnRequestPermissionsResultCallback,
  OnItemSelectedListener,
  CompoundButton.OnCheckedChangeListener {

  private var bStart: TextView? = null
  private var bStop: TextView? = null
  private var rvCategories: RecyclerView? = null
  private var previewView: PreviewView? = null
  private var graphicOverlay: GraphicOverlay? = null
  private var cameraProvider: ProcessCameraProvider? = null
  private var previewUseCase: Preview? = null
  private var analysisUseCase: ImageAnalysis? = null
  private var imageProcessor: VisionImageProcessor? = null
  private var needUpdateGraphicOverlayImageSourceInfo = false
  private var selectedModel = POSE_DETECTION
  private var lensFacing = CameraSelector.LENS_FACING_BACK
  private var cameraSelector: CameraSelector? = null


  //screen recording
  var screenRecordingPath = ""
  private var timer: Timer? = Timer()
  private var timerTask: TimerTask? = null


  private val TAG = "MainActivity"
  private var videofile = ""
  //private var dialog: AlertDialog.Builder? = null
  private val REQUEST_CODE = 1000
  private var mScreenDensity = 0
  // private var btn_action: Button? = null

  private var mProjectionManager: MediaProjectionManager? = null
  private val DISPLAY_WIDTH = 720
  private val DISPLAY_HEIGHT = 1280
  private var mMediaProjection: MediaProjection? = null
  private var mVirtualDisplay: VirtualDisplay? = null
  // private var mMediaProjectionCallback: MediaProjectionCallback? = null
  private var mMediaRecorder: MediaRecorder? = null
  private val ORIENTATIONS = SparseIntArray()
  private val REQUEST_PERMISSION_KEY = 1
  var isRecording = false
  private var calendar = Calendar.getInstance()



  @RequiresApi(api = Build.VERSION_CODES.O)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "onCreate")
    if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
      Toast.makeText(
        applicationContext,
        "CameraX is only supported on SDK version >=21. Current SDK version is " +
          VERSION.SDK_INT,
        Toast.LENGTH_LONG
      )
        .show()
      return
    }
    if (savedInstanceState != null) {
      selectedModel =
        savedInstanceState.getString(
          STATE_SELECTED_MODEL,
          OBJECT_DETECTION
        )
    }
    cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    setContentView(R.layout.activity_vision_camerax_live_preview)
    startForegroundService(Intent(this@CameraXLivePreviewActivity, BackgroundService::class.java))
    saveImageToInternal()

    previewView = findViewById(R.id.preview_view)

    bStart = findViewById<TextView>(R.id.btnStart)
    bStop = findViewById<TextView>(R.id.btnStop)
    rvCategories = findViewById<RecyclerView>(R.id.rvCategories)

    if (previewView == null) {
      Log.d(TAG, "previewView is null")
    }
    graphicOverlay = findViewById(R.id.graphic_overlay)
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null")
    }
    val spinner = findViewById<Spinner>(R.id.spinner)
    val options: MutableList<String> = ArrayList()
    options.add(OBJECT_DETECTION)
    options.add(OBJECT_DETECTION_CUSTOM)
    options.add(CUSTOM_AUTOML_OBJECT_DETECTION)
    options.add(FACE_DETECTION)
    options.add(TEXT_RECOGNITION)
    options.add(BARCODE_SCANNING)
    options.add(IMAGE_LABELING)
    options.add(IMAGE_LABELING_CUSTOM)
    options.add(CUSTOM_AUTOML_LABELING)
    options.add(POSE_DETECTION)
    options.add(SELFIE_SEGMENTATION)

    // Creating adapter for spinner
    val dataAdapter =
      ArrayAdapter(this, R.layout.spinner_style, options)
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    // attaching data adapter to spinner
    spinner.adapter = dataAdapter
    spinner.onItemSelectedListener = this
    val facingSwitch =
      findViewById<ToggleButton>(R.id.facing_switch)
    facingSwitch.setOnCheckedChangeListener(this)
    ViewModelProvider(
      this,
      ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )
      .get(CameraXViewModel::class.java)
      .processCameraProvider
      .observe(
        this,
        Observer { provider: ProcessCameraProvider? ->
          cameraProvider = provider
          if (allPermissionsGranted()) {
            bindAllCameraUseCases()
          }
        }
      )

    val settingsButton = findViewById<ImageView>(R.id.settings_button)
    settingsButton.setOnClickListener {
      val intent =
        Intent(applicationContext, SettingsActivity::class.java)
      intent.putExtra(
        SettingsActivity.EXTRA_LAUNCH_SOURCE,
        LaunchSource.CAMERAX_LIVE_PREVIEW
      )
      startActivity(intent)
    }

    if (!allPermissionsGranted()) {
      runtimePermissions
    }


    bStart!!.setOnClickListener {
      if (Utility.catSelected){
        bStart?.visibility = View.GONE
        bStop?.visibility = View.VISIBLE
        onToggleScreenShare()
      }
      else{
        Toast.makeText(this,"Please select any category",Toast.LENGTH_SHORT).show()
      }
    }
    bStop!!.setOnClickListener {
      bStart?.visibility = View.VISIBLE
      bStop?.visibility = View.GONE
      if (isRecording) {
        mMediaRecorder!!.stop()
        mMediaRecorder!!.reset()
        Log.v(TAG, "Stopping Recording")
        stopScreenSharing()
      }

      Handler(Looper.getMainLooper()).postDelayed({
        val intent = Intent(this@CameraXLivePreviewActivity, PlayOptionActivity::class.java)
      //  intent.putExtra("VIDEO_VIEW",path)
        intent.putExtra("SCREEN_VIDEO_VIEW",screenRecordingPath)
        startActivity(intent)
        finish()
      }, 1000)
    }

    //screen recording
    // dialog = AlertDialog.Builder(this)

    val metrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(metrics)

    mScreenDensity = metrics.densityDpi

    mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    setCategories()

  }

  override fun onSaveInstanceState(bundle: Bundle) {
    super.onSaveInstanceState(bundle)
    bundle.putString(STATE_SELECTED_MODEL, selectedModel)
  }

  @Synchronized
  override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent?.getItemAtPosition(pos).toString()
    Log.d(TAG, "Selected model: $selectedModel")
    bindAnalysisUseCase()
  }

  override fun onNothingSelected(parent: AdapterView<*>?) {
    // Do nothing.
  }

  override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
    if (cameraProvider == null) {
      return
    }
    val newLensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
      CameraSelector.LENS_FACING_BACK
    } else {
      CameraSelector.LENS_FACING_FRONT
    }
    val newCameraSelector =
      CameraSelector.Builder().requireLensFacing(newLensFacing).build()
    try {
      if (cameraProvider!!.hasCamera(newCameraSelector)) {
        Log.d(TAG, "Set facing to " + newLensFacing)
        lensFacing = newLensFacing
        cameraSelector = newCameraSelector
        bindAllCameraUseCases()
        return
      }
    } catch (e: CameraInfoUnavailableException) {
      // Falls through
    }
    Toast.makeText(
      applicationContext, "This device does not have lens with facing: $newLensFacing",
      Toast.LENGTH_SHORT
    )
      .show()
  }

  public override fun onResume() {
    super.onResume()
    bindAllCameraUseCases()
  }

  override fun onPause() {
    super.onPause()

    imageProcessor?.run {
      this.stop()
    }
  }

  public override fun onDestroy() {
    super.onDestroy()
    imageProcessor?.run {
      this.stop()
    }
    //screen recording
    stopService(Intent(this@CameraXLivePreviewActivity, BackgroundService::class.java))
    destroyMediaProjection()
  }

  private fun bindAllCameraUseCases() {
    if (cameraProvider != null) {
      // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
      cameraProvider!!.unbindAll()
      bindPreviewUseCase()
      bindAnalysisUseCase()
    }
  }

  private fun bindPreviewUseCase() {
    if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
      return
    }
    if (cameraProvider == null) {
      return
    }
    if (previewUseCase != null) {
      cameraProvider!!.unbind(previewUseCase)
    }

    val builder = Preview.Builder()
    val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
    if (targetResolution != null) {
      builder.setTargetResolution(targetResolution)
    }
    previewUseCase = builder.build()
    previewUseCase!!.setSurfaceProvider(previewView!!.getSurfaceProvider())
    cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */this, cameraSelector!!, previewUseCase)
  }

  private fun bindAnalysisUseCase() {
    if (cameraProvider == null) {
      return
    }
    if (analysisUseCase != null) {
      cameraProvider!!.unbind(analysisUseCase)
    }
    if (imageProcessor != null) {
      imageProcessor!!.stop()
    }
    imageProcessor = try {
      when (selectedModel) {
        OBJECT_DETECTION -> {
          Log.i(
            TAG,
            "Using Object Detector Processor"
          )
          val objectDetectorOptions =
            PreferenceUtils.getObjectDetectorOptionsForLivePreview(this)
          ObjectDetectorProcessor(
            this, objectDetectorOptions
          )
        }
        OBJECT_DETECTION_CUSTOM -> {
          Log.i(
            TAG,
            "Using Custom Object Detector (with object labeler) Processor"
          )
          val localModel = LocalModel.Builder()
            .setAssetFilePath("custom_models/object_labeler.tflite")
            .build()
          val customObjectDetectorOptions =
            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel)
          ObjectDetectorProcessor(
            this, customObjectDetectorOptions
          )
        }
        CUSTOM_AUTOML_OBJECT_DETECTION -> {
          Log.i(
            TAG,
            "Using Custom AutoML Object Detector Processor"
          )
          val customAutoMLODTLocalModel = LocalModel.Builder()
            .setAssetManifestFilePath("automl/manifest.json")
            .build()
          val customAutoMLODTOptions = PreferenceUtils
            .getCustomObjectDetectorOptionsForLivePreview(this, customAutoMLODTLocalModel)
          ObjectDetectorProcessor(
            this, customAutoMLODTOptions
          )
        }
        TEXT_RECOGNITION -> {
          Log.i(
            TAG,
            "Using on-device Text recognition Processor"
          )
          TextRecognitionProcessor(this)
        }
        FACE_DETECTION -> {
          Log.i(
            TAG,
            "Using Face Detector Processor"
          )
          val faceDetectorOptions =
            PreferenceUtils.getFaceDetectorOptions(this)
          FaceDetectorProcessor(this, faceDetectorOptions)
        }
        BARCODE_SCANNING -> {
          Log.i(
            TAG,
            "Using Barcode Detector Processor"
          )
          BarcodeScannerProcessor(this)
        }
        IMAGE_LABELING -> {
          Log.i(
            TAG,
            "Using Image Label Detector Processor"
          )
          LabelDetectorProcessor(
            this, ImageLabelerOptions.DEFAULT_OPTIONS
          )
        }
        IMAGE_LABELING_CUSTOM -> {
          Log.i(
            TAG,
            "Using Custom Image Label (Birds) Detector Processor"
          )
          val localClassifier = LocalModel.Builder()
            .setAssetFilePath("custom_models/bird_classifier.tflite")
            .build()
          val customImageLabelerOptions =
            CustomImageLabelerOptions.Builder(localClassifier).build()
          LabelDetectorProcessor(
            this, customImageLabelerOptions
          )
        }
        CUSTOM_AUTOML_LABELING -> {
          Log.i(
            TAG,
            "Using Custom AutoML Image Label Detector Processor"
          )
          val customAutoMLLabelLocalModel = LocalModel.Builder()
            .setAssetManifestFilePath("automl/manifest.json")
            .build()
          val customAutoMLLabelOptions = CustomImageLabelerOptions
            .Builder(customAutoMLLabelLocalModel)
            .setConfidenceThreshold(0f)
            .build()
          LabelDetectorProcessor(
            this, customAutoMLLabelOptions
          )
        }
        POSE_DETECTION -> {
          val poseDetectorOptions =
            PreferenceUtils.getPoseDetectorOptionsForLivePreview(this)
          val shouldShowInFrameLikelihood =
            PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this)
          val visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this)
          val rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this)
          val runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this)
          PoseDetectorProcessor(
            this, poseDetectorOptions, shouldShowInFrameLikelihood, visualizeZ, rescaleZ,
            runClassification, /* isStreamMode = */ true
          )
        }
        SELFIE_SEGMENTATION -> SegmenterProcessor(this)
        else -> throw IllegalStateException("Invalid model name")
      }
    } catch (e: Exception) {
      Log.e(
        TAG,
        "Can not create image processor: $selectedModel",
        e
      )
      Toast.makeText(
        applicationContext,
        "Can not create image processor: " + e.localizedMessage,
        Toast.LENGTH_LONG
      )
        .show()
      return
    }

    val builder = ImageAnalysis.Builder()
    val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
    if (targetResolution != null) {
      builder.setTargetResolution(targetResolution)
    }
    analysisUseCase = builder.build()

    needUpdateGraphicOverlayImageSourceInfo = true

    analysisUseCase?.setAnalyzer(
      // imageProcessor.processImageProxy will use another thread to run the detection underneath,
      // thus we can just runs the analyzer itself on main thread.
      ContextCompat.getMainExecutor(this),
      ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
        if (needUpdateGraphicOverlayImageSourceInfo) {
          val isImageFlipped =
            lensFacing == CameraSelector.LENS_FACING_FRONT
          val rotationDegrees =
            imageProxy.imageInfo.rotationDegrees
          if (rotationDegrees == 0 || rotationDegrees == 180) {
            graphicOverlay!!.setImageSourceInfo(
              imageProxy.width, imageProxy.height, isImageFlipped
            )
          } else {
            graphicOverlay!!.setImageSourceInfo(
              imageProxy.height, imageProxy.width, isImageFlipped
            )
          }
          needUpdateGraphicOverlayImageSourceInfo = false
        }
        try {
          imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
        } catch (e: MlKitException) {
          Log.e(
            TAG,
            "Failed to process image. Error: " + e.localizedMessage
          )
          Toast.makeText(
            applicationContext,
            e.localizedMessage,
            Toast.LENGTH_SHORT
          )
            .show()
        }
      }
    )
    cameraProvider!!.bindToLifecycle( /* lifecycleOwner= */this, cameraSelector!!, analysisUseCase)
  }

  private val requiredPermissions: Array<String?>
    get() = try {
      val info = this.packageManager
        .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
      val ps = info.requestedPermissions
      if (ps != null && ps.isNotEmpty()) {
        ps
      } else {
        arrayOfNulls(0)
      }
    } catch (e: Exception) {
      arrayOfNulls(0)
    }

  private fun allPermissionsGranted(): Boolean {
    for (permission in requiredPermissions) {
      if (!isPermissionGranted(this, permission)) {
        return false
      }
    }
    return true
  }

  private val runtimePermissions: Unit
    get() {
      val allNeededPermissions: MutableList<String?> = ArrayList()
      for (permission in requiredPermissions) {
        if (!isPermissionGranted(this, permission)) {
          allNeededPermissions.add(permission)
        }
      }
      if (allNeededPermissions.isNotEmpty()) {
        ActivityCompat.requestPermissions(
          this,
          allNeededPermissions.toTypedArray(),
          PERMISSION_REQUESTS
        )
      }
    }
  @RequiresApi(api = Build.VERSION_CODES.O)
  override fun onRequestPermissionsResult(requestCode: Int,
                                          permissions: Array<String>,
                                          grantResults: IntArray
  ) {
    Log.i(TAG, "Permission granted!")
    if (allPermissionsGranted()) {
      bindAllCameraUseCases()

      //screen recording
      when (requestCode) {
        REQUEST_PERMISSION_KEY -> {
          if (grantResults.size > 0 && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            onToggleScreenShare()
          } else {
            isRecording = false
            Snackbar.make(
              findViewById(android.R.id.content),
              "Please enable Microphone and Storage permissions.",
              Snackbar.LENGTH_INDEFINITE
            ).setAction(
              "ENABLE"
            ) {
              val intent = Intent()
              intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
              intent.addCategory(Intent.CATEGORY_DEFAULT)
              intent.data = Uri.parse("package:$packageName")
              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
              intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
              startActivity(intent)
            }.show()
          }
          return
        }
      }
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

  }

  companion object {
    private const val TAG = "CameraXLivePreview"
    private const val PERMISSION_REQUESTS = 1
    private const val OBJECT_DETECTION = "Object Detection"
    private const val OBJECT_DETECTION_CUSTOM = "Custom Object Detection"
    private const val CUSTOM_AUTOML_OBJECT_DETECTION = "Custom AutoML Object Detection (Flower)"
    private const val FACE_DETECTION = "Face Detection"
    private const val TEXT_RECOGNITION = "Text Recognition"
    private const val BARCODE_SCANNING = "Barcode Scanning"
    private const val IMAGE_LABELING = "Image Labeling"
    private const val IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)"
    private const val CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)"
    private const val POSE_DETECTION = "Pose Detection"
    private const val SELFIE_SEGMENTATION = "Selfie Segmentation"

    private const val STATE_SELECTED_MODEL = "selected_model"

    private fun isPermissionGranted(
      context: Context,
      permission: String?
    ): Boolean {
      if (ContextCompat.checkSelfPermission(context, permission!!)
        == PackageManager.PERMISSION_GRANTED
      ) {
        Log.i(TAG, "Permission granted: $permission")
        return true
      }
      Log.i(TAG, "Permission NOT granted: $permission")
      return false
    }
  }

  private fun saveImageToInternal() {
    val bm = BitmapFactory.decodeResource(resources, R.drawable.r)
    val extStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
    val file = File(extStorageDirectory, "r.jpg")
    if (!file.exists()) {
      try {
        val outStream = FileOutputStream(file)
        bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
        outStream.flush()
        outStream.close()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }


  //screen recording
  @RequiresApi(api = Build.VERSION_CODES.O)
  open fun onToggleScreenShare() {
    if (!isRecording) {
      val PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
      )
      if (!Function.hasPermissions(this, *PERMISSIONS)) {
        ActivityCompat.requestPermissions(
          this,
          PERMISSIONS,
          REQUEST_PERMISSION_KEY
        )
      } else {
        initRecorder()
        shareScreen()
      }
    } else {
      mMediaRecorder!!.stop()
      mMediaRecorder!!.reset()
      stopScreenSharing()
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun shareScreen() {
    if (mMediaProjection == null) {
      startActivityForResult(
        mProjectionManager!!.createScreenCaptureIntent(),
        REQUEST_CODE
      )
      return
    }
    mVirtualDisplay = createVirtualDisplay()
    mMediaRecorder!!.start()
    isRecording = true
  }

  private fun createVirtualDisplay(): VirtualDisplay {
    return mMediaProjection!!.createVirtualDisplay(
      "MainActivity",
      DISPLAY_WIDTH,
      DISPLAY_HEIGHT,
      mScreenDensity,
      DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
      mMediaRecorder!!.surface,
      null,
      null
    )
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun initRecorder() {
    mMediaRecorder = MediaRecorder()
    try {
      val file = File("/storage/emulated/0/pose_detection/")
      if (!file.exists()) {
        file.mkdirs()
      }
      calendar = Calendar.getInstance()
      Log.e("e", "Before")
      videofile =
        "/storage/emulated/0/pose_detection/" + SimpleDateFormat("ddMMYYYY_hhmmss").format(
          calendar.time
        )+"_"+Utility.category+"_videoskeleton" + ".mp4"
      val file1 = File(videofile)
      Log.e("e", "after")
      val fileWriter = FileWriter(file1)
      fileWriter.append("")
      fileWriter.flush()
      fileWriter.close()
      mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
      mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
      mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      Log.e("e", "after")
      mMediaRecorder!!.setVideoSize(
        DISPLAY_WIDTH,
        DISPLAY_HEIGHT
      )
      mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
      mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
      mMediaRecorder!!.setVideoEncodingBitRate(512 * 1000)
      mMediaRecorder!!.setVideoFrameRate(30)
      mMediaRecorder!!.setVideoEncodingBitRate(3000000)
      val rotation = windowManager.defaultDisplay.rotation
      val orientation = ORIENTATIONS[rotation + 90]
      mMediaRecorder!!.setOrientationHint(orientation)
      mMediaRecorder!!.setOutputFile(videofile)
      mMediaRecorder!!.prepare()
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  private fun stopScreenSharing():String {
    if (mVirtualDisplay == null) {
      return ""
    }
    mVirtualDisplay!!.release()
    destroyMediaProjection()
    isRecording = false
    MediaScannerConnection.scanFile(
      this, arrayOf(videofile), null
    ) { path, uri ->
      Log.i("External", "scanned$path:")
      Log.i("External", "-> uri=$uri")
      screenRecordingPath = path
    }
    return screenRecordingPath
  }

  private fun destroyMediaProjection() {
    if (mMediaProjection != null) {
      // mMediaProjection!!.unregisterCallback(mMediaProjectionCallback)
      mMediaProjection!!.stop()
      mMediaProjection = null
    }
    Log.i(TAG, "MediaProjection Stopped")
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode != REQUEST_CODE) {
      Log.e(TAG, "Unknown request code: $requestCode")
      return
    }
    if (resultCode != RESULT_OK) {
      Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
      isRecording = false
      return
    }
    // mMediaProjectionCallback = MediaProjectionCallback()

    mMediaProjection = mProjectionManager!!.getMediaProjection(resultCode, data!!)
    // moveTaskToBack(true);
    timerTask = object : TimerTask() {
      override fun run() {
        runOnUiThread {
          timerTask = object : TimerTask() {
            override fun run() {
              runOnUiThread {
                // mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
                mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
                  "MainActivity",
                  DISPLAY_WIDTH,
                  DISPLAY_HEIGHT,
                  mScreenDensity,
                  DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                  mMediaRecorder!!.surface,
                  null,
                  null
                )
                isRecording = true
                mMediaRecorder!!.start()
              }
            }
          }
          timer!!.schedule(timerTask, 100 )
        }
      }
    }
    timer!!.schedule(timerTask, 100)
  }

/*  @RequiresApi(VERSION_CODES.O)
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

  }*/


  /*
      private class MediaProjectionCallback : MediaProjection.Callback() {
          override fun onStop() {
              if (isRecording) {
                  isRecording = false
                  actionBtnReload()
                  mMediaRecorder.stop()
                  mMediaRecorder.reset()
              }
              mMediaProjection = null
              stopScreenSharing()
          }
      }
  */

  override fun onBackPressed() {
    if (isRecording) {
      mMediaRecorder!!.stop()
      mMediaRecorder!!.reset()
      Log.v(TAG, "Stopping Recording")
      stopScreenSharing()
      finish()
      /*Snackbar.make(
          findViewById(android.R.id.content), "Wanna Stop recording and exit?",
          Snackbar.LENGTH_INDEFINITE
      ).setAction(
          "Stop"
      ) {
          mMediaRecorder!!.stop()
          mMediaRecorder!!.reset()
          Log.v(TAG, "Stopping Recording")
          stopScreenSharing()
          finish()
      }.show()*/
    }
    else {
      finish()
    }
  }

  // set cat
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
    val cAdapter = CatAdapter(catList, object : CatAdapter.OnCatClickInterface{
      override fun onClick(cat: String) {
        Utility.category = cat
        Utility.catSelected = true
      }
    } )
    rvCategories?.adapter = cAdapter
    cAdapter.notifyDataSetChanged()
  }
}
