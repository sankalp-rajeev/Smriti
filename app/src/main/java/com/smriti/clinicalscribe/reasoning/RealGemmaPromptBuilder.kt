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

        return """
            You are Smriti, an offline documentation and referral-support assistant for community health workers.

            Safety rules:
            - This is not a diagnosis.
            - CHW confirmation is required before saving any record.
            - Every referral or follow-up recommendation must cite one supplied protocol citation.
            - If no protocol chunk is supplied, do not invent a citation and mark the result uncertain.
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

            Required JSON shape:
            {"patientId":"string","observationText":"string","structuredNote":"string","protocolCitation":"string","suggestedFollowUp":"string","uncertain":boolean,"clarificationPrompt":null|"string","referralFlag":null|{"urgency":"string","reason":"string","protocolBasis":"string","recommendedFacility":"string","dangerSigns":["string"]}}
        """.trimIndent()
    }
}
