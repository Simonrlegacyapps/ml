package com.example.demomlkit.utils

import java.util.*

/**
 * Represents Pose classification result as outputted by [PoseClassifier]. Can be manipulated.
 */
class ClassificationResult {
    // For an entry in this map, the key is the class name, and the value is how many times this class
    // appears in the top K nearest neighbors. The value is in range [0, K] and could be a float after
    // EMA smoothing. We use this number to represent the confidence of a pose being in this class.
    private val classConfidences: MutableMap<String, Int>
    val allClasses: Set<String>
        get() = classConfidences.keys

    fun getClassConfidence(className: String): Int {
        return if (classConfidences.containsKey(className) && classConfidences[className] != null) classConfidences[className]!! else 0
    }

    val maxConfidenceClass: String
        get() = Collections.max<Map.Entry<String, Int>>(
            classConfidences.entries
        ) { (_, value), (_, value1) -> (value - value1).toInt() }.key

    fun incrementClassConfidence(className: String) {
        classConfidences[className] = (if (classConfidences.containsKey(className) && classConfidences[className] != null) classConfidences[className]!! + 1 else 1)
    }

    fun putClassConfidence(className: String, confidence: Int) {
        classConfidences[className] = confidence
    }

    init {
        classConfidences = HashMap()
    }
}