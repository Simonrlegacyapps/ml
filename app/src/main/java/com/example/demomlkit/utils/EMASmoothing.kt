package com.example.demomlkit.utils

import android.os.SystemClock
import java.util.*
import java.util.concurrent.LinkedBlockingDeque

/**
 * Runing EMA smoothing over a window with given stream of pose classification results.
 */
class EMASmoothing @JvmOverloads constructor(
    private val windowSize: Int = DEFAULT_WINDOW_SIZE,
    private val alpha: Float = DEFAULT_ALPHA) {
    // This is a window of ClassificationResult as outputted by the PoseClassifier
    // run smoothing over this window of size.
    private val window: Deque<ClassificationResult>
    private var lastInputMs: Long = 0

    companion object {
        private const val DEFAULT_WINDOW_SIZE = 10
        private const val DEFAULT_ALPHA = 0.2f
        private const val RESET_THRESHOLD_MS: Long = 100
    }

    init {
        window = LinkedBlockingDeque<ClassificationResult>(windowSize)
    }

    fun getSmoothedResult(classificationResult: ClassificationResult): ClassificationResult {
        // Resets memory if the input is too far away from the previous one in time.
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastInputMs > RESET_THRESHOLD_MS) window.clear()

        lastInputMs = nowMs

        // If we are at window size, remove the last result.
        if (window.size == windowSize) window.pollLast()

        // Insert at the beginning of the window.
        window.addFirst(classificationResult)
        val allClasses: MutableSet<String> = HashSet()
        for (result in window) {
            allClasses.addAll(result.allClasses)
        }
        val smoothedResult = ClassificationResult()
        for (className in allClasses) {
            var factor = 1
            var topSum = 0
            var bottomSum = 0
            for (result in window) {
                val value: Int = result.getClassConfidence(className)
                topSum += factor * value
                bottomSum += factor
                factor = (factor * (1.0 - alpha)).toInt()
            }
            smoothedResult.putClassConfidence(className, topSum / bottomSum)
        }
        return smoothedResult
    }
}