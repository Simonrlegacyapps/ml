package com.example.demomlkit.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
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
    private lateinit var isLensBack : String
    private lateinit var isFlashOn : String
    private var catSelected = false
    private var category = ""

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PrefManager.putString("isLoggedIn", "yes")
        setCategories()
        initListeners()
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initListeners() {
        binding.startRecordingButton.setOnClickListener {
            if (catSelected)
            startActivity(
                Intent(this, CamActivity::class.java)
                .putExtra("category", category)
                .putExtra("isFlash", isFlashOn)
                .putExtra("isLensBack", isLensBack))
            else toast(this, "Please select any category")
        }
        binding.goToListButton.setOnClickListener {
            startActivity(Intent(this, RecordedVideoListActivity::class.java))
        }
        binding.rotateCameraIconFront.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.VISIBLE
            binding.rotateCameraIconFront.visibility = View.GONE

            binding.myCameraView.facing = Facing.FRONT
        }
        binding.rotateCameraIconBack.setOnClickListener {
            binding.rotateCameraIconBack.visibility = View.GONE
            binding.rotateCameraIconFront.visibility = View.VISIBLE
            binding.flashIconOn.visibility = View.GONE
            binding.flashIconOff.visibility = View.VISIBLE

            binding.myCameraView.facing = Facing.BACK
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
        binding.myCameraView.facing = Facing.BACK
        isLensBack = "yes"
        isFlashOn = "no"
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