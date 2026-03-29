package com.visionassist.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.visionassist.R
import com.visionassist.VisionAssistApp
import com.visionassist.camera.CameraManager
import com.visionassist.config.AppConfig
import com.visionassist.analysis.SceneAnalyzer
import com.visionassist.detector.DetectedObject
import com.visionassist.detector.ObjectDetectorInterface
import com.visionassist.navigation.CompassManager
import com.visionassist.navigation.NavigationManager
import com.visionassist.speech.SpeechManager
import com.visionassist.utils.AnnouncementBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity(), CameraManager.FrameCallback, CompassManager.CompassListener, NavigationManager.NavigationListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var cameraManager: CameraManager
    private lateinit var objectDetector: ObjectDetectorInterface
    private lateinit var sceneAnalyzer: SceneAnalyzer
    private lateinit var speechManager: SpeechManager
    private lateinit var config: AppConfig
    private lateinit var compassManager: CompassManager
    private lateinit var navigationManager: NavigationManager

    private var previewView: PreviewView? = null
    private var statusText: TextView? = null
    private var debugOverlay: TextView? = null

    private var lastAnnouncementTime = 0L
    private var lastDetectedObjects: List<DetectedObject> = emptyList()
    private var lastFrameWidth = 640
    private var lastFrameHeight = 640

    @Volatile
    private var isDetectorReady = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            showStatus("Camera permission required")
            speechManager.speak("Camera permission required. Please enable in settings.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initComponents()
        setupUI()
    }

    private fun initComponents() {
        val app = VisionAssistApp.getInstance()

        config = app.appConfig
        speechManager = app.speechManager
        sceneAnalyzer = SceneAnalyzer()

        objectDetector = app.objectDetectorFactory.createDetector(config.detectionEngine)
        cameraManager = CameraManager(this, this)

        // Initialize navigation components
        compassManager = CompassManager(this)
        compassManager.listener = this
        navigationManager = NavigationManager(this, speechManager)
        navigationManager.listener = this
    }

    private fun setupUI() {
        previewView = findViewById(R.id.preview_view)
        statusText = findViewById(R.id.status_text)
        debugOverlay = findViewById(R.id.debug_overlay)

        findViewById<View>(R.id.btn_settings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<View>(R.id.btn_toggle)?.setOnClickListener {
            toggleDetection()
        }

        findViewById<View>(R.id.btn_test)?.setOnClickListener {
            testAnnouncement()
        }

        cameraManager.getPreview()?.setSurfaceProvider(previewView?.surfaceProvider)
    }

    override fun onResume() {
        super.onResume()

        // Re-initialize if engine changed
        val app = VisionAssistApp.getInstance()
        if (app.objectDetectorFactory.getCurrentEngine() != config.detectionEngine) {
            objectDetector = app.objectDetectorFactory.createDetector(config.detectionEngine)
            if (isDetectorReady) {
                initializeDetector()
            }
        }

        checkCameraPermission()
        speechManager.initialize()

        // Start navigation (GPS + compass)
        navigationManager.startNavigation(this)
        compassManager.start()
    }

    override fun onPause() {
        super.onPause()
        stopDetection()
        compassManager.stop()
        navigationManager.stopNavigation()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
        objectDetector.close()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        if (!cameraManager.hasCameraPermission()) {
            showStatus("Camera permission not granted")
            return
        }

        cameraManager.setFrameCallback(this)
        cameraManager.start()
        initializeDetector()
    }

    private fun initializeDetector() {
        showStatus("Initializing ${objectDetector.getName()}...")

        objectDetector.initialize(object : ObjectDetectorInterface.DetectionCallback {
            override fun onInitialized() {
                isDetectorReady = true
                showStatus("${objectDetector.getName()} ready")
                speechManager.speak("${objectDetector.getName()} detector ready")
            }

            override fun onSuccess(objects: List<DetectedObject>) {
                processDetectionResults(objects)
            }

            override fun onFailure(error: String) {
                Timber.e("Detector error: $error")
                showStatus("Detection error: $error")
            }
        })
    }

    private fun processDetectionResults(objects: List<DetectedObject>) {
        lastDetectedObjects = objects
        updateDebugOverlay(objects)

        // Pass all detected objects to navigation manager for advance warning
        objects.forEach { obj ->
            navigationManager.processDetectedObject(obj)
        }

        val relevantObjects = filterRelevantObjects(objects)

        if (relevantObjects.isNotEmpty()) {
            val now = System.currentTimeMillis()
            if (now - lastAnnouncementTime >= config.reportIntervalMs) {
                announceObstacles(relevantObjects)
                lastAnnouncementTime = now
            }
        }
    }

    private fun filterRelevantObjects(objects: List<DetectedObject>): List<DetectedObject> {
        val enabledCategories = config.enabledCategories
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

        return objects
            .filter { obj ->
                obj.confidence >= config.minConfidence &&
                        (enabledCategories.isEmpty() || obj.labelIndex in enabledCategories)
            }
            .filter { obj ->
                obj.estimatedDistance?.let { it <= config.distanceThreshold } ?: true
            }
            .let { filtered ->
                if (config.focusClosestOnly) {
                    filtered.sortedBy { it.estimatedDistance ?: Float.MAX_VALUE }.take(3)
                } else {
                    filtered.take(5)
                }
            }
    }

    private fun announceObstacles(objects: List<DetectedObject>) {
        val frameWidth = lastFrameWidth
        val frameHeight = lastFrameHeight

        if (objects.size == 1) {
            val obj = objects[0]
            val announcement = AnnouncementBuilder.buildObstacleAnnouncement(obj, frameWidth, frameHeight)
            speechManager.speak(announcement)
            speechManager.vibrate(SpeechManager.VibrationPattern.SHORT)
        } else {
            val closest = objects.minByOrNull { it.estimatedDistance ?: Float.MAX_VALUE }
            closest?.let { obj ->
                val announcement = AnnouncementBuilder.buildObstacleAnnouncement(obj, frameWidth, frameHeight)
                speechManager.speak(announcement)
                speechManager.vibrate(SpeechManager.VibrationPattern.DOUBLE)
            }

            lifecycleScope.launch {
                delay(500)
                val summary = AnnouncementBuilder.buildMultipleObjectsAnnouncement(objects)
                if (summary.isNotEmpty()) {
                    speechManager.speak(summary)
                }
            }
        }
    }

    private fun toggleDetection() {
        if (isDetectorReady) {
            stopDetection()
            showStatus("Detection paused")
            speechManager.speak("Detection paused")
        } else {
            initializeDetector()
            showStatus("Detection resumed")
            speechManager.speak("Detection resumed")
        }
    }

    private fun stopDetection() {
        isDetectorReady = false
        lastAnnouncementTime = 0L
    }

    private fun testAnnouncement() {
        speechManager.speak("Testing speech synthesis. Object detection ready.")
        speechManager.vibrate(SpeechManager.VibrationPattern.LONG)
    }

    private fun updateDebugOverlay(objects: List<DetectedObject>) {
        debugOverlay?.let { overlay ->
            overlay.visibility = View.VISIBLE
            val text = buildString {
                appendLine("Engine: ${objectDetector.getName()}")
                appendLine("FPS: ${calculateFPS()}")
                appendLine("Objects: ${objects.size}")
                objects.take(5).forEach { obj ->
                    val dist = obj.estimatedDistance?.let { "%.1fm".format(it) } ?: "?"
                    appendLine("${obj.label} $dist")
                }
            }
            overlay.text = text
        }
    }

    private var lastFrameTime = 0L
    private var fps = 0
    private val frameTimeHistory = mutableListOf<Long>()
    private val fpsHistorySize = 10

    private fun calculateFPS(): Int {
        val now = System.currentTimeMillis()
        if (lastFrameTime > 0) {
            val delta = now - lastFrameTime
            if (delta > 0) {
                frameTimeHistory.add(delta)
                if (frameTimeHistory.size > fpsHistorySize) {
                    frameTimeHistory.removeAt(0)
                }
                fps = if (frameTimeHistory.isNotEmpty()) {
                    (frameTimeHistory.average() / 1).toInt().coerceIn(1, 60)
                    (1000 / frameTimeHistory.average()).toInt().coerceIn(1, 60)
                } else {
                    0
                }
            }
        }
        lastFrameTime = now
        return fps
    }

    private fun showStatus(message: String) {
        statusText?.text = message
        Timber.d("Status: $message")
    }

    override fun onFrame(bitmap: Bitmap, timestamp: Long) {
        if (!isDetectorReady) return

        lastFrameWidth = bitmap.width
        lastFrameHeight = bitmap.height

        val startTime = System.currentTimeMillis()
        objectDetector.detect(bitmap, bitmap.width, bitmap.height)
        Timber.v("Frame callback: ${System.currentTimeMillis() - startTime}ms")
    }

    // CompassManager.CompassListener implementation
    override fun onHeadingChanged(heading: Float) {
        navigationManager.updateHeading(heading)
    }

    // NavigationManager.NavigationListener implementation
    override fun onLocationChanged(location: android.location.Location) {
        Timber.v("$TAG: Location: ${location.latitude}, ${location.longitude}")
    }

    override fun onDirectionChanged(direction: Int, targetDistance: Float?) {
        // Optional: announce direction periodically
    }

    override fun onObstacleWarning(obstacle: DetectedObject, distance: Float, direction: Int) {
        // Navigation manager already announced via SpeechManager
        Timber.d("$TAG: Advance obstacle warning logged")
    }
}
