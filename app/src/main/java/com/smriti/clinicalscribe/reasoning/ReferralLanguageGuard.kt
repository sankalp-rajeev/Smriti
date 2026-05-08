package com.smriti.clinicalscribe.reasoning

import java.text.Normalizer

object ReferralLanguageGuard {
    fun containsReferralLikeLanguage(text: String): Boolean {
        val normalized = normalizeSpacing(text.lowercase())
        if (normalized.isBlank()) return false

        val searchable = removeNegatedRoutineContexts(normalized)
        if (searchable.isBlank()) return false

        val latinFolded = foldLatinDiacritics(searchable)
        return ENGLISH_REFERRAL_REGEX.containsMatchIn(searchable) ||
            SPANISH_REFERRAL_REGEX.containsMatchIn(latinFolded) ||
            SWAHILI_REFERRAL_REGEX.containsMatchIn(latinFolded) ||
            HINDI_REFERRAL_TERMS.any { term -> searchable.contains(term) }
    }

    private fun removeNegatedRoutineContexts(text: String): String {
        return NEGATED_ROUTINE_CONTEXTS.fold(text) { current, phrase ->
            current.replace(phrase, " ")
        }.replace(Regex("\\s+"), " ").trim()
    }

    private fun normalizeSpacing(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }

    private fun foldLatinDiacritics(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private val ENGLISH_REFERRAL_REGEX = Regex(
        "\\b(referral|refer|urgent|emergency|facility|hospital|danger signs?|escalate|immediate review)\\b"
    )
    private val SPANISH_REFERRAL_REGEX = Regex(
        "\\b(derivacion|referir|urgente|emergencia|hospital|centro de salud|signos de alarma|revision inmediata)\\b"
    )
    private val SWAHILI_REFERRAL_REGEX = Regex(
        "\\b(rufaa|haraka|dharura|hospitali|kituo cha afya|dalili za hatari|mapitio ya haraka)\\b"
    )
    private val HINDI_REFERRAL_TERMS = listOf(
        "रेफरल",
        "भेजें",
        "तुरंत",
        "आपात",
        "अस्पताल",
        "स्वास्थ्य केंद्र",
        "खतरे के संकेत",
        "तत्काल समीक्षा",
        "तत्काल"
    )
    private val NEGATED_ROUTINE_CONTEXTS = listOf(
        "no urgent danger signs",
        "no danger signs",
        "no danger sign",
        "no referral",
        "no emergency",
        "no hospital",
        "without danger signs",
        "without danger sign",
        "sin signos de alarma",
        "no signos de alarma",
        "hakuna dalili za hatari"
    )
}
