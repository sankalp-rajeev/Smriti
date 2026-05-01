package com.smriti.clinicalscribe.data

import com.smriti.clinicalscribe.transcript.TranscriptResult
import java.util.UUID

data class PatientRegistrationDraft(
    val name: String = "",
    val age: String = "",
    val pregnancyWeeks: String = "",
    val village: String = "",
    val countryCode: String = "IN",
    val preferredLanguage: String = "en",
    val notes: String = ""
) {
    fun applySpeechResult(
        field: PatientRegistrationField,
        result: TranscriptResult
    ): PatientRegistrationSpeechUpdate {
        return when (result) {
            is TranscriptResult.Success -> {
                val transcript = result.transcript.trim()
                PatientRegistrationSpeechUpdate(
                    draft = updateField(field, transcript),
                    message = "Offline speech filled ${field.label}. Review and edit before saving.",
                    canUseManualFallback = true
                )
            }

            is TranscriptResult.Unavailable -> PatientRegistrationSpeechUpdate(
                draft = this,
                message = "Offline speech unavailable: ${result.reason}",
                canUseManualFallback = true
            )

            is TranscriptResult.Error -> PatientRegistrationSpeechUpdate(
                draft = this,
                message = "Offline speech error: ${result.reason}",
                canUseManualFallback = true
            )
        }
    }

    fun toPatient(
        idProvider: () -> String = { "patient-manual-${UUID.randomUUID()}" }
    ): PatientRegistrationResult {
        val trimmedName = name.trim()
        val trimmedVillage = village.trim()
        val parsedAge = age.extractFirstInt()
        val parsedPregnancyWeeks = pregnancyWeeks.extractFirstInt()
        val trimmedCountryCode = countryCode.trim().uppercase()
        val trimmedLanguage = preferredLanguage.trim().lowercase()

        val errors = buildList {
            if (trimmedName.isBlank()) add("Name is required.")
            if (parsedAge == null || parsedAge <= 0) add("Age is required.")
            if (parsedPregnancyWeeks == null || parsedPregnancyWeeks <= 0) {
                add("Weeks pregnant is required.")
            }
            if (trimmedVillage.isBlank()) add("Village is required.")
            if (trimmedCountryCode.isBlank()) add("Country code is required.")
            if (trimmedLanguage.isBlank()) add("Preferred language is required.")
        }
        if (errors.isNotEmpty()) {
            return PatientRegistrationResult.Invalid(errors)
        }

        return PatientRegistrationResult.Valid(
            Patient(
                id = idProvider(),
                name = trimmedName,
                age = parsedAge!!,
                sex = "F",
                pregnancyWeeks = parsedPregnancyWeeks,
                village = trimmedVillage,
                riskSummary = notes.trim().ifBlank { "New patient registration. No prior visits recorded yet." },
                country = countryNameFor(trimmedCountryCode),
                countryCode = trimmedCountryCode,
                preferredLanguage = trimmedLanguage,
                protocolRegion = protocolRegionFor(trimmedCountryCode),
                scenarioPreview = "New patient",
                notes = notes.trim().ifBlank { null }
            )
        )
    }

    private fun updateField(
        field: PatientRegistrationField,
        value: String
    ): PatientRegistrationDraft {
        return when (field) {
            PatientRegistrationField.NAME -> copy(name = value)
            PatientRegistrationField.AGE -> copy(age = value)
            PatientRegistrationField.PREGNANCY_WEEKS -> copy(pregnancyWeeks = value)
            PatientRegistrationField.VILLAGE -> copy(village = value)
        }
    }

    private fun String.extractFirstInt(): Int? {
        return Regex("\\d+").find(this)?.value?.toIntOrNull()
    }

    private fun countryNameFor(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "IN" -> "India"
            "BD" -> "Bangladesh"
            "ET" -> "Ethiopia"
            "KE" -> "Kenya"
            "PE" -> "Peru"
            "CO" -> "Colombia"
            else -> countryCode.uppercase()
        }
    }

    private fun protocolRegionFor(countryCode: String): String {
        return when (countryCode.uppercase()) {
            "IN" -> "INDIA"
            "BD" -> "BANGLADESH"
            "ET" -> "ETHIOPIA"
            "KE" -> "AFRICA_REGION"
            "PE", "CO" -> "SOUTH_AMERICA_REGION"
            else -> "GLOBAL_CORE"
        }
    }
}

enum class PatientRegistrationField(val label: String) {
    NAME("patient name"),
    AGE("patient age"),
    PREGNANCY_WEEKS("weeks pregnant"),
    VILLAGE("village name")
}

data class PatientRegistrationSpeechUpdate(
    val draft: PatientRegistrationDraft,
    val message: String,
    val canUseManualFallback: Boolean
)

sealed interface PatientRegistrationResult {
    data class Valid(val patient: Patient) : PatientRegistrationResult
    data class Invalid(val errors: List<String>) : PatientRegistrationResult
}
