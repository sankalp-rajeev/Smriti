package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientLanguages
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk

class RealGemmaAgent(
    private val textClient: RealGemmaTextClient = UnavailableGemmaTextClient(),
    private val promptBuilder: RealGemmaPromptBuilder = RealGemmaPromptBuilder(),
    private val outputParser: RealGemmaOutputParser = RealGemmaOutputParser(),
    private val safetyPostProcessor: RealGemmaSafetyPostProcessor = RealGemmaSafetyPostProcessor()
) : GemmaAgent {
    fun initializeModel(): Boolean {
        // TODO LiteRT-LM integration: initialize only in a future client implementation, never in this scaffold.
        return false
    }

    suspend fun generateVisitReasoning(
        patient: Patient,
        visitHistory: List<VisitLog>,
        observationText: String,
        protocolChunks: List<ProtocolChunk>
    ): VisitReasoningResult {
        val prompt = try {
            promptBuilder.buildVisitReasoningPrompt(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = protocolChunks
            )
        } catch (_: RuntimeException) {
            return safeUncertainResult(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = protocolChunks,
                status = "Experimental Real Gemma path unavailable: prompt construction failed."
            )
        }

        val generation = try {
            textClient.generateText(prompt)
        } catch (_: RuntimeException) {
            return safeUncertainResult(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = protocolChunks,
                status = "Experimental Real Gemma path unavailable: text generation failed safely."
            )
        }

        return when (generation) {
            is TextGenerationResult.Success -> parseGeneratedTextSafely(
                rawOutput = generation.text,
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = protocolChunks
            )
            is TextGenerationResult.Unavailable -> safeUncertainResult(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = protocolChunks,
                status = generation.status
            )
            is TextGenerationResult.Failed -> safeUncertainResult(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = protocolChunks,
                status = "Experimental Real Gemma path unavailable: ${generation.error}"
            )
        }
    }

    private fun parseGeneratedTextSafely(
        rawOutput: String,
        patient: Patient,
        visitHistory: List<VisitLog>,
        observationText: String,
        protocolChunks: List<ProtocolChunk>
    ): VisitReasoningResult {
        return try {
            when (val parsed = outputParser.parseVisitReasoning(rawOutput, patient, observationText, protocolChunks)) {
                is RealGemmaParseResult.Success -> safetyPostProcessor.enforce(
                    result = parsed.result,
                    languageCode = patient.preferredLanguage
                )
                is RealGemmaParseResult.Rejected -> safeUncertainResult(
                    patient = patient,
                    visitHistory = visitHistory,
                    observationText = observationText,
                    protocolChunks = protocolChunks,
                    status = "Experimental Real Gemma output rejected: ${parsed.reason}"
                )
            }
        } catch (_: RuntimeException) {
            safeUncertainResult(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = protocolChunks,
                status = "Experimental Real Gemma output rejected safely."
            )
        }
    }

    private fun safeUncertainResult(
        patient: Patient,
        visitHistory: List<VisitLog>,
        observationText: String,
        protocolChunks: List<ProtocolChunk>,
        status: String
    ): VisitReasoningResult {
        val citation = if (protocolChunks.isEmpty()) {
            "No matching protocol citation"
        } else {
            protocolChunks.take(3).joinToString(separator = "; ") { it.citation }
        }
        val latestHistory = visitHistory.firstOrNull()?.structuredNote ?: "No prior visit history available."
        val patientLanguage = PatientLanguages.forPatient(patient)
        val safetyWording = patientLanguage.safetyWording

        return VisitReasoningResult(
            patientId = patient.id,
            observationText = observationText,
            structuredNote = buildString {
                append("Observation:\n${observationText.trim()}")
                append("\n\nRelevant history:\n")
                append(latestHistory)
                append("\n\nProtocol-grounded support:\n")
                append(status)
                append(" $safetyWording ")
                append("Protocol citation required before recommendation.")
            },
            referralFlag = null,
            protocolCitation = citation,
            suggestedFollowUp = "Use MockGemmaAgent fallback or ask the CHW to review manually. Protocol citation required before recommendation. Protocol citation: $citation",
            protocolChunk = protocolChunks.firstOrNull(),
            uncertain = true,
            clarificationPrompt = "$status Continue with mock fallback for demo-safe protocol support."
        )
    }

    override suspend fun generateVisitNote(
        patient: Patient,
        visitHistory: List<VisitLog>,
        observationText: String,
        protocolChunks: List<ProtocolChunk>
    ): VisitReasoningResult {
        return generateVisitReasoning(patient, visitHistory, observationText, protocolChunks)
    }

    override suspend fun generateSupervisorSummary(
        patients: List<Patient>,
        visits: List<VisitLog>,
        referrals: List<ReferralFlag>
    ): SupervisorSummary {
        // TODO LiteRT-LM integration: generate a structured supervisor summary through Gemma function-call parsing.
        // TODO LiteRT-LM integration: apply timeout/error fallback to MockGemmaAgent for end-of-day summary generation.
        return SupervisorSummary(
            totalVisits = visits.size,
            referralsFlagged = referrals.size,
            urgentCases = emptyList(),
            followUpsDue = emptyList(),
            narrative = "Real Gemma unavailable. LiteRT-LM model support is not initialized, so supervisor summary generation requires MockGemmaAgent fallback."
        )
    }
}
