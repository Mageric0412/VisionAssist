# Changelog

All notable changes to this project will be documented in this file.

## [1.1.1.0] - 2026-03-29

### Fixed

- Fall detection state machine: removed dead POTENTIAL_FALL state, fixed race condition in detection flow

### Added

- Advanced obstacle ahead warnings with distance-based alert thresholds
- Real-time navigation direction announcements

## [1.1.0.0] - 2026-03-29

### Added

- **Emergency Contacts Module**: Add, edit, and manage emergency contacts with immediate call functionality
- **Places Memory Module**: Save and recall places of interest with geofencing and location-based reminders
- **Fall Detection System**: Automatic fall detection with countdown timer and emergency alert escalation
- **Navigation Manager**: Enhanced navigation with compass direction and voice-guided announcements
- **Speech Manager**: TTS (Text-to-Speech) and vibration feedback for obstacle alerts and system notifications

### Technical Details

- Three detection engine support: Google ML Kit, Huawei HMS ML Kit, TFLite YOLOv8
- Room database for local storage of contacts and places
- CameraX integration for camera preview and frame analysis
- Scene analyzer for indoor/outdoor detection
- WCAG AAA compliant accessibility design

## [1.0.0] - 2026-03-22

### Added

- Initial VisionAssist release
- Real-time obstacle detection
- Voice announcements for detected objects
- Three detection engine options (Google ML Kit, Huawei HMS, TFLite YOLOv8)
