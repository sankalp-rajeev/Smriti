package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.rag.ProtocolChunk

sealed class RealGemmaParseResult {
    data class Success(val result: VisitReasoningResult) : RealGemmaParseResult()
    data class Rejected(val reason: String, val fallback: VisitReasoningResult) : RealGemmaParseResult()
}

class RealGemmaOutputParser {
    fun parseVisitReasoning(
        rawOutput: String,
        patient: Patient,
        originalObservationText: String,
        protocolChunks: List<ProtocolChunk>
    ): RealGemmaParseResult {
        val trimmed = rawOutput.trim()
        if (!trimmed.startsWith("{")) {
            return rejected("Output was not a single compact JSON object.", patient, originalObservationText, protocolChunks)
        }

        val json = try {
            JsonContractParser(trimmed).parseObject()
        } catch (_: IllegalArgumentException) {
            return rejected("Output was invalid JSON.", patient, originalObservationText, protocolChunks)
        }

        val missingField = REQUIRED_FIELDS.firstOrNull { !json.containsKey(it) }
        if (missingField != null) {
            return rejected("Output missed required field: $missingField.", patient, originalObservationText, protocolChunks)
        }

        val patientId = json.requiredString("patientId")
            ?: return rejected("patientId must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val observationText = json.requiredString("observationText")
            ?: return rejected("observationText must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val structuredNote = json.requiredString("structuredNote")
            ?: return rejected("structuredNote must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val protocolCitation = json.requiredString("protocolCitation")
            ?: return rejected("protocolCitation must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val suggestedFollowUp = json.requiredString("suggestedFollowUp")
            ?: return rejected("suggestedFollowUp must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val uncertain = json["uncertain"] as? Boolean
        if (uncertain == null) {
            return rejected("uncertain must be a boolean.", patient, originalObservationText, protocolChunks)
        }
        val clarificationPrompt = json.optNullableString("clarificationPrompt")

        val referralValue = json["referralFlag"]
        val referralFlag = if (referralValue == null) {
            null
        } else {
            parseReferralFlag(referralValue, patient.id)
                ?: return rejected("referralFlag must be null or a complete object.", patient, originalObservationText, protocolChunks)
        }

        val combinedSafetyText = listOf(
            structuredNote,
            protocolCitation,
            suggestedFollowUp,
            referralFlag?.reason.orEmpty(),
            referralFlag?.protocolBasis.orEmpty()
        ).joinToString(separator = "\n")
        if (containsDiagnosticLanguage(combinedSafetyText)) {
            return rejected("Output used diagnostic language.", patient, originalObservationText, protocolChunks)
        }

        val allowedCitations = protocolChunks.map { it.citation }.toSet()
        val noProtocolCitation = protocolCitation.equals(NO_MATCHING_CITATION, ignoreCase = true)
        if (protocolChunks.isEmpty()) {
            if (!noProtocolCitation || referralFlag != null) {
                return rejected("Output invented a protocol citation when no protocol chunk was supplied.", patient, originalObservationText, protocolChunks)
            }
        } else {
            if (protocolCitation !in allowedCitations) {
                return rejected("protocolCitation did not match a supplied protocol chunk.", patient, originalObservationText, protocolChunks)
            }
            if (referralFlag != null && referralFlag.protocolBasis !in allowedCitations) {
                return rejected("Referral or recommendation was not grounded in a supplied protocol citation.", patient, originalObservationText, protocolChunks)
            }
        }

        if (!uncertain && suggestedFollowUp.isNotBlank() && (protocolChunks.isEmpty() || noProtocolCitation)) {
            return rejected("Recommendation text was present without a valid protocol citation.", patient, originalObservationText, protocolChunks)
        }

        val matchedChunk = protocolChunks.firstOrNull { it.citation == protocolCitation }
        return RealGemmaParseResult.Success(
            VisitReasoningResult(
                patientId = patientId,
                observationText = observationText,
                structuredNote = structuredNote,
                referralFlag = referralFlag,
                protocolCitation = protocolCitation,
                suggestedFollowUp = suggestedFollowUp,
                protocolChunk = matchedChunk,
                uncertain = uncertain,
                clarificationPrompt = clarificationPrompt
            )
        )
    }

    private fun parseReferralFlag(value: Any?, patientId: String): ReferralFlag? {
        val json = value as? Map<*, *> ?: return null
        val urgency = json.requiredString("urgency") ?: return null
        val reason = json.requiredString("reason") ?: return null
        val protocolBasis = json.requiredString("protocolBasis") ?: return null
        val recommendedFacility = json.requiredString("recommendedFacility") ?: return null
        val dangerSignsJson = json["dangerSigns"] as? List<*> ?: return null
        val dangerSigns = dangerSignsJson.mapNotNull { value ->
            (value as? String)?.trim()?.ifBlank { null }
        }.joinToString(separator = ", ")
        if (dangerSigns.isBlank()) return null

        return ReferralFlag(
            patientId = patientId,
            urgency = urgency,
            reason = reason,
            protocolBasis = protocolBasis,
            recommendedFacility = recommendedFacility,
            dangerSigns = dangerSigns,
            createdAtMillis = 0L
        )
    }

    private fun rejected(
        reason: String,
        patient: Patient,
        originalObservationText: String,
        protocolChunks: List<ProtocolChunk>
    ): RealGemmaParseResult.Rejected {
        val citation = if (protocolChunks.isEmpty()) {
            NO_MATCHING_CITATION
        } else {
            protocolChunks.first().citation
        }
        return RealGemmaParseResult.Rejected(
            reason = reason,
            fallback = VisitReasoningResult(
                patientId = patient.id,
                observationText = originalObservationText,
                structuredNote = "Real Gemma output rejected: $reason This is not a diagnosis. CHW confirmation required.",
                referralFlag = null,
                protocolCitation = citation,
                suggestedFollowUp = "Use MockGemmaAgent fallback or ask the CHW to review manually. Protocol citation: $citation",
                protocolChunk = protocolChunks.firstOrNull(),
                uncertain = true,
                clarificationPrompt = "Real Gemma output was rejected safely: $reason"
            )
        )
    }

    private fun Map<*, *>.requiredString(name: String): String? {
        return (this[name] as? String)?.trim()?.ifBlank { null }
    }

    private fun Map<*, *>.optNullableString(name: String): String? {
        return (this[name] as? String)?.trim()?.ifBlank { null }
    }

    private fun containsDiagnosticLanguage(text: String): Boolean {
        val lower = text.lowercase()
        val unsafePhrases = listOf(
            "diagnosis:",
            "diagnosis is",
            "diagnosed",
            "patient has preeclampsia",
            "patient has eclampsia",
            "patient has hypertension",
            "has preeclampsia",
            "has eclampsia"
        )
        return unsafePhrases.any { lower.contains(it) } ||
            Regex("\\b(has|have|having)\\s+(preeclampsia|eclampsia|hypertension)\\b").containsMatchIn(lower)
    }

    private class JsonContractParser(private val input: String) {
        private var index = 0

        fun parseObject(): Map<String, Any?> {
            val value = parseValue()
            skipWhitespace()
            if (index != input.length) throw IllegalArgumentException("Trailing content")
            val rootObject = value as? Map<*, *> ?: throw IllegalArgumentException("Root was not an object")
            return rootObject.toStringKeyMap()
        }

        private fun Map<*, *>.toStringKeyMap(): Map<String, Any?> {
            return buildMap {
                for ((key, value) in this@toStringKeyMap) {
                    if (key !is String) throw IllegalArgumentException("Object key was not a string")
                    put(key, value)
                }
            }
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            if (index >= input.length) throw IllegalArgumentException("Unexpected end")
            return when (input[index]) {
                '{' -> parseObjectValue()
                '[' -> parseArrayValue()
                '"' -> parseString()
                't' -> parseLiteral("true", true)
                'f' -> parseLiteral("false", false)
                'n' -> parseLiteral("null", null)
                else -> throw IllegalArgumentException("Unsupported JSON token")
            }
        }

        private fun parseObjectValue(): Map<String, Any?> {
            expect('{')
            skipWhitespace()
            val values = linkedMapOf<String, Any?>()
            if (peek('}')) {
                index++
                return values
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                values[key] = parseValue()
                skipWhitespace()
                when {
                    peek(',') -> index++
                    peek('}') -> {
                        index++
                        return values
                    }
                    else -> throw IllegalArgumentException("Expected object separator")
                }
            }
        }

        private fun parseArrayValue(): List<Any?> {
            expect('[')
            skipWhitespace()
            val values = mutableListOf<Any?>()
            if (peek(']')) {
                index++
                return values
            }
            while (true) {
                values += parseValue()
                skipWhitespace()
                when {
                    peek(',') -> index++
                    peek(']') -> {
                        index++
                        return values
                    }
                    else -> throw IllegalArgumentException("Expected array separator")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val builder = StringBuilder()
            while (index < input.length) {
                val char = input[index++]
                when (char) {
                    '"' -> return builder.toString()
                    '\\' -> builder.append(parseEscape())
                    else -> builder.append(char)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun parseEscape(): Char {
            if (index >= input.length) throw IllegalArgumentException("Unterminated escape")
            return when (val escaped = input[index++]) {
                '"', '\\', '/' -> escaped
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> parseUnicodeEscape()
                else -> throw IllegalArgumentException("Unsupported escape")
            }
        }

        private fun parseUnicodeEscape(): Char {
            if (index + 4 > input.length) throw IllegalArgumentException("Bad unicode escape")
            val hex = input.substring(index, index + 4)
            index += 4
            return hex.toIntOrNull(16)?.toChar() ?: throw IllegalArgumentException("Bad unicode escape")
        }

        private fun parseLiteral(literal: String, value: Any?): Any? {
            if (!input.startsWith(literal, index)) throw IllegalArgumentException("Bad literal")
            index += literal.length
            return value
        }

        private fun skipWhitespace() {
            while (index < input.length && input[index].isWhitespace()) {
                index++
            }
        }

        private fun expect(char: Char) {
            if (!peek(char)) throw IllegalArgumentException("Expected $char")
            index++
        }

        private fun peek(char: Char): Boolean {
            return index < input.length && input[index] == char
        }
    }

    private companion object {
        val REQUIRED_FIELDS = listOf(
            "patientId",
            "observationText",
            "structuredNote",
            "protocolCitation",
            "suggestedFollowUp",
            "uncertain",
            "clarificationPrompt",
            "referralFlag"
        )
        const val NO_MATCHING_CITATION = "No matching protocol citation"
    }
}
