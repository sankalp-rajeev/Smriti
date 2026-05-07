package com.smriti.clinicalscribe.rag

object MultilingualProtocolQueryExpander {
    fun expandForProtocolRetrieval(
        rawText: String,
        preferredLanguage: String?
    ): String {
        if (rawText.isBlank()) return rawText

        val language = preferredLanguage?.trim()?.lowercase().orEmpty()
        val normalized = rawText.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
        val hints = linkedSetOf<String>()

        val headache = hasHeadache(normalized, language)
        val blurredVision = hasBlurredVision(normalized, language)
        val reducedFetalMovement = hasReducedFetalMovement(normalized, language)
        val bpReading = highBloodPressureReading(normalized)
        val highBp = bpReading?.let { it.systolic >= 140 || it.diastolic >= 90 } == true
        val severeHighBp = bpReading?.let { it.systolic >= 160 || it.diastolic >= 110 } == true

        if (headache) {
            hints += "severe headache headache danger sign"
        }
        if (blurredVision) {
            hints += "blurred vision visual symptoms danger sign"
        }
        if (reducedFetalMovement) {
            hints += "reduced fetal movement danger sign"
        }
        if (highBp) {
            hints += "high blood pressure hypertension danger sign"
            hints += "high blood pressure hypertension ANC danger signs referral"
        }
        if (severeHighBp) {
            hints += "severe hypertension urgent referral danger signs"
        }
        if (headache && blurredVision) {
            hints += "pregnancy danger signs headache blurred vision referral"
        }
        if (reducedFetalMovement) {
            hints += "reduced fetal movement pregnancy danger sign referral"
        }

        return if (hints.isEmpty()) {
            rawText
        } else {
            rawText + "\n" + hints.joinToString(separator = "\n")
        }
    }

    private fun hasHeadache(text: String, language: String): Boolean {
        return shouldCheck(language, "hi", text.hasDevanagari()) && text.containsAny(
            "तेज़ सिर दर्द",
            "सिर दर्द",
            "सर दर्द"
        ) || shouldCheck(language, "es") && text.containsAny(
            "dolor de cabeza fuerte",
            "dolor de cabeza intenso"
        ) || shouldCheck(language, "sw") && text.containsAny(
            "maumivu makali ya kichwa",
            "maumivu ya kichwa"
        )
    }

    private fun hasBlurredVision(text: String, language: String): Boolean {
        return shouldCheck(language, "hi", text.hasDevanagari()) && text.containsAny(
            "धुंधला",
            "धुंधला दिख",
            "आँख",
            "आंख"
        ) || shouldCheck(language, "es") && text.containsAny(
            "ve borroso",
            "visión borrosa",
            "vision borrosa"
        ) || shouldCheck(language, "sw") && text.containsAny(
            "kuona ukungu",
            "matatizo ya macho",
            "haoni vizuri"
        )
    }

    private fun hasReducedFetalMovement(text: String, language: String): Boolean {
        return shouldCheck(language, "hi", text.hasDevanagari()) && text.containsAny(
            "बच्चे की हलचल कम",
            "हलचल कम",
            "movement कम",
            "कम movement"
        ) || shouldCheck(language, "es") && text.containsAny(
            "menos movimiento del bebé",
            "menos movimiento del bebe",
            "movimientos disminuidos",
            "movimiento fetal disminuido"
        ) || shouldCheck(language, "sw") && text.containsAny(
            "harakati za mtoto zimepungua",
            "mtoto amepungua kucheza",
            "mwendo wa mtoto umepungua"
        )
    }

    private fun highBloodPressureReading(text: String): BloodPressureReading? {
        val hasBpMarker = text.containsAny(
            "bp",
            "बीपी",
            "रक्तचाप",
            "presión arterial",
            "presion arterial",
            "shinikizo la damu"
        ) || Regex("""\bpa\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)

        if (!hasBpMarker) return null

        return BP_SLASH_REGEX.find(text)?.toReading()
            ?: BP_OVER_REGEX.find(text)?.toReading()
    }

    private fun MatchResult.toReading(): BloodPressureReading? {
        val systolic = groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val diastolic = groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        return BloodPressureReading(systolic = systolic, diastolic = diastolic)
    }

    private fun shouldCheck(
        actualLanguage: String,
        targetLanguage: String,
        scriptPresent: Boolean = false
    ): Boolean {
        return actualLanguage.isBlank() || actualLanguage == targetLanguage || scriptPresent
    }

    private fun String.containsAny(vararg phrases: String): Boolean {
        return phrases.any { contains(it) }
    }

    private fun String.hasDevanagari(): Boolean {
        return any { it in '\u0900'..'\u097F' }
    }

    private data class BloodPressureReading(
        val systolic: Int,
        val diastolic: Int
    )

    private val BP_SLASH_REGEX = Regex("""\b(\d{2,3})\s*/\s*(\d{2,3})\b""")
    private val BP_OVER_REGEX = Regex("""\b(\d{2,3})\s*(?:over|पर|sobre)\s*(\d{2,3})\b""")
}
