package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.VisitLog
import com.smriti.clinicalscribe.rag.ProtocolChunk

class RealGemmaPromptBuilder {
    fun buildVisitReasoningPrompt(
        patient: Patient,
        visitHistory: List<VisitLog>,
        observationText: String,
        protocolChunks: List<ProtocolChunk>
    ): String {
        val historySummary = visitHistory
            .sortedByDescending { it.visitDateMillis }
            .take(5)
            .joinToString(separator = "\n") { visit ->
                "- ${visit.structuredNote.take(240)} | Citation: ${visit.protocolCitation}"
            }
            .ifBlank { "- No prior visits available in local history." }

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
            - Every referral or follow-up recommendation must cite exactly one supplied protocol citation.
            - If no protocol chunk is supplied, return an uncertain result with an empty protocolCitation.
            - Avoid diagnostic wording such as "diagnosis", "diagnosed with", or "patient has preeclampsia".
            - Return compact JSON only. Do not include markdown, commentary, or extra text.

            Patient identity:
            - id: ${patient.id}
            - label: ${patient.displayLabel()}
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
        """.trimIndent()
    }
}
