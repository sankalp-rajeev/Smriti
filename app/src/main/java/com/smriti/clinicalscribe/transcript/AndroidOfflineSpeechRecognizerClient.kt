package com.smriti.clinicalscribe.transcript

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class AndroidOfflineSpeechRecognizerClient(
    private val context: Context,
    private val languageTag: String = "hi-IN"
) : SpeechToTextClient {
    override suspend fun transcribeAudioFile(audioPath: String): TranscriptResult {
        if (audioPath.isBlank()) {
            return TranscriptResult.Error("Audio path is blank.")
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return TranscriptResult.Unavailable("Android speech recognition service is unavailable on this device.")
        }

        val offlineIntent = offlineRecognizerIntent()
        if (offlineIntent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) != true) {
            return TranscriptResult.Unavailable("Offline speech recognition could not be requested.")
        }

        return TranscriptResult.Unavailable(
            "Android offline speech recognition is available only as a live recognizer in this skeleton; stored audio-file transcription requires a local ASR engine. Please provide a manual transcript."
        )
    }

    fun offlineRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
}
