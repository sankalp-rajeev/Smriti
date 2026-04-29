package com.smriti.clinicalscribe.data

import com.smriti.clinicalscribe.rag.ProtocolChunk
import com.smriti.clinicalscribe.reasoning.MockGemmaAgent
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalVisitMemoryStoreTest {
    private val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
    private val protocolChunk = ProtocolChunk(
        id = "danger-headache",
        title = "Danger Signs",
        source = "Smriti Demo Maternal Health Protocol",
        section = "Danger Signs",
        text = "Severe headache and blurred vision require same-day referral support.",
        keywords = "headache|blurred vision|bp 150",
        referralLevel = "SAME_DAY"
    )

    @Test
    fun seedDemoIfNeededLoadsPatientsProtocolsAndSeededMeenaHistory() = runBlocking {
        val store = fakeStore()

        val snapshot = store.seedDemoIfNeeded(listOf(protocolChunk), nowMillis = SEED_TIME)

        assertEquals(DemoSeedData.patients.size, snapshot.patients.size)
        assertEquals(2, store.historyForPatient(snapshot, patient.id).size)
        assertEquals(0, snapshot.referrals.size)
    }

    @Test
    fun confirmedVisitIsPersistedAfterReviewSave() = runBlocking {
        val store = fakeStore()
        val result = visitResultWithReferral()

        val snapshot = store.saveConfirmedVisit(
            result = result,
            editedNote = "Edited CHW-confirmed note. This is not a diagnosis.",
            editedFollowUp = "Edited follow-up with citation.",
            voiceNote = null,
            nowMillis = RETURN_VISIT_TIME
        )

        val savedVisit = snapshot.visits.single()
        assertEquals(patient.id, savedVisit.patientId)
        assertEquals(result.observationText, savedVisit.observationText)
        assertEquals("Edited CHW-confirmed note. This is not a diagnosis.", savedVisit.structuredNote)
        assertEquals("Edited follow-up with citation.", savedVisit.suggestedFollowUp)
        assertTrue(savedVisit.confirmed)
    }

    @Test
    fun returningToSamePatientIncludesLatestSavedVisitInHistory() = runBlocking {
        val store = fakeStore()
        store.resetDemoData(listOf(protocolChunk), nowMillis = SEED_TIME)

        val snapshot = store.saveConfirmedVisit(
            result = visitResultWithReferral(),
            editedNote = "Latest confirmed danger-sign visit. This is not a diagnosis.",
            editedFollowUp = "Same-day referral support with citation.",
            voiceNote = null,
            nowMillis = RETURN_VISIT_TIME
        )

        val history = store.historyForPatient(snapshot, patient.id)
        assertEquals(3, history.size)
        assertEquals("Latest confirmed danger-sign visit. This is not a diagnosis.", history.first().structuredNote)
        assertEquals(RETURN_VISIT_TIME, history.first().visitDateMillis)
    }

    @Test
    fun referralFlagPersistsOnlyAfterConfirmation() = runBlocking {
        val store = fakeStore()
        val generatedResult = visitResultWithReferral()

        assertEquals(emptyList<ReferralFlag>(), store.refresh().referrals)

        val snapshot = store.saveConfirmedVisit(
            result = generatedResult,
            editedNote = generatedResult.structuredNote,
            editedFollowUp = generatedResult.suggestedFollowUp,
            voiceNote = null,
            nowMillis = RETURN_VISIT_TIME
        )

        val savedReferral = snapshot.referrals.single()
        assertEquals(patient.id, savedReferral.patientId)
        assertNotNull(savedReferral.visitLogId)
        assertEquals(snapshot.visits.single().id, savedReferral.visitLogId)
    }

    @Test
    fun unconfirmedGeneratedResultDoesNotSave() = runBlocking {
        val store = fakeStore()
        store.resetDemoData(listOf(protocolChunk), nowMillis = SEED_TIME)
        visitResultWithReferral()

        val snapshot = store.refresh()

        assertEquals(2, store.historyForPatient(snapshot, patient.id).size)
        assertEquals(emptyList<ReferralFlag>(), snapshot.referrals)
    }

    @Test
    fun resetDemoDataClearsSavedVisitsAndRestoresSeededMeenaHistory() = runBlocking {
        val store = fakeStore()
        store.resetDemoData(listOf(protocolChunk), nowMillis = SEED_TIME)
        val afterSave = store.saveConfirmedVisit(
            result = visitResultWithReferral(),
            editedNote = "Temporary saved test visit. This is not a diagnosis.",
            editedFollowUp = "Temporary follow-up.",
            voiceNote = null,
            nowMillis = RETURN_VISIT_TIME
        )
        assertEquals(3, store.historyForPatient(afterSave, patient.id).size)
        assertEquals(1, afterSave.referrals.size)

        val resetSnapshot = store.resetDemoData(listOf(protocolChunk), nowMillis = SEED_TIME)

        val resetHistory = store.historyForPatient(resetSnapshot, patient.id)
        assertEquals(2, resetHistory.size)
        assertEquals(emptyList<ReferralFlag>(), resetSnapshot.referrals)
        assertTrue(resetHistory.none { it.structuredNote.contains("Temporary saved test visit") })

        val summary = MockGemmaAgent().generateSupervisorSummary(
            patients = resetSnapshot.patients,
            visits = resetSnapshot.visits,
            referrals = resetSnapshot.referrals
        )
        assertEquals(2, summary.totalVisits)
        assertEquals(0, summary.referralsFlagged)
    }

    private fun fakeStore(): LocalVisitMemoryStore {
        return LocalVisitMemoryStore(
            patientDao = FakePatientDao(),
            visitLogDao = FakeVisitLogDao(),
            referralFlagDao = FakeReferralFlagDao(),
            protocolChunkDao = FakeProtocolChunkDao()
        )
    }

    private fun visitResultWithReferral(): VisitReasoningResult {
        val referral = ReferralFlag(
            patientId = patient.id,
            urgency = "SAME_DAY",
            reason = "Protocol-grounded referral suggestion only, not a diagnosis.",
            protocolBasis = protocolChunk.citation,
            recommendedFacility = "Nearest PHC",
            dangerSigns = "headache, blurred vision",
            createdAtMillis = RETURN_VISIT_TIME
        )
        return VisitReasoningResult(
            patientId = patient.id,
            observationText = "Meena reports severe headache and blurred vision.",
            structuredNote = "Observation support only. This is not a diagnosis. CHW confirmation required.",
            referralFlag = referral,
            protocolCitation = protocolChunk.citation,
            suggestedFollowUp = "Contact supervisor and support same-day referral confirmation.",
            protocolChunk = protocolChunk,
            uncertain = false,
            clarificationPrompt = null
        )
    }

    private class FakePatientDao : PatientDao {
        private val patients = mutableListOf<Patient>()

        override suspend fun getAll(): List<Patient> {
            return patients.sortedBy { it.name }
        }

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

        override suspend fun getAll(): List<ProtocolChunk> {
            return chunks.toList()
        }

        override suspend fun upsertAll(chunks: List<ProtocolChunk>) {
            chunks.forEach { chunk ->
                this.chunks.removeAll { it.id == chunk.id }
                this.chunks.add(chunk)
            }
        }
    }

    private companion object {
        const val SEED_TIME = 1_700_000_000_000L
        const val RETURN_VISIT_TIME = SEED_TIME + 1_000L
    }
}
