package com.smriti.clinicalscribe.transcript

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidOfflineSpeechRecognizerClient(
    private val context: Context,
    languageFallbackOrder: List<String> = DEFAULT_LANGUAGE_FALLBACK_ORDER,
    private val recognizerAvailability: (Context) -> Boolean = { SpeechRecognizer.isRecognitionAvailable(it) },
    private val onDeviceRecognizerAvailability: (Context) -> Boolean = { defaultOnDeviceRecognizerAvailability(it) },
    private val recognizerFactory: (Context, RecognizerPath) -> SpeechRecognizer = { ctx, path ->
        when (path) {
            RecognizerPath.ON_DEVICE -> createOnDeviceRecognizer(ctx)
            RecognizerPath.SYSTEM_OFFLINE_PREFERRED -> SpeechRecognizer.createSpeechRecognizer(ctx)
        }
    },
    private val logMessage: (String) -> Unit = { message -> Log.d(LOG_TAG, message) }
) : SpeechToTextClient {
    private val languageFallbackOrder = languageFallbackOrder
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { DEFAULT_LANGUAGE_FALLBACK_ORDER }

    override suspend fun transcribeAudioFile(audioPath: String): TranscriptResult {
        if (audioPath.isBlank()) {
            return TranscriptResult.Error("Audio path is blank.")
        }

        if (!recognizerAvailability(context)) {
            return TranscriptResult.Unavailable(
                reason = "Android speech recognition service is unavailable on this device.",
                debugMetadata = mapOf("recognitionAvailable" to "false")
            )
        }

        val offlineIntent = offlineRecognizerIntent(languageFallbackOrder.first())
        if (offlineIntent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) != true) {
            return TranscriptResult.Unavailable("Offline speech recognition could not be requested.")
        }

        return TranscriptResult.Unavailable(
            "Android offline speech recognition is available only as live microphone recognition here; stored audio-file transcription for $audioPath requires a local ASR engine. Please provide or edit the manual transcript."
        )
    }

    override suspend fun transcribeLiveSpeech(): TranscriptResult {
        val diagnostics = diagnosticsSnapshot()
        logDiagnostics(diagnostics)

        if (!diagnostics.isRecognitionAvailable) {
            return TranscriptResult.Unavailable(
                reason = "Android speech recognition service is unavailable on this device.",
                debugMetadata = diagnostics.asDebugMetadata()
            )
        }

        val recognizerPath = diagnostics.selectedRecognizerPath
            ?: RecognizerPath.SYSTEM_OFFLINE_PREFERRED

        return suspendCancellableCoroutine { continuation ->
            runOnMainThread {
                if (!continuation.isActive) return@runOnMainThread

                var currentRecognizer: SpeechRecognizer? = null
                var isFinished = false

                fun destroyCurrentRecognizer() {
                    currentRecognizer?.let { recognizer ->
                        runCatching { recognizer.destroy() }
                    }
                    currentRecognizer = null
                }

                fun finish(result: TranscriptResult) {
                    if (isFinished) return
                    isFinished = true
                    destroyCurrentRecognizer()
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                fun startLanguageAttempt(languageIndex: Int) {
                    if (!continuation.isActive || isFinished) return

                    val languageTag = languageFallbackOrder[languageIndex]
                    val offlineIntent = offlineRecognizerIntent(languageTag)
                    if (offlineIntent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) != true) {
                        finish(TranscriptResult.Unavailable("Offline speech recognition could not be requested."))
                        return
                    }

                    val recognizer = runCatching { recognizerFactory(context, recognizerPath) }
                        .getOrElse { error ->
                            finish(
                                TranscriptResult.Error(
                                    reason = "Could not create Android speech recognizer: ${error.message ?: error.javaClass.simpleName}",
                                    debugMetadata = diagnostics.asDebugMetadata() + mapOf(
                                        "recognizerPath" to recognizerPath.logLabel,
                                        "languageTag" to languageTag
                                    )
                                )
                            )
                            return
                        }
                    currentRecognizer = recognizer
                    logMessage("Starting ${recognizerPath.logLabel} recognizer with language=$languageTag")

                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) = Unit
                        override fun onBeginningOfSpeech() = Unit
                        override fun onRmsChanged(rmsdB: Float) = Unit
                        override fun onBufferReceived(buffer: ByteArray?) = Unit
                        override fun onEndOfSpeech() = Unit
                        override fun onPartialResults(partialResults: Bundle?) = Unit
                        override fun onEvent(eventType: Int, params: Bundle?) = Unit

                        override fun onError(error: Int) {
                            val shouldFallback = shouldTryNextLanguage(
                                errorCode = error,
                                languageIndex = languageIndex,
                                languageCount = languageFallbackOrder.size
                            )
                            logMessage(
                                "SpeechRecognizer error=$error message=\"${friendlyMessageForRecognitionError(error)}\" language=$languageTag fallback=$shouldFallback"
                            )

                            if (shouldFallback) {
                                destroyCurrentRecognizer()
                                startLanguageAttempt(languageIndex + 1)
                            } else {
                                finish(resultForRecognitionError(error, diagnostics, recognizerPath, languageTag))
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val transcript = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                ?.trim()
                                .orEmpty()

                            if (transcript.isBlank()) {
                                finish(
                                    TranscriptResult.Unavailable(
                                        reason = "No speech recognized. Try again or type the transcript.",
                                        debugMetadata = diagnostics.asDebugMetadata() + mapOf(
                                            "recognizerPath" to recognizerPath.logLabel,
                                            "languageTag" to languageTag,
                                            "emptyResults" to "true"
                                        )
                                    )
                                )
                                return
                            }

                            val confidence = results
                                ?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                                ?.firstOrNull()
                                ?.takeIf { it >= 0f }

                            logMessage("SpeechRecognizer success language=$languageTag path=${recognizerPath.logLabel}")
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

                    runCatching {
                        recognizer.startListening(offlineIntent)
                    }.onFailure { error ->
                        finish(
                            TranscriptResult.Error(
                                reason = "Could not start Android offline speech recognition: ${error.message ?: error.javaClass.simpleName}",
                                debugMetadata = diagnostics.asDebugMetadata() + mapOf(
                                    "recognizerPath" to recognizerPath.logLabel,
                                    "languageTag" to languageTag
                                )
                            )
                        )
                    }
                }

                continuation.invokeOnCancellation {
                    runOnMainThread {
                        runCatching { currentRecognizer?.cancel() }
                        destroyCurrentRecognizer()
                    }
                }

                startLanguageAttempt(languageIndex = 0)
            }
        }
    }

    fun diagnosticsSnapshot(): OfflineSpeechRecognizerDiagnostics {
        val recognitionAvailable = recognizerAvailability(context)
        val onDeviceAvailable = onDeviceRecognizerAvailability(context)
        return OfflineSpeechRecognizerDiagnostics(
            isRecognitionAvailable = recognitionAvailable,
            isOnDeviceRecognitionAvailable = onDeviceAvailable,
            selectedRecognizerPath = when {
                onDeviceAvailable -> RecognizerPath.ON_DEVICE
                recognitionAvailable -> RecognizerPath.SYSTEM_OFFLINE_PREFERRED
                else -> null
            },
            requestedLanguageSequence = languageFallbackOrder
        )
    }

    fun offlineRecognizerIntent(languageTag: String = languageFallbackOrder.first()): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun logDiagnostics(diagnostics: OfflineSpeechRecognizerDiagnostics) {
        logMessage("isRecognitionAvailable=${diagnostics.isRecognitionAvailable}")
        logMessage("isOnDeviceRecognitionAvailable=${diagnostics.isOnDeviceRecognitionAvailable}")
        logMessage("selectedRecognizerPath=${diagnostics.selectedRecognizerPath?.logLabel ?: "none"}")
        logMessage("requestedLanguageSequence=${diagnostics.requestedLanguageSequence.joinToString(",")}")
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            Handler(Looper.getMainLooper()).post(block)
        }
    }

    enum class RecognizerPath(val logLabel: String) {
        ON_DEVICE("on-device"),
        SYSTEM_OFFLINE_PREFERRED("system-offline-preferred")
    }

    data class OfflineSpeechRecognizerDiagnostics(
        val isRecognitionAvailable: Boolean,
        val isOnDeviceRecognitionAvailable: Boolean,
        val selectedRecognizerPath: RecognizerPath?,
        val requestedLanguageSequence: List<String>
    ) {
        fun asDebugMetadata(): Map<String, String> {
            return mapOf(
                "recognitionAvailable" to isRecognitionAvailable.toString(),
                "onDeviceRecognitionAvailable" to isOnDeviceRecognitionAvailable.toString(),
                "selectedRecognizerPath" to (selectedRecognizerPath?.logLabel ?: "none"),
                "requestedLanguageSequence" to requestedLanguageSequence.joinToString(",")
            )
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE_TAG = "en-IN"
        const val LOG_TAG = "SmritiOfflineSpeech"
        val DEFAULT_LANGUAGE_FALLBACK_ORDER = listOf("en-IN", "en-US", "en")

        fun resultForRecognitionError(
            errorCode: Int,
            diagnostics: OfflineSpeechRecognizerDiagnostics? = null,
            recognizerPath: RecognizerPath? = null,
            languageTag: String? = null
        ): TranscriptResult {
            val debugMetadata = mutableMapOf(
                "errorCode" to errorCode.toString(),
                "friendlyMessage" to friendlyMessageForRecognitionError(errorCode)
            )
            diagnostics?.let { debugMetadata.putAll(it.asDebugMetadata()) }
            recognizerPath?.let { debugMetadata["recognizerPath"] = it.logLabel }
            languageTag?.let { debugMetadata["languageTag"] = it }

            return when (errorCode) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> TranscriptResult.Error(
                    reason = "Microphone permission is required for live offline speech recognition.",
                    debugMetadata = debugMetadata
                )

                SpeechRecognizer.ERROR_CLIENT -> TranscriptResult.Error(
                    reason = friendlyMessageForRecognitionError(errorCode),
                    debugMetadata = debugMetadata
                )

                else -> TranscriptResult.Unavailable(
                    reason = friendlyMessageForRecognitionError(errorCode),
                    debugMetadata = debugMetadata
                )
            }
        }

        fun friendlyMessageForRecognitionError(errorCode: Int): String {
            return when (errorCode) {
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                    "Offline speech language pack unavailable on this device. Please type or use sample transcript."

                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
                    "This language is not supported by the device recognizer."

                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    "No speech recognized. Try again or type the transcript."

                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_SERVER,
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
                    "Offline speech recognizer unavailable. Please type or use sample transcript."

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    "Android speech recognizer is busy. Please retry offline recognition or type the transcript."

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "Microphone permission is required for live offline speech recognition."

                else -> "Android speech recognition failed. Please type or use sample transcript."
            }
        }

        fun isLanguageFallbackError(errorCode: Int): Boolean {
            return errorCode == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
                errorCode == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
        }

        fun shouldTryNextLanguage(
            errorCode: Int,
            languageIndex: Int,
            languageCount: Int
        ): Boolean {
            return isLanguageFallbackError(errorCode) && languageIndex + 1 < languageCount
        }

        private fun defaultOnDeviceRecognizerAvailability(context: Context): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        }

        private fun createOnDeviceRecognizer(context: Context): SpeechRecognizer {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        }
    }
}
