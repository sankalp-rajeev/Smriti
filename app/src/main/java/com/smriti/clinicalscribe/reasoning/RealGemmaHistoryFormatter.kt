package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.VisitLog

interface RealGemmaHistoryFormatter {
    fun format(visitHistory: List<VisitLog>, maxHistoryVisits: Int): String

    object Default : RealGemmaHistoryFormatter {
        override fun format(visitHistory: List<VisitLog>, maxHistoryVisits: Int): String {
            return visitHistory
                .sortedByDescending { it.visitDateMillis }
                .take(maxHistoryVisits)
                .joinToString(separator = "\n") { visit ->
                    "- ${visit.structuredNote.take(240)} | Citation: ${visit.protocolCitation}"
                }
                .ifBlank { "- No prior visits available in local history." }
        }
    }

    object Compact : RealGemmaHistoryFormatter {
        override fun format(visitHistory: List<VisitLog>, maxHistoryVisits: Int): String {
            return visitHistory
                .sortedByDescending { it.visitDateMillis }
                .take(maxHistoryVisits)
                .mapIndexed { index, visit ->
                    val label = "V" + (index + 1).toString().padStart(2, '0')
                    val issue = visit.observationText.compactText(44)
                    val action = visit.suggestedFollowUp.compactText(44)
                    val citation = visit.protocolCitation.compactText(40)
                    "$label: date=${visit.visitDateMillis}; issue=$issue; action=$action; citation=$citation"
                }
                .joinToString(separator = "\n")
                .ifBlank { "V00: none" }
        }

        private fun String.compactText(maxLength: Int): String {
            return replace(Regex("\\s+"), " ")
                .replace("\"", "'")
                .trim()
                .take(maxLength)
        }
    }
}
