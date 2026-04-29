package com.smriti.clinicalscribe.benchmark

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.pipeline.VisitPipelineInput
import com.smriti.clinicalscribe.pipeline.VisitReasoningPipeline
import com.smriti.clinicalscribe.rag.ProtocolRetrievalContext
import com.smriti.clinicalscribe.rag.ProtocolRegion
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import com.smriti.clinicalscribe.reasoning.MockGemmaAgent
import com.smriti.clinicalscribe.transcript.SimulatedTranscriptClient

data class SyntheticBenchmarkCase(
    val id: String,
    val title: String,
    val countryCode: String?,
    val region: String,
    val patientContext: String,
    val transcript: String,
    val priorHistory: List<String> = emptyList(),
    val expectedCitationPrefix: String,
    val expectedReferralRequired: Boolean,
    val expectedUncertain: Boolean,
    val expectedRetrievalLevel: ExpectedRetrievalLevel,
    val notes: String
)

enum class ExpectedRetrievalLevel {
    EXACT_COUNTRY,
    REGION,
    GLOBAL_CORE,
    NONE
}

data class SyntheticBenchmarkResult(
    val case: SyntheticBenchmarkCase,
    val retrievedCitations: List<String>,
    val selectedRetrievalLevel: ExpectedRetrievalLevel,
    val referralRequired: Boolean,
    val uncertain: Boolean,
    val clarificationPrompt: String?,
    val pass: Boolean,
    val reason: String
)

object GlobalSyntheticBenchmarkCases {
    val cases: List<SyntheticBenchmarkCase> = listOf(
        SyntheticBenchmarkCase(
            id = "india_anc_danger_signs",
            title = "India ANC danger signs",
            countryCode = "IN",
            region = ProtocolRegion.INDIA.name,
            patientContext = "Synthetic pregnant ANC patient in an ASHA-supported setting.",
            transcript = "Meena reports severe headache, blurred vision, high BP 150 over 95, and reduced fetal movement today.",
            expectedCitationPrefix = "Smriti Demo Maternal Health Protocol",
            expectedReferralRequired = true,
            expectedUncertain = false,
            expectedRetrievalLevel = ExpectedRetrievalLevel.EXACT_COUNTRY,
            notes = "Preserves the default India-focused Meena danger-sign behavior."
        ),
        SyntheticBenchmarkCase(
            id = "india_normal_anc_followup",
            title = "India normal ANC follow-up",
            countryCode = "IN",
            region = ProtocolRegion.INDIA.name,
            patientContext = "Synthetic ASHA ANC follow-up without danger signs.",
            transcript = "ASHA ANC routine visit for nutrition counseling and iron folic tablets. Fetal activity is present and next contact is planned.",
            expectedCitationPrefix = "India ASHA ANC danger signs",
            expectedReferralRequired = false,
            expectedUncertain = false,
            expectedRetrievalLevel = ExpectedRetrievalLevel.EXACT_COUNTRY,
            notes = "Proves routine India ANC retrieval does not create a false referral."
        ),
        SyntheticBenchmarkCase(
            id = "bangladesh_maternal_danger_sign",
            title = "Bangladesh maternal danger sign",
            countryCode = "BD",
            region = ProtocolRegion.BANGLADESH.name,
            patientContext = "Synthetic Bangladesh CHW maternal visit.",
            transcript = "Bangladesh CHW records bd vaginal bleeding during pregnancy and plans urgent facility contact.",
            expectedCitationPrefix = "Bangladesh CHW maternal danger signs",
            expectedReferralRequired = true,
            expectedUncertain = false,
            expectedRetrievalLevel = ExpectedRetrievalLevel.EXACT_COUNTRY,
            notes = "Proves exact Bangladesh country retrieval and referral support."
        ),
        SyntheticBenchmarkCase(
            id = "ethiopia_maternal_danger_sign",
            title = "Ethiopia HEW maternal danger sign",
            countryCode = "ET",
            region = ProtocolRegion.ETHIOPIA.name,
            patientContext = "Synthetic Ethiopia HEW pregnancy visit.",
            transcript = "Ethiopia HEW notes hew danger signs with convulsion during pregnancy and requests urgent referral support.",
            expectedCitationPrefix = "Ethiopia HEW maternal danger signs",
            expectedReferralRequired = true,
            expectedUncertain = false,
            expectedRetrievalLevel = ExpectedRetrievalLevel.EXACT_COUNTRY,
            notes = "Proves exact Ethiopia country retrieval and danger-sign referral behavior."
        ),
        SyntheticBenchmarkCase(
            id = "africa_region_fallback",
            title = "Africa-region fallback",
            countryCode = "KE",
            region = ProtocolRegion.AFRICA_REGION.name,
            patientContext = "Synthetic CHW visit in an Africa-region country without country-specific chunks.",
            transcript = "Africa referral note: danger sign referral is needed because the pregnant patient reports bleeding.",
            expectedCitationPrefix = "Regional CHW maternal danger signs",
            expectedReferralRequired = true,
            expectedUncertain = false,
            expectedRetrievalLevel = ExpectedRetrievalLevel.REGION,
            notes = "Proves region fallback outranks global when an exact country chunk is absent."
        ),
        SyntheticBenchmarkCase(
            id = "south_america_region_fallback",
            title = "South America-region fallback",
            countryCode = "PE",
            region = ProtocolRegion.SOUTH_AMERICA_REGION.name,
            patientContext = "Synthetic South America regional ANC visit.",
            transcript = "South America high BP regional blood pressure concern with BP 150 over 95 and headache.",
            expectedCitationPrefix = "Regional CHW maternal danger signs",
            expectedReferralRequired = true,
            expectedUncertain = false,
            expectedRetrievalLevel = ExpectedRetrievalLevel.REGION,
            notes = "Proves South America regional fallback for a country without exact chunks."
        ),
        SyntheticBenchmarkCase(
            id = "global_core_fallback",
            title = "Global core fallback",
            countryCode = "NP",
            region = "SOUTH_ASIA_REGION",
            patientContext = "Synthetic country context not represented in the local pack.",
            transcript = "Mother reports maternal postpartum danger with heavy bleeding after birth.",
            expectedCitationPrefix = "WHO ANC Maternal danger signs",
            expectedReferralRequired = true,
            expectedUncertain = false,
            expectedRetrievalLevel = ExpectedRetrievalLevel.GLOBAL_CORE,
            notes = "Proves GLOBAL_CORE emergency fallback when country and region chunks are absent."
        ),
        SyntheticBenchmarkCase(
            id = "vague_incomplete_observation",
            title = "Vague incomplete observation",
            countryCode = null,
            region = ProtocolRegion.GLOBAL_CORE.name,
            patientContext = "Synthetic ANC visit with insufficient observation detail.",
            transcript = "Patient says she feels off today but no symptoms, vitals, movement, or timing were recorded.",
            expectedCitationPrefix = "No matching protocol citation",
            expectedReferralRequired = false,
            expectedUncertain = true,
            expectedRetrievalLevel = ExpectedRetrievalLevel.NONE,
            notes = "Proves incomplete observations stay uncertain and ask for clarification."
        ),
        SyntheticBenchmarkCase(
            id = "global_routine_no_danger",
            title = "No-danger-sign routine visit",
            countryCode = null,
            region = ProtocolRegion.GLOBAL_CORE.name,
            patientContext = "Synthetic routine global ANC contact.",
            transcript = "Routine ANC contact for counseling, birth plan, transport plan, and next visit scheduling.",
            expectedCitationPrefix = "WHO ANC Recommendation B1.2",
            expectedReferralRequired = false,
            expectedUncertain = false,
            expectedRetrievalLevel = ExpectedRetrievalLevel.GLOBAL_CORE,
            notes = "Proves routine global retrieval without a false referral."
        ),
        SyntheticBenchmarkCase(
            id = "return_visit_prior_history",
            title = "Return visit with prior history relevance",
            countryCode = "IN",
            region = ProtocolRegion.INDIA.name,
            patientContext = "Synthetic return ANC visit with prior borderline blood-pressure history.",
            transcript = "Return visit: ASHA blood pressure check shows high BP 150 over 95 with headache today.",
            priorHistory = listOf(
                "Prior ANC visit recorded borderline blood pressure and routine follow-up."
            ),
            expectedCitationPrefix = "Smriti Demo Maternal Health Protocol",
            expectedReferralRequired = true,
            expectedUncertain = false,
            expectedRetrievalLevel = ExpectedRetrievalLevel.EXACT_COUNTRY,
            notes = "Proves return-visit context still flows through the local reasoning pipeline."
        )
    )
}

class SyntheticBenchmarkRunner(
    private val protocolRetriever: ProtocolRetriever,
    private val pipeline: VisitReasoningPipeline = VisitReasoningPipeline(
        protocolRetriever = protocolRetriever,
        gemmaAgent = MockGemmaAgent(),
        speechToTextClient = SimulatedTranscriptClient()
    )
) {
    suspend fun run(case: SyntheticBenchmarkCase): SyntheticBenchmarkResult {
        val patient = case.syntheticPatient()
        val priorVisits = case.priorHistory.mapIndexed { index, note ->
            VisitLog(
                patientId = patient.id,
                visitDateMillis = 1_700_000_000_000L - index,
                observationText = note,
                structuredNote = note,
                protocolCitation = "Synthetic prior history",
                suggestedFollowUp = "Use prior history only as context.",
                confirmed = true
            )
        }

        val result = pipeline.process(
            VisitPipelineInput(
                patient = patient,
                priorVisits = priorVisits,
                transcriptText = case.transcript,
                protocolContext = ProtocolRetrievalContext(
                    countryCode = case.countryCode,
                    region = case.region
                )
            )
        )
        val reasoning = result.reasoningResult
        val retrievedCitations = result.protocolChunks.map { it.citation }.ifEmpty {
            listOfNotNull(reasoning?.protocolCitation?.takeIf { it.isNotBlank() })
        }
        val selectedLevel = result.protocolChunks.firstOrNull()?.let { chunk ->
            when {
                case.countryCode != null && chunk.countryCode == case.countryCode -> ExpectedRetrievalLevel.EXACT_COUNTRY
                chunk.region == case.region && chunk.region != ProtocolRegion.GLOBAL_CORE.name -> ExpectedRetrievalLevel.REGION
                chunk.region == ProtocolRegion.GLOBAL_CORE.name -> ExpectedRetrievalLevel.GLOBAL_CORE
                else -> ExpectedRetrievalLevel.NONE
            }
        } ?: ExpectedRetrievalLevel.NONE

        val failures = buildList {
            if (retrievedCitations.none { it.startsWith(case.expectedCitationPrefix) }) {
                add("expected citation prefix '${case.expectedCitationPrefix}', got $retrievedCitations")
            }
            if (selectedLevel != case.expectedRetrievalLevel) {
                add("expected retrieval ${case.expectedRetrievalLevel}, got $selectedLevel")
            }
            if ((reasoning?.referralFlag != null) != case.expectedReferralRequired) {
                add("expected referral=${case.expectedReferralRequired}, got ${reasoning?.referralFlag != null}")
            }
            if ((reasoning?.uncertain ?: true) != case.expectedUncertain) {
                add("expected uncertain=${case.expectedUncertain}, got ${reasoning?.uncertain}")
            }
            if (case.expectedUncertain && reasoning?.clarificationPrompt.isNullOrBlank()) {
                add("expected clarification prompt for uncertain case")
            }
        }

        return SyntheticBenchmarkResult(
            case = case,
            retrievedCitations = retrievedCitations,
            selectedRetrievalLevel = selectedLevel,
            referralRequired = reasoning?.referralFlag != null,
            uncertain = reasoning?.uncertain ?: true,
            clarificationPrompt = reasoning?.clarificationPrompt,
            pass = failures.isEmpty(),
            reason = failures.ifEmpty { listOf("passed") }.joinToString(separator = "; ")
        )
    }

    private fun SyntheticBenchmarkCase.syntheticPatient(): Patient {
        return Patient(
            id = "synthetic-$id",
            name = title,
            age = 27,
            sex = "F",
            pregnancyWeeks = 28,
            village = region,
            riskSummary = patientContext
        )
    }
}
