package com.smriti.clinicalscribe.reasoning

data class PaperNoteVisionExtraction(
    val patientName: String,
    val visitDate: String,
    val bloodPressure: String,
    val symptoms: List<String>,
    val followUpPlan: String,
    val confidence: PaperNoteVisionConfidence,
    val needsReview: Boolean,
    val safetyNote: String
) {
    fun toObservationText(): String {
        return listOf(
            "Paper note patient: ${patientName.ifBlank { "Not recorded" }}",
            "Date: ${visitDate.ifBlank { "Not recorded" }}",
            "BP: ${bloodPressure.ifBlank { "Not recorded" }}",
            "Symptoms: ${symptoms.joinToString().ifBlank { "Not recorded" }}",
            "Plan: ${followUpPlan.ifBlank { "Not recorded" }}"
        ).joinToString(separator = "\n")
    }

    fun toStructuredNote(): String {
        return listOf(
            "Scanned paper note extraction.",
            "Patient name: ${patientName.ifBlank { "Not recorded" }}",
            "Visit date: ${visitDate.ifBlank { "Not recorded" }}",
            "Blood pressure: ${bloodPressure.ifBlank { "Not recorded" }}",
            "Symptoms written: ${symptoms.joinToString().ifBlank { "Not recorded" }}",
            "Follow-up plan written: ${followUpPlan.ifBlank { "Not recorded" }}",
            "Safety note: Text was extracted from a paper note. Review before saving."
        ).joinToString(separator = "\n")
    }
}

enum class PaperNoteVisionConfidence(val chwMessage: String) {
    HIGH("Looks clear — please review"),
    MEDIUM("Some text was hard to read — please check carefully"),
    LOW("Text was unclear — please fill in missing details");

    companion object {
        fun fromRaw(value: String): PaperNoteVisionConfidence? {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}

sealed class PaperNoteVisionParseResult {
    data class Success(val extraction: PaperNoteVisionExtraction) : PaperNoteVisionParseResult()
    data class Rejected(val reason: String) : PaperNoteVisionParseResult()
}
