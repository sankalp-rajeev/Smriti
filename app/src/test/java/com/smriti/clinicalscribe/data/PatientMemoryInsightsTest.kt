package com.smriti.clinicalscribe.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientMemoryInsightsTest {
    @Test
    fun amaraHasMissedFollowUpAlert() {
        val visits = DemoSeedData.initialVisitLogs(nowMillis = SEED_TIME)

        val alerts = PatientMemoryInsights.missedFollowUpAlerts(
            patientId = "patient-amara",
            visits = visits,
            nowMillis = SEED_TIME
        )

        assertEquals(1, alerts.size)
        assertEquals("patient-amara", alerts.single().patientId)
        assertEquals(7, alerts.single().daysOverdue)
        assertEquals(
            "Missed follow-up: Follow-up was due after the previous ANC visit. Outcome unknown. Confirm before today's visit.",
            alerts.single().message
        )
    }

    @Test
    fun noteAsOngoingDoesNotMarkCompletedInLocalLogic() {
        val visits = DemoSeedData.initialVisitLogs(nowMillis = SEED_TIME)
        val before = PatientMemoryInsights.missedFollowUpAlerts("patient-amara", visits, SEED_TIME)

        val after = PatientMemoryInsights.missedFollowUpAlerts("patient-amara", visits, SEED_TIME)

        assertEquals(before, after)
        assertEquals(false, visits.first { it.id == before.single().visitId }.followUpCompleted)
    }

    @Test
    fun amaraLegacyFreeTextFollowUpStillShowsAlertWhenStructuredFieldsAreMissing() {
        val visits = DemoSeedData.initialVisitLogs(nowMillis = SEED_TIME).map { visit ->
            if (visit.id == 3_001L) {
                visit.copy(followUpDueDateMillis = null, followUpCompleted = null)
            } else {
                visit
            }
        }

        val alerts = PatientMemoryInsights.missedFollowUpAlerts(
            patientId = "patient-amara",
            visits = visits,
            nowMillis = SEED_TIME
        )

        assertEquals(1, alerts.size)
        assertEquals(3_001L, alerts.single().visitId)
        assertEquals(
            "Missed follow-up: Follow-up was due after the previous ANC visit. Outcome unknown. Confirm before today's visit.",
            alerts.single().message
        )
    }

    @Test
    fun confirmedLegacyFreeTextFollowUpDoesNotShowAlert() {
        val visits = DemoSeedData.initialVisitLogs(nowMillis = SEED_TIME).map { visit ->
            if (visit.id == 3_001L) {
                visit.copy(followUpDueDateMillis = null, followUpCompleted = true)
            } else {
                visit
            }
        }

        val alerts = PatientMemoryInsights.missedFollowUpAlerts(
            patientId = "patient-amara",
            visits = visits,
            nowMillis = SEED_TIME
        )

        assertEquals(emptyList<MissedFollowUpAlert>(), alerts)
    }

    @Test
    fun fatimaShowsRisingBpHistorySignal() {
        val patient = DemoSeedData.patients.first { it.id == "patient-fatima" }
        val visits = DemoSeedData.initialVisitLogs(nowMillis = SEED_TIME)

        val signal = PatientMemoryInsights.risingBloodPressureSignal(patient, visits)

        assertNotNull(signal)
        assertEquals(
            listOf("118/76", "125/80", "132/84", "138/88"),
            signal!!.readings.map { it.label }
        )
        assertTrue(signal.message.contains("Review and monitor"))
        assertTrue(!signal.message.contains("preeclampsia", ignoreCase = true))
    }

    @Test
    fun graceDoesNotShowFalseHistorySignal() {
        val patient = DemoSeedData.patients.first { it.id == "patient-grace" }
        val visits = DemoSeedData.initialVisitLogs(nowMillis = SEED_TIME)

        assertNull(PatientMemoryInsights.risingBloodPressureSignal(patient, visits))
    }

    @Test
    fun bpParserReadsCompactBloodPressurePatterns() {
        val visit = VisitLog(
            patientId = "patient-test",
            visitDateMillis = SEED_TIME,
            observationText = "BP 132/84 today.",
            structuredNote = "Routine note.",
            protocolCitation = "Protocol",
            suggestedFollowUp = "Follow up.",
            confirmed = true
        )

        val reading = PatientMemoryInsights.parseBloodPressure(visit)

        assertEquals("132/84", reading!!.label)
    }

    private companion object {
        const val SEED_TIME = 1_700_000_000_000L
    }
}
