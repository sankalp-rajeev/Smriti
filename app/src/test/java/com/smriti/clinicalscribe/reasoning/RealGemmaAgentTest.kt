package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealGemmaAgentTest {
    private val agent = RealGemmaAgent()
    private val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
    private val history = DemoSeedData.initialVisitLogs(nowMillis = 1_700_000_000_000L)
    private val protocolChunks = ProtocolRetriever.fromJson(assetCorpusJson())
        .retrieve("severe headache and blurred vision with BP 150 over 95")

    @Test
    fun initializeModelReturnsUnavailableForStub() {
        assertFalse(agent.initializeModel())
    }

    @Test
    fun unavailableVisitReasoningDoesNotCrashAndReturnsSafeResult() = runBlocking {
        val result = agent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision. BP 150 over 95.",
            protocolChunks = protocolChunks
        )

        assertTrue(result.uncertain)
        assertNull(result.referralFlag)
        assertTrue(result.structuredNote.contains("Real Gemma unavailable"))
        assertTrue(result.structuredNote.contains("not a diagnosis"))
        assertTrue(result.structuredNote.contains("CHW confirmation required"))
        assertTrue(result.suggestedFollowUp.contains("MockGemmaAgent fallback"))
    }

    @Test
    fun unavailableSupervisorSummaryReturnsSafeFallbackMessage() = runBlocking {
        val summary = agent.generateSupervisorSummary(
            patients = DemoSeedData.patients,
            visits = history,
            referrals = emptyList()
        )

        assertTrue(summary.narrative.contains("Real Gemma unavailable"))
        assertTrue(summary.narrative.contains("MockGemmaAgent fallback"))
        assertTrue(summary.urgentCases.isEmpty())
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
