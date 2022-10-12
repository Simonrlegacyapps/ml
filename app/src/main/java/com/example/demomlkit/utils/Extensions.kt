package com.example.demomlkit.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.atan2


fun getCoordinates(pose : Pose, angle : TextView) {
        try {
            // Shoulder Pose
            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

            // Elbow Pose
            val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
            val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)

            // Wrist Pose
            val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
            val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

            // Hip Pose
            val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

            // Knee Pose
            val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
            val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

            // Ankle Pose
            val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
            val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

            // Now get Coordinates of all above these poses...
            val leftShoulderP = leftShoulder!!.position
            val lShoulderX = leftShoulderP.x
            val lShoulderY = leftShoulderP.y
            val rightSoulderP = rightShoulder!!.position
            val rShoulderX = rightSoulderP.x
            val rShoulderY = rightSoulderP.y


            val leftElbowP = leftElbow!!.position
            val lElbowX = leftElbowP.x
            val lElbowY = leftElbowP.y
            val rightElbowP = rightElbow!!.position
            val rElbowX = rightElbowP.x
            val rElbowY = rightElbowP.y


            val leftWristP = leftWrist!!.position
            val lWristX = leftWristP.x
            val lWristY = leftWristP.y
            val rightWristP = rightWrist!!.position
            val rWristX = rightWristP.x
            val rWristY = rightWristP.y


            val leftHipP = leftHip!!.position
            val lHipX = leftHipP.x
            val lHipY = leftHipP.y
            val rightHipP = rightHip!!.position
            val rHipX = rightHipP.x
            val rHipY = rightHipP.y


            val leftKneeP = leftKnee!!.position
            val lKneeX = leftKneeP.x
            val lKneeY = leftKneeP.y
            val rightKneeP = rightKnee!!.position
            val rKneeX = rightKneeP.x
            val rKneeY = rightKneeP.y

            val leftAnkleP = leftAnkle!!.position
            val lAnkleX = leftAnkleP.x
            val lAnkleY = leftAnkleP.y
            val rightAnkleP = rightAnkle!!.position
            val rAnkleX = rightAnkleP.x
            val rAnkleY = rightAnkleP.y

            // Angle Text
            val leftArmAngle: Double = getAngle(leftShoulder, leftElbow, leftWrist)
            val leftArmAngleText = String.format("%.2f", leftArmAngle)
            val rightArmAngle: Double = getAngle(rightShoulder, rightElbow, rightWrist)
            val rightArmAngleText = String.format("%.2f", rightArmAngle)
            val leftLegAngle: Double = getAngle(leftHip, leftKnee, leftAnkle)
            val leftLegAngleText = String.format("%.2f", leftLegAngle)
            val rightLegAngle: Double = getAngle(rightHip, rightKnee, rightAnkle)
            val rightLegAngleText = String.format("%.2f", rightLegAngle)

            val angleText = """
                Left Arm : $leftArmAngleText
                Right Arm : $rightArmAngleText
                Left Leg : $leftLegAngleText
                Right Leg : $rightLegAngleText
                """.trimIndent()

            angle.text = angleText

            displayAll(
                lShoulderX, lShoulderY, rShoulderX, rShoulderY,
                lElbowX, lElbowY, rElbowX, rElbowY,
                lWristX, lWristY, rWristX, rWristY,
                lHipX, lHipY, rHipX, rHipY,
                lKneeX, lKneeY, rKneeX, rKneeY,
                lAnkleX, lAnkleY, rAnkleX, rAnkleY
            )
        } catch (e: Exception) {
            Log.d("coordinatesException", "getCoordinates: ${e.message}")
        }
    }

fun displayAll(
        lShoulderX: Float, lShoulderY: Float, rShoulderX: Float, rShoulderY: Float,
        lElbowX: Float, lElbowY: Float, rElbowX: Float, rElbowY: Float,
        lWristX: Float, lWristY: Float, rWristX: Float, rWristY: Float,
        lHipX: Float, lHipY: Float, rHipX: Float, rHipY: Float,
        lKneeX: Float, lKneeY: Float, rKneeX: Float, rKneeY: Float,
        lAnkleX: Float, lAnkleY: Float, rAnkleX: Float, rAnkleY: Float
    ) {
        val paint = Paint()
        paint.color = Color.GREEN
        val strokeWidth = 4.0f
        paint.strokeWidth = strokeWidth
//        val drawBitmap = Bitmap.createBitmap(
//            resizedBitmap.getWidth(),
//            resizedBitmap.getHeight(),
//            resizedBitmap.getConfig()
//        )
        val canvas = Canvas()
        // canvas.drawBitmap(resizedBitmap, 0f, 0f, null)


        canvas.drawLine(lShoulderX, lShoulderY, rShoulderX, rShoulderY, paint)


        canvas.drawLine(rShoulderX, rShoulderY, rElbowX, rElbowY, paint)


        canvas.drawLine(rElbowX, rElbowY, rWristX, rWristY, paint)


        canvas.drawLine(lShoulderX, lShoulderY, lElbowX, lElbowY, paint)


        canvas.drawLine(lElbowX, lElbowY, lWristX, lWristY, paint)


        canvas.drawLine(rShoulderX, rShoulderY, rHipX, rHipY, paint)


        canvas.drawLine(lShoulderX, lShoulderY, lHipX, lHipY, paint)


        canvas.drawLine(lHipX, lHipY, rHipX, rHipY, paint)


        canvas.drawLine(rHipX, rHipY, rKneeX, rKneeY, paint)


        canvas.drawLine(lHipX, lHipY, lKneeX, lKneeY, paint)


        canvas.drawLine(rKneeX, rKneeY, rAnkleX, rAnkleY, paint)


        canvas.drawLine(lKneeX, lKneeY, lAnkleX, lAnkleY, paint)

//        val singleton = Singleton.getInstance()
//        singleton.setMyImage(drawBitmap)
//        startActivity(intent)
    }


 fun getAngle(firstPoint: PoseLandmark, midPoint: PoseLandmark, lastPoint: PoseLandmark): Double {
    var result = Math.toDegrees(
        atan2(lastPoint.position.y - midPoint.position.y,
        lastPoint.position.x - midPoint.position.x) - atan2(firstPoint.position.y - midPoint.position.y,
        firstPoint.position.x - midPoint.position.x).toDouble())
    result = abs(result) // Angle should never be negative
    if (result > 180) result = 360.0 - result
    return result
}
