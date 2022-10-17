package com.example.demomlkit.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.util.*


class Draw(ctx: Context, var pose: Pose, angleText: TextView) : View(ctx) {
    private var mAngleText: TextView = angleText
    private var paint: Paint = Paint()

    init {
        paint.color = Color.GREEN
        paint.strokeWidth = 10.0f
        paint.style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        try {

//            val drawBitmap = Bitmap.createBitmap(
//                resizedBitmap.getWidth(),
//                resizedBitmap.getHeight(),
//                resizedBitmap.getConfig()
//            )
//
//            val canvas = Canvas(drawBitmap)
//
//            canvas.drawBitmap(resizedBitmap, 0f, 0f, null)



            pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)!!.inFrameLikelihood
            pose.allPoseLandmarks[0]?.landmarkType

            Log.d("TAGleftrighteye", "Left Eye: ${pose.getPoseLandmark(PoseLandmark.LEFT_EYE)!!.inFrameLikelihood} " +
                    "Right Eye: ${pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)!!.inFrameLikelihood}" +
            "Right Eye LandmarkType: ${pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)!!.landmarkType}")



            // Getting Coordinates of all above the poses...
            val noseP = pose.getPoseLandmark(PoseLandmark.NOSE)!!.position

            val innerEyeLeftP = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER)!!.position

            val midEyeLeftP = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)!!.position

            val outerEyeLeftP = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER)!!.position

            val innerEyeRightP = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER)!!.position

            val midEyeRightP = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)!!.position

            val outerEyeRightP = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER)!!.position

            val leftMouthP = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH)!!.position

            val rightMouthP = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH)!!.position

            val leftShoulderP = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)!!.position

            val rightShoulderP = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)!!.position

            val leftElbowP = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)!!.position

            val rightElbowP = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)!!.position

            val leftWristP = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)!!.position

            val rightWristP = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)!!.position

            val leftPinkyFingerP = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)!!.position

            val rightPinkyFingerP = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)!!.position

            val leftFingerIndexP = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)!!.position

            val rightFingerIndexP = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)!!.position

            val leftThumbP = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)!!.position

            val rightThumbP = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)!!.position

            val leftHipP = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)!!.position

            val rightHipP = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)!!.position

            val leftKneeP = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)!!.position

            val rightKneeP = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)!!.position

            val leftAnkleP = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)!!.position

            val rightAnkleP = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)!!.position

            val leftHeelP = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)!!.position

            val rightHeelP = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)!!.position

            val leftFootIndexP = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)!!.position

            val rightFootIndexP = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)!!.position

            // Getting Angle Text
            val leftArmAngle: Double = getAngle(
                pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)!!,
                pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)!!,
                pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)!!
            )
            val leftArmAngleText = String.format("%.2f", leftArmAngle)

            val rightArmAngle: Double = getAngle(
                pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)!!,
                pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)!!,
                pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)!!
            )
            val rightArmAngleText = String.format("%.2f", rightArmAngle)

            val leftLegAngle: Double = getAngle(
                pose.getPoseLandmark(PoseLandmark.LEFT_HIP)!!,
                pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)!!,
                pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)!!
            )
            val leftLegAngleText = String.format("%.2f", leftLegAngle)

            val rightLegAngle: Double = getAngle(
                pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)!!,
                pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)!!,
                pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)!!
            )
            val rightLegAngleText = String.format("%.2f", rightLegAngle)

            val angleText = """
                Left Arm : $leftArmAngleText'
                Right Arm : $rightArmAngleText'
                Left Leg : $leftLegAngleText'
                Right Leg : $rightLegAngleText'
                """.trimIndent()

            mAngleText.text = angleText

            if (canvas != null)
                displayAllPoses(
                    noseP = noseP,
                    innerEyeLeftP = innerEyeLeftP,
                    midEyeLeftP = midEyeLeftP,
                    outerEyeLeftP = outerEyeLeftP,
                    innerEyeRightP = innerEyeRightP,
                    midEyeRightP = midEyeRightP,
                    outerEyeRightP = outerEyeRightP,
                    leftMouthP = leftMouthP,
                    rightMouthP = rightMouthP,
                    leftShoulderP = leftShoulderP,
                    rightShoulderP = rightShoulderP,
                    leftElbowP = leftElbowP,
                    rightElbowP = rightElbowP,
                    leftWristP = leftWristP,
                    rightWristP = rightWristP,
                    leftPinkyFingerP = leftPinkyFingerP,
                    rightPinkyFingerP = rightPinkyFingerP,
                    leftFingerIndexP = leftFingerIndexP,
                    rightFingerIndexP = rightFingerIndexP,
                    leftThumbP = leftThumbP,
                    rightThumbP = rightThumbP,
                    leftHipP = leftHipP,
                    rightHipP = rightHipP,
                    leftKneeP = leftKneeP,
                    rightKneeP = rightKneeP,
                    leftAnkleP = leftAnkleP,
                    rightAnkleP = rightAnkleP,
                    leftHeelP = leftHeelP,
                    rightHeelP = rightHeelP,
                    leftFootIndexP = leftFootIndexP,
                    rightFootIndexP = rightFootIndexP,
                    canvas = canvas
                )
        } catch (e: Exception) {
            Log.d("coordinatesException", "getCoordinates: ${e.message}")
        }
    }

    private fun displayAllPoses(
        noseP: PointF,
        innerEyeLeftP: PointF,
        midEyeLeftP: PointF,
        outerEyeLeftP: PointF,
        innerEyeRightP: PointF,
        midEyeRightP: PointF,
        outerEyeRightP: PointF,
        leftMouthP: PointF,
        rightMouthP: PointF,
        leftShoulderP: PointF,
        rightShoulderP: PointF,
        leftElbowP: PointF,
        rightElbowP: PointF,
        leftWristP: PointF,
        rightWristP: PointF,
        leftPinkyFingerP: PointF,
        rightPinkyFingerP: PointF,
        leftFingerIndexP: PointF,
        rightFingerIndexP: PointF,
        leftThumbP: PointF,
        rightThumbP: PointF,
        leftHipP: PointF,
        rightHipP: PointF,
        leftKneeP: PointF,
        rightKneeP: PointF,
        leftAnkleP: PointF,
        rightAnkleP: PointF,
        leftHeelP: PointF,
        rightHeelP: PointF,
        leftFootIndexP: PointF,
        rightFootIndexP: PointF,
        canvas: Canvas
    ) {
        val lShoulderX = leftShoulderP.x
        val lShoulderY = leftShoulderP.y
        val rShoulderX = rightShoulderP.x
        val rShoulderY = rightShoulderP.y

        val lElbowX = leftElbowP.x
        val lElbowY = leftElbowP.y
        val rElbowX = rightElbowP.x
        val rElbowY = rightElbowP.y

        val lWristX = leftWristP.x
        val lWristY = leftWristP.y
        val rWristX = rightWristP.x
        val rWristY = rightWristP.y

        val lHipX = leftHipP.x
        val lHipY = leftHipP.y
        val rHipX = rightHipP.x
        val rHipY = rightHipP.y

        val lKneeX = leftKneeP.x
        val lKneeY = leftKneeP.y
        val rKneeX = rightKneeP.x
        val rKneeY = rightKneeP.y

        val lAnkleX = leftAnkleP.x
        val lAnkleY = leftAnkleP.y
        val rAnkleX = rightAnkleP.x
        val rAnkleY = rightAnkleP.y

        val noseX = noseP.x
        val noseY = noseP.y

        val innerEyeLeftX = innerEyeLeftP.x
        val innerEyeLeftY = innerEyeLeftP.y
        val midEyeLeftX = midEyeLeftP.x
        val midEyeLeftY = midEyeLeftP.y
        val outerEyeLeftX = outerEyeLeftP.x
        val outerEyeLeftY = outerEyeLeftP.y

        val innerEyeRightX = innerEyeRightP.x
        val innerEyeRightY = innerEyeRightP.y
        val midEyeRightX = midEyeRightP.x
        val midEyeRightY = midEyeRightP.y
        val outerEyeRightX = outerEyeRightP.x
        val outerEyeRightY = outerEyeRightP.y

        val leftMouthX = leftMouthP.x
        val leftMouthY = leftMouthP.y
        val rightMouthX = rightMouthP.x
        val rightMouthY = rightMouthP.y

        val leftPinkyFingerX = leftPinkyFingerP.x
        val leftPinkyFingerY = leftPinkyFingerP.y
        val rightPinkyFingerX = rightPinkyFingerP.x
        val rightPinkyFingerY = rightPinkyFingerP.y

        val leftFingerIndexX = leftFingerIndexP.x
        val leftFingerIndexY = leftFingerIndexP.y
        val rightFingerIndexX = rightFingerIndexP.x
        val rightFingerIndexY = rightFingerIndexP.y

        val leftThumbX = leftThumbP.x
        val leftThumbY = leftThumbP.y
        val rightThumbX = rightThumbP.x
        val rightThumbY = rightThumbP.y

        val leftHeelX = leftHeelP.x
        val leftHeelY = leftHeelP.y

        val rightHeelX = rightHeelP.x
        val rightHeelY = rightHeelP.y

        val leftFootIndexX = leftFootIndexP.x
        val leftFootIndexY = leftFootIndexP.y

        val rightFootIndexX = rightFootIndexP.x
        val rightFootIndexY = rightFootIndexP.y

        canvas.drawLine(innerEyeLeftX, innerEyeLeftY, midEyeLeftX, midEyeLeftY, paint) // left eye
        canvas.drawLine(midEyeLeftX, midEyeLeftY, outerEyeLeftX, outerEyeLeftY, paint)

        canvas.drawLine(innerEyeRightX, innerEyeRightY, midEyeRightX, midEyeRightY, paint) // right eye
        canvas.drawLine(midEyeRightX, midEyeRightY, outerEyeRightX, outerEyeRightY, paint)

        canvas.drawLine(innerEyeLeftX, innerEyeLeftY, noseX, noseY, paint) // eye to nose
        canvas.drawLine(innerEyeRightX, innerEyeRightY, noseX, noseY, paint)

        canvas.drawLine(leftMouthX, leftMouthY, rightMouthX, rightMouthY, paint) // mouth/ lips

        canvas.drawLine(lWristX, lWristY, leftPinkyFingerX, leftPinkyFingerY, paint) // Left pinky finegr
        canvas.drawLine(lWristX, lWristY, leftFingerIndexX, leftFingerIndexY, paint) // Left index finegr
        canvas.drawLine(lWristX, lWristY, leftThumbX, leftThumbY, paint) // Left thumb

        canvas.drawLine(rWristX, rWristY, rightPinkyFingerX, rightPinkyFingerY, paint) // Right pinky finegr
        canvas.drawLine(rWristX, rWristY, rightFingerIndexX, rightFingerIndexY, paint) // Right index finegr
        canvas.drawLine(rWristX, rWristY, rightThumbX, rightThumbY, paint) // Right thumb

        canvas.drawLine(lAnkleX, lAnkleY, leftHeelX, leftHeelY, paint) // Left ankle to heel
        canvas.drawLine(leftHeelX, leftHeelY, leftFootIndexX, leftFootIndexY, paint) // Left  heel to FootIndex
        canvas.drawLine(lAnkleX, lAnkleY, leftFootIndexX, leftFootIndexY, paint) // Left ankle to FootIndex

        canvas.drawLine(rAnkleX, rAnkleY, rightHeelX, rightHeelY, paint) // Right ankle to heel
        canvas.drawLine(rightHeelX, rightHeelY, rightFootIndexX, rightFootIndexY, paint) // Right  heel to FootIndex
        canvas.drawLine(rAnkleX, rAnkleY, rightFootIndexX, rightFootIndexY, paint) // Right ankle to FootIndex

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
    }

}