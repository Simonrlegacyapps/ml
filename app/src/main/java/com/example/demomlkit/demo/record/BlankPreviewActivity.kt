package com.google.mlkit.vision.demo.record

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.pose.Pose

class BlankPreviewActivity : AppCompatActivity() {

    private lateinit var relativeDraw: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blank_preview)

        relativeDraw = findViewById(R.id.relativeDraw)

       // println("POSESSS ${Utility.poses}")

       /* for (pose in Utility.poses){

        }*/


       /* for (pose in Utility.poses) {
            processLines(pose)
            Handler(Looper.getMainLooper()).postDelayed({
                processLines(pose)
            }, 3000) //millis

        }*/
    }

    private fun processLines(it: Pose) {
        if(relativeDraw.childCount>3){
            relativeDraw.removeViewAt(3)
        }
        if(it.allPoseLandmarks.isNotEmpty()){

            if(relativeDraw.childCount>3){
                relativeDraw.removeViewAt(3)
            }

            Utility.poses.add(it)
            val element = Draw(applicationContext,it)
            relativeDraw.addView(element)
        }
    }

}