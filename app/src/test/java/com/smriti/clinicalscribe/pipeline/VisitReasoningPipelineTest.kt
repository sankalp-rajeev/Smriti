package com.smriti.clinicalscribe.pipeline

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import com.smriti.clinicalscribe.reasoning.GemmaAgent
import com.smriti.clinicalscribe.reasoning.MockGemmaAgent
import com.smriti.clinicalscribe.reasoning.SupervisorSummary
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
import com.smriti.clinicalscribe.transcript.SimulatedTranscriptClient
import com.smriti.clinicalscribe.transcript.SpeechToTextClient
import com.smriti.clinicalscribe.transcript.TranscriptMetadata
import com.smriti.clinicalscribe.transcript.TranscriptResult
import com.smriti.clinicalscribe.transcript.TranscriptSourceKind
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitReasoningPipelineTest {
    private val patient = Patient(
        id = "patient-meena",
        name = "Meena",
        age = 28,
        sex = "F",
        pregnancyWeeks = 28,
        village = "Rampur",
        riskSummary = "Prior ANC visit saved locally."
    )
    private val history = listOf(
        VisitLog(
            patientId = patient.id,
            visitDateMillis = 1_700_000_000_000L,
            observationText = "Prior routine ANC visit.",
            structuredNote = "Prior history included.",
            protocolCitation = "Smriti Demo Maternal Health Protocol Routine ANC",
            suggestedFollowUp = "Routine follow-up.",
            confirmed = true
        )
    )
    private val headacheChunk = ProtocolChunk(
        id = "danger-headache",
        title = "Danger Signs",
        source = "Smriti Demo Maternal Health Protocol",
        section = "Danger Signs",
        text = "Severe headache and blurred vision require same-day referral support.",
        keywords = "headache|blurred vision|bp 150",
        referralLevel = "SAME_DAY"
    )

    @Test
    fun providedTranscriptSkipsAsr() = runBlocking {
        val asr = CapturingSpeechClient(TranscriptResult.Error("Should not be called."))
        val agent = CapturingGemmaAgent()
        val pipeline = pipeline(agent = agent, asr = asr)

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = history,
                transcriptText = "Meena reports severe headache and blurred vision.",
                audioPath = "local/audio.m4a"
            )
        )

        assertEquals(0, asr.callCount)
        assertEquals(1, agent.callCount)
        assertEquals(TranscriptSourceKind.MANUAL, result.transcriptSource.source)
        assertTrue(result.transcriptText.contains("severe headache"))
    }

    @Test
    fun audioPathCallsSpeechToTextClient() = runBlocking {
        val asr = CapturingSpeechClient(
            TranscriptResult.Success(
                transcript = "Meena reports severe headache and blurred vision.",
                metadata = TranscriptMetadata(source = TranscriptSourceKind.SIMULATED, sourceLabel = "fake-asr")
            )
        )
        val agent = CapturingGemmaAgent()
        val pipeline = pipeline(agent = agent, asr = asr)

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = history,
                audioPath = "local/audio.m4a"
            )
        )

        assertEquals(1, asr.callCount)
        assertEquals("local/audio.m4a", asr.lastAudioPath)
        assertEquals(1, agent.callCount)
        assertEquals("fake-asr", result.transcriptSource.sourceLabel)
    }

    @Test
    fun asrUnavailableReturnsSafeManualTranscriptRequest() = runBlocking {
        val asr = CapturingSpeechClient(
            TranscriptResult.Unavailable("Offline recognizer pack unavailable.")
        )
        val agent = CapturingGemmaAgent()
        val pipeline = pipeline(agent = agent, asr = asr)

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = history,
                audioPath = "local/audio.m4a"
            )
        )

        assertEquals(1, asr.callCount)
        assertEquals(0, agent.callCount)
        assertNull(result.reasoningResult)
        assertTrue(result.unavailableReason!!.contains("Offline recognizer"))
        assertTrue(result.warnings.joinToString().contains("manual transcript"))
    }

    @Test
    fun successfulTranscriptCallsGemmaAgent() = runBlocking {
        val agent = CapturingGemmaAgent()
        val pipeline = pipeline(agent = agent)

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = history,
                transcriptText = "Meena reports severe headache and blurred vision."
            )
        )

        assertEquals(1, agent.callCount)
        assertEquals(patient, agent.patient)
        assertEquals(history, agent.history)
        assertEquals(result.transcriptText, agent.observationText)
        assertTrue(result.reasoningResult!!.structuredNote.contains("not a diagnosis"))
    }

    @Test
    fun normalTranscriptPathDoesNotRequireAudio() = runBlocking {
        val asr = CapturingSpeechClient(TranscriptResult.Error("Audio path should not be required."))
        val agent = CapturingGemmaAgent()
        val pipeline = pipeline(agent = agent, asr = asr)

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = history,
                transcriptText = "Meena reports severe headache and blurred vision."
            )
        )

        assertEquals(0, asr.callCount)
        assertEquals(1, agent.callCount)
        assertNotNull(result.reasoningResult)
    }

    @Test
    fun sampleDangerSignTranscriptGeneratesMockReferralThroughPipeline() = runBlocking {
        val pipeline = VisitReasoningPipeline(
            protocolRetriever = ProtocolRetriever.fromJson(assetCorpusJson()),
            gemmaAgent = MockGemmaAgent(),
            speechToTextClient = SimulatedTranscriptClient()
        )

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = history,
                transcriptText = SimulatedTranscriptClient.DEFAULT_SAMPLE_TRANSCRIPT
            )
        )

        val reasoning = result.reasoningResult
        assertNotNull(reasoning)
        assertEquals(TranscriptSourceKind.MANUAL, result.transcriptSource.source)
        val referral = reasoning!!.referralFlag ?: throw AssertionError("Expected referral flag")
        assertTrue(referral.dangerSigns.contains("headache"))
        assertTrue(referral.dangerSigns.contains("blurred vision"))
        assertTrue(reasoning.protocolCitation.contains("Smriti Demo Maternal Health Protocol"))
        assertTrue(reasoning.structuredNote.contains("CHW confirmation is required"))
    }

    @Test
    fun protocolChunksArePassedThrough() = runBlocking {
        val agent = CapturingGemmaAgent()
        val pipeline = pipeline(agent = agent)

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = history,
                transcriptText = "Meena reports severe headache and blurred vision."
            )
        )

        assertEquals(listOf(headacheChunk), result.protocolChunks)
        assertEquals(listOf(headacheChunk), agent.protocolChunks)
    }

    @Test
    fun pipelineDoesNotIntroduceDiagnosticOrAutonomousWording() = runBlocking {
        val pipeline = pipeline(
            agent = CapturingGemmaAgent(),
            asr = CapturingSpeechClient(TranscriptResult.Unavailable("Offline recognizer unavailable."))
        )

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = history,
                audioPath = "local/audio.m4a"
            )
        )
        val pipelineText = (result.warnings + listOf(result.unavailableReason.orEmpty()))
            .joinToString(separator = "\n")
            .lowercase()

        assertFalse(pipelineText.contains("diagnosis"))
        assertFalse(pipelineText.contains("diagnose"))
        assertFalse(pipelineText.contains("treat without"))
        assertFalse(pipelineText.contains("save automatically"))
    }

    private fun pipeline(
        agent: CapturingGemmaAgent = CapturingGemmaAgent(),
        asr: SpeechToTextClient = CapturingSpeechClient(
            TranscriptResult.Success("Meena reports severe headache and blurred vision.")
        )
    ): VisitReasoningPipeline {
        return VisitReasoningPipeline(
            protocolRetriever = ProtocolRetriever(listOf(headacheChunk)),
            gemmaAgent = agent,
            speechToTextClient = asr
        )
    }

    private fun assetCorpusJson(): String {
        val modulePath = File("src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        val rootPath = File("app/src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        return when {
            modulePath.exists() -> modulePath.readText()
            else -> rootPath.readText()
        }
    }

    private class CapturingSpeechClient(private val result: TranscriptResult) : SpeechToTextClient {
        var callCount: Int = 0
            private set
        var lastAudioPath: String? = null
            private set

        override suspend fun transcribeAudioFile(audioPath: String): TranscriptResult {
            callCount += 1
            lastAudioPath = audioPath
            return result
        }
    }

    private class CapturingGemmaAgent : GemmaAgent {
        var callCount: Int = 0
            private set
        var patient: Patient? = null
            private set
        var history: List<VisitLog> = emptyList()
            private set
        var observationText: String = ""
            private set
        var protocolChunks: List<ProtocolChunk> = emptyList()
            private set

        override suspend fun generateVisitNote(
            patient: Patient,
            visitHistory: List<VisitLog>,
            observationText: String,
            protocolChunks: List<ProtocolChunk>
        ): VisitReasoningResult {
            callCount += 1
            this.patient = patient
            this.history = visitHistory
            this.observationText = observationText
            this.protocolChunks = protocolChunks
            return VisitReasoningResult(
                patientId = patient.id,
                observationText = observationText,
                structuredNote = "Protocol-grounded support only; not a diagnosis. CHW confirmation required.",
                referralFlag = null,
                protocolCitation = protocolChunks.firstOrNull()?.citation ?: "",
                suggestedFollowUp = "Confirm with CHW before saving.",
                protocolChunk = protocolChunks.firstOrNull(),
                uncertain = false,
                clarificationPrompt = null
            )
        }

        override suspend fun generateSupervisorSummary(
            patients: List<Patient>,
            visits: List<VisitLog>,
            referrals: List<ReferralFlag>
        ): SupervisorSummary {
            return SupervisorSummary(
                totalVisits = visits.size,
                referralsFlagged = referrals.size,
                urgentCases = emptyList(),
                followUpsDue = emptyList(),
                narrative = "Summary generated from confirmed local visits."
            )
        }
    }
}
