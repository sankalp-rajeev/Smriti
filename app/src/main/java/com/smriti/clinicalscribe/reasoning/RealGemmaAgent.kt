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
        val scenario = patient.id
        val promptProtocolChunks = protocolChunks.take(MAX_PROMPT_PROTOCOL_CHUNKS)
        val prompt = try {
            SmritiLatencyLogger.measure("promptBuild", scenario) {
                promptBuilder.buildVisitReasoningPrompt(
                    patient = patient,
                    visitHistory = visitHistory.take(MAX_PROMPT_HISTORY_VISITS),
                    observationText = observationText,
                    protocolChunks = promptProtocolChunks
                )
            }
        } catch (_: RuntimeException) {
            return safeUncertainResult(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = promptProtocolChunks,
                status = "Experimental Real Gemma path unavailable: prompt construction failed."
            )
        }

        val generation = try {
            var result: TextGenerationResult? = null
            val duration = kotlin.system.measureTimeMillis {
                result = textClient.generateText(prompt)
            }
            SmritiLatencyLogger.log("realGemmaGenerateCall", duration, scenario)
            result ?: TextGenerationResult.Failed("Text generation returned no result.")
        } catch (_: RuntimeException) {
            return safeUncertainResult(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = promptProtocolChunks,
                status = "Experimental Real Gemma path unavailable: text generation failed safely."
            )
        }

        return when (generation) {
            is TextGenerationResult.Success -> parseGeneratedTextSafely(
                rawOutput = generation.text,
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = promptProtocolChunks
            )
            is TextGenerationResult.Unavailable -> safeUncertainResult(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = promptProtocolChunks,
                status = generation.status
            )
            is TextGenerationResult.Failed -> safeUncertainResult(
                patient = patient,
                visitHistory = visitHistory,
                observationText = observationText,
                protocolChunks = promptProtocolChunks,
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
            var parsed: RealGemmaParseResult? = null
            val duration = kotlin.system.measureTimeMillis {
                parsed = outputParser.parseVisitReasoning(rawOutput, patient, observationText, protocolChunks)
            }
            SmritiLatencyLogger.log("parseSafetyCitationValidation", duration, patient.id)
            val parseResult = parsed ?: error("Parser returned no result.")
            when (parseResult) {
                is RealGemmaParseResult.Success -> safetyPostProcessor.enforce(
                    result = parseResult.result,
                    languageCode = patient.preferredLanguage
                )
                is RealGemmaParseResult.Rejected -> {
                    RealGemmaDebugLogger.logParserFailure(
                        rawOutput = rawOutput,
                        reason = parseResult.reason
                    )
                    safeUncertainResult(
                        patient = patient,
                        visitHistory = visitHistory,
                        observationText = observationText,
                        protocolChunks = protocolChunks,
                        status = "Experimental Real Gemma output rejected: ${parseResult.reason}"
                    )
                }
            }
        } catch (error: RuntimeException) {
            RealGemmaDebugLogger.logParserFailure(
                rawOutput = rawOutput,
                reason = error.message ?: "parser threw an exception"
            )
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
            suggestedFollowUp = "RealGemma reasoning is unavailable. Ask the CHW to review manually and retry after setup. Protocol citation required before recommendation. Protocol citation: $citation",
            protocolChunk = protocolChunks.firstOrNull(),
            uncertain = true,
            clarificationPrompt = "$status Complete local RealGemma setup or retry; no mock clinical output was generated."
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
        return SupervisorSummary(
            totalVisits = visits.size,
            referralsFlagged = referrals.size,
            urgentCases = emptyList(),
            followUpsDue = emptyList(),
            narrative = "RealGemma supervisor reasoning unavailable. Complete local model setup and retry."
        )
    }

    private companion object {
        const val MAX_PROMPT_HISTORY_VISITS = 3
        const val MAX_PROMPT_PROTOCOL_CHUNKS = 2
    }
}
