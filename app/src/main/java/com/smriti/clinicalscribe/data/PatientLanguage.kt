package com.smriti.clinicalscribe.data

data class PatientLanguage(
    val code: String,
    val englishName: String,
    val displayLabel: String,
    val safetyWording: String
)

object PatientLanguages {
    val English = PatientLanguage(
        code = "en",
        englishName = "English",
        displayLabel = "EN / English",
        safetyWording = "This is not a diagnosis. CHW confirmation is required before saving."
    )
    val Hindi = PatientLanguage(
        code = "hi",
        englishName = "Hindi",
        displayLabel = "हिन्दी / Hindi",
        safetyWording = "यह निदान नहीं है। CHW की पुष्टि आवश्यक है।"
    )
    val Swahili = PatientLanguage(
        code = "sw",
        englishName = "Swahili",
        displayLabel = "Kiswahili / Swahili",
        safetyWording = "Hii si utambuzi wa ugonjwa. Uthibitisho wa mfanyakazi wa afya unahitajika."
    )
    val Spanish = PatientLanguage(
        code = "es",
        englishName = "Spanish",
        displayLabel = "Español / Spanish",
        safetyWording = "Esto no es un diagnóstico. Se requiere confirmación de la trabajadora de salud."
    )

    private val supported = listOf(English, Hindi, Swahili, Spanish)
        .associateBy { it.code }

    fun fromCode(code: String?): PatientLanguage {
        return supported[code?.lowercase()] ?: English
    }

    fun forPatient(patient: Patient): PatientLanguage {
        return fromCode(patient.preferredLanguage)
    }
}
