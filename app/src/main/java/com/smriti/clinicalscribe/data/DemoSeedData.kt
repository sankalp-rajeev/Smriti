package com.smriti.clinicalscribe.data

import com.smriti.clinicalscribe.rag.ProtocolChunk

object DemoSeedData {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    val patients = listOf(
        Patient(
            id = "patient-meena",
            name = "Meena Sharma",
            age = 28,
            sex = "F",
            pregnancyWeeks = 32,
            village = "Rampur",
            riskSummary = "Third trimester ANC follow-up. Prior visit noted borderline BP and mild ankle swelling.",
            country = "India",
            countryCode = "IN",
            preferredLanguage = "hi",
            protocolRegion = "INDIA",
            scenarioPreview = "Danger-sign referral demo"
        ),
        Patient(
            id = "patient-fatima",
            name = "Fatima Begum",
            age = 24,
            sex = "F",
            pregnancyWeeks = 20,
            village = "Kushtia",
            riskSummary = "Rising blood pressure trend across prior ANC visits.",
            country = "Bangladesh",
            countryCode = "BD",
            preferredLanguage = "en",
            protocolRegion = "BANGLADESH",
            scenarioPreview = "Rising BP trend"
        ),
        Patient(
            id = "patient-amara",
            name = "Amara Tesfaye",
            age = 30,
            sex = "F",
            pregnancyWeeks = 36,
            village = "Adama",
            riskSummary = "Elevated-risk ANC follow-up was recommended; completion unknown.",
            country = "Ethiopia",
            countryCode = "ET",
            preferredLanguage = "en",
            protocolRegion = "ETHIOPIA",
            scenarioPreview = "Missed follow-up data for Phase B"
        ),
        Patient(
            id = "patient-grace",
            name = "Grace Achieng",
            age = 26,
            sex = "F",
            pregnancyWeeks = 28,
            village = "Kisumu",
            riskSummary = "Routine ANC history with normal vitals and no referral flags.",
            country = "Kenya",
            countryCode = "KE",
            preferredLanguage = "sw",
            protocolRegion = "AFRICA_REGION",
            scenarioPreview = "Routine visit, no false alarm"
        ),
        Patient(
            id = "patient-priya",
            name = "Priya Devi",
            age = 19,
            sex = "F",
            pregnancyWeeks = 16,
            village = "Sitapur",
            riskSummary = "Early first pregnancy with minimal prior ANC data.",
            country = "India",
            countryCode = "IN",
            preferredLanguage = "hi",
            protocolRegion = "INDIA",
            scenarioPreview = "Incomplete observation / clarification"
        ),
        Patient(
            id = "patient-lucia",
            name = "Lucia Fernandez",
            age = 22,
            sex = "F",
            pregnancyWeeks = 24,
            village = "Cusco",
            riskSummary = "Routine ANC history for South America regional/global fallback demo.",
            country = "Peru",
            countryCode = "PE",
            preferredLanguage = "es",
            protocolRegion = "SOUTH_AMERICA_REGION",
            scenarioPreview = "South America/global protocol fallback"
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
                id = 1_001,
                patientId = "patient-meena",
                visitDateMillis = nowMillis - (21L * DAY_MILLIS),
                observationText = "Routine ANC visit. BP 128/82. Mild ankle swelling. No headache or blurred vision.",
                structuredNote = "Prior ANC visit documented borderline blood pressure around 128/82 and mild ankle swelling. No danger signs reported at that visit.",
                protocolCitation = "WHO ANC Contact schedule",
                suggestedFollowUp = "Repeat BP check at next ANC visit.",
                confirmed = true
            ),
            VisitLog(
                id = 1_002,
                patientId = "patient-meena",
                visitDateMillis = nowMillis - (45L * DAY_MILLIS),
                observationText = "Routine ANC visit. BP 125/80. Fetal movement present. Counseled on nutrition and iron tablets.",
                structuredNote = "Routine ANC contact. BP 125/80, fetal movement present, nutrition and iron tablet adherence discussed.",
                protocolCitation = "WHO ANC Contact schedule",
                suggestedFollowUp = "Continue routine ANC follow-up.",
                confirmed = true
            ),
            VisitLog(
                id = 1_003,
                patientId = "patient-meena",
                visitDateMillis = nowMillis - (70L * DAY_MILLIS),
                observationText = "Counseled on nutrition and iron tablets. Fetal movement present.",
                structuredNote = "Early ANC contact completed. BP 122/78, nutrition counseling completed, fetal movement not a concern.",
                protocolCitation = "WHO ANC Contact schedule",
                suggestedFollowUp = "Continue routine ANC follow-up.",
                confirmed = true
            ),
            VisitLog(
                id = 2_001,
                patientId = "patient-fatima",
                visitDateMillis = nowMillis - (84L * DAY_MILLIS),
                observationText = "Bangladesh ANC visit. BP 118/76. No danger signs.",
                structuredNote = "Routine ANC visit. BP 118/76, no danger signs reported.",
                protocolCitation = "Bangladesh CHW maternal danger signs",
                suggestedFollowUp = "Continue routine ANC follow-up.",
                confirmed = true
            ),
            VisitLog(
                id = 2_002,
                patientId = "patient-fatima",
                visitDateMillis = nowMillis - (56L * DAY_MILLIS),
                observationText = "Bangladesh ANC visit. BP 125/80. No headache or blurred vision.",
                structuredNote = "Routine ANC visit. BP 125/80, no danger signs reported.",
                protocolCitation = "Bangladesh CHW maternal danger signs",
                suggestedFollowUp = "Recheck blood pressure at next ANC visit.",
                confirmed = true
            ),
            VisitLog(
                id = 2_003,
                patientId = "patient-fatima",
                visitDateMillis = nowMillis - (28L * DAY_MILLIS),
                observationText = "Bangladesh ANC visit. BP 132/84. Mild fatigue, no danger signs.",
                structuredNote = "ANC visit documented BP 132/84 with mild fatigue and no danger signs.",
                protocolCitation = "Bangladesh CHW maternal danger signs",
                suggestedFollowUp = "Repeat BP check and confirm symptoms at next visit.",
                confirmed = true
            ),
            VisitLog(
                id = 2_004,
                patientId = "patient-fatima",
                visitDateMillis = nowMillis - (7L * DAY_MILLIS),
                observationText = "Bangladesh ANC visit. BP 138/88. No headache, blurred vision, or bleeding.",
                structuredNote = "ANC visit documented BP 138/88. Rising BP trend noted for CHW review; no danger signs reported.",
                protocolCitation = "Bangladesh CHW maternal danger signs",
                suggestedFollowUp = "Recheck BP soon and confirm danger signs with supervisor if symptoms appear.",
                confirmed = true
            ),
            VisitLog(
                id = 3_001,
                patientId = "patient-amara",
                visitDateMillis = nowMillis - (21L * DAY_MILLIS),
                observationText = "Ethiopia ANC visit. Fatigue and possible anemia risk. Follow-up recommended.",
                structuredNote = "ANC follow-up documented fatigue and anemia risk. Follow-up was recommended for elevated-risk ANC review.",
                protocolCitation = "Ethiopia HEW maternal danger signs",
                suggestedFollowUp = "Follow-up was due within 14 days; outcome unknown.",
                confirmed = true,
                followUpDueDateMillis = nowMillis - (7L * DAY_MILLIS),
                followUpCompleted = false
            ),
            VisitLog(
                id = 3_002,
                patientId = "patient-amara",
                visitDateMillis = nowMillis - (49L * DAY_MILLIS),
                observationText = "Routine Ethiopia ANC visit. BP 118/74. Nutrition and iron counseling.",
                structuredNote = "Routine ANC visit. BP 118/74, nutrition and iron counseling completed.",
                protocolCitation = "Ethiopia HEW maternal danger signs",
                suggestedFollowUp = "Continue ANC contacts and iron counseling.",
                confirmed = true
            ),
            VisitLog(
                id = 4_001,
                patientId = "patient-grace",
                visitDateMillis = nowMillis - (35L * DAY_MILLIS),
                observationText = "Kenya ANC visit. BP 112/72. Fetal movement present. No danger signs.",
                structuredNote = "Routine ANC visit with normal vitals: BP 112/72, fetal movement present, no referral flags.",
                protocolCitation = "Regional CHW maternal danger signs",
                suggestedFollowUp = "Continue routine ANC follow-up.",
                confirmed = true
            ),
            VisitLog(
                id = 4_002,
                patientId = "patient-grace",
                visitDateMillis = nowMillis - (14L * DAY_MILLIS),
                observationText = "Kenya ANC visit. BP 116/74. Eating well. No bleeding, headache, or blurred vision.",
                structuredNote = "Routine ANC visit with normal vitals: BP 116/74. No danger signs or referral flags.",
                protocolCitation = "Regional CHW maternal danger signs",
                suggestedFollowUp = "Continue routine ANC follow-up.",
                confirmed = true
            ),
            VisitLog(
                id = 5_001,
                patientId = "patient-priya",
                visitDateMillis = nowMillis - (10L * DAY_MILLIS),
                observationText = "First pregnancy early ANC registration. Minimal prior data recorded.",
                structuredNote = "Early ANC registration for first pregnancy. No vitals trend available yet.",
                protocolCitation = "India ASHA ANC danger signs",
                suggestedFollowUp = "Ask for complete symptoms, vitals, and next visit date at follow-up.",
                confirmed = true
            ),
            VisitLog(
                id = 6_001,
                patientId = "patient-lucia",
                visitDateMillis = nowMillis - (42L * DAY_MILLIS),
                observationText = "Peru ANC visit. BP 110/70. Routine counseling. No danger signs.",
                structuredNote = "Routine ANC visit in Peru. BP 110/70, no danger signs, counseling completed.",
                protocolCitation = "Regional CHW maternal danger signs",
                suggestedFollowUp = "Continue routine ANC follow-up.",
                confirmed = true
            ),
            VisitLog(
                id = 6_002,
                patientId = "patient-lucia",
                visitDateMillis = nowMillis - (18L * DAY_MILLIS),
                observationText = "Peru ANC visit. BP 114/72. Fetal movement present. No danger signs.",
                structuredNote = "Routine ANC visit in Peru. BP 114/72, fetal movement present, no country-specific protocol chunk required.",
                protocolCitation = "Regional CHW maternal danger signs",
                suggestedFollowUp = "Continue routine ANC follow-up with regional/global protocol fallback if needed.",
                confirmed = true
            )
        )
    }
}
