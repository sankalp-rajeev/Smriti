package com.smriti.clinicalscribe.transcript

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidOfflineSpeechRecognizerClient(
    private val context: Context,
    private val languageTag: String = DEFAULT_LANGUAGE_TAG,
    private val recognizerAvailability: (Context) -> Boolean = { SpeechRecognizer.isRecognitionAvailable(it) },
    private val recognizerFactory: (Context) -> SpeechRecognizer = { SpeechRecognizer.createSpeechRecognizer(it) }
) : SpeechToTextClient {
    override suspend fun transcribeAudioFile(audioPath: String): TranscriptResult {
        if (audioPath.isBlank()) {
            return TranscriptResult.Error("Audio path is blank.")
        }

        if (!recognizerAvailability(context)) {
            return TranscriptResult.Unavailable("Android speech recognition service is unavailable on this device.")
        }

        val offlineIntent = offlineRecognizerIntent()
        if (offlineIntent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) != true) {
            return TranscriptResult.Unavailable("Offline speech recognition could not be requested.")
        }

        return TranscriptResult.Unavailable(
            "Android offline speech recognition is available only as live microphone recognition here; stored audio-file transcription for $audioPath requires a local ASR engine. Please provide or edit the manual transcript."
        )
    }

    override suspend fun transcribeLiveSpeech(): TranscriptResult {
        if (!recognizerAvailability(context)) {
            return TranscriptResult.Unavailable("Android speech recognition service is unavailable on this device.")
        }

        val offlineIntent = offlineRecognizerIntent()
        if (offlineIntent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) != true) {
            return TranscriptResult.Unavailable("Offline speech recognition could not be requested.")
        }

        return suspendCancellableCoroutine { continuation ->
            runOnMainThread {
                if (!continuation.isActive) return@runOnMainThread

                val recognizer = runCatching { recognizerFactory(context) }
                    .getOrElse { error ->
                        continuation.resume(
                            TranscriptResult.Error(
                                "Could not create Android speech recognizer: ${error.message ?: error.javaClass.simpleName}"
                            )
                        )
                        return@runOnMainThread
                    }

                fun finish(result: TranscriptResult) {
                    recognizer.destroy()
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit

                    override fun onError(error: Int) {
                        finish(resultForRecognitionError(error))
                    }

                    override fun onResults(results: Bundle?) {
                        val transcript = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()

                        if (transcript.isBlank()) {
                            finish(TranscriptResult.Unavailable("Android offline speech recognition returned no transcript. Please provide a manual transcript."))
                            return
                        }

                        val confidence = results
                            ?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                            ?.firstOrNull()
                            ?.takeIf { it >= 0f }

                        finish(
                            TranscriptResult.Success(
                                transcript = transcript,
                                metadata = TranscriptMetadata(
                                    source = TranscriptSourceKind.ANDROID_OFFLINE_SPEECH,
                                    sourceLabel = "android-offline-speech",
                                    isOffline = true,
                                    languageTag = languageTag
                                ),
                                confidence = confidence
                            )
                        )
                    }
                })

                continuation.invokeOnCancellation {
                    runOnMainThread {
                        recognizer.cancel()
                        recognizer.destroy()
                    }
                }

                runCatching {
                    recognizer.startListening(offlineIntent)
                }.onFailure { error ->
                    finish(
                        TranscriptResult.Error(
                            "Could not start Android offline speech recognition: ${error.message ?: error.javaClass.simpleName}"
                        )
                    )
                }
            }
        }
    }

    fun offlineRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            Handler(Looper.getMainLooper()).post(block)
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE_TAG = "en-IN"

        fun resultForRecognitionError(errorCode: Int): TranscriptResult {
            return when (errorCode) {
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_SERVER -> TranscriptResult.Unavailable(
                    "Android speech recognition reported a network/server error. Offline recognition is unavailable; please provide a manual transcript."
                )

                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> TranscriptResult.Unavailable(
                    "Android offline speech recognition did not capture a usable transcript. Please provide a manual transcript."
                )

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> TranscriptResult.Unavailable(
                    "Android speech recognizer is busy. Please retry offline recognition or provide a manual transcript."
                )

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> TranscriptResult.Error(
                    "Microphone permission is required for live offline speech recognition."
                )

                else -> TranscriptResult.Error("Android speech recognition failed with error code $errorCode.")
            }
        }
    }
}
