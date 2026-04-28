package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.rag.ProtocolChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealGemmaOutputParserTest {
    private val parser = RealGemmaOutputParser()
    private val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
    private val protocol = ProtocolChunk(
        id = "danger-headache",
        title = "Maternal danger signs",
        source = "Smriti Demo Maternal Health Protocol",
        section = "Danger Signs",
        text = "Severe headache with blurred vision requires same-day referral support.",
        keywords = "headache, blurred vision",
        referralLevel = "SAME_DAY"
    )

    @Test
    fun validJsonParsesSuccessfully() {
        val result = parser.parseVisitReasoning(
            rawOutput = validJson(),
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertTrue("Expected valid JSON to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
        val visit = (result as RealGemmaParseResult.Success).result
        assertEquals(patient.id, visit.patientId)
        assertEquals(protocol.citation, visit.protocolCitation)
        assertNotNull(visit.referralFlag)
        assertEquals(protocol.citation, visit.referralFlag!!.protocolBasis)
    }

    @Test
    fun invalidJsonIsRejectedSafely() {
        val result = parser.parseVisitReasoning(
            rawOutput = "not json",
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "not a single compact JSON")
    }

    @Test
    fun malformedJsonIsRejectedSafely() {
        val result = parser.parseVisitReasoning(
            rawOutput = """{"patientId":""",
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "invalid JSON")
    }

    @Test
    fun harmlessExtraNumericJsonFieldsDoNotBreakParsing() {
        val withNumericMetadata = validJson().replace(
            oldValue = "\"referralFlag\":",
            newValue = "\"bpSystolic\":150,\"bpDiastolic\":95,\"referralFlag\":"
        )

        val result = parser.parseVisitReasoning(
            rawOutput = withNumericMetadata,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertTrue("Expected numeric metadata to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
    }

    @Test
    fun missingNullableClarificationPromptParsesAsNull() {
        val withoutClarificationPrompt = validJson()
            .replace("""  "clarificationPrompt":null,""", "")

        val result = parser.parseVisitReasoning(
            rawOutput = withoutClarificationPrompt,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertTrue("Expected missing nullable clarificationPrompt to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
        assertNull((result as RealGemmaParseResult.Success).result.clarificationPrompt)
    }

    @Test
    fun diagnosticLanguageIsRejected() {
        val unsafe = validJson(
            structuredNote = "This is a diagnosis: patient has preeclampsia.",
            referralReason = "Protocol-grounded support requested."
        )

        val result = parser.parseVisitReasoning(
            rawOutput = unsafe,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "diagnostic language")
    }

    @Test
    fun recommendationWithoutCitationIsRejected() {
        val noCitation = validJson(protocolCitation = "No matching protocol citation")

        val result = parser.parseVisitReasoning(
            rawOutput = noCitation,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "not valid model output")
    }

    @Test
    fun semicolonJoinedMultipleCitationsAreRejected() {
        val joined = validJson(
            protocolCitation = "${protocol.citation}; WHO ANC Contact schedule",
            referralFlag = "null"
        )

        val result = parser.parseVisitReasoning(
            rawOutput = joined,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "exactly match one supplied protocol citation")
    }

    @Test
    fun noMatchingProtocolCitationIsRejectedEvenWithoutProtocolChunks() {
        val noMatching = validJson(
            protocolCitation = "No matching protocol citation",
            suggestedFollowUp = "CHW should collect missing vitals and review manually.",
            referralFlag = "null",
            uncertain = true
        )

        val result = parser.parseVisitReasoning(
            rawOutput = noMatching,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = emptyList()
        )

        assertRejected(result, "not valid model output")
    }

    @Test
    fun noProtocolCaseCannotInventCitation() {
        val invented = validJson(protocolCitation = "Invented Protocol Section 1")

        val result = parser.parseVisitReasoning(
            rawOutput = invented,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = emptyList()
        )

        assertRejected(result, "invented a protocol citation")
    }

    @Test
    fun noProtocolNoCitationUncertainOutputParses() {
        val noProtocol = validJson(
            protocolCitation = "",
            suggestedFollowUp = "CHW should review manually because no matching local protocol was retrieved.",
            referralFlag = "null",
            uncertain = true
        )

        val result = parser.parseVisitReasoning(
            rawOutput = noProtocol,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = emptyList()
        )

        assertTrue("Expected uncertain no-protocol JSON to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
        val visit = (result as RealGemmaParseResult.Success).result
        assertNull(visit.referralFlag)
        assertTrue(visit.uncertain)
        assertEquals("", visit.protocolCitation)
        assertNull(visit.protocolChunk)
    }

    @Test
    fun referralWithoutValidCitationIsRejected() {
        val invalidReferral = validJson(
            referralFlag = """
                {
                  "urgency":"SAME_DAY",
                  "reason":"Protocol-grounded referral suggestion; not a diagnosis.",
                  "protocolBasis":"Invented Protocol Section 1",
                  "recommendedFacility":"Primary health centre",
                  "dangerSigns":["headache","blurred vision"]
                }
            """.trimIndent()
        )

        val result = parser.parseVisitReasoning(
            rawOutput = invalidReferral,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "not grounded in a supplied protocol citation")
    }

    private fun assertRejected(result: RealGemmaParseResult, reasonFragment: String) {
        assertTrue("Expected rejection containing '$reasonFragment', got: ${result.describe()}", result is RealGemmaParseResult.Rejected)
        val rejected = result as RealGemmaParseResult.Rejected
        assertTrue(
            "Expected rejection reason to contain '$reasonFragment', got '${rejected.reason}'",
            rejected.reason.contains(reasonFragment)
        )
        assertTrue("Rejected output should return an uncertain fallback", rejected.fallback.uncertain)
        assertTrue("Fallback should preserve not-a-diagnosis language", rejected.fallback.structuredNote.contains("not a diagnosis"))
        assertTrue("Fallback should require CHW confirmation", rejected.fallback.structuredNote.contains("CHW confirmation required"))
    }

    private fun RealGemmaParseResult.describe(): String {
        return when (this) {
            is RealGemmaParseResult.Success -> "Success(${result.protocolCitation})"
            is RealGemmaParseResult.Rejected -> "Rejected($reason)"
        }
    }

    private fun validJson(
        protocolCitation: String = protocol.citation,
        structuredNote: String = "Observation support only. This is not a diagnosis. CHW confirmation required.",
        suggestedFollowUp: String = "Same-day referral support. Protocol citation: ${protocol.citation}",
        referralReason: String = "Protocol-grounded referral suggestion; not a diagnosis.",
        referralFlag: String = """
            {
              "urgency":"SAME_DAY",
              "reason":"$referralReason",
              "protocolBasis":"$protocolCitation",
              "recommendedFacility":"Primary health centre",
              "dangerSigns":["headache","blurred vision"]
            }
        """.trimIndent(),
        uncertain: Boolean = false
    ): String {
        return """
            {
              "patientId":"${patient.id}",
              "observationText":"Meena reports severe headache and blurred vision.",
              "structuredNote":"$structuredNote",
              "protocolCitation":"$protocolCitation",
              "suggestedFollowUp":"$suggestedFollowUp",
              "uncertain":$uncertain,
              "clarificationPrompt":null,
              "referralFlag":$referralFlag
            }
        """.trimIndent()
    }
}
