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

package com.google.mlkit.vision.demo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.demo.java.ChooserActivity
import com.google.mlkit.vision.demo.kotlin.CameraXLivePreviewActivity
import com.google.mlkit.vision.demo.record.RecordActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener


class EntryChoiceActivity : AppCompatActivity() {

  var hasGranted = false
  val APP_STORAGE_ACCESS_REQUEST_CODE = 501 // Any value

  @RequiresApi(Build.VERSION_CODES.R)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_vision_entry_choice)

    val requiredPermissions = when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA)
      else -> listOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA)
    }

    findViewById<TextView>(R.id.java_entry_point).setOnClickListener {
      val intent = Intent(this@EntryChoiceActivity, ChooserActivity::class.java)
      startActivity(intent)
    }

    findViewById<TextView>(R.id.kotlin_entry_point).setOnClickListener {

      if (hasGranted){
        val intent =
          Intent(
            this@EntryChoiceActivity, CameraXLivePreviewActivity::class.java
          )
        startActivity(intent)
      }
      else{
        Toast.makeText(this@EntryChoiceActivity,"You should grant all permissions",Toast.LENGTH_SHORT).show()
      }
    }

    findViewById<TextView>(R.id.record_voideo_entry_point).setOnClickListener {
      if (hasGranted){
        val intent =
          Intent(
            this@EntryChoiceActivity, RecordActivity::class.java
          )
        startActivity(intent)
      }
      else{
        Toast.makeText(this@EntryChoiceActivity,"You should grant all permissions",Toast.LENGTH_SHORT).show()
      }

    }

    Dexter.withActivity(this).withPermissions(requiredPermissions).withListener(object :MultiplePermissionsListener{
      override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
        if (report != null) {
          if (report.isAnyPermissionPermanentlyDenied) {

            if (SDK_INT >= Build.VERSION_CODES.R) {
              if (Environment.isExternalStorageManager()) {
                // Permission granted. Now resume your workflow.
                hasGranted = true
              }
              else{
                Toast.makeText(this@EntryChoiceActivity,"You should grant all permissions",Toast.LENGTH_SHORT).show()
                val intent = Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID))
                startActivityForResult(intent, APP_STORAGE_ACCESS_REQUEST_CODE)
                hasGranted = false
              }
            }
            else{
              hasGranted = true
            }

            }
          else {
            Toast.makeText(this@EntryChoiceActivity,"All permissions granted",Toast.LENGTH_SHORT).show()
            hasGranted = true
            }
        }
      }
      override fun onPermissionRationaleShouldBeShown(
        permissions: MutableList<PermissionRequest>?,
        token: PermissionToken?
      ) {

      }
    }).check()
  }

  @RequiresApi(Build.VERSION_CODES.R)
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == RESULT_OK && requestCode == APP_STORAGE_ACCESS_REQUEST_CODE) {
      if (Environment.isExternalStorageManager()) {
        hasGranted = true
      }
    }
  }


}
