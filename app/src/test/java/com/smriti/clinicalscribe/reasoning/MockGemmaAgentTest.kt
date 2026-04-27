package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockGemmaAgentTest {
    private val agent = MockGemmaAgent()
    private val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
    private val history = DemoSeedData.initialVisitLogs(nowMillis = 1_700_000_000_000L)
    private val retriever = ProtocolRetriever.fromJson(assetCorpusJson())
    private val dangerProtocols = retriever.retrieve(
        "Meena has headache and blurred vision since yesterday. BP 150 over 95."
    )
    private val dangerProtocol = dangerProtocols.first()

    @Test
    fun normalMaternalVisitProducesStructuredVisitNote() = runBlocking {
        val result = agent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena reports good fetal movement today. BP 120 over 80. Eating well with no warning symptoms.",
            protocolChunks = listOf(dangerProtocol)
        )

        assertTrue(result.structuredNote.contains("Observation:"))
        assertTrue(result.structuredNote.contains("Relevant history:"))
        assertTrue(result.structuredNote.contains("Protocol-grounded support:"))
        assertTrue(result.structuredNote.contains("Documentation support only"))
        assertFalse(result.structuredNote.contains("Assessment support"))
        assertNull(result.referralFlag)
    }

    @Test
    fun dangerSignsProduceProtocolGroundedReferralSuggestion() = runBlocking {
        val result = agent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has headache and blurred vision since yesterday. BP 150 over 95.",
            protocolChunks = dangerProtocols
        )

        val referral = result.referralFlag

        assertNotNull(referral)
        assertTrue(referral!!.reason.contains("Protocol-grounded referral suggestion"))
        assertTrue(referral.reason.contains("not a diagnosis"))
        assertTrue(referral.protocolBasis.contains("Smriti Demo Maternal Health Protocol"))
        assertTrue(referral.protocolBasis.contains("Danger Signs"))
        assertTrue(referral.dangerSigns.contains("headache"))
        assertTrue(referral.dangerSigns.contains("blurred vision"))
        assertTrue(referral.dangerSigns.contains("high blood pressure"))
    }

    @Test
    fun outputIncludesProtocolCitationLanguage() = runBlocking {
        val result = agent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena reports good fetal movement today. BP 120 over 80. Eating well with no warning symptoms.",
            protocolChunks = listOf(dangerProtocol)
        )

        assertTrue(result.protocolCitation.contains("Smriti Demo Maternal Health Protocol"))
        assertTrue(result.structuredNote.contains("Protocol citation: Smriti Demo Maternal Health Protocol"))
        assertTrue(result.suggestedFollowUp.contains("Protocol citation: Smriti Demo Maternal Health Protocol"))
    }

    @Test
    fun outputDoesNotUseDiagnosticLanguage() = runBlocking {
        val result = agent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has headache and blurred vision since yesterday. BP 150 over 95.",
            protocolChunks = dangerProtocols
        )

        val combinedOutput = listOf(
            result.structuredNote,
            result.suggestedFollowUp,
            result.referralFlag?.reason.orEmpty()
        ).joinToString(separator = "\n").lowercase()

        assertFalse(combinedOutput.contains("diagnosed with"))
    }

    @Test
    fun noProtocolMatchProducesUncertainNoCitationOutput() = runBlocking {
        val result = agent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "The household water pump is broken and the school roof needs repair.",
            protocolChunks = emptyList()
        )

        assertTrue(result.uncertain)
        assertNull(result.referralFlag)
        assertTrue(result.protocolCitation.contains("No matching protocol citation"))
        assertTrue(result.structuredNote.contains("No matching protocol citation"))
        assertTrue(result.clarificationPrompt!!.contains("No matching local protocol"))
    }

    @Test
    fun supervisorSummaryDeduplicatesRepeatedUrgentCasesForDisplay() = runBlocking {
        val first = agent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has headache and blurred vision since yesterday. BP 150 over 95.",
            protocolChunks = dangerProtocols
        ).referralFlag!!
        val latest = first.copy(
            dangerSigns = "headache, blurred vision, high blood pressure, reduced fetal movement",
            createdAtMillis = first.createdAtMillis + 1_000L
        )

        val summary = agent.generateSupervisorSummary(
            patients = DemoSeedData.patients,
            visits = history,
            referrals = listOf(first, latest)
        )

        assertEquals(1, summary.urgentCases.size)
        assertTrue(summary.urgentCases.single().contains("Meena - SAME_DAY"))
        assertTrue(summary.urgentCases.single().contains("reduced fetal movement"))
        assertFalse(summary.urgentCases.single().contains("Protocol-grounded referral suggestion"))
    }

    private fun assetCorpusJson(): String {
        val modulePath = File("src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        val rootPath = File("app/src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        return when {
            modulePath.exists() -> modulePath.readText()
            else -> rootPath.readText()
        }
    }
}
