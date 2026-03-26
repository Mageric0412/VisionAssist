package com.visionassist.utils

import com.visionassist.detector.DetectedObject
import com.visionassist.detector.SceneAnalysis

/**
 * Helper to build human-readable announcements from detection results
 */
object AnnouncementBuilder {

    // Constants for distance thresholds (meters)
    private const val DISTANCE_IMMEDIATE_DANGER = 0.3f
    private const val DISTANCE_VERY_CLOSE = 0.5f
    private const val DISTANCE_1_METER = 1.0f
    private const val DISTANCE_2_METERS = 2.0f
    private const val DISTANCE_3_METERS = 3.0f
    private const val DISTANCE_4_METERS = 4.0f
    private const val DISTANCE_5_METERS = 5.0f
    private const val DISTANCE_7_METERS = 7.0f
    private const val DISTANCE_10_METERS = 10.0f

    // Constants for size thresholds (relative area)
    private const val SIZE_VERY_LARGE = 0.3f
    private const val SIZE_LARGE = 0.15f
    private const val SIZE_MEDIUM = 0.05f
    private const val SIZE_SMALL = 0.01f

    // Constants for string formatting
    private const val PART_SEPARATOR = ", "
    private const val DIRECTION_SEPARATOR = " "
    private const val MAX_TYPES_TO_REPORT = 3

    /**
     * Build announcement for the most relevant obstacle
     */
    fun buildObstacleAnnouncement(
        obj: DetectedObject,
        frameWidth: Int,
        frameHeight: Int
    ): String {
        val parts = mutableListOf<String>()

        // Object type
        parts.add(obj.label)

        // Direction
        val direction = obj.getDirection(frameWidth, frameHeight)
        val directionText = buildDirectionText(direction)
        if (directionText.isNotEmpty()) {
            parts.add(directionText)
        }

        // Distance
        obj.estimatedDistance?.let { distance ->
            parts.add(formatDistance(distance))
        }

        // Size indicator (relative to frame)
        val relativeArea = obj.area / (frameWidth * frameHeight)
        val sizeText = getSizeDescription(relativeArea)
        if (sizeText.isNotEmpty()) {
            parts.add(sizeText)
        }

        return parts.joinToString(PART_SEPARATOR)
    }

    /**
     * Build direction text from direction enum
     */
    private fun buildDirectionText(direction: DetectedObject.Direction): String {
        val parts = mutableListOf<String>()

        when (direction.horizontal) {
            DetectedObject.HorizontalDirection.LEFT -> parts.add("on your left")
            DetectedObject.HorizontalDirection.CENTER -> {} // Skip center
            DetectedObject.HorizontalDirection.RIGHT -> parts.add("on your right")
        }

        when (direction.vertical) {
            DetectedObject.VerticalDirection.TOP -> parts.add("above")
            DetectedObject.VerticalDirection.MIDDLE -> {} // Skip middle
            DetectedObject.VerticalDirection.BOTTOM -> parts.add("below")
        }

        return parts.joinToString(DIRECTION_SEPARATOR)
    }

    /**
     * Format distance for speech (non-overlapping ranges)
     */
    fun formatDistance(meters: Float): String {
        return when {
            meters < DISTANCE_IMMEDIATE_DANGER -> "very close, immediate danger"
            meters < DISTANCE_VERY_CLOSE -> "very close"
            meters < DISTANCE_1_METER -> "${(meters * 100).toInt()} centimeters away"
            meters < DISTANCE_2_METERS -> "about 1 meter ahead"
            meters < DISTANCE_3_METERS -> "about 2 meters ahead"
            meters < DISTANCE_4_METERS -> "about 3 meters ahead"
            meters < DISTANCE_5_METERS -> "about 4 meters ahead"
            meters < DISTANCE_7_METERS -> "about 5 meters ahead"
            meters < DISTANCE_10_METERS -> "far, about 8 meters"
            else -> "very far, about ${meters.toInt()} meters"
        }
    }

    /**
     * Get size description based on relative object size
     * @param relativeArea Area relative to frame (0.0 - 1.0)
     */
    private fun getSizeDescription(relativeArea: Float): String {
        return when {
            relativeArea > SIZE_VERY_LARGE -> "very large object"
            relativeArea > SIZE_LARGE -> "large object"
            relativeArea > SIZE_MEDIUM -> "medium object"
            relativeArea > SIZE_SMALL -> "small object"
            else -> "tiny object"
        }
    }

    /**
     * Build summary announcement for multiple objects
     */
    fun buildMultipleObjectsAnnouncement(objects: List<DetectedObject>): String {
        if (objects.isEmpty()) return ""

        val count = objects.size
        val types = objects.map { it.label }.distinct().take(MAX_TYPES_TO_REPORT)

        return when {
            count == 1 -> "1 ${types[0]} ahead"
            count == 2 -> "2 obstacles: ${types.joinToString(" and ")}"
            count <= 5 -> "$count obstacles including ${types.joinToString(", ")}"
            else -> "multiple obstacles around you"
        }
    }

    /**
     * Build scene description
     */
    fun buildSceneAnnouncement(analysis: SceneAnalysis): String {
        val parts = mutableListOf<String>()

        when (analysis.environment) {
            SceneAnalysis.Environment.INDOOR -> parts.add("Indoor area")
            SceneAnalysis.Environment.OUTDOOR -> parts.add("Outdoor area")
            SceneAnalysis.Environment.UNKNOWN -> {}
        }

        when (analysis.lighting) {
            SceneAnalysis.Lighting.LOW -> parts.add("low lighting, be careful")
            SceneAnalysis.Lighting.BRIGHT -> parts.add("bright lighting")
            else -> {}
        }

        return parts.joinToString(". ")
    }
}
