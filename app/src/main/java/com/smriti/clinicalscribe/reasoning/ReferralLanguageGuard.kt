package com.smriti.clinicalscribe.reasoning

object ReferralLanguageGuard {
    fun containsReferralLikeLanguage(text: String): Boolean {
        val normalized = text.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isBlank()) return false

        return REFERRAL_LIKE_TERMS.any { term -> normalized.contains(term) }
    }

    private val REFERRAL_LIKE_TERMS = listOf(
        "referral",
        "refer",
        "urgent",
        "emergency",
        "hospital transfer",
        "immediate review",
        "रेफरल",
        "तत्काल",
        "आपात",
        "अस्पताल",
        "तुरंत",
        "referencia",
        "derivación",
        "derivacion",
        "urgente",
        "emergencia",
        "hospital",
        "traslado",
        "rufaa",
        "dharura",
        "hospitali",
        "uhamisho",
        "haraka"
    )
}
