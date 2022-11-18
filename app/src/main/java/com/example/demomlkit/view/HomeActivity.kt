package com.example.demomlkit.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.demomlkit.R
import com.example.demomlkit.databinding.ActivityHomeBinding
import com.example.demomlkit.utils.PrefManager
import com.example.demomlkit.utils.toast
import com.example.demomlkit.view.adapter.CategoriesAdapter
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash

class HomeActivity : AppCompatActivity(), CategoriesAdapter.OnCatClickInterface {
    lateinit var binding: ActivityHomeBinding
    private var isLensBack = "yes"
    private var isFlashOn = "no"
    private var catSelected = false
    private var category = ""

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.CAMERA)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    add(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) return
            else toast(this, "Permissions are not granted")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PrefManager.putString("isLoggedIn", "yes")
        checkAllPermissions()
        setCategories()
        initListeners()
    }

    private fun checkAllPermissions() {
        if (allPermissionsGranted()) return
        else ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initListeners() {
        binding.startRecordingButton.setOnClickListener {
            if (catSelected) {
                if (!(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted) {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    startActivity(intent)
                } else startActivity(
                        Intent(this, CamActivity::class.java)
                            .putExtra("category", category)
                            .putExtra("isFlash", isFlashOn)
                            .putExtra("isLensBack", isLensBack))
            }
            else toast(this, "Please select any category")
        }

        binding.goToListButton.setOnClickListener {
            startActivity(Intent(this, RecordedVideoListActivity::class.java))
        }

        binding.rotateCameraIconFront.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.VISIBLE
            binding.rotateCameraIconFront.visibility = View.GONE

            binding.myCameraView.facing = Facing.FRONT
            isLensBack = "no"
        }
        binding.rotateCameraIconBack.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.GONE
            binding.rotateCameraIconFront.visibility = View.VISIBLE
            binding.flashIconOn.visibility = View.GONE
            binding.flashIconOff.visibility = View.VISIBLE

            binding.myCameraView.facing = Facing.BACK
            isLensBack = "yes"
        }
        binding.flashIconOn.setOnClickListener {
            binding.flashIconOn.visibility = View.GONE
            binding.flashIconOff.visibility = View.VISIBLE
            isFlashOn = "no"
            binding.myCameraView.flash = Flash.OFF
        }
        binding.flashIconOff.setOnClickListener {
            binding.flashIconOn.visibility = View.VISIBLE
            binding.flashIconOff.visibility = View.GONE
            isFlashOn = "yes"
            binding.myCameraView.flash = Flash.OFF
        }
    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }

    private fun startCamera() {
        binding.myCameraView.setLifecycleOwner(this)
        if (isLensBack == "yes") binding.myCameraView.facing = Facing.BACK
        else binding.myCameraView.facing = Facing.FRONT
    }

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

    override fun onClick(cat: String) {
        category = cat
        catSelected = true
        binding.startRecordingButton.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700))
    }

}