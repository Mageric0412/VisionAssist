package com.visionassist.config

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AppConfig
 * Regression: Tests for configuration defaults and validation
 * Found by /qa on 2026-03-23
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppConfigTest {

    private fun createRealConfig(): AppConfig {
        // Use a real SharedPreferences via Robolectric's runtime context
        val context = Robolectric.buildActivity(android.app.Activity::class.java).get()
        return AppConfig(context)
    }

    // ========== Default values tests ==========

    @Test
    fun `default detection engine is google mlkit`() {
        val config = createRealConfig()
        assertEquals(AppConfig.ENGINE_GOOGLE_ML, config.detectionEngine)
    }

    @Test
    fun `default speech engine is android tts`() {
        val config = createRealConfig()
        assertEquals(AppConfig.ENGINE_ANDROID_TTS, config.speechEngine)
    }

    @Test
    fun `default speech rate is 1_0`() {
        val config = createRealConfig()
        assertEquals(1.0f, config.speechRate, 0.01f)
    }

    @Test
    fun `default speech pitch is 1_0`() {
        val config = createRealConfig()
        assertEquals(1.0f, config.speechPitch, 0.01f)
    }

    @Test
    fun `default min confidence is 0_6`() {
        val config = createRealConfig()
        assertEquals(0.6f, config.minConfidence, 0.01f)
    }

    @Test
    fun `default report interval is 1500ms`() {
        val config = createRealConfig()
        assertEquals(1500L, config.reportIntervalMs)
    }

    @Test
    fun `default vibration feedback is true`() {
        val config = createRealConfig()
        assertTrue(config.vibrationFeedback)
    }

    @Test
    fun `default camera facing is back`() {
        val config = createRealConfig()
        assertEquals(AppConfig.CAMERA_BACK, config.cameraFacing)
    }

    @Test
    fun `default distance threshold is 3_0`() {
        val config = createRealConfig()
        assertEquals(3.0f, config.distanceThreshold, 0.01f)
    }

    @Test
    fun `default focus closest only is true`() {
        val config = createRealConfig()
        assertTrue(config.focusClosestOnly)
    }

    @Test
    fun `default enabled categories is basic set`() {
        val config = createRealConfig()
        assertEquals("0,1,2,3,4,5,6,7,8,9", config.enabledCategories)
    }

    @Test
    fun `report distance is true by default`() {
        val config = createRealConfig()
        assertTrue(config.reportDistance)
    }

    @Test
    fun `report direction is true by default`() {
        val config = createRealConfig()
        assertTrue(config.reportDirection)
    }

    @Test
    fun `report size is true by default`() {
        val config = createRealConfig()
        assertTrue(config.reportSize)
    }

    @Test
    fun `report scene is true by default`() {
        val config = createRealConfig()
        assertTrue(config.reportScene)
    }

    // ========== Value coercion tests ==========

    @Test
    fun `speech rate is coerced to valid range`() {
        val config = createRealConfig()
        config.speechRate = 3.0f
        assertEquals(2.0f, config.speechRate, 0.01f)
    }

    @Test
    fun `speech rate is coerced to minimum 0_5`() {
        val config = createRealConfig()
        config.speechRate = 0.1f
        assertEquals(0.5f, config.speechRate, 0.01f)
    }

    @Test
    fun `min confidence is coerced to 0 to 1 range`() {
        val config = createRealConfig()
        config.minConfidence = 1.5f
        assertEquals(1.0f, config.minConfidence, 0.01f)
    }

    @Test
    fun `report interval minimum is 500ms`() {
        val config = createRealConfig()
        config.reportIntervalMs = 100L
        assertEquals(500L, config.reportIntervalMs)
    }

    @Test
    fun `distance threshold is coerced to valid range`() {
        val config = createRealConfig()
        config.distanceThreshold = 15.0f
        assertEquals(10.0f, config.distanceThreshold, 0.01f)
    }

    @Test
    fun `distance threshold minimum is 0_5`() {
        val config = createRealConfig()
        config.distanceThreshold = 0.1f
        assertEquals(0.5f, config.distanceThreshold, 0.01f)
    }

    // ========== Setter tests ==========

    @Test
    fun `setting detection engine calls edit putString`() {
        val config = createRealConfig()
        config.detectionEngine = "huawei_hms"
        assertEquals("huawei_hms", config.detectionEngine)
    }

    // ========== Engine constants tests ==========

    @Test
    fun `HMS API key is empty by default`() {
        assertEquals("", AppConfig.HMS_API_KEY)
    }
}
