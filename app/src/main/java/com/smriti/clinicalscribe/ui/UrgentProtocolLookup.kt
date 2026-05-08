package com.smriti.clinicalscribe.ui

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.rag.ProtocolChunk
import com.smriti.clinicalscribe.rag.ProtocolRetrievalContext
import com.smriti.clinicalscribe.rag.ProtocolRetriever

data class UrgentProtocolSign(
    val label: String,
    val queryText: String
)

data class UrgentProtocolLookupResult(
    val observedSigns: List<String>,
    val freeText: String,
    val contextLabel: String,
    val guidanceChunk: ProtocolChunk?,
    val urgentReviewMayBeNeeded: Boolean
) {
    val hasGuidance: Boolean = guidanceChunk != null
}

object UrgentProtocolLookupSigns {
    val all = listOf(
        UrgentProtocolSign("Severe headache", "severe headache headache danger signs"),
        UrgentProtocolSign("Blurred vision", "blurred vision visual symptoms"),
        UrgentProtocolSign("High blood pressure", "high blood pressure high bp bp 150 150 over 95"),
        UrgentProtocolSign("Reduced fetal movement", "reduced fetal movement less movement"),
        UrgentProtocolSign("Bleeding", "bleeding vaginal bleeding blood loss"),
        UrgentProtocolSign("Convulsions", "convulsions seizure fits"),
        UrgentProtocolSign("Severe abdominal pain", "severe abdominal pain stomach pain"),
        UrgentProtocolSign("Fever", "fever high temperature chills")
    )
}

object UrgentProtocolLookupBuilder {
    fun lookup(
        selectedSigns: List<UrgentProtocolSign>,
        freeText: String,
        patient: Patient?,
        retriever: ProtocolRetriever
    ): UrgentProtocolLookupResult {
        val query = buildQuery(selectedSigns, freeText)
        val context = patient?.protocolContext() ?: ProtocolRetrievalContext(region = "GLOBAL_CORE")
        val guidance = retriever.retrieve(query = query, context = context).firstOrNull()
        return UrgentProtocolLookupResult(
            observedSigns = selectedSigns.map { it.label },
            freeText = freeText.trim(),
            contextLabel = patient?.protocolContextLabel() ?: "Global guidance",
            guidanceChunk = guidance,
            urgentReviewMayBeNeeded = guidance?.let { isUrgentGuidance(it) } == true
        )
    }

    private fun buildQuery(
        selectedSigns: List<UrgentProtocolSign>,
        freeText: String
    ): String {
        return (selectedSigns.joinToString(separator = " ") { it.queryText } + " " + freeText)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isUrgentGuidance(chunk: ProtocolChunk): Boolean {
        val normalized = listOf(chunk.referralLevel, chunk.text, chunk.title, chunk.topic)
            .joinToString(separator = " ")
            .lowercase()
        return listOf(
            "immediate",
            "same_day",
            "same-day",
            "within_24h",
            "urgent",
            "danger sign",
            "referral",
            "escalat"
        ).any { it in normalized }
    }
}
