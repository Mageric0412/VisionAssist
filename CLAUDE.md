# VisionAssist - Claude Code Context

## Project Overview

VisionAssist is an Android application for visually impaired users that provides real-time obstacle detection and voice announcements. It supports three detection engines: Google ML Kit, Huawei HMS ML Kit, and TFLite YOLOv8.

## Architecture

```
VisionAssist
├── app/
│   ├── src/main/java/com/visionassist/
│   │   ├── VisionAssistApp.kt         # Application class
│   │   ├── ui/
│   │   │   ├── MainActivity.kt        # Main detection screen
│   │   │   └── SettingsActivity.kt    # Settings screen
│   │   ├── detector/
│   │   │   ├── ObjectDetectorInterface.kt  # Detector interface
│   │   │   ├── GoogleMLKitDetector.kt      # Google ML Kit impl
│   │   │   ├── HuaweiHMSDetector.kt        # Huawei HMS impl
│   │   │   ├── TFLiteYOLODetector.kt       # TFLite YOLO impl
│   │   │   ├── DetectedObject.kt            # Detection result data class
│   │   │   └── ObjectDetectorFactory.kt    # Factory for creating detectors
│   │   ├── camera/
│   │   │   └── CameraManager.kt       # CameraX frame capture
│   │   ├── speech/
│   │   │   └── SpeechManager.kt       # TTS and vibration
│   │   ├── analysis/
│   │   │   └── SceneAnalyzer.kt       # Indoor/outdoor detection
│   │   ├── config/
│   │   │   └── AppConfig.kt          # SharedPreferences wrapper
│   │   └── utils/
│   │       ├── AnnouncementBuilder.kt # Speech text generation
│   │       └── PerformanceMonitor.kt  # FPS/latency tracking
│   └── src/test/                      # Unit tests
├── build.gradle                       # Root build config
└── gradle/                            # Gradle wrapper (not committed)
```

## Key Technical Details

### Detection Engines
- **Google ML Kit**: Default, most compatible
- **Huawei HMS**: Best on Huawei devices
- **TFLite YOLOv8**: Offline, requires model file in assets

### COCO Labels
The app uses COCO 80-class labels. See `ObjectDetectorInterface.kt` for the full list.

### Camera Configuration
- Preview: 640x480
- Analysis: 640x640 (square for YOLO)
- Target FPS: 15

### Distance Estimation
Uses object size in frame to estimate distance. Assumes known average object sizes.

## Building

```bash
# Requires Android SDK 34 and Gradle 8.2
./gradlew assembleDebug
./gradlew installDebug
```

## Testing

See [TESTING.md](TESTING.md) for detailed testing guide.

```bash
./gradlew test                    # Run unit tests
./gradlew test jacocoTestReport  # Run with coverage
```

## Important Files

| File | Purpose |
|------|---------|
| `app/build.gradle` | Dependencies and SDK versions |
| `app/src/main/AndroidManifest.xml` | Permissions and components |
| `ObjectDetectorInterface.kt` | Core detection interface + COCO labels |
| `MainActivity.kt` | Main UI and detection loop |

## Common Issues

1. **HMS API Key**: Set in `AppConfig.HMS_API_KEY` before HMS will work
2. **YOLO Model**: Place `yolov8n.tflite` in `app/src/main/assets/`
3. **Camera Permission**: Required for detection to work

## Code Style

- Kotlin 1.9.22
- Android SDK 34
- JVM target 17
- 4-space indentation
