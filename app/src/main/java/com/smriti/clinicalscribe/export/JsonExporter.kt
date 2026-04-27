package com.smriti.clinicalscribe.export

import android.content.Context
import com.smriti.clinicalscribe.audio.VoiceNoteMetadata
import com.smriti.clinicalscribe.reasoning.SupervisorSummary
import com.smriti.clinicalscribe.reasoning.VisitReasoningResult
import java.io.File

class JsonExporter(private val exportDir: File) {
    init {
        exportDir.mkdirs()
    }

    fun exportVisit(
        result: VisitReasoningResult,
        editedNote: String,
        editedFollowUp: String,
        voiceNote: VoiceNoteMetadata?
    ): File {
        val file = File(exportDir, "visit_${result.patientId}_${System.currentTimeMillis()}.json")
        file.writeText(visitJson(result, editedNote, editedFollowUp, voiceNote))
        return file
    }

    fun exportSummary(summary: SupervisorSummary): File {
        val file = File(exportDir, "summary_${System.currentTimeMillis()}.json")
        file.writeText(summaryJson(summary))
        return file
    }

    companion object {
        fun appPrivate(context: Context): JsonExporter {
            return JsonExporter(File(context.filesDir, "exports"))
        }

        fun visitJson(
            result: VisitReasoningResult,
            editedNote: String,
            editedFollowUp: String,
            voiceNote: VoiceNoteMetadata?
        ): String {
            val referral = result.referralFlag
            return buildString {
                append("{\n")
                append("  \"patient_id\": \"${result.patientId.escapeJson()}\",\n")
                append("  \"observation_text\": \"${result.observationText.escapeJson()}\",\n")
                append("  \"structured_note\": \"${editedNote.escapeJson()}\",\n")
                append("  \"protocol_citation\": \"${result.protocolCitation.escapeJson()}\",\n")
                append("  \"suggested_follow_up\": \"${editedFollowUp.escapeJson()}\",\n")
                append("  \"uncertain\": ${result.uncertain},\n")
                append("  \"safety\": \"Not a diagnosis. CHW confirmation required.\",\n")
                append("  \"voice_note\": ")
                if (voiceNote == null) {
                    append("null,\n")
                } else {
                    append("{\n")
                    append("    \"audio_file_path\": \"${voiceNote.audioFilePath.escapeJson()}\",\n")
                    append("    \"audio_duration_seconds\": ${voiceNote.audioDurationSeconds},\n")
                    append("    \"transcript_source\": \"REAL_ASR_PENDING\"\n")
                    append("  },\n")
                }
                append("  \"referral_flag\": ")
                if (referral == null) {
                    append("null\n")
                } else {
                    append("{\n")
                    append("    \"urgency\": \"${referral.urgency.escapeJson()}\",\n")
                    append("    \"reason\": \"${referral.reason.escapeJson()}\",\n")
                    append("    \"protocol_basis\": \"${referral.protocolBasis.escapeJson()}\",\n")
                    append("    \"recommended_facility\": \"${referral.recommendedFacility.escapeJson()}\",\n")
                    append("    \"danger_signs\": \"${referral.dangerSigns.escapeJson()}\"\n")
                    append("  }\n")
                }
                append("}\n")
            }
        }

        fun summaryJson(summary: SupervisorSummary): String {
            return buildString {
                append("{\n")
                append("  \"total_visits\": ${summary.totalVisits},\n")
                append("  \"referrals_flagged\": ${summary.referralsFlagged},\n")
                append("  \"narrative\": \"${summary.narrative.escapeJson()}\",\n")
                append("  \"urgent_cases\": ${summary.urgentCases.toJsonArray()},\n")
                append("  \"follow_ups_due\": ${summary.followUpsDue.toJsonArray()},\n")
                append("  \"safety\": \"Not a diagnosis. CHW confirmation required.\"\n")
                append("}\n")
            }
        }

        private fun List<String>.toJsonArray(): String {
            return joinToString(prefix = "[", postfix = "]") { "\"${it.escapeJson()}\"" }
        }

        private fun String.escapeJson(): String {
            return buildString {
                this@escapeJson.forEach { char ->
                    when (char) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(char)
                    }
                }
            }
        }
    }
}
