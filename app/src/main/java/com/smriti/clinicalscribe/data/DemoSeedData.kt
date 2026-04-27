package com.smriti.clinicalscribe.data

import com.smriti.clinicalscribe.rag.ProtocolChunk

object DemoSeedData {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    val patients = listOf(
        Patient(
            id = "patient-meena",
            name = "Meena",
            age = 28,
            sex = "F",
            pregnancyWeeks = 32,
            village = "Rampur",
            riskSummary = "Third trimester ANC follow-up. Prior visit noted borderline BP."
        ),
        Patient(
            id = "patient-asha",
            name = "Asha",
            age = 24,
            sex = "F",
            pregnancyWeeks = 20,
            village = "Rampur",
            riskSummary = "Routine ANC check. No danger signs in last visit."
        )
    )

    val protocolChunks = listOf(
        ProtocolChunk(
            id = "anc-danger-signs",
            title = "Pregnancy danger signs",
            source = "WHO ANC",
            section = "Recommendation B1.2",
            text = "Severe headache, blurred vision, high blood pressure, bleeding, convulsions, and reduced fetal movement require urgent assessment and referral.",
            keywords = "headache blurred vision high blood pressure bp bleeding convulsions fetal movement"
        ),
        ProtocolChunk(
            id = "anc-routine-followup",
            title = "Routine antenatal follow-up",
            source = "WHO ANC",
            section = "Contact schedule",
            text = "Routine antenatal contacts should document symptoms, blood pressure, fetal movement, counseling, and planned follow-up.",
            keywords = "routine antenatal follow up blood pressure fetal movement counseling"
        )
    )

    fun initialVisitLogs(nowMillis: Long = System.currentTimeMillis()): List<VisitLog> {
        return listOf(
            VisitLog(
                patientId = "patient-meena",
                visitDateMillis = nowMillis - (21L * DAY_MILLIS),
                observationText = "Routine ANC visit. BP 138/88. Mild ankle swelling. No headache or blurred vision.",
                structuredNote = "Prior ANC visit documented borderline blood pressure and mild ankle swelling. No danger signs reported at that visit.",
                protocolCitation = "WHO ANC Contact schedule",
                suggestedFollowUp = "Repeat BP check at next ANC visit.",
                confirmed = true
            ),
            VisitLog(
                patientId = "patient-meena",
                visitDateMillis = nowMillis - (45L * DAY_MILLIS),
                observationText = "Counseled on nutrition and iron tablets. Fetal movement present.",
                structuredNote = "Nutrition counseling completed. Iron tablet adherence discussed. Fetal movement present.",
                protocolCitation = "WHO ANC Contact schedule",
                suggestedFollowUp = "Continue routine ANC follow-up.",
                confirmed = true
            )
        )
    }
}
