package com.visionassist.detector

import android.graphics.Bitmap

/**
 * Interface for object detection engines
 * Implementations: Google ML Kit, Huawei HMS, TFLite YOLO
 */
interface ObjectDetectorInterface {

    /**
     * Initialize the detector - must be called before detect()
     */
    fun initialize(callback: DetectionCallback)

    /**
     * Detect objects in the given bitmap
     * Note: The bitmap may be recycled after this call returns.
     * Implementations should make a copy if needed for async processing.
     *
     * @param bitmap Input image (may be recycled after call returns)
     * @param frameWidth Original frame width for relative calculations
     * @param frameHeight Original frame height for relative calculations
     */
    fun detect(bitmap: Bitmap, frameWidth: Int, frameHeight: Int)

    /**
     * Check if detector is ready
     */
    fun isReady(): Boolean

    /**
     * Release resources
     */
    fun close()

    /**
     * Get detector name for UI display
     */
    fun getName(): String

    interface DetectionCallback {
        fun onSuccess(objects: List<DetectedObject>)
        fun onFailure(error: String)
        fun onInitialized()
    }
}

/**
 * COCO object labels for common detections (COCO 80-class)
 */
object COCOLabels {
    private val labels = mapOf(
        0 to "person",       1 to "bicycle",     2 to "car",
        3 to "motorcycle",    4 to "airplane",    5 to "bus",
        6 to "train",         7 to "truck",       8 to "boat",
        9 to "traffic light", 10 to "fire hydrant", 11 to "stop sign",
        12 to "parking meter",13 to "bench",      14 to "bird",
        15 to "cat",          16 to "dog",         17 to "horse",
        18 to "sheep",        19 to "cow",         20 to "elephant",
        21 to "bear",         22 to "zebra",       23 to "giraffe",
        24 to "backpack",     25 to "umbrella",   26 to "handbag",
        27 to "tie",          28 to "suitcase",    29 to "frisbee",
        30 to "skis",         31 to "snowboard",   32 to "sports ball",
        33 to "kite",         34 to "baseball bat", 35 to "baseball glove",
        36 to "skateboard",   37 to "surfboard",   38 to "tennis racket",
        39 to "bottle",       40 to "wine glass",  41 to "cup",
        42 to "fork",         43 to "knife",       44 to "spoon",
        45 to "bowl",         46 to "banana",      47 to "apple",
        48 to "sandwich",     49 to "orange",      50 to "broccoli",
        51 to "carrot",       52 to "hot dog",     53 to "pizza",
        54 to "donut",        55 to "cake",        56 to "chair",
        57 to "couch",        58 to "potted plant", 59 to "bed",
        60 to "dining table", 61 to "toilet",      62 to "tv",
        63 to "laptop",       64 to "mouse",        65 to "remote",
        66 to "keyboard",     67 to "cell phone",   68 to "microwave",
        69 to "oven",         70 to "toaster",      71 to "sink",
        72 to "refrigerator", 73 to "book",         74 to "clock",
        75 to "vase",         76 to "scissors",     77 to "teddy bear",
        78 to "hair drier",   79 to "toothbrush"
    )

    fun getLabel(index: Int): String = labels[index] ?: "unknown"

    /** Look up COCO index by label text */
    fun getIndex(labelText: String): Int = labels.entries.find { it.value == labelText }?.key ?: -1

    /** Obstacle-relevant labels for visually impaired (high priority) */
    val obstacleLabels = setOf(
        0,  // person
        1,  // bicycle
        2,  // car
        3,  // motorcycle
        5,  // bus
        6,  // train
        7,  // truck
        9,  // traffic light
        10, // fire hydrant
        11, // stop sign
        12, // parking meter
        13, // bench
        56, // chair
        57, // couch
        58, // potted plant
        59, // bed
        60, // dining table
        61  // toilet
    )

    /** Small obstacles that are hard to see */
    val smallObstacles = setOf(
        39, // bottle
        40, // wine glass
        41, // cup
        42, // fork
        43, // knife
        44, // spoon
        73, // book
        76  // scissors
    )
}
