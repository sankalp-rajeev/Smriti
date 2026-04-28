package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.rag.ProtocolChunk

data class ProtocolCitationValidation(
    val acceptedCitation: String,
    val matchedChunk: ProtocolChunk?
)

class ProtocolCitationValidator {
    fun validate(
        protocolCitation: String,
        referralProtocolBasis: String?,
        protocolChunks: List<ProtocolChunk>,
        uncertain: Boolean,
        hasReferral: Boolean
    ): ProtocolCitationValidationResult {
        val citation = protocolCitation.trim()
        val referralBasis = referralProtocolBasis?.trim()

        if (citation.equals(NO_MATCHING_CITATION, ignoreCase = true)) {
            return ProtocolCitationValidationResult.Rejected(
                "\"$NO_MATCHING_CITATION\" is not valid model output; use an empty citation with uncertain=true."
            )
        }

        if (protocolChunks.isEmpty()) {
            return when {
                citation.isNotEmpty() ->
                    ProtocolCitationValidationResult.Rejected("Output invented a protocol citation when no protocol chunk was supplied.")
                hasReferral ->
                    ProtocolCitationValidationResult.Rejected("Referral was present when no protocol chunk was supplied.")
                !uncertain ->
                    ProtocolCitationValidationResult.Rejected("No-protocol output must set uncertain=true.")
                else ->
                    ProtocolCitationValidationResult.Accepted(
                        ProtocolCitationValidation(
                            acceptedCitation = "",
                            matchedChunk = null
                        )
                    )
            }
        }

        val allowed = protocolChunks.associateBy { it.citation }
        val matchedChunk = allowed[citation]
            ?: return ProtocolCitationValidationResult.Rejected(
                "protocolCitation must exactly match one supplied protocol citation."
            )

        if (referralBasis != null && referralBasis !in allowed) {
            return ProtocolCitationValidationResult.Rejected(
                "Referral or recommendation was not grounded in a supplied protocol citation."
            )
        }

        return ProtocolCitationValidationResult.Accepted(
            ProtocolCitationValidation(
                acceptedCitation = citation,
                matchedChunk = matchedChunk
            )
        )
    }

    private companion object {
        const val NO_MATCHING_CITATION = "No matching protocol citation"
    }
}

sealed class ProtocolCitationValidationResult {
    data class Accepted(val validation: ProtocolCitationValidation) : ProtocolCitationValidationResult()
    data class Rejected(val reason: String) : ProtocolCitationValidationResult()
}
