package com.smriti.clinicalscribe.pipeline

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk
import com.smriti.clinicalscribe.rag.ProtocolRetrievalContext
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import com.smriti.clinicalscribe.reasoning.GemmaAgent
import com.smriti.clinicalscribe.reasoning.SmritiLatencyLogger
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
import com.smriti.clinicalscribe.transcript.SpeechToTextClient
import com.smriti.clinicalscribe.transcript.TranscriptMetadata
import com.smriti.clinicalscribe.transcript.TranscriptResult
import com.smriti.clinicalscribe.transcript.TranscriptSourceKind

class VisitReasoningPipeline(
    private val protocolRetriever: ProtocolRetriever,
    private val gemmaAgent: GemmaAgent,
    private val speechToTextClient: SpeechToTextClient
) {
    suspend fun process(input: VisitPipelineInput): VisitPipelineResult {
        val transcript = resolveTranscript(input)
        if (transcript.text.isBlank()) {
            return VisitPipelineResult(
                transcriptSource = transcript.metadata,
                transcriptText = "",
                protocolChunks = emptyList(),
                reasoningResult = null,
                warnings = transcript.warnings,
                unavailableReason = transcript.unavailableReason
            )
        }

        val protocolChunks = SmritiLatencyLogger.measure("protocolRetrieval", input.patient.id) {
            protocolRetriever.retrieve(transcript.text, input.protocolContext)
        }
        val reasoningResult = gemmaAgent.generateVisitNote(
            patient = input.patient,
            visitHistory = input.priorVisits,
            observationText = transcript.text,
            protocolChunks = protocolChunks
        )

        return VisitPipelineResult(
            transcriptSource = transcript.metadata,
            transcriptText = transcript.text,
            protocolChunks = protocolChunks,
            reasoningResult = reasoningResult,
            warnings = transcript.warnings,
            unavailableReason = null
        )
    }

    private suspend fun resolveTranscript(input: VisitPipelineInput): ResolvedTranscript {
        val manualTranscript = input.transcriptText?.trim().orEmpty()
        if (manualTranscript.isNotBlank()) {
            return ResolvedTranscript(
                text = manualTranscript,
                metadata = TranscriptMetadata(
                    source = TranscriptSourceKind.MANUAL,
                    sourceLabel = "manual",
                    isOffline = true
                )
            )
        }

        val audioPath = input.audioPath?.trim().orEmpty()
        if (audioPath.isBlank()) {
            return ResolvedTranscript(
                text = "",
                metadata = TranscriptMetadata(source = TranscriptSourceKind.UNAVAILABLE),
                warnings = listOf("Transcript text or local audio path is required before visit reasoning."),
                unavailableReason = "Missing transcript text and audio path."
            )
        }

        return when (val result = speechToTextClient.transcribeAudioFile(audioPath)) {
            is TranscriptResult.Success -> ResolvedTranscript(
                text = result.transcript.trim(),
                metadata = result.metadata,
                warnings = if (result.confidence == null) {
                    emptyList()
                } else {
                    listOf("Transcript confidence: ${result.confidence}.")
                }
            )

            is TranscriptResult.Unavailable -> ResolvedTranscript(
                text = "",
                metadata = TranscriptMetadata(source = TranscriptSourceKind.UNAVAILABLE),
                warnings = listOf("Offline ASR unavailable. Please provide or confirm a manual transcript before reasoning."),
                unavailableReason = result.reason
            )

            is TranscriptResult.Error -> ResolvedTranscript(
                text = "",
                metadata = TranscriptMetadata(source = TranscriptSourceKind.UNAVAILABLE),
                warnings = listOf("Offline ASR failed. Please provide or confirm a manual transcript before reasoning."),
                unavailableReason = result.reason
            )
        }
    }
}

data class VisitPipelineInput(
    val patient: Patient,
    val priorVisits: List<VisitLog>,
    val transcriptText: String? = null,
    val audioPath: String? = null,
    val protocolContext: ProtocolRetrievalContext? = ProtocolRetrievalContext(
        countryCode = "IN",
        region = "INDIA"
    )
)

data class VisitPipelineResult(
    val transcriptSource: TranscriptMetadata,
    val transcriptText: String,
    val protocolChunks: List<ProtocolChunk>,
    val reasoningResult: VisitReasoningResult?,
    val warnings: List<String>,
    val unavailableReason: String?
)

private data class ResolvedTranscript(
    val text: String,
    val metadata: TranscriptMetadata,
    val warnings: List<String> = emptyList(),
    val unavailableReason: String? = null
)
