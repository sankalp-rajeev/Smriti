package com.smriti.clinicalscribe.export

import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.rag.ProtocolChunk
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonExporterTest {
    @Test
    fun visitJsonIncludesProtocolCitationAndReferralFlag() {
        val protocol = ProtocolChunk(
            id = "mh_severe_headache",
            title = "Severe headache in pregnancy",
            source = "Smriti Demo Maternal Health Protocol",
            section = "Danger Signs 1.1",
            text = "Severe headache during pregnancy is a danger sign.",
            keywords = "headache",
            referralLevel = "SAME_DAY"
        )
        val referral = ReferralFlag(
            patientId = "patient-meena",
            urgency = "SAME_DAY",
            reason = "Protocol-grounded referral suggestion only, not a diagnosis.",
            protocolBasis = protocol.citation,
            recommendedFacility = "Nearest PHC",
            dangerSigns = "headache",
            createdAtMillis = 1_700_000_000_000L
        )
        val result = VisitReasoningResult(
            patientId = "patient-meena",
            observationText = "Severe headache",
            structuredNote = "Observation:\nSevere headache",
            referralFlag = referral,
            protocolCitation = protocol.citation,
            suggestedFollowUp = "Contact supervisor.",
            protocolChunk = protocol,
            uncertain = false,
            clarificationPrompt = null
        )

        val json = JsonExporter.visitJson(
            result = result,
            editedNote = result.structuredNote,
            editedFollowUp = result.suggestedFollowUp,
            voiceNote = null
        )

        assertTrue(json.contains("\"protocol_citation\": \"Smriti Demo Maternal Health Protocol Danger Signs 1.1\""))
        assertTrue(json.contains("\"referral_flag\""))
        assertTrue(json.contains("\"protocol_basis\": \"Smriti Demo Maternal Health Protocol Danger Signs 1.1\""))
        assertTrue(json.contains("Not a diagnosis. CHW confirmation required."))
    }
}
