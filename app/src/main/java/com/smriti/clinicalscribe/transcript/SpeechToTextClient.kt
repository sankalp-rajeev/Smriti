package com.smriti.clinicalscribe.transcript

interface SpeechToTextClient {
    suspend fun transcribeAudioFile(audioPath: String): TranscriptResult

    suspend fun transcribeLiveSpeech(): TranscriptResult {
        return TranscriptResult.Unavailable("Live offline speech recognition is unavailable for this client.")
    }
}

sealed interface TranscriptResult {
    data class Success(
        val transcript: String,
        val metadata: TranscriptMetadata = TranscriptMetadata(source = TranscriptSourceKind.SIMULATED),
        val confidence: Float? = null
    ) : TranscriptResult

    data class Unavailable(
        val reason: String,
        val debugMetadata: Map<String, String> = emptyMap()
    ) : TranscriptResult

    data class Error(
        val reason: String,
        val debugMetadata: Map<String, String> = emptyMap()
    ) : TranscriptResult
}

data class TranscriptMetadata(
    val source: TranscriptSourceKind,
    val sourceLabel: String = source.name,
    val isOffline: Boolean = true,
    val languageTag: String? = null
)

enum class TranscriptSourceKind {
    MANUAL,
    SIMULATED,
    ANDROID_OFFLINE_SPEECH,
    UNAVAILABLE
}
