package com.visionassist.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.visionassist.R
import com.visionassist.VisionAssistApp
import com.visionassist.emergency.EmergencyContact
import com.visionassist.emergency.EmergencyContactManager
import com.visionassist.speech.SpeechManager
import timber.log.Timber
import java.util.Locale

/**
 * Full-screen activity shown when a fall is detected
 *
 * Behavior:
 * - 5 second countdown displayed
 * - Any tap cancels and returns
 * - After countdown: initiates emergency call
 * - Uses primary contact, or first contact, or opens dialer with 112
 */
class FallAlertActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FallAlertActivity"
        private const val COUNTDOWN_SECONDS = 5
        private const val REQUEST_CALL_PERMISSION = 1001
    }

    interface FallAlertListener {
        fun onAlertCancelled()
        fun onAlertConfirmed(contact: EmergencyContact?)
    }

    private var countdownTimer: CountDownTimer? = null
    private var remainingSeconds = COUNTDOWN_SECONDS

    private lateinit var emergencyContactManager: EmergencyContactManager
    private lateinit var speechManager: SpeechManager

    private val textCountdown by lazy { findViewById<android.widget.TextView>(R.id.text_countdown) }
    private val textMessage by lazy { findViewById<android.widget.TextView>(R.id.text_message) }
    private val btnCancel by lazy { findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fall_alert)

        // Keep screen on
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize managers
        val app = VisionAssistApp.getInstance()
        emergencyContactManager = EmergencyContactManager(this)
        speechManager = app.speechManager

        // Announce fall detection
        speechManager.speak(getString(R.string.fall_detected_message))

        // Cancel button handler
        btnCancel.setOnClickListener {
            cancelAlert()
        }

        // Start countdown
        startCountdown()

        Timber.d("$TAG: Activity started")
    }

    private fun startCountdown() {
        countdownTimer = object : CountDownTimer(
            COUNTDOWN_SECONDS * 1000L,
            1000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                textCountdown.text = remainingSeconds.toString()
                Timber.v("$TAG: Countdown: $remainingSeconds")
            }

            override fun onFinish() {
                textCountdown.text = "0"
                textMessage.text = getString(R.string.fall_calling)
                initiateEmergencyCall()
            }
        }.start()
    }

    private fun cancelAlert() {
        countdownTimer?.cancel()
        countdownTimer = null

        speechManager.speak(getString(R.string.fall_cancel))
        Timber.d("$TAG: Alert cancelled")

        // Notify listener if needed
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun initiateEmergencyCall() {
        val contact = emergencyContactManager.getBestContactForAlert()

        if (contact == null) {
            // No contacts set - prompt user to add them
            Timber.w("$TAG: No emergency contacts set")
            textMessage.text = getString(R.string.fall_no_contacts)
            speechManager.speak(getString(R.string.fall_no_contacts))

            // Open dialer with emergency number (112 works internationally)
            openDialerWithNumber("112")
            return
        }

        if (contact.isEmergencyNumber()) {
            // Direct call for emergency numbers (120, 110, 119, 112)
            Timber.d("$TAG: Calling emergency number: ${contact.phoneNumber}")
            makePhoneCall(contact.phoneNumber)
        } else {
            // For regular contacts, open dialer with number for confirmation
            Timber.d("$TAG: Opening dialer for contact: ${contact.name}")
            openDialerWithNumber(contact.phoneNumber)
        }
    }

    private fun makePhoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } else {
            // Fall back to dialer if no permission
            openDialerWithNumber(phoneNumber)
        }
    }

    private fun openDialerWithNumber(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)

        // Give user a moment to see the dialer, then close
        window.decorView.postDelayed({
            finish()
        }, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        countdownTimer = null
    }

    /**
     * Any touch cancels the alert
     */
    override fun onUserInteraction() {
        super.onUserInteraction()
        // Don't cancel immediately - let user tap to cancel
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            cancelAlert()
            return true
        }
        return super.onTouchEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back button from dismissing without action
        cancelAlert()
    }
}
