package com.visionassist

import android.app.Application
import android.util.Log
import com.visionassist.config.AppConfig
import com.visionassist.detector.ObjectDetectorFactory
import com.visionassist.speech.SpeechManager
import timber.log.Timber

class VisionAssistApp : Application() {

    lateinit var speechManager: SpeechManager
        private set

    lateinit var appConfig: AppConfig
        private set

    lateinit var objectDetectorFactory: ObjectDetectorFactory
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        initTimber()
        initComponents()
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initComponents() {
        appConfig = AppConfig(this)
        speechManager = SpeechManager(this)
        objectDetectorFactory = ObjectDetectorFactory(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        speechManager.release()
    }

    companion object {
        private const val TAG = "VisionAssistApp"

        @Volatile
        private lateinit var instance: VisionAssistApp

        fun getInstance(): VisionAssistApp = instance
    }
}
