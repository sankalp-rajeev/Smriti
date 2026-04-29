package com.smriti.clinicalscribe.transcript

class SimulatedTranscriptClient(
    private val transcript: String = DEFAULT_SAMPLE_TRANSCRIPT,
    private val resultOverride: TranscriptResult? = null
) : SpeechToTextClient {
    override suspend fun transcribeAudioFile(audioPath: String): TranscriptResult {
        resultOverride?.let { return it }

        return if (transcript.isBlank()) {
            TranscriptResult.Error("Simulated transcript is blank.")
        } else {
            TranscriptResult.Success(
                transcript = transcript,
                metadata = TranscriptMetadata(
                    source = TranscriptSourceKind.SIMULATED,
                    sourceLabel = "simulated",
                    isOffline = true,
                    languageTag = "hi-IN/en-IN"
                ),
                confidence = 1.0f
            )
        }
    }

    companion object {
        const val DEFAULT_SAMPLE_TRANSCRIPT =
            "Meena is 28 years old and 7 months pregnant. She reports severe headache and blurred vision. Blood pressure is 150 over 95. She has reduced fetal movement today."
    }
}
