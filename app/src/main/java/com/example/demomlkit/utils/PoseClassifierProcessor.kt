package com.example.demomlkit.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import com.google.common.base.Preconditions
import com.google.mlkit.vision.pose.Pose
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Accepts a stream of [Pose] for classification and Rep counting.
 */
class PoseClassifierProcessor @WorkerThread constructor(context: Context, isStreamMode: Boolean) {
    private val isStreamMode: Boolean
    private var emaSmoothing: EMASmoothing? = null
    private var repCounters: MutableList<RepetitionCounter>? = null
    private var poseClassifier: PoseClassifier? = null
    private var lastRepResult: String? = null

    private fun loadPoseSamples(context: Context) { // here I am loading poses from .csv file
        val poseSamples: MutableList<PoseSample?> = ArrayList<PoseSample?>()
        try {
            val reader = BufferedReader(
                InputStreamReader(context.assets.open(POSE_SAMPLES_FILE))
            )
            var csvLine = reader.readLine()
            while (csvLine != null) {
                // If line is not a valid PoseSample, will get null and skip adding to the list.
                val poseSample: PoseSample? = PoseSample.getPoseSample(csvLine, ",")
                if (poseSample != null)
                    poseSamples.add(poseSample)
                csvLine = reader.readLine()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error when loading pose samples.\n$e")
        }
        poseClassifier = PoseClassifier(poseSamples)
        if (isStreamMode) {
            for (className in POSE_CLASSES) {
                repCounters!!.add(RepetitionCounter(className))
            }
        }
    }

    /**
     * Given a new Pose input, returns a list of formatted Strings with Pose classification results.
     * Currently it returns up to 2 strings as following:
     * 0: PoseClass : X reps
     * 1: PoseClass : [0.0-1.0] confidence
     */
    @RequiresApi(Build.VERSION_CODES.N)
    @WorkerThread
    fun getPoseResult(pose: Pose): MutableList<String?> {
        Preconditions.checkState(Looper.myLooper() != Looper.getMainLooper())
        val result: MutableList<String?> = ArrayList()
        var classification: ClassificationResult = poseClassifier!!.classify(pose)

        // Update { RepetitionCounter } if {@code isStreamMode}.
        if (isStreamMode) {
            // Feed pose to smoothing even if no pose found.
            classification = emaSmoothing!!.getSmoothedResult(classification)

            // Return early without updating repCounter if no pose found.
            if (pose.allPoseLandmarks.isEmpty()) {
                result.add(lastRepResult)
                return result
            }
            for (repCounter in repCounters!!) {
                val repsBefore = repCounter.numRepeats
                val repsAfter = repCounter.addClassificationResult(classification)
                if (repsAfter > repsBefore) {
                    // Play a fun beep when rep counter updates.
                    val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP)
                    lastRepResult = String.format(
                        Locale.US, "%s : %d reps", repCounter.className, repsAfter
                    )
                    break
                }
            }
            result.add(lastRepResult)
        }

        // Add maxConfidence class of current frame to result if pose is found.
        if (pose.allPoseLandmarks.isNotEmpty()) {
            val maxConfidenceClass: String = classification.maxConfidenceClass
            val range = classification.getClassConfidence(maxConfidenceClass) / poseClassifier!!.confidenceRange() //.toString()
            val maxConfidenceClassResult = String.format(
                    Locale.US, "%s : %d confidence", maxConfidenceClass, range
                )
            result.add(maxConfidenceClassResult)
        }
        return result
    }

    companion object {
        private const val TAG = "PoseClassifierProcessor"
        private const val POSE_SAMPLES_FILE = "pose/fitness_pose_samples.csv"

        // Specify classes for which we want rep counting.
        // These are the labels in the given {@code POSE_SAMPLES_FILE}. You can set your own class labels
        // for your pose samples.
        private const val PUSHUPS_CLASS = "pushups_down"
        private const val SQUATS_CLASS = "squats_down"
        private val POSE_CLASSES = arrayOf(
            PUSHUPS_CLASS, SQUATS_CLASS
        )
    }

    init {
        Preconditions.checkState(Looper.myLooper() != Looper.getMainLooper())
        this.isStreamMode = isStreamMode
        if (isStreamMode) {
            emaSmoothing = EMASmoothing()
            repCounters = ArrayList<RepetitionCounter>()
            lastRepResult = ""
        }
        loadPoseSamples(context)
    }
}