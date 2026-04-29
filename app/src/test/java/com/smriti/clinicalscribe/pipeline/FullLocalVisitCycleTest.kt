package com.smriti.clinicalscribe.pipeline

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.LocalVisitMemoryStore
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientDao
import com.smriti.clinicalscribe.data.ProtocolChunkDao
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.ReferralFlagDao
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.data.VisitLogDao
import com.smriti.clinicalscribe.rag.ProtocolChunk
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import com.smriti.clinicalscribe.reasoning.MockGemmaAgent
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
import com.smriti.clinicalscribe.transcript.SimulatedTranscriptClient
import com.smriti.clinicalscribe.transcript.SpeechToTextClient
import com.smriti.clinicalscribe.transcript.TranscriptResult
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullLocalVisitCycleTest {
    private val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
    private val retriever = ProtocolRetriever.fromJson(assetCorpusJson())
    private val agent = MockGemmaAgent()

    @Test
    fun normalAncFollowUpProducesCitedNonDiagnosticNoReferralResult() = runBlocking {
        val store = fakeStore()
        val seeded = store.seedDemoIfNeeded(retriever.allChunks(), nowMillis = SEED_TIME)

        val reasoning = processTranscript(
            priorVisits = store.historyForPatient(seeded, patient.id),
            transcript = "Meena reports normal blood pressure 120 over 80. Fetal movement is present. Eating well and taking iron tablets."
        )

        assertEquals(null, reasoning.referralFlag)
        assertFalse(reasoning.uncertain)
        assertTrue(reasoning.protocolCitation.contains("Smriti Demo Maternal Health Protocol"))
        assertSafetyWording(reasoning)

        val beforeConfirmation = store.refresh()
        assertEquals(2, beforeConfirmation.visits.size)
        assertEquals(0, beforeConfirmation.referrals.size)
    }

    @Test
    fun incompleteObservationProducesUncertainClarificationWithoutReferralOrSave() = runBlocking {
        val store = fakeStore()
        val seeded = store.seedDemoIfNeeded(retriever.allChunks(), nowMillis = SEED_TIME)

        val reasoning = processTranscript(
            priorVisits = store.historyForPatient(seeded, patient.id),
            transcript = "Meena feels unwell today but vitals were not taken."
        )

        assertEquals(null, reasoning.referralFlag)
        assertTrue(reasoning.uncertain)
        assertTrue(reasoning.clarificationPrompt!!.contains("No matching local protocol"))
        assertTrue(reasoning.protocolCitation.contains("No matching protocol citation"))
        assertSafetyWording(reasoning)

        val beforeConfirmation = store.refresh()
        assertEquals(2, beforeConfirmation.visits.size)
        assertEquals(0, beforeConfirmation.referrals.size)
    }

    @Test
    fun fullLocalVisitCyclePersistsOnlyAfterConfirmationAndFeedsReturnHistoryAndSummary() = runBlocking {
        val store = fakeStore()
        val seeded = store.seedDemoIfNeeded(retriever.allChunks(), nowMillis = SEED_TIME)
        val initialHistory = store.historyForPatient(seeded, patient.id)
        assertEquals(2, initialHistory.size)

        val asr = UnavailableSpeechClient()
        val pipeline = VisitReasoningPipeline(
            protocolRetriever = retriever,
            gemmaAgent = agent,
            speechToTextClient = asr
        )
        val pipelineResult = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = initialHistory,
                transcriptText = SimulatedTranscriptClient.DEFAULT_SAMPLE_TRANSCRIPT,
                audioPath = "local/audio-should-not-be-needed.m4a"
            )
        )
        val reasoning = pipelineResult.reasoningResult ?: throw AssertionError("Expected reasoning result")

        assertEquals(0, asr.callCount)
        assertTrue(pipelineResult.protocolChunks.isNotEmpty())
        assertTrue(pipelineResult.protocolChunks.first().source.contains("Smriti Demo Maternal Health Protocol"))
        val referral = reasoning.referralFlag ?: throw AssertionError("Expected referral flag")
        assertTrue(reasoning.protocolCitation.contains("Smriti Demo Maternal Health Protocol"))
        assertSafetyWording(reasoning)

        val beforeConfirmation = store.refresh()
        assertEquals(2, beforeConfirmation.visits.size)
        assertEquals(0, beforeConfirmation.referrals.size)

        val confirmed = store.saveConfirmedVisit(
            result = reasoning.copy(
                referralFlag = referral.copy(createdAtMillis = FIRST_SAVE_TIME)
            ),
            editedNote = reasoning.structuredNote,
            editedFollowUp = reasoning.suggestedFollowUp,
            voiceNote = null,
            nowMillis = FIRST_SAVE_TIME
        )
        val returnHistory = store.historyForPatient(confirmed, patient.id)
        assertEquals(3, returnHistory.size)
        assertEquals(reasoning.observationText, returnHistory.first().observationText)
        assertTrue(returnHistory.first().confirmed)
        assertEquals(1, confirmed.referrals.size)
        assertEquals(returnHistory.first().id, confirmed.referrals.single().visitLogId)

        val summary = agent.generateSupervisorSummary(
            patients = confirmed.patients,
            visits = confirmed.visits,
            referrals = confirmed.referrals
        )
        assertEquals(3, summary.totalVisits)
        assertEquals(1, summary.referralsFlagged)
        assertEquals(1, summary.urgentCases.size)
        assertTrue(summary.urgentCases.single().startsWith("Meena - SAME_DAY"))
        assertFalse(summary.urgentCases.single().contains("Protocol-grounded referral suggestion"))
        assertFalse(summary.urgentCases.single().contains("not a diagnosis"))

        val repeatedSave = store.saveConfirmedVisit(
            result = reasoning.copy(
                referralFlag = referral.copy(createdAtMillis = SECOND_SAVE_TIME)
            ),
            editedNote = reasoning.structuredNote,
            editedFollowUp = reasoning.suggestedFollowUp,
            voiceNote = null,
            nowMillis = SECOND_SAVE_TIME
        )
        val repeatedSummary = agent.generateSupervisorSummary(
            patients = repeatedSave.patients,
            visits = repeatedSave.visits,
            referrals = repeatedSave.referrals
        )
        assertEquals(4, repeatedSummary.totalVisits)
        assertEquals(2, repeatedSummary.referralsFlagged)
        assertEquals(1, repeatedSummary.urgentCases.size)
        assertTrue(repeatedSummary.urgentCases.single().startsWith("Meena - SAME_DAY"))
        assertFalse(repeatedSummary.urgentCases.single().contains("Severe or persistent headache during pregnancy"))

        val reset = store.resetDemoData(retriever.allChunks(), nowMillis = SEED_TIME)
        assertEquals(2, store.historyForPatient(reset, patient.id).size)
        assertEquals(0, reset.referrals.size)
    }

    @Test
    fun audioPathOnlyReturnsSafeUnavailableResultWithoutSaving() = runBlocking {
        val store = fakeStore()
        val seeded = store.seedDemoIfNeeded(retriever.allChunks(), nowMillis = SEED_TIME)
        val asr = UnavailableSpeechClient()
        val pipeline = VisitReasoningPipeline(
            protocolRetriever = retriever,
            gemmaAgent = agent,
            speechToTextClient = asr
        )

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = store.historyForPatient(seeded, patient.id),
                audioPath = "local/audio.m4a"
            )
        )

        assertEquals(1, asr.callCount)
        assertEquals(null, result.reasoningResult)
        assertTrue(result.warnings.joinToString().contains("manual transcript"))
        assertEquals(2, store.refresh().visits.size)
        assertEquals(0, store.refresh().referrals.size)
    }

    private suspend fun processTranscript(
        priorVisits: List<VisitLog>,
        transcript: String
    ): VisitReasoningResult {
        val pipeline = VisitReasoningPipeline(
            protocolRetriever = retriever,
            gemmaAgent = agent,
            speechToTextClient = UnavailableSpeechClient()
        )
        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = priorVisits,
                transcriptText = transcript
            )
        )
        return result.reasoningResult ?: throw AssertionError("Expected reasoning result")
    }

    private fun assertSafetyWording(reasoning: VisitReasoningResult) {
        val combined = listOf(
            reasoning.structuredNote,
            reasoning.suggestedFollowUp,
            reasoning.referralFlag?.reason.orEmpty()
        ).joinToString(separator = "\n").lowercase()
        assertTrue(combined.contains("not a diagnosis"))
        assertTrue(combined.contains("chw confirmation") || combined.contains("confirm"))
    }

    private fun fakeStore(): LocalVisitMemoryStore {
        return LocalVisitMemoryStore(
            patientDao = FakePatientDao(),
            visitLogDao = FakeVisitLogDao(),
            referralFlagDao = FakeReferralFlagDao(),
            protocolChunkDao = FakeProtocolChunkDao()
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

    private class UnavailableSpeechClient : SpeechToTextClient {
        var callCount = 0
            private set

        override suspend fun transcribeAudioFile(audioPath: String): TranscriptResult {
            callCount += 1
            return TranscriptResult.Unavailable("Offline ASR unavailable in normal test flow.")
        }
    }

    private class FakePatientDao : PatientDao {
        private val patients = mutableListOf<Patient>()

        override suspend fun getAll(): List<Patient> = patients.sortedBy { it.name }

        override suspend fun upsertAll(patients: List<Patient>) {
            patients.forEach { patient ->
                this.patients.removeAll { it.id == patient.id }
                this.patients.add(patient)
            }
        }
    }

    private class FakeVisitLogDao : VisitLogDao {
        private val visits = mutableListOf<VisitLog>()
        private var nextId = 1L

        override suspend fun getForPatient(patientId: String): List<VisitLog> {
            return getAll().filter { it.patientId == patientId }
        }

        override suspend fun getAll(): List<VisitLog> {
            return visits.sortedByDescending { it.visitDateMillis }
        }

        override suspend fun insert(visitLog: VisitLog): Long {
            val id = if (visitLog.id == 0L) nextId++ else visitLog.id
            visits.removeAll { it.id == id }
            visits.add(visitLog.copy(id = id))
            return id
        }

        override suspend fun deleteAll() {
            visits.clear()
        }
    }

    private class FakeReferralFlagDao : ReferralFlagDao {
        private val referrals = mutableListOf<ReferralFlag>()
        private var nextId = 1L

        override suspend fun getAll(): List<ReferralFlag> {
            return referrals.sortedByDescending { it.createdAtMillis }
        }

        override suspend fun insert(referralFlag: ReferralFlag): Long {
            val id = if (referralFlag.id == 0L) nextId++ else referralFlag.id
            referrals.removeAll { it.id == id }
            referrals.add(referralFlag.copy(id = id))
            return id
        }

        override suspend fun deleteAll() {
            referrals.clear()
        }
    }

    private class FakeProtocolChunkDao : ProtocolChunkDao {
        private val chunks = mutableListOf<ProtocolChunk>()

        override suspend fun getAll(): List<ProtocolChunk> = chunks.toList()

        override suspend fun upsertAll(chunks: List<ProtocolChunk>) {
            chunks.forEach { chunk ->
                this.chunks.removeAll { it.id == chunk.id }
                this.chunks.add(chunk)
            }
        }
    }

    private companion object {
        const val SEED_TIME = 1_700_000_000_000L
        const val FIRST_SAVE_TIME = SEED_TIME + 1_000L
        const val SECOND_SAVE_TIME = SEED_TIME + 2_000L
    }
}
