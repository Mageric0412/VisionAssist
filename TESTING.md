# VisionAssist Testing Guide

## Philosophy

100% test coverage is the key to great vibe coding. Tests let you move fast, trust your instincts, and ship with confidence — without them, vibe coding is just yolo coding. With tests, it's a superpower.

## Framework

- **Unit Testing**: JUnit 4 + Mockito + Robolectric
- **Android Test**: AndroidX Test (for Android-specific components)

## Running Tests

### Run all unit tests
```bash
./gradlew test
```

### Run a specific test class
```bash
./gradlew test --tests "com.visionassist.utils.AnnouncementBuilderTest"
```

### Run with coverage
```bash
./gradlew test jacocoTestReport
```

### Run Android instrumented tests (on device/emulator)
```bash
./gradlew connectedAndroidTest
```

## Test Structure

```
app/src/
├── main/java/com/visionassist/     # Main source code
└── test/
    └── java/com/visionassist/
        ├── utils/                  # Utility tests
        ├── detector/               # Detector tests
        ├── analysis/               # Analysis tests
        └── config/                # Config tests
```

## Test Layers

### Unit Tests (test/)
Pure Kotlin/Java classes with no Android dependencies.
- `AnnouncementBuilderTest` — Distance formatting, announcement building
- `DetectedObjectTest` — Geometry calculations, direction detection
- `AppConfigTest` — Configuration defaults and validation

### Android Tests (androidTest/)
Tests that require Android framework.
- `SceneAnalyzerTest` — Environment and lighting detection

## Conventions

### Naming
- Test class: `{ClassName}Test.kt`
- Test method: `test name describes expected behavior using underscores`
- Example: `formatDistance returns very close danger for meters less than 0_3`

### Structure
```kotlin
@Test
fun `test name`() {
    // Arrange - set up test data
    val input = 0.5f

    // Act - perform the action
    val result = AnnouncementBuilder.formatDistance(input)

    // Assert - verify the result
    assertEquals("50 centimeters away", result)
}
```

### Regression Tests
When fixing a bug, always write a regression test:
```kotlin
// Regression: ISSUE-NNN — describe what broke
// Found by /qa on YYYY-MM-DD
// Report: .gstack/qa-reports/qa-report-{domain}-{date}.md
@Test
fun `regression issue NNN - description`() {
    // ...
}
```

## Coverage Goals

| Module | Target | Current |
|--------|--------|---------|
| AnnouncementBuilder | 100% | 100% |
| DetectedObject | 100% | 95% |
| SceneAnalyzer | 80% | 60% |
| AppConfig | 100% | 85% |
| ObjectDetectorInterface | 90% | 0% |

## Adding Tests

When adding new functionality:
1. Write the test BEFORE implementing (TDD recommended)
2. Cover happy path AND edge cases
3. For conditionals (if/else, when), test BOTH paths
4. For error handling, test the error case
5. Run tests BEFORE committing

When fixing bugs:
1. Write a regression test that reproduces the bug
2. Fix the bug
3. Verify the test passes
4. Commit the test + fix together
