package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupervisorSummaryFormatterTest {
    private val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
    private val asha = DemoSeedData.patients.first { it.id == "patient-asha" }

    @Test
    fun urgentCasesUseLatestReferralPerPatient() {
        val older = referral(
            dangerSigns = "headache, blurred vision",
            createdAtMillis = 100L
        )
        val latest = referral(
            dangerSigns = "headache, blurred vision, high blood pressure, reduced fetal movement",
            createdAtMillis = 200L
        )

        val urgentCases = SupervisorSummaryFormatter.urgentCases(
            patients = DemoSeedData.patients,
            referrals = listOf(older, latest)
        )

        assertEquals(1, urgentCases.size)
        assertEquals(
            "Meena - SAME_DAY - headache, blurred vision, high blood pressure, reduced fetal movement. Citation: Smriti Demo Maternal Health Protocol - Danger Signs.",
            urgentCases.single()
        )
    }

    @Test
    fun urgentCaseFormatStaysConciseAndCited() {
        val urgentCase = SupervisorSummaryFormatter.urgentCases(
            patients = DemoSeedData.patients,
            referrals = listOf(referral())
        ).single()

        assertTrue(urgentCase.contains("Meena - SAME_DAY"))
        assertTrue(urgentCase.contains("Citation: Smriti Demo Maternal Health Protocol"))
        assertFalse(urgentCase.contains("Protocol-grounded referral suggestion"))
        assertFalse(urgentCase.contains("not a diagnosis"))
    }

    @Test
    fun urgentCaseExtractsConciseSignsFromVerboseSavedText() {
        val urgentCase = SupervisorSummaryFormatter.urgentCases(
            patients = DemoSeedData.patients,
            referrals = listOf(
                referral(
                    dangerSigns = "Protocol-grounded referral suggestion only, not a diagnosis: Severe headache, blurred vision, high blood pressure, bleeding, convulsions, and reduced fetal movement require urgent assessment and referral.",
                    createdAtMillis = 300L
                )
            )
        ).single()

        assertEquals(
            "Meena - SAME_DAY - headache, blurred vision, high blood pressure, reduced fetal movement, convulsions, bleeding. Citation: Smriti Demo Maternal Health Protocol - Danger Signs.",
            urgentCase
        )
        assertFalse(urgentCase.contains("Protocol-grounded referral suggestion"))
        assertFalse(urgentCase.contains("urgent assessment and referral"))
    }

    @Test
    fun urgentCaseUsesFirstProtocolCitationOnly() {
        val urgentCase = SupervisorSummaryFormatter.urgentCases(
            patients = DemoSeedData.patients,
            referrals = listOf(
                referral(
                    dangerSigns = "headache",
                    createdAtMillis = 400L,
                    protocolBasis = "Smriti Demo Maternal Health Protocol - Danger Signs; WHO ANC Contact schedule"
                )
            )
        ).single()

        assertEquals(
            "Meena - SAME_DAY - headache. Citation: Smriti Demo Maternal Health Protocol - Danger Signs.",
            urgentCase
        )
    }

    @Test
    fun mockSupervisorSummaryCountsConfirmedLocalDataAndLatestUrgentCases() = runBlocking {
        val visits = listOf(
            visit(id = 1L, followUp = "Routine ANC follow-up. Protocol citation: WHO ANC Contact schedule."),
            visit(id = 2L, followUp = "Same-day referral support. Protocol citation: Smriti Demo Maternal Health Protocol - Danger Signs."),
            visit(id = 3L, followUp = "Return visit BP review. Protocol citation: WHO ANC Contact schedule.")
        )
        val older = referral(
            dangerSigns = "headache, blurred vision",
            createdAtMillis = 100L
        )
        val latest = referral(
            dangerSigns = "headache, blurred vision, high blood pressure, reduced fetal movement",
            createdAtMillis = 300L
        )

        val summary = MockGemmaAgent().generateSupervisorSummary(
            patients = DemoSeedData.patients,
            visits = visits,
            referrals = listOf(older, latest)
        )

        assertEquals(3, summary.totalVisits)
        assertEquals(2, summary.referralsFlagged)
        assertEquals(1, summary.urgentCases.size)
        assertEquals(
            "Meena - SAME_DAY - headache, blurred vision, high blood pressure, reduced fetal movement. Citation: Smriti Demo Maternal Health Protocol - Danger Signs.",
            summary.urgentCases.single()
        )
        assertFalse(summary.urgentCases.single().contains("long protocol explanation"))
        assertEquals(3, summary.followUpsDue.size)
    }

    @Test
    fun urgentCasesKeepOnlyLatestReferralForEachPatient() {
        val meenaOlder = referral(
            patientId = patient.id,
            dangerSigns = "headache",
            createdAtMillis = 100L
        )
        val meenaLatest = referral(
            patientId = patient.id,
            dangerSigns = "headache, blurred vision",
            createdAtMillis = 300L
        )
        val ashaOlder = referral(
            patientId = asha.id,
            dangerSigns = "bleeding",
            createdAtMillis = 200L
        )
        val ashaLatest = referral(
            patientId = asha.id,
            dangerSigns = "bleeding, convulsions",
            createdAtMillis = 400L
        )

        val urgentCases = SupervisorSummaryFormatter.urgentCases(
            patients = DemoSeedData.patients,
            referrals = listOf(meenaOlder, meenaLatest, ashaOlder, ashaLatest)
        )

        assertEquals(2, urgentCases.size)
        assertEquals(
            "Asha - SAME_DAY - convulsions, bleeding. Citation: Smriti Demo Maternal Health Protocol - Danger Signs.",
            urgentCases.first()
        )
        assertEquals(
            "Meena - SAME_DAY - headache, blurred vision. Citation: Smriti Demo Maternal Health Protocol - Danger Signs.",
            urgentCases.last()
        )
        assertFalse(urgentCases.joinToString().contains("Asha - SAME_DAY - bleeding."))
        assertFalse(urgentCases.joinToString().contains("Meena - SAME_DAY - headache."))
    }

    private fun referral(
        patientId: String = patient.id,
        dangerSigns: String = "headache, blurred vision, high blood pressure",
        createdAtMillis: Long = 100L,
        protocolBasis: String = "Smriti Demo Maternal Health Protocol - Danger Signs"
    ): ReferralFlag {
        return ReferralFlag(
            patientId = patientId,
            urgency = "SAME_DAY",
            reason = "Protocol-grounded referral suggestion only, not a diagnosis: long protocol explanation.",
            protocolBasis = protocolBasis,
            recommendedFacility = "Nearest PHC",
            dangerSigns = dangerSigns,
            createdAtMillis = createdAtMillis
        )
    }

    private fun visit(
        id: Long,
        followUp: String
    ): VisitLog {
        return VisitLog(
            id = id,
            patientId = patient.id,
            visitDateMillis = 1_700_000_000_000L + id,
            observationText = "Confirmed local visit $id",
            structuredNote = "Confirmed local note $id. This is not a diagnosis. CHW confirmation required.",
            protocolCitation = "WHO ANC Contact schedule",
            suggestedFollowUp = followUp,
            confirmed = true
        )
    }
}
