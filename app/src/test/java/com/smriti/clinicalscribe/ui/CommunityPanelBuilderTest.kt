package com.smriti.clinicalscribe.ui

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.FollowUpTask
import com.smriti.clinicalscribe.data.FollowUpTaskScheduler
import com.smriti.clinicalscribe.data.FollowUpTaskSource
import com.smriti.clinicalscribe.data.FollowUpTaskStatus
import com.smriti.clinicalscribe.data.ReferralFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityPanelBuilderTest {
    private val now = 1_800_000_000_000L
    private val patients = DemoSeedData.patients
    private val visits = DemoSeedData.initialVisitLogs(now)

    @Test
    fun seededDemoBaselineCountsUseLocalRosterHistoryAndFollowUpTasks() {
        val panel = CommunityPanelBuilder.build(
            patients = patients,
            visits = visits,
            referrals = emptyList(),
            followUpTasks = seededFollowUpTasks(),
            nowMillis = now
        )

        assertEquals(6, panel.totalPatients)
        assertEquals(6, panel.pregnantPatients)
        assertEquals(3, panel.thirdTrimesterCount)
        assertEquals(1, panel.nearTermCount)
        assertEquals(0, panel.urgentReferralSavedCount)
        assertEquals(1, panel.openFollowUpCount)
        assertEquals(1, panel.overdueFollowUpCount)
        assertEquals(0, panel.dueOrUpcomingFollowUpCount)
        assertEquals(1, panel.historySignalCount)
        assertEquals(0, panel.missedRecentVisitCount)
        assertEquals(4, panel.routineCount)
        assertEquals(listOf("Bangladesh", "Ethiopia", "India", "Kenya", "Peru"), panel.countriesRepresented)
        assertEquals(listOf("English", "Hindi", "Spanish", "Swahili"), panel.noteLanguagesRepresented)
    }

    @Test
    fun followUpCountsComeFromActiveFollowUpTasks() {
        val panel = CommunityPanelBuilder.build(
            patients = patients,
            visits = visits,
            referrals = emptyList(),
            followUpTasks = listOf(
                followUpTask("overdue", "patient-grace", now - DAY),
                followUpTask("due", "patient-meena", now),
                followUpTask("upcoming", "patient-lucia", now + DAY),
                followUpTask("done", "patient-amara", now, FollowUpTaskStatus.COMPLETED)
            ),
            nowMillis = now
        )

        assertEquals(3, panel.openFollowUpCount)
        assertEquals(1, panel.overdueFollowUpCount)
        assertEquals(2, panel.dueOrUpcomingFollowUpCount)
        assertEquals(listOf("Follow-up overdue", "Follow-up due", "Follow-up upcoming"), panel.followUpPatients.map { it.label })
    }

    @Test
    fun urgentReferralSavedCountStaysSeparateFromFollowUpCount() {
        val panel = CommunityPanelBuilder.build(
            patients = patients,
            visits = visits,
            referrals = listOf(referral("patient-meena")),
            followUpTasks = listOf(followUpTask("amara-overdue", "patient-amara", now - DAY)),
            nowMillis = now
        )

        assertEquals(1, panel.urgentReferralSavedCount)
        assertEquals(1, panel.openFollowUpCount)
        assertEquals("Meena Sharma", panel.urgentPatients.single().patientName)
        assertEquals("Amara Tesfaye", panel.followUpPatients.single().patientName)
    }

    @Test
    fun communityPriorityOrdersUrgentThenFollowUpsThenHistorySignalThenRoutine() {
        val panel = CommunityPanelBuilder.build(
            patients = patients,
            visits = visits,
            referrals = listOf(referral("patient-lucia")),
            followUpTasks = listOf(
                followUpTask("grace-overdue", "patient-grace", now - DAY),
                followUpTask("meena-due", "patient-meena", now)
            ),
            nowMillis = now
        )

        assertEquals(
            listOf("Lucia Fernandez", "Grace Achieng", "Meena Sharma", "Fatima Begum"),
            panel.todayFocus.take(4).map { it.patientName }
        )
        assertEquals(
            listOf("Urgent review saved", "Follow-up overdue", "Follow-up due", "History signal"),
            panel.todayFocus.take(4).map { it.label }
        )
    }

    @Test
    fun noRecentVisitUsesLocalVisitDatesOnly() {
        val oldVisitTime = now - (90L * DAY)
        val oldVisits = visits.map { visit ->
            if (visit.patientId == "patient-priya") visit.copy(visitDateMillis = oldVisitTime) else visit
        }

        val panel = CommunityPanelBuilder.build(
            patients = patients,
            visits = oldVisits,
            referrals = emptyList(),
            followUpTasks = emptyList(),
            nowMillis = now
        )

        assertEquals(1, panel.missedRecentVisitCount)
        assertTrue(panel.todayFocus.any { it.patientName == "Priya Devi" && it.label == "No recent visit" })
    }

    @Test
    fun buildingPanelDoesNotCreateVisitsFollowUpsOrReferrals() {
        val mutableVisits = visits.toMutableList()
        val mutableReferrals = mutableListOf(referral("patient-meena"))
        val mutableTasks = mutableListOf(followUpTask("task", "patient-amara", now - DAY))

        CommunityPanelBuilder.build(
            patients = patients,
            visits = mutableVisits,
            referrals = mutableReferrals,
            followUpTasks = mutableTasks,
            nowMillis = now
        )

        assertEquals(visits.size, mutableVisits.size)
        assertEquals(1, mutableReferrals.size)
        assertEquals(1, mutableTasks.size)
    }

    @Test
    fun resetDemoBaselineRestoresExpectedPanelState() {
        val panel = CommunityPanelBuilder.build(
            patients = DemoSeedData.patients,
            visits = DemoSeedData.initialVisitLogs(now),
            referrals = emptyList(),
            followUpTasks = seededFollowUpTasks(),
            nowMillis = now
        )

        assertEquals(6, panel.totalPatients)
        assertEquals(0, panel.urgentReferralSavedCount)
        assertEquals(1, panel.openFollowUpCount)
        assertEquals("Amara Tesfaye", panel.todayFocus.first().patientName)
        assertEquals("Follow-up overdue", panel.todayFocus.first().label)
        assertFalse(panel.narrative.contains("diagnosis", ignoreCase = true))
    }

    private fun seededFollowUpTasks(): List<FollowUpTask> {
        val patientsById = patients.associateBy { it.id }
        return visits.mapNotNull { visit ->
            FollowUpTaskScheduler.taskForSeededVisit(
                patient = patientsById[visit.patientId],
                visit = visit,
                nowMillis = now
            )
        }
    }

    private fun referral(patientId: String): ReferralFlag {
        return ReferralFlag(
            patientId = patientId,
            urgency = "SAME_DAY",
            reason = "Urgent review saved by health worker.",
            protocolBasis = "WHO ANC Recommendation B1.2",
            recommendedFacility = "Nearest health facility",
            dangerSigns = "headache, blurred vision",
            createdAtMillis = now
        )
    }

    private fun followUpTask(
        id: String,
        patientId: String,
        dueDateMillis: Long,
        status: String = FollowUpTaskStatus.OPEN
    ): FollowUpTask {
        val patient = patients.first { it.id == patientId }
        return FollowUpTask(
            id = id,
            patientId = patientId,
            patientName = patient.name,
            createdFromVisitId = null,
            dueDateMillis = dueDateMillis,
            reason = "Check again",
            language = patient.preferredLanguage,
            status = status,
            createdAtMillis = now,
            completedAtMillis = null,
            updatedAtMillis = now,
            source = FollowUpTaskSource.MANUAL
        )
    }

    private companion object {
        const val DAY = 24L * 60L * 60L * 1000L
    }
}
