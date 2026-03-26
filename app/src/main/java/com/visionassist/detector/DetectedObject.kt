package com.visionassist.detector

/**
 * Represents a detected object in the camera frame
 */
data class DetectedObject(
    val label: String,
    val labelIndex: Int,
    val confidence: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val estimatedDistance: Float? = null // meters, estimated from size
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = width * height

    /**
     * Get relative position in frame (0.0 - 1.0)
     */
    fun getRelativeX(frameWidth: Int): Float = centerX / frameWidth

    fun getRelativeY(frameHeight: Int): Float = centerY / frameHeight

    /**
     * Get direction relative to frame center
     */
    fun getDirection(frameWidth: Int, frameHeight: Int): Direction {
        val relX = getRelativeX(frameWidth)
        val relY = getRelativeY(frameHeight)

        val horizontal = when {
            relX < 0.33f -> HorizontalDirection.LEFT
            relX > 0.66f -> HorizontalDirection.RIGHT
            else -> HorizontalDirection.CENTER
        }

        val vertical = when {
            relY < 0.33f -> VerticalDirection.TOP
            relY > 0.66f -> VerticalDirection.BOTTOM
            else -> VerticalDirection.MIDDLE
        }

        return Direction(horizontal, vertical)
    }

    enum class HorizontalDirection { LEFT, CENTER, RIGHT }
    enum class VerticalDirection { TOP, MIDDLE, BOTTOM }

    data class Direction(
        val horizontal: HorizontalDirection,
        val vertical: VerticalDirection
    )
}

/**
 * Result of scene analysis
 */
data class SceneAnalysis(
    val environment: Environment = Environment.UNKNOWN,
    val lighting: Lighting = Lighting.UNKNOWN,
    val objects: List<DetectedObject> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Environment { INDOOR, OUTDOOR, UNKNOWN }
    enum class Lighting { BRIGHT, NORMAL, LOW, UNKNOWN }
}
