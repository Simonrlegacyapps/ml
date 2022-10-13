package com.example.demomlkit.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.demomlkit.R
import com.example.demomlkit.databinding.ActivityMainBinding
import com.example.demomlkit.utils.PrefManager
import com.example.demomlkit.utils.toast
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.pose.PoseDetector

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.CAMERA)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startActivity(Intent(this, CamActivity::class.java))
                finish()
            } else toast(this, "Permissions not granted by the user.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkUserStatus()
    }

    private fun checkUserStatus() {
        if (PrefManager.getString("isLoggedIn") == "yes") {
            startActivity(Intent(this, CamActivity::class.java))
            finish()
        } else {
            binding.loginButton.setOnClickListener {
                if (allPermissionsGranted()) {

                    startActivity(Intent(this, CamActivity::class.java))
                    finish()
                } else ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}
