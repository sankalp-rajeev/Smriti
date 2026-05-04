package com.smriti.clinicalscribe.ui

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.PatientLanguages
import com.smriti.clinicalscribe.data.PatientMemoryInsights
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog

enum class PatientChipTone {
    Urgent,
    Caution,
    Routine
}

data class PatientStatusChip(
    val label: String,
    val tone: PatientChipTone
)

object PatientRosterUiLogic {
    fun filterPatients(
        patients: List<Patient>,
        query: String
    ): List<Patient> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return patients
        return patients.filter { patient ->
            listOf(
                patient.name,
                patient.country,
                patient.countryCode,
                patient.village
            ).any { it.lowercase().contains(normalized) }
        }
    }

    fun sortPatients(
        patients: List<Patient>,
        visits: List<VisitLog>,
        referrals: List<ReferralFlag>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Patient> {
        return patients.sortedWith(
            compareBy<Patient> {
                attentionRank(
                    patient = it,
                    visits = visits,
                    referrals = referrals,
                    nowMillis = nowMillis
                )
            }.thenBy { it.name.lowercase() }
        )
    }

    fun statusChips(
        patient: Patient,
        visits: List<VisitLog>,
        referrals: List<ReferralFlag>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<PatientStatusChip> {
        val chips = mutableListOf<PatientStatusChip>()
        if (referrals.any { it.patientId == patient.id }) {
            chips += PatientStatusChip("Referral saved", PatientChipTone.Urgent)
        }
        if (PatientMemoryInsights.missedFollowUpAlerts(patient.id, visits, nowMillis).isNotEmpty()) {
            chips += PatientStatusChip("Follow-up due", PatientChipTone.Caution)
        }
        if (PatientMemoryInsights.risingBloodPressureSignal(patient, visits) != null) {
            chips += PatientStatusChip("History signal", PatientChipTone.Caution)
        }
        val weeks = patient.pregnancyWeeks
        if (weeks != null && weeks >= 40) {
            chips += PatientStatusChip("Overdue", PatientChipTone.Urgent)
        } else if (weeks != null && weeks >= 36) {
            chips += PatientStatusChip("Near term", PatientChipTone.Caution)
        }
        if (chips.isEmpty()) {
            chips += PatientStatusChip("Routine", PatientChipTone.Routine)
        }
        return chips
    }

    fun attentionRank(
        patient: Patient,
        visits: List<VisitLog>,
        referrals: List<ReferralFlag>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (referrals.any { it.patientId == patient.id }) return 0
        if (PatientMemoryInsights.missedFollowUpAlerts(patient.id, visits, nowMillis).isNotEmpty()) return 1
        if (PatientMemoryInsights.risingBloodPressureSignal(patient, visits) != null) return 2
        val weeks = patient.pregnancyWeeks
        if (weeks != null && weeks >= 36) return 3
        return 4
    }
}

object PatientVisitUiText {
    fun gestationLabel(patient: Patient): String {
        val weeks = patient.pregnancyWeeks ?: return "Pregnancy weeks not recorded"
        return "$weeks weeks - ${trimesterLabel(weeks)}"
    }

    fun trimesterLabel(weeks: Int): String {
        return when {
            weeks >= 28 -> "Third trimester"
            weeks >= 14 -> "Second trimester"
            else -> "First trimester"
        }
    }

    fun countryVillage(patient: Patient): String {
        return listOf(patient.country, patient.village)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
    }

    fun noteLanguageName(patient: Patient): String {
        return PatientLanguages.forPatient(patient).englishName
    }

    fun noteLanguageDisplayLabel(patient: Patient): String {
        return PatientLanguages.forPatient(patient).displayLabel
    }

    fun outputLanguageLabel(code: String): String {
        return when (code.lowercase()) {
            "hi" -> "HI"
            "sw" -> "SW"
            "es" -> "ES"
            else -> "EN"
        }
    }
}

object VisitSampleTranscripts {
    fun forPatient(patient: Patient): String {
        return when (patient.id) {
            "patient-fatima" -> Fatima
            "patient-amara" -> Amara
            "patient-grace" -> Grace
            "patient-priya" -> Priya
            "patient-lucia" -> Lucia
            else -> Meena
        }
    }

    const val Meena: String =
        "Meena reports severe headache and blurred vision today. BP is 150/95. She says fetal movement is reduced compared with yesterday."

    const val Fatima: String =
        "Fatima feels well today. BP is 136/86. No headache, blurred vision, bleeding, or reduced fetal movement. Please note her recent BP history."

    const val Amara: String =
        "Amara came for a routine visit after a missed follow-up. She feels tired but reports no bleeding, headache, blurred vision, or reduced fetal movement."

    const val Grace: String =
        "Grace came for a routine visit. BP is 116/74. Fetal movement is present. She is eating well and has no complaints today."

    const val Priya: String =
        "Priya came today but details are unclear. Symptoms, BP, fetal movement, and next visit plan were not fully recorded."

    const val Lucia: String =
        "Lucia reports heavy bleeding and strong abdominal pain today. She feels weak and has not felt fetal movement since morning."
}

data class LanguageChoice(
    val code: String,
    val label: String,
    val confirmationLabel: String
)

object LanguageChoices {
    val options = listOf(
        LanguageChoice("en", "English (EN)", "English"),
        LanguageChoice("hi", "Hindi (HI)", "Hindi"),
        LanguageChoice("es", "Spanish (ES)", "Spanish"),
        LanguageChoice("sw", "Swahili (SW)", "Swahili")
    )

    fun labelFor(code: String): String {
        return PatientVisitUiText.outputLanguageLabel(code)
    }
}
