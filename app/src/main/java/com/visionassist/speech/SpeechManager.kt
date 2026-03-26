package com.visionassist.speech

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.visionassist.VisionAssistApp
import com.visionassist.config.AppConfig
import com.visionassist.utils.AnnouncementBuilder
import timber.log.Timber
import java.util.Locale

/**
 * Speech manager supporting Android TTS and Huawei HMS TTS
 * Provides obstacle announcements and haptic feedback
 */
class SpeechManager(private val context: Context) {

    private val config get() = VisionAssistApp.getInstance().appConfig

    // Android TTS
    private var androidTts: TextToSpeech? = null
    private var androidTtsReady = false

    // Huawei TTS - lazy init when needed
    private var huaweiTtsReady = false

    // Vibration
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private var isInitialized = false

    // Reusable Bundle to avoid per-call allocation
    private val volumeBundle = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
    }

    // Vibration constants
    companion object {
        private const val VIBRATION_DURATION_SHORT = 50L
        private const val VIBRATION_DURATION_LONG = 200L
        private const val VIBRATION_PATTERN_DOUBLE = longArrayOf(0, 50, 50, 50)
        private const val DEFAULT_VOLUME = 1.0f
    }

    fun initialize() {
        if (isInitialized) return
        initAndroidTts()
        isInitialized = true
    }

    private fun initAndroidTts() {
        androidTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                androidTts?.let { tts ->
                    val result = tts.setLanguage(Locale.US)
                    androidTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                            result != TextToSpeech.LANG_NOT_SUPPORTED

                    if (androidTtsReady) {
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}
                            override fun onDone(utteranceId: String?) {}
                            override fun onError(utteranceId: String?) {
                                Timber.e("TTS error: $utteranceId")
                            }
                        })
                        applyTtsSettings()
                        Timber.d("Android TTS initialized successfully")
                    } else {
                        Timber.w("Android TTS language not supported")
                    }
                }
            } else {
                Timber.e("Android TTS initialization failed with status: $status")
            }
        }
    }

    private fun applyTtsSettings() {
        androidTts?.setSpeechRate(config.speechRate)
        androidTts?.setPitch(config.speechPitch)
    }

    /**
     * Speak the given text
     * @param text Text to speak
     * @param queueMode Queue mode (QUEUE_FLUSH or QUEUE_ADD)
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) {
            initialize()
        }

        when (config.speechEngine) {
            AppConfig.ENGINE_HUAWEI_TTS -> {
                speakWithHuawei(text)
            }
            else -> {
                speakWithAndroid(text, queueMode)
            }
        }
    }

    private fun speakWithAndroid(text: String, queueMode: Int) {
        if (!androidTtsReady) {
            Timber.w("Android TTS not ready, skipping speech")
            return
        }

        androidTts?.speak(text, queueMode, volumeBundle, "utterance_${System.currentTimeMillis()}")
        Timber.v("Speaking: $text")
    }

    private fun speakWithHuawei(text: String) {
        // Huawei TTS integration would go here
        // For now, fallback to Android TTS
        Timber.w("Huawei TTS not implemented, using Android TTS")
        speakWithAndroid(text, TextToSpeech.QUEUE_FLUSH)
    }

    /**
     * Speak obstacle information using AnnouncementBuilder
     * @deprecated Use AnnouncementBuilder.buildObstacleAnnouncement() directly
     */
    @Deprecated("Use AnnouncementBuilder.buildObstacleAnnouncement() instead", ReplaceWith("AnnouncementBuilder.buildObstacleAnnouncement(obj, frameWidth, frameHeight)"))
    fun announceObstacle(
        obj: com.visionassist.detector.DetectedObject,
        frameWidth: Int,
        frameHeight: Int
    ) {
        val announcement = AnnouncementBuilder.buildObstacleAnnouncement(obj, frameWidth, frameHeight)
        speak(announcement)
    }

    /**
     * Speak scene description
     */
    fun announceScene(scene: String) {
        if (config.reportScene) {
            speak(scene)
        }
    }

    /**
     * Provide haptic feedback
     */
    fun vibrate(pattern: VibrationPattern = VibrationPattern.SHORT) {
        if (!config.vibrationFeedback) return

        try {
            when (pattern) {
                VibrationPattern.SHORT -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_SHORT, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                VibrationPattern.DOUBLE -> {
                    vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN_DOUBLE, -1))
                }
                VibrationPattern.LONG -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_LONG, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Vibration permission denied")
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Vibration argument invalid")
        }
    }

    /**
     * Stop current speech
     */
    fun stop() {
        androidTts?.stop()
    }

    /**
     * Release resources
     */
    fun release() {
        androidTts?.stop()
        androidTts?.shutdown()
        androidTts = null
        androidTtsReady = false
        isInitialized = false
    }

    enum class VibrationPattern {
        SHORT, DOUBLE, LONG
    }
}
