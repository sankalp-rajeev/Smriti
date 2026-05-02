package com.smriti.clinicalscribe.reasoning

class PaperNoteVisionParser {
    fun parse(rawOutput: String): PaperNoteVisionParseResult {
        val jsonText = extractFirstJsonObject(rawOutput)
            ?: return PaperNoteVisionParseResult.Rejected("Output was not JSON.")
        val json = try {
            JsonContractParser(jsonText).parseObject()
        } catch (_: IllegalArgumentException) {
            return PaperNoteVisionParseResult.Rejected("Output was invalid JSON.")
        }

        val needsReview = when {
            !json.containsKey("needsReview") ->
                return PaperNoteVisionParseResult.Rejected("needsReview is required.")
            json["needsReview"] != true ->
                return PaperNoteVisionParseResult.Rejected("needsReview must be true.")
            else -> true
        }
        val confidence = PaperNoteVisionConfidence.fromRaw(json.stringValue("confidence").orEmpty())
            ?: return PaperNoteVisionParseResult.Rejected("confidence must be HIGH, MEDIUM, or LOW.")
        val patientName = json.stringValue("patientName")
            ?: return PaperNoteVisionParseResult.Rejected("patientName must be a string.")
        val visitDate = json.stringValue("visitDate")
            ?: return PaperNoteVisionParseResult.Rejected("visitDate must be a string.")
        val bloodPressure = json.stringValue("bloodPressure")
            ?: return PaperNoteVisionParseResult.Rejected("bloodPressure must be a string.")
        val symptoms = json.stringArray("symptoms")
            ?: return PaperNoteVisionParseResult.Rejected("symptoms must be an array of strings.")
        val followUpPlan = json.stringValue("followUpPlan")
            ?: return PaperNoteVisionParseResult.Rejected("followUpPlan must be a string.")
        val safetyNote = json.stringValue("safetyNote")
            ?: return PaperNoteVisionParseResult.Rejected("safetyNote must be a string.")

        if (confidence != PaperNoteVisionConfidence.LOW) {
            val hasAnyExtractedField = listOf(patientName, visitDate, bloodPressure, followUpPlan)
                .any { it.isNotBlank() } || symptoms.isNotEmpty()
            if (!hasAnyExtractedField) {
                return PaperNoteVisionParseResult.Rejected("Non-low confidence extraction cannot be blank.")
            }
        }

        val combined = listOf(
            patientName,
            visitDate,
            bloodPressure,
            symptoms.joinToString(),
            followUpPlan,
            safetyNote
        ).joinToString(separator = "\n")
        if (containsDiagnosticLanguage(combined)) {
            return PaperNoteVisionParseResult.Rejected("Output used diagnostic language.")
        }
        if (containsReferralAdvice(combined)) {
            return PaperNoteVisionParseResult.Rejected("Image extraction cannot include referral advice.")
        }
        if (containsTreatmentRecommendation(combined)) {
            return PaperNoteVisionParseResult.Rejected("Image extraction cannot add treatment recommendations.")
        }

        return PaperNoteVisionParseResult.Success(
            PaperNoteVisionExtraction(
                patientName = patientName,
                visitDate = visitDate,
                bloodPressure = bloodPressure,
                symptoms = symptoms,
                followUpPlan = followUpPlan,
                confidence = confidence,
                needsReview = needsReview,
                safetyNote = safetyNote.ifBlank {
                    REQUIRED_SAFETY_NOTE
                }
            )
        )
    }

    private fun Map<String, Any?>.stringValue(name: String): String? {
        return (this[name] as? String)?.trim()
    }

    private fun Map<String, Any?>.stringArray(name: String): List<String>? {
        val value = this[name] as? List<*> ?: return null
        return value.mapNotNull { (it as? String)?.trim()?.ifBlank { null } }
    }

    private fun containsDiagnosticLanguage(text: String): Boolean {
        val lower = text.lowercase()
        return DIAGNOSTIC_PHRASES.any { lower.contains(it) } ||
            Regex("\\b(has|have|having|diagnosed with)\\s+(preeclampsia|eclampsia|hypertension|anemia)\\b")
                .containsMatchIn(lower)
    }

    private fun containsReferralAdvice(text: String): Boolean {
        val lower = text.lowercase()
        return REFERRAL_PHRASES.any { lower.contains(it) }
    }

    private fun containsTreatmentRecommendation(text: String): Boolean {
        val lower = text.lowercase()
        return TREATMENT_PHRASES.any { lower.contains(it) }
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
            val root = value as? Map<*, *> ?: throw IllegalArgumentException("Root was not an object")
            return buildMap {
                for ((key, item) in root) {
                    if (key !is String) throw IllegalArgumentException("Object key was not a string")
                    put(key, item)
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
                else -> throw IllegalArgumentException("Unsupported token")
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
                when (val char = input[index++]) {
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
            val rawNumber = input.substring(start, index)
            return rawNumber.toDoubleOrNull() ?: throw IllegalArgumentException("Bad number")
        }

        private fun readDigits() {
            val start = index
            while (index < input.length && input[index].isDigit()) index++
            if (start == index) throw IllegalArgumentException("Expected digit")
        }

        private fun skipWhitespace() {
            while (index < input.length && input[index].isWhitespace()) index++
        }

        private fun expect(char: Char) {
            if (!peek(char)) throw IllegalArgumentException("Expected $char")
            index++
        }

        private fun peek(char: Char): Boolean {
            return index < input.length && input[index] == char
        }
    }

    companion object {
        const val REQUIRED_SAFETY_NOTE = "Extracted from image. Health worker must review before saving."
        private val DIAGNOSTIC_PHRASES = listOf(
            "diagnosis:",
            "diagnosis is",
            "diagnosed",
            "patient has preeclampsia",
            "patient has eclampsia",
            "patient has hypertension"
        )
        private val REFERRAL_PHRASES = listOf(
            "refer immediately",
            "urgent referral",
            "same-day referral",
            "needs referral",
            "referral required",
            "send to hospital",
            "go to hospital now"
        )
        private val TREATMENT_PHRASES = listOf(
            "start medication",
            "give medicine",
            "prescribe",
            "administer",
            "treat with",
            "begin treatment"
        )
    }
}
