package com.example.demomlkit.utils

import android.os.Build
import android.util.Pair
import androidx.annotation.RequiresApi
import com.example.demomlkit.utils.PoseEmbedding.getPoseEmbedding
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.Pose
import java.lang.Float.max
import java.util.*


class PoseClassifier(
    poseSamples: List<PoseSample?>, maxDistanceTopK: Int,
    meanDistanceTopK: Int, axesWeights: PointF3D
) {
    private val poseSamples: List<PoseSample?>
    private val maxDistanceTopK: Int
    private val meanDistanceTopK: Int
    private val axesWeights: PointF3D

    constructor(poseSamples: List<PoseSample?>) : this(
        poseSamples,
        MAX_DISTANCE_TOP_K,
        MEAN_DISTANCE_TOP_K,
        AXES_WEIGHTS
    ) {
    }

    /**
     * Returns the max range of confidence values.
     *
     *
     * <Since we calculate confidence by counting></Since>[PoseSample]s that survived
     * outlier-filtering by maxDistanceTopK and meanDistanceTopK, this range is the minimum of two.
     */
    fun confidenceRange(): Int {
        return Math.min(maxDistanceTopK, meanDistanceTopK)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun classify(pose: Pose): ClassificationResult {
        return classify(extractPoseLandmarks(pose))
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun classify(landmarks: List<PointF3D>): ClassificationResult {
        val result = ClassificationResult()
        // Return early if no landmarks detected.
        if (landmarks.isEmpty()) {
            return result
        }

        // We do flipping on X-axis so we are horizontal (mirror) invariant.
        val flippedLandmarks: MutableList<PointF3D> = ArrayList(landmarks)
        multiplyAll(flippedLandmarks, PointF3D.from(-1f, 1f, 1f))
        val embedding: List<PointF3D> = getPoseEmbedding(landmarks)
        val flippedEmbedding: List<PointF3D> = getPoseEmbedding(flippedLandmarks)


        // Classification is done in two stages:
        //  * First we pick top-K samples by MAX distance. It allows to remove samples that are almost
        //    the same as given pose, but maybe has few joints bent in the other direction.
        //  * Then we pick top-K samples by MEAN distance. After outliers are removed, we pick samples
        //    that are closest by average.

        // Keeps max distance on top so we can pop it when top_k size is reached.
        val maxDistances: PriorityQueue<Pair<PoseSample?, Float?>> =
            PriorityQueue<Pair<PoseSample?, Float?>>(
                maxDistanceTopK
            ) { o1: Pair<PoseSample?, Float?>, o2: Pair<PoseSample?, Float?> ->
                -java.lang.Float.compare(
                    o1.second!!, o2.second!!
                )
            }
        // Retrieve top K poseSamples by least distance to remove outliers.
        for (poseSample in poseSamples) {
            val sampleEmbedding: List<PointF3D> = poseSample?.embedding!!
            var originalMax = 0f
            var flippedMax = 0f
            for (i in embedding.indices) {
                originalMax = max(
                    originalMax,
                    maxAbs(multiply(subtract(embedding[i], sampleEmbedding[i]), axesWeights))
                )
                flippedMax = max(
                    flippedMax,
                    maxAbs(
                        multiply(
                            subtract(flippedEmbedding[i], sampleEmbedding[i]), axesWeights
                        )
                    )
                )
            }
            // Set the max distance as min of original and flipped max distance.
            maxDistances.add(Pair<PoseSample?, Float?>(poseSample, Math.min(originalMax, flippedMax)))
            // We only want to retain top n so pop the highest distance.
            if (maxDistances.size > maxDistanceTopK) {
                maxDistances.poll()
            }
        }

        // Keeps higher mean distances on top so we can pop it when top_k size is reached.
        val meanDistances: PriorityQueue<Pair<PoseSample?, Float?>> =
            PriorityQueue<Pair<PoseSample?, Float?>>(
                meanDistanceTopK
            ) { o1: Pair<PoseSample?, Float?>, o2: Pair<PoseSample?, Float?> ->
                -java.lang.Float.compare(
                    o1.second!!, o2.second!!
                )
            }
        // Retrive top K poseSamples by least mean distance to remove outliers.
        for (sampleDistances in maxDistances) {
            val poseSample: PoseSample? = sampleDistances.first
            val sampleEmbedding: List<PointF3D>? = poseSample?.embedding
            var originalSum = 0f
            var flippedSum = 0f
            for (i in embedding.indices) {
                originalSum += sumAbs(
                    multiply(
                        subtract(embedding[i], sampleEmbedding?.get(i)!!), axesWeights
                    )
                )
                flippedSum += sumAbs(
                    multiply(subtract(flippedEmbedding[i], sampleEmbedding[i]), axesWeights)
                )
            }
            // Set the mean distance as min of original and flipped mean distances.
            val meanDistance = Math.min(originalSum, flippedSum) / (embedding.size * 2)
            meanDistances.add(Pair<PoseSample?, Float?>(poseSample, meanDistance))
            // We only want to retain top k so pop the highest mean distance.
            if (meanDistances.size > meanDistanceTopK) {
                meanDistances.poll()
            }
        }
        for (sampleDistances in meanDistances) {
            sampleDistances.first?.className?.let {
                result.incrementClassConfidence(it)
            }
        }
        return result
    }

    companion object {
        private const val TAG = "PoseClassifier"
        private const val MAX_DISTANCE_TOP_K = 30
        private const val MEAN_DISTANCE_TOP_K = 10

        // Note Z has a lower weight as it is generally less accurate than X & Y.
        private val AXES_WEIGHTS = PointF3D.from(1f, 1f, 0.2f)
        private fun extractPoseLandmarks(pose: Pose): List<PointF3D> {
            val landmarks: MutableList<PointF3D> = ArrayList()
            for (poseLandmark in pose.allPoseLandmarks) {
                landmarks.add(poseLandmark.position3D)
            }
            return landmarks
        }
    }

    init {
        this.poseSamples = poseSamples
        this.maxDistanceTopK = maxDistanceTopK
        this.meanDistanceTopK = meanDistanceTopK
        this.axesWeights = axesWeights
    }
}