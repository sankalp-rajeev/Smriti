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
        assertTrue(result.structuredNote.contains("On-device reasoning unavailable"))
        assertTrue(result.structuredNote.contains(PatientLanguages.Hindi.safetyWording))
        assertTrue(result.structuredNote.contains("Health guidance is required before recommendation"))
        assertTrue(result.suggestedFollowUp.contains("On-device reasoning is unavailable"))
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
        assertTrue(result.suggestedFollowUp.contains("Health guidance is required before recommendation"))
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
    fun defaultAgentModeRequiresRealGemma() {
        assertEquals(AgentMode.REAL_GEMMA_REQUIRED, AgentConfig.DEFAULT_MODE)
        assertTrue(GemmaAgentFactory.create() is RealGemmaAgent)
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

        assertTrue(fakeClient.prompt.contains("Return exact JSON only"))
        assertFalse(result.uncertain)
        assertEquals(protocol.citation, result.protocolCitation)
    }

    @Test
    fun realGemmaRequiredFactoryReusesSharedTextClientInstance() = runBlocking {
        val protocol = protocolChunks.first()
        val sharedClient = CountingTextClient(TextGenerationResult.Success(validJson(protocol.citation)))
        val status = RealGemmaRequiredMode.evaluate(
            buildTimeGateEnabled = true,
            localGateEnabled = true,
            modelStatus = foundModelStatus()
        )
        val firstAgent = RealGemmaRequiredAgentFactory.createVisitAgent(
            status = status,
            modelStatus = foundModelStatus(),
            sharedTextClient = sharedClient
        )
        val secondAgent = RealGemmaRequiredAgentFactory.createVisitAgent(
            status = status,
            modelStatus = foundModelStatus(),
            sharedTextClient = sharedClient
        )

        val first = firstAgent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision. BP 150 over 95.",
            protocolChunks = protocolChunks
        )
        val second = secondAgent.generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = "Meena has severe headache and blurred vision. BP 150 over 95.",
            protocolChunks = protocolChunks
        )

        assertFalse(first.uncertain)
        assertFalse(second.uncertain)
        assertEquals(2, sharedClient.callCount)
    }

    @Test
    fun currentSchemaWithEnglishSafetyNoteGetsHindiSafetyWordingAdded() = runBlocking {
        val protocol = protocolChunks.first()
        val fakeAgent = RealGemmaAgent(
            textClient = StaticTextClient(
                TextGenerationResult.Success(
                    currentSchemaJson(
                        protocolCitation = protocol.citation,
                        safetyNote = "This is not a diagnosis. CHW confirmation is required before saving."
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
        assertNotNull(result.referralFlag)
        assertTrue(result.structuredNote.contains("This is not a diagnosis"))
        assertTrue(result.structuredNote.contains(PatientLanguages.Hindi.safetyWording))
    }

    @Test
    fun parserFailurePreservesTranscriptForRetryWithoutReviewOutput() = runBlocking {
        val transcript = "Meena has severe headache and blurred vision."

        val result = RealGemmaAgent(
            textClient = StaticTextClient(TextGenerationResult.Success("""{"summary":"missing referral flag"}"""))
        ).generateVisitNote(
            patient = patient,
            visitHistory = history,
            observationText = transcript,
            protocolChunks = protocolChunks
        )

        assertSafeRejectedResult(result)
        assertEquals(transcript, result.observationText)
        assertTrue(RealGemmaUnavailableResult.isUnavailable(result))
        assertFalse(result.structuredNote.contains("missing referral flag"))
    }

    @Test
    fun unavailableSupervisorSummaryReturnsSafeFallbackMessage() = runBlocking {
        val summary = agent.generateSupervisorSummary(
            patients = DemoSeedData.patients,
            visits = history,
            referrals = emptyList()
        )

        assertTrue(summary.narrative.contains("RealGemma supervisor reasoning unavailable"))
        assertTrue(summary.urgentCases.isEmpty())
    }

    private fun assertSafeRejectedResult(result: VisitReasoningResult) {
        assertTrue(result.uncertain)
        assertNull(result.referralFlag)
        assertTrue(result.structuredNote.contains(PatientLanguages.Hindi.safetyWording))
        assertTrue(result.structuredNote.contains("Health guidance is required before recommendation"))
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

    private fun currentSchemaJson(
        protocolCitation: String,
        safetyNote: String = "This is not a diagnosis. CHW confirmation is required before saving."
    ): String {
        return """
            {
              "summary":"Severe headache, blurred vision, BP 150/95, and reduced fetal movement noted.",
              "referralFlag":true,
              "referralReason":"Danger signs in pregnancy need same-day referral support.",
              "dangerSigns":["severe headache","blurred vision","reduced fetal movement"],
              "followUpPlan":["Arrange same-day referral support and document CHW confirmation."],
              "clarificationQuestion":"",
              "citations":["$protocolCitation"],
              "confidence":"HIGH",
              "safetyNote":"$safetyNote"
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

    private class CountingTextClient(private val result: TextGenerationResult) : RealGemmaTextClient {
        var callCount: Int = 0
            private set

        override suspend fun generateText(prompt: String): TextGenerationResult {
            callCount += 1
            return result
        }
    }

    private fun foundModelStatus(): ModelStatus {
        val filesDir = java.nio.file.Files.createTempDirectory("smriti-reuse-found").toFile()
        val modelFile = LiteRtModelPaths.expectedModelFile(filesDir)
        modelFile.parentFile!!.mkdirs()
        modelFile.writeText("fake model placeholder for reuse test only")
        return ModelAvailability.fromFilesDir(filesDir).check()
    }
}
