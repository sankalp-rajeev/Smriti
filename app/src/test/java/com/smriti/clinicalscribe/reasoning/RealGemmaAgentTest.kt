package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.PatientLanguages
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        assertTrue(result.structuredNote.contains("Experimental Real Gemma path unavailable"))
        assertTrue(result.structuredNote.contains(PatientLanguages.Hindi.safetyWording))
        assertTrue(result.structuredNote.contains("Protocol citation required before recommendation"))
        assertTrue(result.suggestedFollowUp.contains("MockGemmaAgent fallback"))
    }

    @Test
    fun realGemmaAgentUsesPromptBuilderAndTextClientPath() = runBlocking {
        val client = CapturingTextClient(
            TextGenerationResult.Unavailable("Experimental Real Gemma path unavailable for test.")
        )
        val promptedAgent = RealGemmaAgent(textClient = client)

        promptedAgent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision. BP 150 over 95.",
            protocolChunks = protocolChunks
        )

        assertTrue(client.prompt.contains("Required JSON shape"))
        assertTrue(client.prompt.contains(patient.id))
        assertTrue(client.prompt.contains("Meena has severe headache"))
        assertTrue(client.prompt.contains(protocolChunks.first().citation))
    }

    @Test
    fun validFakeJsonFromTextClientParsesIntoReasoningResult() = runBlocking {
        val protocol = protocolChunks.first()
        val fakeAgent = RealGemmaAgent(
            textClient = StaticTextClient(TextGenerationResult.Success(validJson(protocol.citation)))
        )

        val result = fakeAgent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision. BP 150 over 95.",
            protocolChunks = protocolChunks
        )

        assertFalse(result.uncertain)
        assertEquals(patient.id, result.patientId)
        assertEquals(protocol.citation, result.protocolCitation)
        assertNotNull(result.referralFlag)
        assertEquals(protocol.citation, result.referralFlag!!.protocolBasis)
    }

    @Test
    fun parsedValidJsonWithoutSafetyWordingGetsSafetyWordingAdded() = runBlocking {
        val protocol = protocolChunks.first()
        val fakeAgent = RealGemmaAgent(
            textClient = StaticTextClient(
                TextGenerationResult.Success(
                    validJson(
                        protocolCitation = protocol.citation,
                        structuredNote = "Patient reports severe headache and blurred vision with BP 150/95."
                    )
                )
            )
        )

        val result = fakeAgent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision. BP 150 over 95.",
            protocolChunks = protocolChunks
        )

        assertFalse(result.uncertain)
        assertTrue(result.structuredNote.contains(PatientLanguages.Hindi.safetyWording))
    }

    @Test
    fun existingSafetyWordingIsNotDuplicated() = runBlocking {
        val protocol = protocolChunks.first()
        val safeNote = "Protocol-grounded support only. ${PatientLanguages.Hindi.safetyWording}"
        val fakeAgent = RealGemmaAgent(
            textClient = StaticTextClient(
                TextGenerationResult.Success(
                    validJson(
                        protocolCitation = protocol.citation,
                        structuredNote = safeNote
                    )
                )
            )
        )

        val result = fakeAgent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision. BP 150 over 95.",
            protocolChunks = protocolChunks
        )

        assertEquals(1, result.structuredNote.countOccurrences(PatientLanguages.Hindi.safetyWording))
    }

    @Test
    fun invalidFakeJsonReturnsSafeUncertainResult() = runBlocking {
        val result = RealGemmaAgent(
            textClient = StaticTextClient(TextGenerationResult.Success("not json"))
        ).generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision.",
            protocolChunks = protocolChunks
        )

        assertSafeRejectedResult(result)
        assertTrue(result.structuredNote.contains("output rejected"))
    }

    @Test
    fun diagnosticFakeJsonReturnsSafeUncertainResult() = runBlocking {
        val result = RealGemmaAgent(
            textClient = StaticTextClient(
                TextGenerationResult.Success(
                    validJson(
                        protocolCitation = protocolChunks.first().citation,
                        structuredNote = "Patient has preeclampsia."
                    )
                )
            )
        ).generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision.",
            protocolChunks = protocolChunks
        )

        assertSafeRejectedResult(result)
        assertFalse(result.structuredNote.lowercase().contains("patient has preeclampsia"))
    }

    @Test
    fun missingCitationFakeJsonReturnsSafeUncertainResult() = runBlocking {
        val result = RealGemmaAgent(
            textClient = StaticTextClient(
                TextGenerationResult.Success(validJson(protocolCitation = "No matching protocol citation"))
            )
        ).generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision.",
            protocolChunks = protocolChunks
        )

        assertSafeRejectedResult(result)
        assertTrue(result.suggestedFollowUp.contains("Protocol citation required before recommendation"))
    }

    @Test
    fun mockGemmaAgentRemainsUnaffectedByExperimentalScaffold() = runBlocking {
        val result = MockGemmaAgent().generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has headache and blurred vision since yesterday. BP 150 over 95.",
            protocolChunks = protocolChunks
        )

        assertFalse(result.uncertain)
        assertNotNull(result.referralFlag)
        assertTrue(result.structuredNote.contains("Documentation support only"))
    }

    @Test
    fun defaultAgentModeRemainsMock() {
        assertEquals(AgentMode.MOCK, AgentConfig.DEFAULT_MODE)
        assertTrue(GemmaAgentFactory.create() is MockGemmaAgent)
    }

    @Test
    fun realGemmaAgentStillUsesInjectedFakeClientCorrectly() = runBlocking {
        val protocol = protocolChunks.first()
        val fakeClient = CapturingTextClient(
            TextGenerationResult.Success(validJson(protocol.citation))
        )
        val fakeAgent = RealGemmaAgent(textClient = fakeClient)

        val result = fakeAgent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision. BP 150 over 95.",
            protocolChunks = protocolChunks
        )

        assertTrue(fakeClient.prompt.contains("Return compact JSON only"))
        assertFalse(result.uncertain)
        assertEquals(protocol.citation, result.protocolCitation)
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

    private fun assertSafeRejectedResult(result: VisitReasoningResult) {
        assertTrue(result.uncertain)
        assertNull(result.referralFlag)
        assertTrue(result.structuredNote.contains(PatientLanguages.Hindi.safetyWording))
        assertTrue(result.structuredNote.contains("Protocol citation required before recommendation"))
    }

    private fun validJson(
        protocolCitation: String,
        structuredNote: String = "Protocol-grounded support only. No diagnosis generated. CHW confirmation required."
    ): String {
        return """
            {
              "patientId":"${patient.id}",
              "observationText":"Meena reports severe headache and blurred vision.",
              "structuredNote":"$structuredNote",
              "protocolCitation":"$protocolCitation",
              "suggestedFollowUp":"Same-day referral support. Protocol citation: $protocolCitation",
              "uncertain":false,
              "clarificationPrompt":null,
              "referralFlag":{
                "urgency":"SAME_DAY",
                "reason":"Protocol-grounded referral suggestion. No diagnosis generated.",
                "protocolBasis":"$protocolCitation",
                "recommendedFacility":"Primary health centre",
                "dangerSigns":["headache","blurred vision"]
              }
            }
        """.trimIndent()
    }

    private fun String.countOccurrences(needle: String): Int {
        return split(needle).size - 1
    }

    private fun assetCorpusJson(): String {
        val modulePath = File("src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        val rootPath = File("app/src/main/assets/${ProtocolRetriever.ASSET_PATH}")
        return when {
            modulePath.exists() -> modulePath.readText()
            else -> rootPath.readText()
        }
    }

    private class StaticTextClient(private val result: TextGenerationResult) : RealGemmaTextClient {
        override suspend fun generateText(prompt: String): TextGenerationResult = result
    }

    private class CapturingTextClient(private val result: TextGenerationResult) : RealGemmaTextClient {
        var prompt: String = ""
            private set

        override suspend fun generateText(prompt: String): TextGenerationResult {
            this.prompt = prompt
            return result
        }
    }
}
