package com.smriti.clinicalscribe.reasoning

import com.smriti.clinicalscribe.data.HistorySignal
import com.smriti.clinicalscribe.data.MissedFollowUpAlert
import com.smriti.clinicalscribe.data.Patient
import com.smriti.clinicalscribe.data.ReferralFlag
import com.smriti.clinicalscribe.data.VisitLog

data class SupervisorPriorityItem(
    val patientId: String,
    val patientName: String,
    val urgency: String,
    val reason: String,
    val protocolBasis: String,
    val nonDiagnosticSafety: String
)

data class SupervisorPriorityQueue(
    val items: List<SupervisorPriorityItem>
)

sealed interface SupervisorPriorityQueueResult {
    data class Available(val queue: SupervisorPriorityQueue) : SupervisorPriorityQueueResult
    data class Unavailable(val reason: String) : SupervisorPriorityQueueResult
}

sealed interface SupervisorPriorityParseResult {
    data class Success(val queue: SupervisorPriorityQueue) : SupervisorPriorityParseResult
    data class Rejected(val reason: String) : SupervisorPriorityParseResult
}

class SupervisorPriorityPromptBuilder {
    fun buildPrompt(
        patients: List<Patient>,
        todayVisits: List<VisitLog>,
        referrals: List<ReferralFlag>,
        missedFollowUps: List<MissedFollowUpAlert>,
        historySignals: List<HistorySignal>
    ): String {
        val patientsById = patients.associateBy { it.id }
        val patientContext = patients.joinToString(separator = "\n") { patient ->
            "- id=${patient.id}; name=${patient.name}; country=${patient.country}; language=${patient.preferredLanguage}; scenario=${patient.scenarioPreview}"
        }.ifBlank { "- No patients supplied." }
        val visitsContext = todayVisits.joinToString(separator = "\n") { visit ->
            "- patientId=${visit.patientId}; note=${visit.structuredNote.compact()}; followUp=${visit.suggestedFollowUp.compact()}; citation=${visit.protocolCitation}"
        }.ifBlank { "- No confirmed visits from today." }
        val referralsContext = referrals.joinToString(separator = "\n") { referral ->
            val patientName = patientsById[referral.patientId]?.name ?: referral.patientId
            "- patientId=${referral.patientId}; patientName=$patientName; urgency=${referral.urgency}; signs=${referral.dangerSigns}; basis=${referral.protocolBasis}"
        }.ifBlank { "- No saved referral flags." }
        val missedFollowUpContext = missedFollowUps.joinToString(separator = "\n") { alert ->
            val patientName = patientsById[alert.patientId]?.name ?: alert.patientId
            "- patientId=${alert.patientId}; patientName=$patientName; daysOverdue=${alert.daysOverdue}; reason=${alert.reason}; basis=${alert.protocolCitation}"
        }.ifBlank { "- No missed follow-up alerts." }
        val historySignalContext = historySignals.joinToString(separator = "\n") { signal ->
            val patientName = patientsById[signal.patientId]?.name ?: signal.patientId
            "- patientId=${signal.patientId}; patientName=$patientName; signal=${signal.message}; readings=${signal.readings.joinToString(" -> ") { it.label }}"
        }.ifBlank { "- No history signals." }
        val allowedCitations = (todayVisits.map { it.protocolCitation } +
            referrals.map { it.protocolBasis } +
            missedFollowUps.map { it.protocolCitation })
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "No matching protocol citation" }
            .distinct()
            .joinToString(separator = "\n") { "- $it" }
            .ifBlank { "- No supplied protocol citation; use empty protocolBasis." }

        return """
            You are Smriti, an offline supervisor prioritization assistant.

            Safety rules:
            - This is not a diagnosis.
            - Rank follow-up attention only from supplied local data.
            - Do not invent patients, findings, protocols, or citations.
            - Use only urgency values IMMEDIATE, WITHIN_24H, or ROUTINE.
            - protocolBasis must be one supplied citation below, or "" if no citation applies.
            - nonDiagnosticSafety must include "not a diagnosis".
            - Avoid diagnostic wording such as "has preeclampsia", "has eclampsia", "diagnosed", or "patient has hypertension".
            - Return compact JSON only, one object, no markdown.

            Patients:
            $patientContext

            Today's confirmed local visits:
            $visitsContext

            Saved referral flags:
            $referralsContext

            Missed follow-up alerts:
            $missedFollowUpContext

            History signals:
            $historySignalContext

            Supplied protocol citations:
            $allowedCitations

            Required JSON shape:
            {"items":[{"patientId":"string","patientName":"string","urgency":"IMMEDIATE|WITHIN_24H|ROUTINE","reason":"plain language","protocolBasis":"supplied citation or empty","nonDiagnosticSafety":"must include not a diagnosis"}]}

            Return at most 5 items, ranked highest priority first.
        """.trimIndent()
    }

    private fun String.compact(): String {
        return replace(Regex("\\s+"), " ").trim().take(160)
    }
}

class SupervisorPriorityParser {
    fun parse(
        rawOutput: String,
        suppliedProtocolCitations: Set<String>
    ): SupervisorPriorityParseResult {
        val trimmed = rawOutput.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return SupervisorPriorityParseResult.Rejected("Output was not a single JSON object.")
        }
        val json = try {
            JsonParser(trimmed).parseObject()
        } catch (_: IllegalArgumentException) {
            return SupervisorPriorityParseResult.Rejected("Output was invalid JSON.")
        }
        val itemsJson = json["items"] as? List<*> ?: return SupervisorPriorityParseResult.Rejected("items must be an array.")
        val items = mutableListOf<SupervisorPriorityItem>()
        for (value in itemsJson) {
            val itemJson = value as? Map<*, *> ?: return SupervisorPriorityParseResult.Rejected("Each item must be an object.")
            val item = parseItem(itemJson, suppliedProtocolCitations)
                ?: return SupervisorPriorityParseResult.Rejected("Priority item failed validation.")
            items += item
        }
        if (items.size > 5) {
            return SupervisorPriorityParseResult.Rejected("Priority queue returned too many items.")
        }
        return SupervisorPriorityParseResult.Success(SupervisorPriorityQueue(items))
    }

    private fun parseItem(
        itemJson: Map<*, *>,
        suppliedProtocolCitations: Set<String>
    ): SupervisorPriorityItem? {
        val patientId = itemJson.requiredString("patientId") ?: return null
        val patientName = itemJson.requiredString("patientName") ?: return null
        val urgency = itemJson.requiredString("urgency") ?: return null
        if (urgency !in ALLOWED_URGENCY) return null
        val reason = itemJson.requiredString("reason") ?: return null
        val protocolBasis = itemJson.requiredStringAllowingEmpty("protocolBasis") ?: return null
        val nonDiagnosticSafety = itemJson.requiredString("nonDiagnosticSafety") ?: return null
        val combined = listOf(patientName, reason, protocolBasis, nonDiagnosticSafety).joinToString("\n")
        if (containsDiagnosticLanguage(combined)) return null
        if (!nonDiagnosticSafety.lowercase().contains("not a diagnosis")) return null
        if (protocolBasis.isNotBlank() && protocolBasis !in suppliedProtocolCitations) return null
        return SupervisorPriorityItem(
            patientId = patientId,
            patientName = patientName,
            urgency = urgency,
            reason = reason,
            protocolBasis = protocolBasis,
            nonDiagnosticSafety = nonDiagnosticSafety
        )
    }

    private fun Map<*, *>.requiredString(name: String): String? {
        return (this[name] as? String)?.trim()?.ifBlank { null }
    }

    private fun Map<*, *>.requiredStringAllowingEmpty(name: String): String? {
        return (this[name] as? String)?.trim()
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

    private class JsonParser(private val input: String) {
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
            val rawNumber = input.substring(start, index)
            return rawNumber.toLongOrNull() ?: rawNumber.toDoubleOrNull() ?: throw IllegalArgumentException("Bad number")
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
        val ALLOWED_URGENCY = setOf("IMMEDIATE", "WITHIN_24H", "ROUTINE")
    }
}

class SupervisorPriorityQueueGenerator(
    private val textClient: RealGemmaTextClient,
    private val promptBuilder: SupervisorPriorityPromptBuilder = SupervisorPriorityPromptBuilder(),
    private val parser: SupervisorPriorityParser = SupervisorPriorityParser()
) {
    suspend fun generate(
        patients: List<Patient>,
        todayVisits: List<VisitLog>,
        referrals: List<ReferralFlag>,
        missedFollowUps: List<MissedFollowUpAlert>,
        historySignals: List<HistorySignal>
    ): SupervisorPriorityQueueResult {
        val prompt = SmritiLatencyLogger.measure("supervisorPromptBuild", "supervisor") {
            promptBuilder.buildPrompt(
                patients = patients,
                todayVisits = todayVisits,
                referrals = referrals,
                missedFollowUps = missedFollowUps,
                historySignals = historySignals
            )
        }
        val generation = try {
            var result: TextGenerationResult? = null
            val duration = kotlin.system.measureTimeMillis {
                result = textClient.generateText(prompt)
            }
            SmritiLatencyLogger.log("realGemmaGenerateCall", duration, "supervisor")
            result ?: TextGenerationResult.Failed("Supervisor generation returned no result.")
        } catch (_: RuntimeException) {
            return SupervisorPriorityQueueResult.Unavailable(UNAVAILABLE_MESSAGE)
        }
        val suppliedCitations = (todayVisits.map { it.protocolCitation } +
            referrals.map { it.protocolBasis } +
            missedFollowUps.map { it.protocolCitation })
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "No matching protocol citation" }
            .toSet()
        return when (generation) {
            is TextGenerationResult.Success -> {
                val parsed = SmritiLatencyLogger.measure("supervisorParseSafetyCitationValidation", "supervisor") {
                    parser.parse(generation.text, suppliedCitations)
                }
                when (parsed) {
                    is SupervisorPriorityParseResult.Success -> SupervisorPriorityQueueResult.Available(parsed.queue)
                    is SupervisorPriorityParseResult.Rejected -> SupervisorPriorityQueueResult.Unavailable(
                        "$UNAVAILABLE_MESSAGE Invalid or uncited RealGemma priority output."
                    )
                }
            }
            is TextGenerationResult.Unavailable -> SupervisorPriorityQueueResult.Unavailable(
                "$UNAVAILABLE_MESSAGE ${generation.status}"
            )
            is TextGenerationResult.Failed -> SupervisorPriorityQueueResult.Unavailable(
                "$UNAVAILABLE_MESSAGE ${generation.error}"
            )
        }
    }

    private companion object {
        const val UNAVAILABLE_MESSAGE = "On-device RealGemma supervisor reasoning unavailable — please retry."
    }
}

