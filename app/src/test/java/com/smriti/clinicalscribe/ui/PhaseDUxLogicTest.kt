package com.smriti.clinicalscribe.ui

import com.smriti.clinicalscribe.data.DemoSeedData
import com.smriti.clinicalscribe.data.FollowUpTask
import com.smriti.clinicalscribe.data.FollowUpTaskSource
import com.smriti.clinicalscribe.data.FollowUpTaskStatus
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseDUxLogicTest {
    private val now = 1_800_000_000_000L
    private val patients = DemoSeedData.patients
    private val visits = DemoSeedData.initialVisitLogs(now)

    @Test
    fun rosterSearchFiltersByNameCountryAndVillage() {
        assertEquals(listOf("Meena Sharma"), PatientRosterUiLogic.filterPatients(patients, "meena").map { it.name })
        assertEquals(listOf("Grace Achieng"), PatientRosterUiLogic.filterPatients(patients, "kenya").map { it.name })
        assertEquals(listOf("Lucia Fernandez"), PatientRosterUiLogic.filterPatients(patients, "cusco").map { it.name })
        assertTrue(PatientRosterUiLogic.filterPatients(patients, "missing").isEmpty())
    }

    @Test
    fun rosterPrioritySortsUrgentBeforeFollowUpBeforeRoutine() {
        val referrals = listOf(meenaReferral())
        val sorted = PatientRosterUiLogic.sortPatients(patients, visits, referrals, now)

        assertEquals("Meena Sharma", sorted[0].name)
        assertTrue(sorted.indexOfFirst { it.name == "Amara Tesfaye" } < sorted.indexOfFirst { it.name == "Grace Achieng" })
    }

    @Test
    fun rosterPrioritySortsOverdueAndDueFollowUpTasksBeforeHistorySignalAndRoutine() {
        val taskPatients = listOf(
            patients.first { it.id == "patient-fatima" },
            patients.first { it.id == "patient-grace" },
            patients.first { it.id == "patient-lucia" }
        )
        val tasks = listOf(
            followUpTask(patientId = "patient-grace", dueDateMillis = now - 2_000L),
            followUpTask(patientId = "patient-lucia", dueDateMillis = now)
        )

        val sorted = PatientRosterUiLogic.sortPatients(
            patients = taskPatients,
            visits = visits,
            referrals = emptyList(),
            nowMillis = now,
            followUpTasks = tasks
        )

        assertEquals("Grace Achieng", sorted[0].name)
        assertEquals("Lucia Fernandez", sorted[1].name)
        assertEquals("Fatima Begum", sorted[2].name)
    }

    @Test
    fun patientChipsMatchDemoCases() {
        val referrals = listOf(meenaReferral())

        assertLabels("Amara Tesfaye", referrals, "Follow-up due", "Near term")
        assertLabels("Fatima Begum", referrals, "History signal")
        assertLabels("Grace Achieng", referrals, "Routine")
        assertLabels("Meena Sharma", referrals, "Referral saved")
    }

    @Test
    fun patientChipsUseLocalFollowUpTaskState() {
        val grace = patients.first { it.id == "patient-grace" }
        val upcoming = followUpTask(
            patientId = grace.id,
            dueDateMillis = now + (7L * 24L * 60L * 60L * 1000L)
        )

        val labels = PatientRosterUiLogic.statusChips(
            patient = grace,
            visits = emptyList(),
            referrals = emptyList(),
            nowMillis = now,
            followUpTasks = listOf(upcoming)
        ).map { it.label }

        assertTrue(labels.contains("Follow-up upcoming"))
        assertFalse(labels.contains("Routine"))
    }

    @Test
    fun nearTermAndOverdueChipsUseLocalGestationOnly() {
        val nearTerm = patients.first().copy(id = "near", pregnancyWeeks = 36)
        val overdue = patients.first().copy(id = "overdue", pregnancyWeeks = 40)

        assertTrue(PatientRosterUiLogic.statusChips(nearTerm, emptyList(), emptyList(), now).any { it.label == "Near term" })
        assertTrue(PatientRosterUiLogic.statusChips(overdue, emptyList(), emptyList(), now).any { it.label == "Overdue" })
    }

    @Test
    fun eachPatientHasSpecificSampleTranscript() {
        val grace = patients.first { it.id == "patient-grace" }
        val meena = patients.first { it.id == "patient-meena" }
        val priya = patients.first { it.id == "patient-priya" }

        assertTrue(VisitSampleTranscripts.forPatient(meena).contains("severe headache"))
        assertTrue(VisitSampleTranscripts.forPatient(grace).contains("routine visit"))
        assertFalse(VisitSampleTranscripts.forPatient(grace).contains("severe headache"))
        assertTrue(VisitSampleTranscripts.forPatient(priya).contains("unclear"))
    }

    @Test
    fun welcomeGuideSetupAndOfflineSetupAreWiredWithoutMisleadingLanguageSelector() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()
        val welcomeScreens = appSourceFile("ui/WelcomeScreens.kt").readText()
        val roster = appSourceFile("ui/PatientListScreen.kt").readText()

        assertTrue(mainActivity.contains("SmritiScreen.Welcome"))
        assertTrue(mainActivity.contains("SmritiScreen.SetupGuidance"))
        assertTrue(mainActivity.contains("SmritiScreen.OfflineSetup"))
        assertTrue(mainActivity.contains("OfflineSetupScreen("))
        assertFalse(mainActivity.contains("selectedLanguageOverride"))
        assertFalse(mainActivity.contains("copy(preferredLanguage = code)"))
        assertTrue(welcomeScreens.contains("Set up today's patient list"))
        assertTrue(welcomeScreens.contains("Import patient register"))
        assertTrue(welcomeScreens.contains("Add patient manually"))
        assertTrue(welcomeScreens.contains("Help & setup"))
        assertTrue(welcomeScreens.contains("For the health worker who shows up."))
        assertTrue(welcomeScreens.contains("Smriti helps turn one home visit into local memory"))
        assertTrue(welcomeScreens.contains("Remembers the patient"))
        assertTrue(welcomeScreens.contains("Keeps the worker in control"))
        assertTrue(welcomeScreens.contains("Works without internet after setup"))
        assertTrue(welcomeScreens.contains("Patient register is stored on this device"))
        assertTrue(welcomeScreens.contains("Review imported patients on the roster."))
        assertTrue(mainActivity.contains("firstLaunchPrefs.edit().putBoolean(\"welcome_seen\", true).apply()"))
        assertTrue(mainActivity.contains("if (finalRecordingUi)"))
        assertTrue(roster.contains("About Smriti"))
        assertTrue(welcomeScreens.contains("One-time setup needed"))
        assertTrue(welcomeScreens.contains("Continue without model (demo mode)"))
        assertTrue(welcomeScreens.contains("Offline setup"))
        assertTrue(welcomeScreens.contains("OfflineProofCard(status = status)"))
        assertFalse(roster.contains("ModalBottomSheet"))
        assertFalse(mainActivity.contains("Language set to"))
    }

    @Test
    fun patientCardsAndVisitHeaderExplainNoteLanguageOnly() {
        val roster = appSourceFile("ui/PatientListScreen.kt").readText()
        val visit = appSourceFile("ui/VisitScreen.kt").readText()

        assertTrue(roster.contains("Language: \${PatientVisitUiText.noteLanguageName(patient)}"))
        assertTrue(visit.contains("Visit note will be prepared in \${PatientVisitUiText.noteLanguageDisplayLabel(patient)}"))
        assertFalse(roster.contains("Output language:"))
        assertFalse(visit.contains("Output language:"))
    }

    @Test
    fun visitScreenHasRequiredValidationAndFailureStates() {
        val visit = appSourceFile("ui/VisitScreen.kt").readText()

        assertTrue(visit.contains("Please speak or type today's visit observation first."))
        assertTrue(visit.contains("This observation is very short."))
        assertTrue(visit.contains("Note could not be prepared"))
        assertTrue(visit.contains("Try again"))
        assertTrue(visit.contains("Note is being prepared"))
        assertTrue(visit.contains("Please wait until Smriti finishes."))
        assertTrue(visit.contains("Record observation"))
        assertTrue(visit.contains("Preparing editable transcript on device..."))
        assertTrue(visit.contains("Transcript is editable. Audio alone never saves a visit."))
        assertTrue(visit.contains("Follow-up due"))
        assertTrue(visit.contains("Mark done"))
        assertTrue(visit.contains("Reschedule 1 week"))
    }

    @Test
    fun reviewScreenHasPhaseDCardsAndCollapsedSourceSection() {
        val review = appSourceFile("ui/ReviewScreen.kt").readText()

        assertTrue(review.contains("Referral suggested"))
        assertTrue(review.contains("No referral flag"))
        assertTrue(review.contains("More information needed"))
        assertTrue(review.contains("var showSourceDetails by remember(result) { mutableStateOf(false) }"))
        assertTrue(review.contains("Patient history from \$priorVisitCount prior visits"))
        assertTrue(review.contains("Guidance ID"))
        assertFalse(review.contains("Protocol Citation"))
        assertFalse(review.contains("Protocol-grounded"))
    }

    @Test
    fun summaryAndDestructiveDialogsUsePlainFallbackCopy() {
        val summary = appSourceFile("ui/SummaryScreen.kt").readText()
        val roster = appSourceFile("ui/PatientListScreen.kt").readText()

        assertTrue(summary.contains("On-device priority summary unavailable. Showing saved local visit flags."))
        assertTrue(summary.contains("Needs urgent review"))
        assertTrue(summary.contains("View community panel"))
        assertTrue(summary.contains("Saved visits on this device"))
        assertTrue(summary.contains("Reset all demo data?"))
        assertTrue(summary.contains("showDemoControls: Boolean = true"))
        assertTrue(summary.contains("if (showDemoControls)"))
        assertTrue(roster.contains("Add patients from supervisor file"))
        assertFalse(summary.contains("mock"))
        assertFalse(summary.contains("Mock"))
    }

    @Test
    fun communityPanelUsesChwFacingOfflineCopy() {
        val roster = appSourceFile("ui/PatientListScreen.kt").readText()
        val screen = appSourceFile("ui/CommunityPanelScreen.kt").readText()
        val model = appSourceFile("ui/CommunityPanel.kt").readText()

        assertTrue(roster.contains("Community panel"))
        assertTrue(screen.contains("Small local dashboard from saved patient records."))
        assertTrue(screen.contains("Saved on this device"))
        assertTrue(screen.contains("Follow-ups overdue"))
        assertTrue(screen.contains("Urgent review saved"))
        assertTrue(screen.contains("History signal"))
        assertTrue(screen.contains("No recent visit"))
        assertTrue(model.contains("PatientMemoryInsights.risingBloodPressureSignal"))
        listOf(
            "diagnosis",
            "treatment",
            "dosage",
            "risk score",
            "AI triage",
            "prediction",
            "validated"
        ).forEach { forbidden ->
            assertFalse("Found forbidden community panel wording: $forbidden", screen.contains(forbidden, ignoreCase = true))
            assertFalse("Found forbidden community panel model wording: $forbidden", model.contains(forbidden, ignoreCase = true))
        }
    }

    @Test
    fun urgentProtocolLookupUsesChwFacingOfflineCopy() {
        val roster = appSourceFile("ui/PatientListScreen.kt").readText()
        val visit = appSourceFile("ui/VisitScreen.kt").readText()
        val screen = appSourceFile("ui/UrgentProtocolLookupScreen.kt").readText()

        assertTrue(roster.contains("Urgent lookup"))
        assertTrue(visit.contains("Check urgent guidance"))
        assertTrue(screen.contains("Urgent protocol lookup"))
        assertTrue(screen.contains("Check danger signs against cited local guidance."))
        assertTrue(screen.contains("Health guidance used"))
        assertTrue(screen.contains("Urgent review may be needed"))
        assertTrue(screen.contains("Document the observation and contact a supervisor or health facility."))
        assertTrue(screen.contains("This is not a diagnosis"))
        assertTrue(screen.contains("No visit, referral flag, or follow-up task is saved from this lookup."))
        listOf(
            "diagnosed",
            "treatment",
            "dosage",
            "dose",
            "risk score",
            "AI triage",
            "Emergency AI",
            "life-saving recommendation"
        ).forEach { forbidden ->
            assertFalse("Found forbidden urgent lookup wording: $forbidden", screen.contains(forbidden, ignoreCase = true))
        }
    }

    @Test
    fun finalRecordingUiFlagCanHideInternalDemoControlsWithoutRemovingThem() {
        val mainActivity = appSourceFile("MainActivity.kt").readText()
        val visit = appSourceFile("ui/VisitScreen.kt").readText()
        val summary = appSourceFile("ui/SummaryScreen.kt").readText()

        assertTrue(mainActivity.contains("finalRecordingUi: Boolean = BuildConfig.FINAL_RECORDING_UI"))
        assertTrue(mainActivity.contains("recycleRealGemmaEngineAfterVisitNote: Boolean = BuildConfig.RECYCLE_REAL_GEMMA_ENGINE_AFTER_VISIT_NOTE"))
        assertTrue(mainActivity.contains("showDemoControls = !finalRecordingUi"))
        assertTrue(visit.contains("Use sample visit transcript"))
        assertTrue(visit.contains("Use sample paper note"))
        assertTrue(visit.contains("if (showDemoControls)"))
        assertTrue(summary.contains("Reset Demo Data"))
        assertTrue(summary.contains("if (showDemoControls)"))
    }

    @Test
    fun rosterKeepsLocalProofBehindSetupScreen() {
        val roster = appSourceFile("ui/PatientListScreen.kt").readText()
        val welcomeScreens = appSourceFile("ui/WelcomeScreens.kt").readText()

        assertTrue(roster.contains("Check offline setup"))
        assertFalse(roster.contains("OfflineProofCard("))
        assertTrue(roster.contains("LazyColumn("))
        assertEquals(1, Regex("LazyColumn\\(").findAll(roster).count())
        assertTrue(roster.contains("FilterChip("))
        assertTrue(roster.contains("RosterFilter.entries"))
        assertTrue(roster.contains("matchesRosterFilter"))
        assertTrue(welcomeScreens.contains("OfflineProofCard(status = status)"))
        assertTrue(welcomeScreens.contains("Smriti does not diagnose"))
        assertTrue(welcomeScreens.contains("Health worker must review"))
        assertTrue(welcomeScreens.contains("Confirm and save"))
        assertTrue(welcomeScreens.contains("Works without internet after setup"))
    }

    @Test
    fun userFacingUiAvoidsOldTechnicalWording() {
        val uiRoot = appSourceFile("ui")
        val combined = uiRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString(separator = "\n") { it.readText() }

        listOf("Protocol grounded", "Protocol-grounded", "Found, not loaded", "Mock mode", "KAPT", "KSP", "parser", "schema", "RAG", "pipeline")
            .forEach { forbidden -> assertFalse("Found forbidden UI wording: $forbidden", combined.contains(forbidden)) }
    }

    private fun assertLabels(
        patientName: String,
        referrals: List<ReferralFlag>,
        vararg expectedLabels: String
    ) {
        val patient = patients.first { it.name == patientName }
        val labels = PatientRosterUiLogic.statusChips(patient, visits, referrals, now).map { it.label }
        expectedLabels.forEach { assertTrue("$patientName missing $it in $labels", labels.contains(it)) }
    }

    private fun meenaReferral(): ReferralFlag {
        return ReferralFlag(
            patientId = "patient-meena",
            urgency = "IMMEDIATE",
            reason = "Danger signs need same-day referral support.",
            protocolBasis = "WHO ANC Recommendation B1.2",
            recommendedFacility = "Nearest health facility",
            dangerSigns = "severe headache, blurred vision, reduced fetal movement",
            createdAtMillis = now
        )
    }

    private fun followUpTask(
        patientId: String,
        dueDateMillis: Long
    ): FollowUpTask {
        return FollowUpTask(
            id = "task-$patientId-$dueDateMillis",
            patientId = patientId,
            patientName = patients.first { it.id == patientId }.name,
            createdFromVisitId = null,
            dueDateMillis = dueDateMillis,
            reason = "Check again",
            language = "en",
            status = FollowUpTaskStatus.OPEN,
            createdAtMillis = now,
            updatedAtMillis = now,
            source = FollowUpTaskSource.MANUAL
        )
    }

    private fun appSourceFile(relativePath: String): File {
        val modulePath = File("src/main/java/com/smriti/clinicalscribe/$relativePath")
        val rootPath = File("app/src/main/java/com/smriti/clinicalscribe/$relativePath")
        return when {
            modulePath.exists() -> modulePath
            else -> rootPath
        }
    }
}
