package com.example.demomlkit.view

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.demomlkit.R
import com.example.demomlkit.databinding.ActivityMainBinding
import com.example.demomlkit.utils.PrefManager


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).apply {
              //  if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.CAMERA)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
              //  }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else onPermissionDenied()
        }
    }

    private fun onPermissionDenied() {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialog.setTitle("Allow Permissions")
        alertDialog.setMessage("Please allow the required permissions")
        alertDialog.setIcon(R.drawable.ic_permission_foreground)
        alertDialog.setPositiveButton(
            "Allow"
        ) { dialog, which ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        alertDialog.setNegativeButton(
            "NO"
        ) { dialog, which ->
            dialog.cancel()
        }
        alertDialog.show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkUserStatus()
    }

    private fun checkUserStatus() {
        if (PrefManager.getString("isLoggedIn") == "yes") {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            binding.loginButton.setOnClickListener {
                if (allPermissionsGranted()) {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }
}
