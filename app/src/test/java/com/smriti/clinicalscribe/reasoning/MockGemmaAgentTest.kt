package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockGemmaAgentTest {
    private val agent = MockGemmaAgent()
    private val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
    private val history = DemoSeedData.initialVisitLogs(nowMillis = 1_700_000_000_000L)
    private val dangerProtocol = DemoSeedData.protocolChunks.first { it.id == "anc-danger-signs" }

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
            protocolChunks = listOf(dangerProtocol)
        )

        val referral = result.referralFlag

        assertNotNull(referral)
        assertTrue(referral!!.reason.contains("Protocol-grounded referral suggestion"))
        assertTrue(referral.reason.contains("not a diagnosis"))
        assertTrue(referral.protocolBasis.contains("WHO ANC Recommendation B1.2"))
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

        assertTrue(result.protocolCitation.contains("WHO ANC Recommendation B1.2"))
        assertTrue(result.structuredNote.contains("Protocol citation: WHO ANC Recommendation B1.2"))
        assertTrue(result.suggestedFollowUp.contains("Protocol citation: WHO ANC Recommendation B1.2"))
    }

    @Test
    fun outputDoesNotUseDiagnosticLanguage() = runBlocking {
        val result = agent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has headache and blurred vision since yesterday. BP 150 over 95.",
            protocolChunks = listOf(dangerProtocol)
        )

        val combinedOutput = listOf(
            result.structuredNote,
            result.suggestedFollowUp,
            result.referralFlag?.reason.orEmpty()
        ).joinToString(separator = "\n").lowercase()

        assertFalse(combinedOutput.contains("diagnosed with"))
    }
}
