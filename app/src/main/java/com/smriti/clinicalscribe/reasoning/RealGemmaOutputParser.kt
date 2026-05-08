package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.rag.ProtocolChunk

sealed class RealGemmaParseResult {
    data class Success(val result: VisitReasoningResult) : RealGemmaParseResult()
    data class Rejected(val reason: String, val fallback: VisitReasoningResult) : RealGemmaParseResult()
}

class RealGemmaOutputParser(
    private val citationValidator: ProtocolCitationValidator = ProtocolCitationValidator()
) {
    fun parseVisitReasoning(
        rawOutput: String,
        patient: Patient,
        originalObservationText: String,
        protocolChunks: List<ProtocolChunk>
    ): RealGemmaParseResult {
        val jsonText = extractFirstJsonObject(rawOutput)
            ?: return rejected(
                if (rawOutput.trimStart().startsWith("{")) {
                    "Output was invalid JSON."
                } else {
                    "Output was not JSON and no recoverable JSON object was found."
                },
                patient,
                originalObservationText,
                protocolChunks
            )

        val json = try {
            JsonContractParser(jsonText).parseObject()
        } catch (_: IllegalArgumentException) {
            return rejected("Output was invalid JSON.", patient, originalObservationText, protocolChunks)
        }

        return if (json.containsKey("summary") || json.containsAny(CURRENT_SCHEMA_ONLY_KEYS)) {
            parseCurrentSchema(json, patient, originalObservationText, protocolChunks)
        } else {
            parseLegacySchema(json, patient, originalObservationText, protocolChunks)
        }
    }

    private fun parseCurrentSchema(
        json: Map<String, Any?>,
        patient: Patient,
        originalObservationText: String,
        protocolChunks: List<ProtocolChunk>
    ): RealGemmaParseResult {
        val summary = json.requiredString("summary")
            ?: return rejected("summary must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val referralFlagBoolean = json.booleanAlias(REFERRAL_FLAG_KEYS)
            ?: return rejected("Output missed required field: referralFlag.", patient, originalObservationText, protocolChunks)
        val referralReason = json.stringAllowingEmpty("referralReason")
            ?: return rejected("referralReason must be a string.", patient, originalObservationText, protocolChunks)
        val dangerSigns = json.stringArrayAlias(DANGER_SIGN_KEYS, requireArrayFor = "dangerSigns")
            ?: return rejected("dangerSigns must be an array.", patient, originalObservationText, protocolChunks)
        val followUpPlan = json.stringArrayAlias(FOLLOW_UP_KEYS, requireArrayFor = "followUpPlan")
            ?: return rejected("followUpPlan must be an array.", patient, originalObservationText, protocolChunks)
        val clarificationQuestion = json.stringAlias(CLARIFICATION_KEYS)
            ?: return rejected("clarificationQuestion must be a string.", patient, originalObservationText, protocolChunks)
        val citations = json.citationsArray()
            ?: return rejected("citations must be an array.", patient, originalObservationText, protocolChunks)
        val confidence = json.requiredString("confidence")
            ?: return rejected("confidence must be HIGH, MEDIUM, or LOW.", patient, originalObservationText, protocolChunks)
        val safetyNote = json.requiredString("safetyNote")
            ?: return rejected("safetyNote must be a non-empty string.", patient, originalObservationText, protocolChunks)

        if (confidence !in ALLOWED_CONFIDENCE) {
            return rejected("confidence must be HIGH, MEDIUM, or LOW.", patient, originalObservationText, protocolChunks)
        }
        if (referralFlagBoolean && citations.isEmpty()) {
            return rejected("Referral output must include at least one protocol citation.", patient, originalObservationText, protocolChunks)
        }
        if (referralFlagBoolean && referralReason.isBlank()) {
            return rejected("referralReason must be non-empty when referralFlag=true.", patient, originalObservationText, protocolChunks)
        }
        if (referralFlagBoolean && dangerSigns.isEmpty()) {
            return rejected("dangerSigns must be non-empty when referralFlag=true.", patient, originalObservationText, protocolChunks)
        }

        val referralConsistencyText = listOf(
            summary,
            referralReason,
            followUpPlan.joinToString(separator = "\n"),
            dangerSigns.joinToString(separator = "\n"),
            clarificationQuestion,
            safetyNote
        ).joinToString(separator = "\n")
        if (!referralFlagBoolean && ReferralLanguageGuard.containsReferralLikeLanguage(referralConsistencyText)) {
            return rejected("Referral-like language present while referralFlag=false.", patient, originalObservationText, protocolChunks)
        }

        val combinedSafetyText = listOf(
            summary,
            referralReason,
            dangerSigns.joinToString(),
            followUpPlan.joinToString(),
            clarificationQuestion,
            citations.joinToString(),
            safetyNote
        ).joinToString(separator = "\n")
        if (containsDiagnosticLanguage(combinedSafetyText)) {
            return rejected("Output used diagnostic language.", patient, originalObservationText, protocolChunks)
        }

        val uncertain = confidence == "LOW" || clarificationQuestion.isNotBlank()
        val citationValidation = validateCitationArray(
            citations = citations,
            protocolChunks = protocolChunks,
            uncertain = uncertain,
            hasReferral = referralFlagBoolean,
            patient = patient,
            originalObservationText = originalObservationText
        ) ?: return rejected(
            "Referral or recommendation was not grounded in a supplied protocol citation.",
            patient,
            originalObservationText,
            protocolChunks
        )

        if (referralFlagBoolean && !uncertain && followUpPlan.isNotEmpty() && citationValidation.acceptedCitation.isBlank()) {
            return rejected("Recommendation text was present without a valid protocol citation.", patient, originalObservationText, protocolChunks)
        }

        val referralFlag = if (referralFlagBoolean) {
            ReferralFlag(
                patientId = patient.id,
                urgency = citationValidation.matchedChunk?.referralLevel?.ifBlank { null } ?: "REFERRAL_REQUIRED",
                reason = referralReason,
                protocolBasis = citationValidation.acceptedCitation,
                recommendedFacility = "Nearest appropriate health facility",
                dangerSigns = dangerSigns.joinToString(separator = ", "),
                createdAtMillis = 0L
            )
        } else {
            null
        }

        return RealGemmaParseResult.Success(
            VisitReasoningResult(
                patientId = patient.id,
                observationText = originalObservationText,
                structuredNote = buildStructuredNote(
                    observationText = originalObservationText,
                    summary = summary,
                    safetyNote = safetyNote
                ),
                referralFlag = referralFlag,
                protocolCitation = citationValidation.acceptedCitation,
                suggestedFollowUp = followUpPlan.joinToString(separator = " ").ifBlank {
                    "CHW review and confirmation required before saving."
                },
                protocolChunk = citationValidation.matchedChunk,
                uncertain = uncertain,
                clarificationPrompt = clarificationQuestion.ifBlank { null }
            )
        )
    }

    private fun parseLegacySchema(
        json: Map<String, Any?>,
        patient: Patient,
        originalObservationText: String,
        protocolChunks: List<ProtocolChunk>
    ): RealGemmaParseResult {
        val missingField = LEGACY_REQUIRED_FIELDS.firstOrNull { !json.containsKey(it) }
        if (missingField != null) {
            return rejected("Output missed required field: $missingField.", patient, originalObservationText, protocolChunks)
        }

        val patientId = json.requiredString("patientId")
            ?: return rejected("patientId must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val observationText = json.requiredString("observationText")
            ?: return rejected("observationText must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val structuredNote = json.requiredString("structuredNote")
            ?: return rejected("structuredNote must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val protocolCitation = json.requiredStringAllowingEmpty("protocolCitation")
            ?: return rejected("protocolCitation must be a string.", patient, originalObservationText, protocolChunks)
        val suggestedFollowUp = json.requiredString("suggestedFollowUp")
            ?: return rejected("suggestedFollowUp must be a non-empty string.", patient, originalObservationText, protocolChunks)
        val uncertain = json["uncertain"] as? Boolean
            ?: return rejected("uncertain must be a boolean.", patient, originalObservationText, protocolChunks)
        val clarificationPrompt = json.optNullableString("clarificationPrompt")

        val referralValue = json["referralFlag"]
        val referralFlag = if (referralValue == null) {
            null
        } else {
            parseLegacyReferralFlag(referralValue, patient.id)
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

        val citationValidation = when (val validation = citationValidator.validate(
            protocolCitation = protocolCitation,
            referralProtocolBasis = referralFlag?.protocolBasis,
            protocolChunks = protocolChunks,
            uncertain = uncertain,
            hasReferral = referralFlag != null
        )) {
            is ProtocolCitationValidationResult.Accepted -> validation.validation
            is ProtocolCitationValidationResult.Rejected ->
                return rejected(validation.reason, patient, originalObservationText, protocolChunks)
        }

        if (!uncertain && suggestedFollowUp.isNotBlank() && citationValidation.acceptedCitation.isBlank()) {
            return rejected("Recommendation text was present without a valid protocol citation.", patient, originalObservationText, protocolChunks)
        }

        return RealGemmaParseResult.Success(
            VisitReasoningResult(
                patientId = patientId,
                observationText = observationText,
                structuredNote = structuredNote,
                referralFlag = referralFlag,
                protocolCitation = citationValidation.acceptedCitation,
                suggestedFollowUp = suggestedFollowUp,
                protocolChunk = citationValidation.matchedChunk,
                uncertain = uncertain,
                clarificationPrompt = clarificationPrompt
            )
        )
    }

    private fun validateCitationArray(
        citations: List<String>,
        protocolChunks: List<ProtocolChunk>,
        uncertain: Boolean,
        hasReferral: Boolean,
        patient: Patient,
        originalObservationText: String
    ): ProtocolCitationValidation? {
        val primaryCitation = citations.firstOrNull().orEmpty()
        if (!hasReferral && citations.isEmpty()) {
            return ProtocolCitationValidation(
                acceptedCitation = "",
                matchedChunk = null
            )
        }
        val allowedChunksByCitation = protocolChunks.associateBy { it.citation }
        val allowedChunksById = protocolChunks.associateBy { it.id }
        if (citations.any { it.equals(NO_MATCHING_CITATION, ignoreCase = true) || ";" in it }) {
            return null
        }
        if (citations.any { citation -> citation !in allowedChunksByCitation && citation !in allowedChunksById }) {
            return null
        }
        val primaryChunk = allowedChunksByCitation[primaryCitation] ?: allowedChunksById[primaryCitation]
        val normalizedPrimaryCitation = primaryChunk?.citation ?: primaryCitation
        return when (val validation = citationValidator.validate(
            protocolCitation = normalizedPrimaryCitation,
            referralProtocolBasis = if (hasReferral) normalizedPrimaryCitation else null,
            protocolChunks = protocolChunks,
            uncertain = uncertain,
            hasReferral = hasReferral
        )) {
            is ProtocolCitationValidationResult.Accepted -> validation.validation
            is ProtocolCitationValidationResult.Rejected -> {
                rejected(validation.reason, patient, originalObservationText, protocolChunks)
                null
            }
        }
    }

    private fun parseLegacyReferralFlag(value: Any?, patientId: String): ReferralFlag? {
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
                structuredNote = "On-device note could not be prepared: $reason Smriti does not diagnose. Health worker must review before saving.",
                referralFlag = null,
                protocolCitation = citation,
                suggestedFollowUp = "Ask the health worker to review manually and retry after setup. Health guidance: $citation",
                protocolChunk = protocolChunks.firstOrNull(),
                uncertain = true,
                clarificationPrompt = "On-device note could not be prepared safely: $reason"
            )
        )
    }

    private fun buildStructuredNote(
        observationText: String,
        summary: String,
        safetyNote: String
    ): String {
        return listOf(
            "Observation:\n${observationText.trim()}",
            "Relevant history:\nPatient history checked on this device.",
            "Local guidance support:\n${summary.trim()}\n\nSafety note: ${safetyNote.trim()}"
        ).joinToString(separator = "\n\n")
    }

    private fun Map<String, Any?>.containsAny(keys: Set<String>): Boolean {
        return keys.any { containsKey(it) }
    }

    private fun Map<*, *>.requiredString(name: String): String? {
        return (this[name] as? String)?.trim()?.ifBlank { null }
    }

    private fun Map<*, *>.requiredStringAllowingEmpty(name: String): String? {
        return (this[name] as? String)?.trim()
    }

    private fun Map<*, *>.stringAllowingEmpty(name: String): String? {
        return (this[name] as? String)?.trim()
    }

    private fun Map<*, *>.optNullableString(name: String): String? {
        return (this[name] as? String)?.trim()?.ifBlank { null }
    }

    private fun Map<String, Any?>.booleanAlias(names: List<String>): Boolean? {
        for (name in names) {
            if (!containsKey(name)) continue
            val value = this[name]
            if (name == "referral" && value !is Boolean) return null
            return value as? Boolean
        }
        return null
    }

    private fun Map<String, Any?>.stringAlias(names: List<String>): String? {
        for (name in names) {
            if (!containsKey(name)) continue
            return (this[name] as? String)?.trim()
        }
        return null
    }

    private fun Map<String, Any?>.stringArrayAlias(
        names: List<String>,
        requireArrayFor: String
    ): List<String>? {
        for (name in names) {
            if (!containsKey(name)) continue
            val value = this[name]
            if (name == requireArrayFor && value !is List<*>) return null
            return when (value) {
                is List<*> -> value.mapNotNull { (it as? String)?.trim()?.ifBlank { null } }
                is String -> listOf(value.trim()).filter { it.isNotBlank() }
                else -> null
            }
        }
        return null
    }

    private fun Map<String, Any?>.citationsArray(): List<String>? {
        for (name in CITATION_KEYS) {
            if (!containsKey(name)) continue
            val value = this[name]
            if (name == "citations" && value !is List<*>) return null
            return when (value) {
                is List<*> -> value.mapNotNull { (it as? String)?.trim()?.ifBlank { null } }
                is String -> listOf(value.trim()).filter { it.isNotBlank() }
                else -> null
            }
        }
        return null
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

    private fun extractFirstJsonObject(rawOutput: String): String? {
        val raw = rawOutput.trim()
        var start = -1
        var depth = 0
        var inString = false
        var escaped = false

        for (index in raw.indices) {
            val char = raw[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                '{' -> {
                    if (depth == 0) start = index
                    depth += 1
                }
                '}' -> {
                    if (depth == 0) return null
                    depth -= 1
                    if (depth == 0 && start >= 0) {
                        return raw.substring(start, index + 1)
                    }
                }
            }
        }
        return null
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
                '-', in '0'..'9' -> parseNumber()
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

        private fun parseNumber(): Number {
            val start = index
            if (peek('-')) index++
            readDigits()
            if (peek('.')) {
                index++
                readDigits()
            }
            if (peek('e') || peek('E')) {
                index++
                if (peek('+') || peek('-')) index++
                readDigits()
            }
            val rawNumber = input.substring(start, index)
            return if (rawNumber.contains('.') || rawNumber.contains('e', ignoreCase = true)) {
                rawNumber.toDoubleOrNull() ?: throw IllegalArgumentException("Bad number")
            } else {
                rawNumber.toLongOrNull() ?: throw IllegalArgumentException("Bad number")
            }
        }

        private fun readDigits() {
            val start = index
            while (index < input.length && input[index].isDigit()) {
                index++
            }
            if (start == index) throw IllegalArgumentException("Expected number digit")
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
        val REFERRAL_FLAG_KEYS = listOf("referralFlag", "referral_required", "referral_flag", "needsReferral", "referral")
        val CITATION_KEYS = listOf("citations", "cite", "citation", "protocolCitations")
        val FOLLOW_UP_KEYS = listOf("followUpPlan", "follow_up_plan")
        val CLARIFICATION_KEYS = listOf("clarificationQuestion", "clarification_question")
        val DANGER_SIGN_KEYS = listOf("dangerSigns", "danger_signs")
        val CURRENT_SCHEMA_ONLY_KEYS = (
            CITATION_KEYS + FOLLOW_UP_KEYS + CLARIFICATION_KEYS + DANGER_SIGN_KEYS +
                listOf("referralReason", "confidence", "safetyNote")
        ).toSet()
        val ALLOWED_CONFIDENCE = setOf("HIGH", "MEDIUM", "LOW")
        val LEGACY_REQUIRED_FIELDS = listOf(
            "patientId",
            "observationText",
            "structuredNote",
            "protocolCitation",
            "suggestedFollowUp",
            "uncertain",
            "referralFlag"
        )
        const val NO_MATCHING_CITATION = "No matching protocol citation"
    }
}
