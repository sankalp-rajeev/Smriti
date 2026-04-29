package com.smriti.clinicalscribe.transcript

interface SpeechToTextClient {
    suspend fun transcribeAudioFile(audioPath: String): TranscriptResult
}

sealed interface TranscriptResult {
    data class Success(
        val transcript: String,
        val metadata: TranscriptMetadata = TranscriptMetadata(source = TranscriptSourceKind.SIMULATED),
        val confidence: Float? = null
    ) : TranscriptResult

    data class Unavailable(val reason: String) : TranscriptResult

    data class Error(val reason: String) : TranscriptResult
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
