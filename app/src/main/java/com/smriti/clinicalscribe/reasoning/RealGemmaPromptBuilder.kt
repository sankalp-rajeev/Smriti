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

        val protocolContext = protocolChunks
            .joinToString(separator = "\n") { chunk ->
                "- id=${chunk.id}; title=${chunk.title}; citation=${chunk.citation}; guidance=${chunk.text}"
            }
            .ifBlank { "- No matching local protocol chunk was supplied. Do not invent a citation." }
        val citationContract = if (protocolChunks.isEmpty()) {
            """
                Citation contract:
                - No protocol chunk was supplied.
                - Set protocolCitation to "".
                - Set uncertain to true.
                - Set referralFlag to null.
                - Do not write "No matching protocol citation" in protocolCitation.
            """.trimIndent()
        } else {
            val allowedCitations = protocolChunks.joinToString(separator = "\n") { chunk ->
                "- ${chunk.citation}"
            }
            """
                Citation contract:
                - protocolCitation must be exactly one supplied citation string from this list:
                $allowedCitations
                - If multiple protocol chunks are relevant, choose the single most urgent or primary citation.
                - Do not join citations with semicolons.
                - Do not invent, paraphrase, or abbreviate citations.
                - Do not write "No matching protocol citation" in protocolCitation.
                - If referralFlag is not null, referralFlag.protocolBasis must be exactly the same supplied citation used for protocolCitation.
            """.trimIndent()
        }

        return """
            You are Smriti, an offline documentation and referral-support assistant for community health workers.

            Safety rules:
            - This is not a diagnosis.
            - CHW confirmation is required before saving any record.
            - Generate all user-facing output in ${outputLanguage.englishName}. Use plain, non-technical language for a community health worker. Protocol citation IDs may remain in English. Do not translate citation identifiers. Do not diagnose. If uncertain, ask for clarification. Safety wording must appear in ${outputLanguage.englishName}.
            - Required safety wording in ${outputLanguage.englishName}: ${outputLanguage.safetyWording}
            - Every referral or follow-up recommendation must cite exactly one supplied protocol citation.
            - If no protocol chunk is supplied, return an uncertain result with an empty protocolCitation.
            - Avoid diagnostic wording such as "diagnosis", "diagnosed with", or "patient has preeclampsia".
            - Return compact JSON only. Do not include markdown, commentary, or extra text.
            - Output exactly one JSON object and nothing else.
            - Do not add a preface, code fence, markdown, bullet list, repeated JSON object, or trailing explanation.
            - Keep JSON string values single-line; avoid newline characters inside JSON string values.
            - Do not add trailing commas. Keep clinical measurements inside string values, not as standalone JSON numbers.
            - Keep the whole JSON under 700 characters when possible.
            - Use concise values: observationText max 120 chars, structuredNote max 90 chars, suggestedFollowUp max 90 chars, referral reason max 90 chars.

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

            $citationContract

            Required JSON shape:
            {"patientId":"string","observationText":"string","structuredNote":"string","protocolCitation":"exactly one supplied citation or empty string when no protocol chunk is supplied","suggestedFollowUp":"string","uncertain":boolean,"clarificationPrompt":null|"string","referralFlag":null|{"urgency":"string","reason":"string","protocolBasis":"same exact supplied citation as protocolCitation","recommendedFacility":"string","dangerSigns":["string"]}}

            Final output rule:
            Return the JSON object on one line. The first character must be { and the last character must be }.
            Use only these top-level keys. Do not add extra keys.
            For referralFlag, keep urgency, reason, protocolBasis, recommendedFacility, and dangerSigns concise.
        """.trimIndent()
    }

    private companion object {
        const val DEFAULT_MAX_HISTORY_VISITS = 5
    }
}
