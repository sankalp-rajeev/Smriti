package com.smriti.clinicalscribe.reasoning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperNoteVisionParserTest {
    private val parser = PaperNoteVisionParser()

    @Test
    fun parsesValidFencedJsonFromProbeOutput() {
        val result = parser.parse(
            """
            ```json
            {
              "patientName": "Grace Achieng",
              "visitDate": "02 May 2026",
              "bloodPressure": "116/74",
              "symptoms": ["no headache", "no bleeding", "normal fetal movement"],
              "followUpPlan": "routine ANC follow-up",
              "confidence": "HIGH",
              "needsReview": true,
              "safetyNote": "Extracted from image. Health worker must review before saving."
            }
            ```
            """.trimIndent()
        )

        val extraction = (result as PaperNoteVisionParseResult.Success).extraction
        assertEquals("Grace Achieng", extraction.patientName)
        assertEquals("02 May 2026", extraction.visitDate)
        assertEquals("116/74", extraction.bloodPressure)
        assertEquals("routine ANC follow-up", extraction.followUpPlan)
        assertEquals(PaperNoteVisionConfidence.HIGH, extraction.confidence)
        assertTrue(extraction.needsReview)
    }

    @Test
    fun rejectsNeedsReviewFalse() {
        val result = parser.parse(validJson().replace("\"needsReview\": true", "\"needsReview\": false"))

        assertRejected(result, "needsReview must be true")
    }

    @Test
    fun rejectsMissingNeedsReview() {
        val result = parser.parse(validJson().replace(",\n  \"needsReview\": true", ""))

        assertRejected(result, "needsReview is required")
    }

    @Test
    fun rejectsDiagnosticLanguage() {
        val result = parser.parse(validJson(followUpPlan = "Patient has hypertension"))

        assertRejected(result, "diagnostic")
    }

    @Test
    fun rejectsReferralAdviceFromImageAlone() {
        val result = parser.parse(validJson(followUpPlan = "same-day referral required"))

        assertRejected(result, "referral")
    }

    @Test
    fun rejectsTreatmentRecommendationIfDetectable() {
        val result = parser.parse(validJson(followUpPlan = "start medication today"))

        assertRejected(result, "treatment")
    }

    @Test
    fun confidenceMapsToChwFacingMessage() {
        assertEquals("Looks clear — please review", PaperNoteVisionConfidence.HIGH.chwMessage)
        assertEquals(
            "Some text was hard to read — please check carefully",
            PaperNoteVisionConfidence.MEDIUM.chwMessage
        )
        assertEquals(
            "Text was unclear — please fill in missing details",
            PaperNoteVisionConfidence.LOW.chwMessage
        )
    }

    @Test
    fun lowConfidenceAllowsBlankExtractedFieldsButStillRequiresReview() {
        val result = parser.parse(
            """
            {
              "patientName": "",
              "visitDate": "",
              "bloodPressure": "",
              "symptoms": [],
              "followUpPlan": "",
              "confidence": "LOW",
              "needsReview": true,
              "safetyNote": "Extracted from image. Health worker must review before saving."
            }
            """.trimIndent()
        )

        val extraction = (result as PaperNoteVisionParseResult.Success).extraction
        assertEquals(PaperNoteVisionConfidence.LOW, extraction.confidence)
        assertTrue(extraction.needsReview)
    }

    private fun validJson(
        followUpPlan: String = "routine ANC follow-up"
    ): String {
        return """
            {
              "patientName": "Grace Achieng",
              "visitDate": "02 May 2026",
              "bloodPressure": "116/74",
              "symptoms": ["no headache", "no bleeding", "normal fetal movement"],
              "followUpPlan": "$followUpPlan",
              "confidence": "HIGH",
              "needsReview": true,
              "safetyNote": "Extracted from image. Health worker must review before saving."
            }
        """.trimIndent()
    }

    private fun assertRejected(result: PaperNoteVisionParseResult, reasonPart: String) {
        assertTrue(result is PaperNoteVisionParseResult.Rejected)
        assertTrue((result as PaperNoteVisionParseResult.Rejected).reason.contains(reasonPart, ignoreCase = true))
    }
}
