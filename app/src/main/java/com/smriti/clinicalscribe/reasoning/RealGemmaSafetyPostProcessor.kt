package com.smriti.clinicalscribe.reasoning

class RealGemmaSafetyPostProcessor {
    fun enforce(result: VisitReasoningResult): VisitReasoningResult {
        val requiredPhrases = result.structuredNote.missingSafetyPhrases()
        if (requiredPhrases.isEmpty()) {
            return result
        }

        return result.copy(
            structuredNote = buildString {
                append(result.structuredNote.trim())
                append("\n\nSafety note: ")
                append(requiredPhrases.joinToString(separator = " "))
            }
        )
    }

    private fun String.missingSafetyPhrases(): List<String> {
        val lower = lowercase()
        return buildList {
            if (!containsNonDiagnosticWording(lower)) {
                add(NOT_DIAGNOSIS)
            }
            if (!containsChwConfirmation(lower)) {
                add(CHW_CONFIRMATION)
            }
        }
    }

    private fun containsNonDiagnosticWording(lower: String): Boolean {
        return lower.contains("not a diagnosis") ||
            lower.contains("no diagnosis generated")
    }

    private fun containsChwConfirmation(lower: String): Boolean {
        return (lower.contains("chw") && lower.contains("confirm")) ||
            lower.contains("confirmation required")
    }

    private companion object {
        const val NOT_DIAGNOSIS = "This is not a diagnosis."
        const val CHW_CONFIRMATION = "CHW confirmation is required before saving."
    }
}
