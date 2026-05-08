package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.FollowUpTask
import com.smriti.clinicalscribe.data.FollowUpTaskSource
import com.smriti.clinicalscribe.data.FollowUpTaskStatus
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.TranscriptSource
import com.smriti.clinicalscribe.data.VisitLog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupervisorSummaryFormatterTest {
    private val patient = DemoSeedData.patients.first { it.id == "patient-meena" }
    private val grace = DemoSeedData.patients.first { it.id == "patient-grace" }

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
            "Meena Sharma - SAME_DAY - headache, blurred vision, high blood pressure, reduced fetal movement. Health guidance: Smriti Demo Maternal Health Protocol - Danger Signs.",
            urgentCases.single()
        )
    }

    @Test
    fun urgentCaseFormatStaysConciseAndCited() {
        val urgentCase = SupervisorSummaryFormatter.urgentCases(
            patients = DemoSeedData.patients,
            referrals = listOf(referral())
        ).single()

        assertTrue(urgentCase.contains("Meena Sharma - SAME_DAY"))
        assertTrue(urgentCase.contains("Health guidance: Smriti Demo Maternal Health Protocol"))
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
            "Meena Sharma - SAME_DAY - headache, blurred vision, high blood pressure, reduced fetal movement, convulsions, bleeding. Health guidance: Smriti Demo Maternal Health Protocol - Danger Signs.",
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
            "Meena Sharma - SAME_DAY - headache. Health guidance: Smriti Demo Maternal Health Protocol - Danger Signs.",
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
            createdAtMillis = 1_700_000_000_100L
        )
        val latest = referral(
            dangerSigns = "headache, blurred vision, high blood pressure, reduced fetal movement",
            createdAtMillis = 1_700_000_000_300L
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
            "Meena Sharma - SAME_DAY - headache, blurred vision, high blood pressure, reduced fetal movement. Health guidance: Smriti Demo Maternal Health Protocol - Danger Signs.",
            summary.urgentCases.single()
        )
        assertFalse(summary.urgentCases.single().contains("long protocol explanation"))
        assertEquals(2, summary.followUpsDue.size)
        assertTrue(summary.followUpsDue.none { it.contains("Same-day referral support") })
        assertTrue(summary.paperScanNeedsUrgentReview.isEmpty())
    }

    @Test
    fun localSavedSummaryExcludesSeededPriorHistoryFromTodayCounts() {
        val seededHistory = DemoSeedData.initialVisitLogs(1_800_000_000_000L)
        val savedVisit = visit(id = 700L, followUp = "Return tomorrow.")

        val summary = SupervisorSummaryFormatter.buildLocalSavedSummary(
            patients = DemoSeedData.patients,
            visits = seededHistory + savedVisit,
            referrals = emptyList(),
            nowMillis = savedVisit.visitDateMillis
        )

        assertEquals(1, summary.totalVisits)
        assertTrue(summary.followUpsDue.single().contains("Meena Sharma"))
    }

    @Test
    fun localSavedSummaryIncludesOpenOverdueAndUpcomingFollowUpTaskCounts() {
        val now = 1_800_000_000_000L
        val tasks = listOf(
            followUpTask(id = "overdue", dueDateMillis = now - 86_400_000L),
            followUpTask(id = "due", dueDateMillis = now),
            followUpTask(id = "upcoming", dueDateMillis = now + 7L * 86_400_000L),
            followUpTask(id = "done", dueDateMillis = now, status = FollowUpTaskStatus.COMPLETED)
        )

        val summary = SupervisorSummaryFormatter.buildLocalSavedSummary(
            patients = DemoSeedData.patients,
            visits = emptyList(),
            referrals = emptyList(),
            followUpTasks = tasks,
            nowMillis = now
        )

        assertEquals(0, summary.totalVisits)
        assertEquals(3, summary.openFollowUps)
        assertEquals(1, summary.overdueFollowUps)
        assertEquals(1, summary.dueTodayFollowUps)
        assertEquals(1, summary.upcomingFollowUps)
        assertEquals(3, summary.followUpsDue.size)
        assertTrue(summary.followUpsDue.first().contains("overdue"))
    }

    @Test
    fun highRiskConfirmedPaperScanSurfacesNeedsUrgentReviewLineAndSkipsFollowUpCard() {
        val riskyPaper = VisitLog(
            id = 900L,
            patientId = grace.id,
            visitDateMillis = 1_800_000_555_000L,
            observationText = """
                Paper note patient: Grace Achieng
                BP: 190/110
                Symptoms: maumivu ya kichwa, kuona ukungu
                Plan: return to clinic
            """.trimIndent(),
            structuredNote = """
                Blood pressure recording: 190/110 was written on scanned paper note.
                Symptoms written: maumivu ya kichwa; kuona ukungu (visual symptoms documented).
                Paper note extraction only; no referral generated from scan.
            """.trimIndent(),
            protocolCitation = "Paper note extraction only; no referral or diagnosis generated from image.",
            suggestedFollowUp = "routine ANC follow-up",
            confirmed = true,
            transcriptSource = TranscriptSource.PAPER_SCAN
        )
        val summary = SupervisorSummaryFormatter.buildLocalSavedSummary(
            patients = DemoSeedData.patients,
            visits = listOf(riskyPaper),
            referrals = emptyList(),
            narrative = "Test narrative only."
        )
        assertEquals(1, summary.paperScanNeedsUrgentReview.size)
        val urgentLine = summary.paperScanNeedsUrgentReview.single()
        assertTrue(urgentLine.contains("Grace Achieng"))
        assertTrue(urgentLine.contains("BP 190/110"))
        assertTrue(urgentLine.contains("headache and blurred vision"))
        assertTrue(urgentLine.contains("Review scanned note and local guidance."))
        assertTrue(urgentLine.contains("Not a diagnosis"))
        assertTrue(summary.followUpsDue.none { line -> line.contains("Grace Achieng") })
    }

    @Test
    fun referralLikeFollowUpWithoutReferralFlagIsNotShownAsRoutineFollowUp() {
        val inconsistentVisit = visit(
            id = 901L,
            followUp = "तत्काल रेफरल समर्थन की व्यवस्था करें."
        )

        val summary = SupervisorSummaryFormatter.buildLocalSavedSummary(
            patients = DemoSeedData.patients,
            visits = listOf(inconsistentVisit),
            referrals = emptyList(),
            nowMillis = inconsistentVisit.visitDateMillis
        )

        assertEquals(1, summary.totalVisits)
        assertEquals(0, summary.referralsFlagged)
        assertTrue(summary.followUpsDue.none { it.contains("रेफरल") })
        assertTrue(summary.urgentCases.isEmpty())
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
        val graceOlder = referral(
            patientId = grace.id,
            dangerSigns = "bleeding",
            createdAtMillis = 200L
        )
        val graceLatest = referral(
            patientId = grace.id,
            dangerSigns = "bleeding, convulsions",
            createdAtMillis = 400L
        )

        val urgentCases = SupervisorSummaryFormatter.urgentCases(
            patients = DemoSeedData.patients,
            referrals = listOf(meenaOlder, meenaLatest, graceOlder, graceLatest)
        )

        assertEquals(2, urgentCases.size)
        assertEquals(
            "Grace Achieng - SAME_DAY - convulsions, bleeding. Health guidance: Smriti Demo Maternal Health Protocol - Danger Signs.",
            urgentCases.first()
        )
        assertEquals(
            "Meena Sharma - SAME_DAY - headache, blurred vision. Health guidance: Smriti Demo Maternal Health Protocol - Danger Signs.",
            urgentCases.last()
        )
        assertFalse(urgentCases.joinToString().contains("Grace Achieng - SAME_DAY - bleeding."))
        assertFalse(urgentCases.joinToString().contains("Meena Sharma - SAME_DAY - headache."))
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

    private fun followUpTask(
        id: String,
        dueDateMillis: Long,
        status: String = FollowUpTaskStatus.OPEN
    ): FollowUpTask {
        return FollowUpTask(
            id = id,
            patientId = patient.id,
            patientName = patient.name,
            createdFromVisitId = null,
            dueDateMillis = dueDateMillis,
            reason = "Check again",
            language = "en",
            status = status,
            createdAtMillis = dueDateMillis,
            completedAtMillis = null,
            updatedAtMillis = dueDateMillis,
            source = FollowUpTaskSource.MANUAL
        )
    }
}
