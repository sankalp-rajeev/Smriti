package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.PatientMemoryInsights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupervisorPriorityQueueTest {
    @Test
    fun promptIncludesLocalVisitsReferralsMissedFollowUpsAndHistorySignals() {
        val patients = DemoSeedData.patients
        val visits = DemoSeedData.initialVisitLogs(nowMillis = SEED_TIME)
        val missed = PatientMemoryInsights.missedFollowUpAlerts("patient-amara", visits, SEED_TIME)
        val signal = PatientMemoryInsights.risingBloodPressureSignal(
            patient = patients.first { it.id == "patient-fatima" },
            visits = visits
        )

        val prompt = SupervisorPriorityPromptBuilder().buildPrompt(
            patients = patients,
            todayVisits = visits.take(2),
            referrals = emptyList(),
            missedFollowUps = missed,
            historySignals = listOfNotNull(signal)
        )

        assertTrue(prompt.contains("Today's confirmed local visits"))
        assertTrue(prompt.contains("Missed follow-up alerts"))
        assertTrue(prompt.contains("History signals"))
        assertTrue(prompt.contains("patient-amara"))
        assertTrue(prompt.contains("patient-fatima"))
        assertTrue(prompt.contains("not a diagnosis"))
        assertTrue(prompt.contains("\"items\""))
    }

    @Test
    fun parserAcceptsValidPriorityJson() {
        val json = """
            {"items":[{"patientId":"patient-amara","patientName":"Amara Tesfaye","urgency":"WITHIN_24H","reason":"Overdue follow-up outcome is unknown.","protocolBasis":"Ethiopia HEW maternal danger signs","nonDiagnosticSafety":"This is not a diagnosis; CHW confirmation required."}]}
        """.trimIndent()

        val result = SupervisorPriorityParser().parse(
            rawOutput = json,
            suppliedProtocolCitations = setOf("Ethiopia HEW maternal danger signs")
        )

        val queue = (result as SupervisorPriorityParseResult.Success).queue
        assertEquals(1, queue.items.size)
        assertEquals("patient-amara", queue.items.single().patientId)
        assertEquals("WITHIN_24H", queue.items.single().urgency)
    }

    @Test
    fun parserRejectsDiagnosticOutput() {
        val json = """
            {"items":[{"patientId":"patient-fatima","patientName":"Fatima Begum","urgency":"WITHIN_24H","reason":"Patient has hypertension.","protocolBasis":"","nonDiagnosticSafety":"This is not a diagnosis."}]}
        """.trimIndent()

        val result = SupervisorPriorityParser().parse(
            rawOutput = json,
            suppliedProtocolCitations = emptySet()
        )

        assertTrue(result is SupervisorPriorityParseResult.Rejected)
    }

    @Test
    fun parserRejectsInventedCitation() {
        val json = """
            {"items":[{"patientId":"patient-amara","patientName":"Amara Tesfaye","urgency":"WITHIN_24H","reason":"Overdue follow-up outcome is unknown.","protocolBasis":"Invented citation","nonDiagnosticSafety":"This is not a diagnosis."}]}
        """.trimIndent()

        val result = SupervisorPriorityParser().parse(
            rawOutput = json,
            suppliedProtocolCitations = setOf("Ethiopia HEW maternal danger signs")
        )

        assertTrue(result is SupervisorPriorityParseResult.Rejected)
    }

    @Test
    fun parserRejectsInvalidJson() {
        val result = SupervisorPriorityParser().parse(
            rawOutput = "not json",
            suppliedProtocolCitations = emptySet()
        )

        assertTrue(result is SupervisorPriorityParseResult.Rejected)
    }

    private companion object {
        const val SEED_TIME = 1_700_000_000_000L
    }
}
