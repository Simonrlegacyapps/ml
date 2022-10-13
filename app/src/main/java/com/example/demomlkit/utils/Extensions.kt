package com.example.demomlkit.utils

import android.content.Context
import android.widget.Toast
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2

 fun getAngle(firstPoint: PoseLandmark, midPoint: PoseLandmark, lastPoint: PoseLandmark): Double {
    var result = Math.toDegrees(
        atan2(lastPoint.position.y - midPoint.position.y,
        lastPoint.position.x - midPoint.position.x) - atan2(firstPoint.position.y - midPoint.position.y,
        firstPoint.position.x - midPoint.position.x).toDouble())
    result = abs(result) // Angle should never be negative
    if (result > 180) result = 360.0 - result
    return result
}

fun toast(ctx : Context, msg :String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}