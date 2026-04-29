package com.smriti.clinicalscribe.transcript

import android.speech.SpeechRecognizer
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smriti.clinicalscribe.transcript.AndroidOfflineSpeechRecognizerClient.Companion.friendlyMessageForRecognitionError
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualAndroidOfflineSpeechRecognizerProbeTest {
    @Test
    fun logsOfflineSpeechRecognizerReadinessWithoutRequiringSpeechInput() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val client = AndroidOfflineSpeechRecognizerClient(context)
        val diagnostics = client.diagnosticsSnapshot()

        Log.i(TAG, "isRecognitionAvailable=${diagnostics.isRecognitionAvailable}")
        Log.i(TAG, "isOnDeviceRecognitionAvailable=${diagnostics.isOnDeviceRecognitionAvailable}")
        Log.i(TAG, "selectedRecognizerPath=${diagnostics.selectedRecognizerPath?.logLabel ?: "none"}")
        Log.i(TAG, "requestedLanguageSequence=${diagnostics.requestedLanguageSequence.joinToString(",")}")

        val finalStatus = when {
            !diagnostics.isRecognitionAvailable -> "recognition-service-unavailable"
            diagnostics.isOnDeviceRecognitionAvailable -> "on-device-recognizer-available"
            else -> "system-recognizer-offline-preferred"
        }
        Log.i(TAG, "finalStatus=$finalStatus")

        if (!diagnostics.isRecognitionAvailable) {
            val mappedMessage = friendlyMessageForRecognitionError(SpeechRecognizer.ERROR_CLIENT)
            Log.i(TAG, "mappedErrorMessage=$mappedMessage")
        } else {
            val mappedLanguagePackMessage =
                friendlyMessageForRecognitionError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
            Log.i(TAG, "mappedLanguageUnavailableMessage=$mappedLanguagePackMessage")
        }

        assertTrue(diagnostics.requestedLanguageSequence.isNotEmpty())
    }

    private companion object {
        const val TAG = "SmritiOfflineSpeechProbe"
    }
}
