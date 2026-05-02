package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientLanguages
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk

class RealGemmaPromptBuilder(
    private val maxHistoryVisits: Int = DEFAULT_MAX_HISTORY_VISITS,
    private val historyFormatter: RealGemmaHistoryFormatter = RealGemmaHistoryFormatter.Default
) {
    fun buildVisitReasoningPrompt(
        patient: Patient,
        visitHistory: List<VisitLog>,
        observationText: String,
        protocolChunks: List<ProtocolChunk>
    ): String {
        val historySummary = historyFormatter.format(visitHistory, maxHistoryVisits)
        val outputLanguage = PatientLanguages.forPatient(patient)
        val promptProtocolChunks = protocolChunks.take(MAX_PROTOCOL_CHUNKS)

        val protocolContext = promptProtocolChunks
            .joinToString(separator = "\n") { chunk ->
                "- id=${chunk.id}; title=${chunk.title}; citation=${chunk.citation}; guidance=${chunk.text}"
            }
            .ifBlank { "- No matching local protocol chunk was supplied. Do not invent a citation." }
        val allowedCitationSection = promptProtocolChunks
            .joinToString(separator = "\n") { chunk -> "- ${chunk.citation}" }
            .ifBlank { "- none" }
        val citationContract = if (promptProtocolChunks.isEmpty()) {
            """
                Citation contract:
                - No protocol chunk was supplied.
                - Set citations to [].
                - Set referralFlag to false.
                - Set confidence to LOW.
                - Set clarificationQuestion to a concise question asking the CHW to confirm missing details.
                - Do not write "No matching protocol citation" anywhere in the JSON.
            """.trimIndent()
        } else {
            val allowedCitations = promptProtocolChunks.joinToString(separator = "\n") { chunk ->
                "- ${chunk.citation}"
            }
            """
                Citation contract:
                - Use only citation IDs from the supplied protocol chunks.
                - citations must contain supplied citation strings from ALLOWED CITATIONS only:
                $allowedCitations
                - If multiple protocol chunks are relevant, put the single most urgent or primary citation first.
                - Do not join citations with semicolons. Each citation must be a separate array item.
                - Do not invent, paraphrase, or abbreviate citations.
                - Do not write "No matching protocol citation" anywhere in the JSON.
                - If referralFlag is true, citations must include at least one supplied citation.
                - If referralFlag is false and no protocol-specific recommendation is needed, citations may be empty.
                - Routine follow-up wording is allowed for no-danger-sign cases when referralFlag is false.
            """.trimIndent()
        }
        val exampleOutput = promptProtocolChunks.firstOrNull()?.let { primaryProtocol ->
            """

                Example valid output for this Meena danger-sign case:
                {"summary":"Severe headache, blurred vision, BP 150/95, and reduced fetal movement noted. Same-day referral support is protocol-grounded.","referralFlag":true,"referralReason":"Danger signs in pregnancy need same-day referral support.","dangerSigns":["severe headache","blurred vision","reduced fetal movement"],"followUpPlan":["Arrange same-day referral and document CHW confirmation."],"clarificationQuestion":"","citations":["${primaryProtocol.citation}"],"confidence":"HIGH","safetyNote":"${outputLanguage.safetyWording}"}
            """.trimIndent()
        }.orEmpty()

        return """
            You are Smriti, an offline documentation and referral-support assistant for community health workers.

            Safety rules:
            - This is not a diagnosis.
            - CHW confirmation is required before saving any record.
            - Generate all user-facing output in ${outputLanguage.englishName}. Use plain, non-technical language for a community health worker. Protocol citation IDs may remain in English. Do not translate citation identifiers. Do not diagnose. If uncertain, ask for clarification. Safety wording must appear in ${outputLanguage.englishName}.
            - Required safety wording in ${outputLanguage.englishName}: ${outputLanguage.safetyWording}
            - Every referral recommendation must cite at least one supplied protocol citation.
            - Routine no-referral follow-up may use empty citations when no supplied protocol directly supports the plan.
            - If no protocol chunk is supplied, return an uncertain result with citations [].
            - Avoid diagnostic wording such as "diagnosis", "diagnosed with", or "patient has preeclampsia".
            - Return exact JSON only. Do not include markdown, commentary, or extra text.
            - Output exactly one JSON object and nothing else.
            - The first character must be { and the last character must be }.
            - Do not wrap in ```json or any code fence.
            - Do not add a preface, markdown, bullet list, repeated JSON object, or trailing explanation.
            - Keep JSON string values single-line; avoid newline characters inside JSON string values.
            - Do not add trailing commas. Keep clinical measurements inside string values, not as standalone JSON numbers.
            - Keep the whole JSON under 900 characters when possible.
            - Use concise values: summary max 180 chars, referralReason max 100 chars, each followUpPlan item max 90 chars.

            Patient identity:
            - id: ${patient.id}
            - label: ${patient.displayLabel()}
            - preferred output language: ${outputLanguage.englishName} (${outputLanguage.code})
            - village: ${patient.village}
            - pregnancy weeks: ${patient.pregnancyWeeks ?: "unknown"}
            - risk summary: ${patient.riskSummary}

            Prior visit history:
            $historySummary

            CHW observation/transcript:
            ${observationText.trim()}

            Retrieved protocol chunks:
            $protocolContext

            ALLOWED CITATIONS:
            $allowedCitationSection

            $citationContract

            Required JSON shape:
            {"summary":"plain CHW note in ${outputLanguage.englishName}","referralFlag":true|false,"referralReason":"plain reason or empty string","dangerSigns":["string"],"followUpPlan":["string"],"clarificationQuestion":"","citations":["exact supplied citation"],"confidence":"HIGH|MEDIUM|LOW","safetyNote":"${outputLanguage.safetyWording}"}

            Required field rules:
            - referralFlag must always be present as a boolean true or false.
            - citations must always be an array, even when empty.
            - dangerSigns and followUpPlan must always be arrays.
            - clarificationQuestion must be an empty string if not needed.
            - confidence must be exactly HIGH, MEDIUM, or LOW.
            - safetyNote must include the required non-diagnostic CHW-confirmation wording in ${outputLanguage.englishName}.
            - referralReason must be an empty string when referralFlag is false.
            - For routine visits with no danger signs, set referralFlag=false, dangerSigns=[], and provide a brief routine follow-up plan. Use citations only if one of the supplied protocol chunks directly supports the recommendation.
            - Output JSON only. No markdown. No explanations outside JSON.

            $exampleOutput

            Final output rule:
            Return one JSON object only, on one line.
            Use only these top-level keys. Do not add extra keys.
        """.trimIndent()
    }

    private companion object {
        const val DEFAULT_MAX_HISTORY_VISITS = 3
        const val MAX_PROTOCOL_CHUNKS = 2
    }
}
