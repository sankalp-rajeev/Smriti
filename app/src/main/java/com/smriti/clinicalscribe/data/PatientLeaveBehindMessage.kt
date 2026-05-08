package com.smriti.clinicalscribe.data

object PatientLeaveBehindMessageGenerator {
    fun generate(
        patient: Patient,
        visit: VisitLog,
        referral: ReferralFlag?
    ): String {
        val language = PatientLanguages.fromCode(patient.preferredLanguage)
        val firstName = patient.name.trim().substringBefore(" ").ifBlank { patient.name.ifBlank { "Patient" } }
        val observed = observedIssuePhrase(visit, referral, language.code)
        val followUp = safeFollowUpPhrase(visit.suggestedFollowUp, language.code)
        return when (language.code) {
            PatientLanguages.Hindi.code -> hindi(firstName, observed, followUp, referral != null)
            PatientLanguages.Spanish.code -> spanish(firstName, observed, followUp, referral != null)
            PatientLanguages.Swahili.code -> swahili(firstName, observed, followUp, referral != null)
            else -> english(firstName, observed, followUp, referral != null)
        }
    }

    private fun english(
        name: String,
        observed: String,
        followUp: String,
        hasReferral: Boolean
    ): String {
        val reviewLine = if (hasReferral) {
            "Please seek review as advised by your health worker."
        } else {
            "Please keep the follow-up plan shared by your health worker."
        }
        return listOf(
            "$name, today your health worker recorded $observed.",
            reviewLine,
            followUp,
            "Show this message to clinic staff if you go for review.",
            "This is not a diagnosis. A health worker must confirm all details."
        ).joinToString(separator = " ")
    }

    private fun hindi(
        name: String,
        observed: String,
        followUp: String,
        hasReferral: Boolean
    ): String {
        val reviewLine = if (hasReferral) {
            "कृपया स्वास्थ्य कार्यकर्ता की सलाह के अनुसार समीक्षा करवाएं।"
        } else {
            "कृपया स्वास्थ्य कार्यकर्ता द्वारा बताया गया फॉलो-अप याद रखें।"
        }
        return listOf(
            "$name, आज आपकी स्वास्थ्य कार्यकर्ता ने $observed नोट किया है।",
            reviewLine,
            followUp,
            "जरूरत हो तो यह संदेश क्लिनिक स्टाफ को दिखाएं।",
            "यह निदान नहीं है। स्वास्थ्य कार्यकर्ता को सभी विवरणों की पुष्टि करनी चाहिए।"
        ).joinToString(separator = " ")
    }

    private fun spanish(
        name: String,
        observed: String,
        followUp: String,
        hasReferral: Boolean
    ): String {
        val reviewLine = if (hasReferral) {
            "Busque revisión según lo indicado por su trabajadora de salud."
        } else {
            "Siga el plan de seguimiento compartido por su trabajadora de salud."
        }
        return listOf(
            "$name, hoy su trabajadora de salud registró $observed.",
            reviewLine,
            followUp,
            "Muestre este mensaje al personal de salud si acude a revisión.",
            "Esto no es un diagnóstico. Una trabajadora de salud debe confirmar todos los detalles."
        ).joinToString(separator = " ")
    }

    private fun swahili(
        name: String,
        observed: String,
        followUp: String,
        hasReferral: Boolean
    ): String {
        val reviewLine = if (hasReferral) {
            "Tafadhali pata ukaguzi kama ulivyoelekezwa na mhudumu wa afya."
        } else {
            "Tafadhali fuata mpango wa ufuatiliaji uliotolewa na mhudumu wa afya."
        }
        return listOf(
            "$name, leo mhudumu wa afya ameandika $observed.",
            reviewLine,
            followUp,
            "Onyesha ujumbe huu kwa wahudumu wa afya ukienda kwa ukaguzi.",
            "Hii si utambuzi wa ugonjwa. Mhudumu wa afya lazima athibitishe maelezo yote."
        ).joinToString(separator = " ")
    }

    private fun observedIssuePhrase(
        visit: VisitLog,
        referral: ReferralFlag?,
        languageCode: String
    ): String {
        val text = listOf(visit.observationText, visit.structuredNote, referral?.dangerSigns.orEmpty())
            .joinToString(separator = " ")
            .lowercase()
        val items = buildList {
            if ("headache" in text || "सिरदर्द" in text || "maumivu ya kichwa" in text) add(label("headache", languageCode))
            if ("blurred vision" in text || "धुंधला" in text || "visión borrosa" in text || "kuona ukungu" in text) add(label("blurredVision", languageCode))
            if ("high blood pressure" in text || "bp 150" in text || "150/95" in text || "150 over 95" in text) add(label("highBloodPressure", languageCode))
            if ("reduced fetal" in text || "reduced movement" in text || "हलचल कम" in text) add(label("reducedFetalMovement", languageCode))
            if ("bleeding" in text || "sangrado" in text) add(label("bleeding", languageCode))
            if ("fever" in text || "fiebre" in text || "homa" in text) add(label("fever", languageCode))
            if ("fatigue" in text || "tired" in text || "weak" in text) add(label("tiredness", languageCode))
        }.distinct()
        return when {
            items.isNotEmpty() -> items.joinToString()
            else -> label("visitDetails", languageCode)
        }
    }

    private fun safeFollowUpPhrase(followUp: String, languageCode: String): String {
        if (followUp.isBlank()) return label("followAdvice", languageCode)
        return when (languageCode) {
            PatientLanguages.Hindi.code -> "अगला कदम स्वास्थ्य कार्यकर्ता से पुष्टि करके ही करें।"
            PatientLanguages.Spanish.code -> "Confirme el siguiente paso con su trabajadora de salud."
            PatientLanguages.Swahili.code -> "Thibitisha hatua inayofuata na mhudumu wa afya."
            else -> "Confirm the next step with your health worker."
        }
    }

    private fun label(key: String, languageCode: String): String {
        return when (languageCode) {
            PatientLanguages.Hindi.code -> when (key) {
                "headache" -> "सिरदर्द"
                "blurredVision" -> "धुंधला दिखना"
                "highBloodPressure" -> "उच्च रक्तचाप"
                "reducedFetalMovement" -> "बच्चे की हलचल कम होना"
                "bleeding" -> "खून आना"
                "fever" -> "बुखार"
                "tiredness" -> "थकान"
                "followAdvice" -> "अगला कदम स्वास्थ्य कार्यकर्ता से पुष्टि करके ही करें।"
                else -> "आज की यात्रा की जानकारी"
            }
            PatientLanguages.Spanish.code -> when (key) {
                "headache" -> "dolor de cabeza"
                "blurredVision" -> "visión borrosa"
                "highBloodPressure" -> "presión arterial alta"
                "reducedFetalMovement" -> "menos movimiento del bebé"
                "bleeding" -> "sangrado"
                "fever" -> "fiebre"
                "tiredness" -> "cansancio"
                "followAdvice" -> "Confirme el siguiente paso con su trabajadora de salud."
                else -> "los detalles de la visita"
            }
            PatientLanguages.Swahili.code -> when (key) {
                "headache" -> "maumivu ya kichwa"
                "blurredVision" -> "kuona ukungu"
                "highBloodPressure" -> "shinikizo la damu lililo juu"
                "reducedFetalMovement" -> "mtoto kucheza kidogo"
                "bleeding" -> "kutokwa na damu"
                "fever" -> "homa"
                "tiredness" -> "uchovu"
                "followAdvice" -> "Thibitisha hatua inayofuata na mhudumu wa afya."
                else -> "taarifa za ziara ya leo"
            }
            else -> when (key) {
                "headache" -> "headache"
                "blurredVision" -> "blurred vision"
                "highBloodPressure" -> "high blood pressure"
                "reducedFetalMovement" -> "reduced fetal movement"
                "bleeding" -> "bleeding"
                "fever" -> "fever"
                "tiredness" -> "tiredness"
                "followAdvice" -> "Confirm the next step with your health worker."
                else -> "today's visit details"
            }
        }
    }
}
