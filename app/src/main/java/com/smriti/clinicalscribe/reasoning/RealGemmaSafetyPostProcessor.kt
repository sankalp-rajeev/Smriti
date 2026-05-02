package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.PatientLanguages

class RealGemmaSafetyPostProcessor {
    fun enforce(
        result: VisitReasoningResult,
        languageCode: String = PatientLanguages.English.code
    ): VisitReasoningResult {
        val requiredSafetyWording = PatientLanguages.fromCode(languageCode).safetyWording
        if (result.structuredNote.hasSafetyWording(requiredSafetyWording, languageCode)) {
            return result
        }

        return result.copy(
            structuredNote = buildString {
                append(result.structuredNote.trim())
                append("\n\nSafety note: ")
                append(requiredSafetyWording)
            }
        )
    }

    fun hasRequiredSafetyWording(
        text: String,
        languageCode: String = PatientLanguages.English.code
    ): Boolean {
        val requiredSafetyWording = PatientLanguages.fromCode(languageCode).safetyWording
        return text.hasSafetyWording(requiredSafetyWording, languageCode)
    }

    private fun String.hasSafetyWording(requiredSafetyWording: String, languageCode: String): Boolean {
        return contains(
            other = requiredSafetyWording,
            ignoreCase = PatientLanguages.fromCode(languageCode) == PatientLanguages.English
        )
    }
}
