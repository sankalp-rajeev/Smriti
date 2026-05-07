package com.smriti.clinicalscribe.data

import android.content.Context
import org.json.JSONObject

object DemoSupervisorRegisterImporter {
    const val ASSET_PATH = "demo/smriti_patients.json"

    fun fromAsset(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): SupervisorRegister {
        val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        return fromJson(json, nowMillis)
    }

    fun fromJson(
        json: String,
        nowMillis: Long = System.currentTimeMillis()
    ): SupervisorRegister {
        val root = JSONObject(json)
        val patientsArray = root.getJSONArray("patients")
        val patients = mutableListOf<Patient>()
        val visits = mutableListOf<VisitLog>()

        for (patientIndex in 0 until patientsArray.length()) {
            val patientJson = patientsArray.getJSONObject(patientIndex)
            val patientId = patientJson.getString("id")
            patients += Patient(
                id = patientId,
                name = patientJson.getString("name"),
                age = patientJson.getInt("age"),
                sex = patientJson.optString("sex", "F"),
                pregnancyWeeks = patientJson.optIntOrNull("pregnancyWeeks"),
                village = patientJson.optString("village"),
                riskSummary = patientJson.optString("riskSummary"),
                country = patientJson.optString("country"),
                countryCode = patientJson.optString("countryCode"),
                preferredLanguage = patientJson.optString("preferredLanguage", "en"),
                protocolRegion = patientJson.optString("protocolRegion", "GLOBAL_CORE"),
                scenarioPreview = patientJson.optString("scenarioPreview"),
                notes = patientJson.optStringOrNull("notes")
            )

            val visitsArray = patientJson.optJSONArray("priorVisits")
            if (visitsArray != null) {
                for (visitIndex in 0 until visitsArray.length()) {
                    val visitJson = visitsArray.getJSONObject(visitIndex)
                    val daysAgo = visitJson.optLong("daysAgo", 0L)
                    val followUpDueDaysAgo = visitJson.optLongOrNull("followUpDueDaysAgo")
                    visits += VisitLog(
                        id = visitJson.getLong("id"),
                        patientId = patientId,
                        visitDateMillis = nowMillis - daysAgo.daysToMillis(),
                        observationText = visitJson.optString("observationText"),
                        structuredNote = visitJson.optString("structuredNote"),
                        protocolCitation = visitJson.optString("protocolCitation"),
                        suggestedFollowUp = visitJson.optString("suggestedFollowUp"),
                        confirmed = visitJson.optBoolean("confirmed", true),
                        transcriptSource = TranscriptSource.SEEDED_PRIOR_HISTORY,
                        followUpDueDateMillis = followUpDueDaysAgo?.let {
                            nowMillis - it.daysToMillis()
                        },
                        followUpCompleted = visitJson.optBooleanOrNull("followUpCompleted")
                    )
                }
            }
        }

        return SupervisorRegister(patients = patients, priorVisits = visits)
    }

    private fun Long.daysToMillis(): Long = this * 24L * 60L * 60L * 1000L

    private fun JSONObject.optIntOrNull(name: String): Int? {
        return if (has(name) && !isNull(name)) optInt(name) else null
    }

    private fun JSONObject.optLongOrNull(name: String): Long? {
        return if (has(name) && !isNull(name)) optLong(name) else null
    }

    private fun JSONObject.optBooleanOrNull(name: String): Boolean? {
        return if (has(name) && !isNull(name)) optBoolean(name) else null
    }

    private fun JSONObject.optStringOrNull(name: String): String? {
        return if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null
    }
}
