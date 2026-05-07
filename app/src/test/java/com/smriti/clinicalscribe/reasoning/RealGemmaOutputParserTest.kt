package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.rag.ProtocolChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealGemmaOutputParserTest {
    private val parser = RealGemmaOutputParser()
    private val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
    private val grace = DemoSeedData.patients.first { it.id == "patient-grace" }
    private val protocol = ProtocolChunk(
        id = "danger-headache",
        title = "Maternal danger signs",
        source = "Smriti Demo Maternal Health Protocol",
        section = "Danger Signs",
        text = "Severe headache with blurred vision requires same-day referral support.",
        keywords = "headache, blurred vision",
        referralLevel = "SAME_DAY"
    )
    private val routineProtocol = ProtocolChunk(
        id = "routine-anc",
        title = "Routine antenatal follow-up",
        source = "Smriti Demo Maternal Health Protocol",
        section = "Routine ANC",
        text = "Routine ANC follow-up documentation should note current concern, danger-sign screen, counseling, and next contact.",
        keywords = "routine, anc, follow-up",
        referralLevel = "NONE"
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
    fun validExactCurrentSchemaParsesSuccessfully() {
        val result = parser.parseVisitReasoning(
            rawOutput = validCurrentJson(),
            patient = patient,
            originalObservationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = listOf(protocol)
        )

        assertTrue("Expected current schema to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
        val visit = (result as RealGemmaParseResult.Success).result
        assertEquals(patient.id, visit.patientId)
        assertEquals(protocol.citation, visit.protocolCitation)
        assertNotNull(visit.referralFlag)
        assertEquals(protocol.citation, visit.referralFlag!!.protocolBasis)
        assertTrue(visit.structuredNote.contains("Safety note:"))
        assertTrue(visit.structuredNote.contains("Local guidance support:"))
        assertTrue(visit.structuredNote.contains("Patient history checked on this device."))
        assertFalse(visit.structuredNote.contains("Protocol-grounded"))
        assertFalse(visit.structuredNote.contains("RealGemma context"))
    }

    @Test
    fun aliasReferralRequiredParsesToReferralFlag() {
        val aliasJson = validCurrentJson()
            .replace("\"referralFlag\":true", "\"referral_required\":true")

        val result = parser.parseVisitReasoning(
            rawOutput = aliasJson,
            patient = patient,
            originalObservationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = listOf(protocol)
        )

        assertTrue("Expected referral_required alias to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
        assertNotNull((result as RealGemmaParseResult.Success).result.referralFlag)
    }

    @Test
    fun markdownFencedJsonIsExtractedAndParsed() {
        val fenced = "```json\n${validCurrentJson()}\n```"

        val result = parser.parseVisitReasoning(
            rawOutput = fenced,
            patient = patient,
            originalObservationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = listOf(protocol)
        )

        assertTrue("Expected fenced JSON to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
    }

    @Test
    fun surroundingTextJsonIsExtractedAndParsed() {
        val surrounded = "Here is the result:\n${validCurrentJson()}\nDone."

        val result = parser.parseVisitReasoning(
            rawOutput = surrounded,
            patient = patient,
            originalObservationText = "Meena reports severe headache and blurred vision.",
            protocolChunks = listOf(protocol)
        )

        assertTrue("Expected embedded JSON to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
    }

    @Test
    fun missingReferralFlagAndAliasesIsRejected() {
        val missingReferral = validCurrentJson()
            .replace("\"referralFlag\":true,", "")

        val result = parser.parseVisitReasoning(
            rawOutput = missingReferral,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "referralFlag")
    }

    @Test
    fun currentSchemaReferralWithoutCitationIsRejected() {
        val noCitation = validCurrentJson()
            .replace("\"citations\":[\"${protocol.citation}\"]", "\"citations\":[]")

        val result = parser.parseVisitReasoning(
            rawOutput = noCitation,
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "Referral output must include")
    }

    @Test
    fun referralOutputWithoutCitationStillRejectedEvenWhenDangerSignsAreClear() {
        val noCitation = validCurrentJson(
            summary = "Severe headache, blurred vision, BP 150/95, and reduced fetal movement noted.",
            referralReason = "Danger signs in pregnancy need same-day referral support."
        ).replace("\"citations\":[\"${protocol.citation}\"]", "\"citations\":[]")

        val result = parser.parseVisitReasoning(
            rawOutput = noCitation,
            patient = patient,
            originalObservationText = "तेज़ सिर दर्द, धुंधला दिख रहा है, BP 150/95, बच्चे की हलचल कम.",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "Referral output must include")
    }

    @Test
    fun currentSchemaDiagnosticLanguageIsRejected() {
        val unsafe = validCurrentJson(
            summary = "Patient has preeclampsia.",
            referralReason = "Protocol-grounded referral support requested."
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
    fun graceRoutineNoDangerSignOutputWithEmptyCitationsParsesWithoutReferral() {
        val result = parser.parseVisitReasoning(
            rawOutput = routineCurrentJson(citations = "[]"),
            patient = grace,
            originalObservationText = "Grace has routine ANC follow-up. Normal vitals and no danger signs.",
            protocolChunks = listOf(routineProtocol)
        )

        assertTrue("Expected Grace routine output to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
        val visit = (result as RealGemmaParseResult.Success).result
        assertNull(visit.referralFlag)
        assertEquals("", visit.protocolCitation)
        assertNull(visit.protocolChunk)
        assertTrue(visit.suggestedFollowUp.contains("routine", ignoreCase = true))
        assertTrue(visit.structuredNote.contains("Hii si utambuzi wa ugonjwa"))
    }

    @Test
    fun graceRoutineNoDangerSignOutputWithSuppliedCitationParsesWithoutReferral() {
        val result = parser.parseVisitReasoning(
            rawOutput = routineCurrentJson(citations = "[\"${routineProtocol.citation}\"]"),
            patient = grace,
            originalObservationText = "Grace has routine ANC follow-up. Normal vitals and no danger signs.",
            protocolChunks = listOf(routineProtocol)
        )

        assertTrue("Expected Grace routine cited output to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
        val visit = (result as RealGemmaParseResult.Success).result
        assertNull(visit.referralFlag)
        assertEquals(routineProtocol.citation, visit.protocolCitation)
    }

    @Test
    fun graceRoutineOutputWithInventedCitationIsRejected() {
        val result = parser.parseVisitReasoning(
            rawOutput = routineCurrentJson(citations = "[\"Invented Routine ANC Citation\"]"),
            patient = grace,
            originalObservationText = "Grace has routine ANC follow-up. Normal vitals and no danger signs.",
            protocolChunks = listOf(routineProtocol)
        )

        assertRejected(result, "not grounded in a supplied protocol citation")
    }

    @Test
    fun currentSchemaReferralWithSuppliedCitationParses() {
        val result = parser.parseVisitReasoning(
            rawOutput = validCurrentJson(),
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertTrue("Expected referral with supplied citation to parse, got: ${result.describe()}", result is RealGemmaParseResult.Success)
        assertNotNull((result as RealGemmaParseResult.Success).result.referralFlag)
    }

    @Test
    fun invalidJsonIsRejectedSafely() {
        val result = parser.parseVisitReasoning(
            rawOutput = "not json",
            patient = patient,
            originalObservationText = "Original observation",
            protocolChunks = listOf(protocol)
        )

        assertRejected(result, "not JSON")
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
    fun multilingualSafetyWordingDoesNotBypassDiagnosticRejection() {
        val unsafe = validJson(
            structuredNote = "Nota de apoyo. Esto no es un diagnóstico. Se requiere confirmación de la trabajadora de salud. Patient has preeclampsia.",
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
        assertTrue("Fallback should preserve not-a-diagnosis language", rejected.fallback.structuredNote.contains("does not diagnose"))
        assertTrue("Fallback should require health worker review", rejected.fallback.structuredNote.contains("Health worker must review"))
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
        suggestedFollowUp: String = "Same-day referral support. Health guidance: ${protocol.citation}",
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

    private fun validCurrentJson(
        summary: String = "Severe headache, blurred vision, BP 150/95, and reduced fetal movement noted.",
        referralReason: String = "Danger signs in pregnancy need same-day referral support.",
        safetyNote: String = "This is not a diagnosis. CHW confirmation is required before saving."
    ): String {
        return """
            {
              "summary":"$summary",
              "referralFlag":true,
              "referralReason":"$referralReason",
              "dangerSigns":["severe headache","blurred vision","reduced fetal movement"],
              "followUpPlan":["Arrange same-day referral support and document CHW confirmation."],
              "clarificationQuestion":"",
              "citations":["${protocol.citation}"],
              "confidence":"HIGH",
              "safetyNote":"$safetyNote"
            }
        """.trimIndent()
    }

    private fun routineCurrentJson(
        citations: String,
        safetyNote: String = "Hii si utambuzi wa ugonjwa. Uthibitisho wa mfanyakazi wa afya unahitajika."
    ): String {
        return """
            {
              "summary":"Routine ANC follow-up with normal vitals and no danger signs reported.",
              "referralFlag":false,
              "referralReason":"",
              "dangerSigns":[],
              "followUpPlan":["Continue routine ANC follow-up and routine monitoring."],
              "clarificationQuestion":"",
              "citations":$citations,
              "confidence":"MEDIUM",
              "safetyNote":"$safetyNote"
            }
        """.trimIndent()
    }
}
