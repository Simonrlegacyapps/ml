package com.google.mlkit.vision.demo.record

import com.google.mlkit.vision.pose.Pose
import java.util.concurrent.CopyOnWriteArrayList

object Utility {
    var poses = ArrayList<Pose>()
  //  var empList: List<String> = CopyOnWriteArrayList()
    var timeDelay = mutableListOf<Long>()

    var category = ""
    var catSelected = false
}