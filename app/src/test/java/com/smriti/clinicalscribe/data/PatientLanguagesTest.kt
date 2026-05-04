package com.smriti.clinicalscribe.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PatientLanguagesTest {
    @Test
    fun demoPatientLanguageMappingsAreCorrect() {
        val patientsById = DemoSeedData.patients.associateBy { it.id }

        assertEquals("hi", patientsById.getValue("patient-meena").preferredLanguage)
        assertEquals("hi", patientsById.getValue("patient-priya").preferredLanguage)
        assertEquals("sw", patientsById.getValue("patient-grace").preferredLanguage)
        assertEquals("es", patientsById.getValue("patient-lucia").preferredLanguage)
        assertEquals("en", patientsById.getValue("patient-fatima").preferredLanguage)
        assertEquals("en", patientsById.getValue("patient-amara").preferredLanguage)

        assertEquals("Peru", patientsById.getValue("patient-lucia").country)
    }

    @Test
    fun patientNoteLanguageMappingsSurviveDefaultUiLanguageChanges() {
        val patientsById = DemoSeedData.patients.associateBy { it.id }

        val defaultLanguageForNewPatients = "es"

        assertEquals("es", defaultLanguageForNewPatients)
        assertEquals("hi", patientsById.getValue("patient-meena").preferredLanguage)
        assertEquals("hi", patientsById.getValue("patient-priya").preferredLanguage)
        assertEquals("sw", patientsById.getValue("patient-grace").preferredLanguage)
        assertEquals("es", patientsById.getValue("patient-lucia").preferredLanguage)
        assertEquals("en", patientsById.getValue("patient-fatima").preferredLanguage)
        assertEquals("en", patientsById.getValue("patient-amara").preferredLanguage)
    }

    @Test
    fun languageCodeToDisplayMappingWorks() {
        assertEquals("EN / English", PatientLanguages.fromCode("en").displayLabel)
        assertEquals("हिन्दी / Hindi", PatientLanguages.fromCode("hi").displayLabel)
        assertEquals("Kiswahili / Swahili", PatientLanguages.fromCode("sw").displayLabel)
        assertEquals("Español / Spanish", PatientLanguages.fromCode("es").displayLabel)
        assertEquals("EN / English", PatientLanguages.fromCode("unknown").displayLabel)
    }

    @Test
    fun safetyWordingExistsForDemoLanguages() {
        assertEquals(
            "This is not a diagnosis. CHW confirmation is required before saving.",
            PatientLanguages.English.safetyWording
        )
        assertEquals("यह निदान नहीं है। CHW की पुष्टि आवश्यक है।", PatientLanguages.Hindi.safetyWording)
        assertEquals(
            "Esto no es un diagnóstico. Se requiere confirmación de la trabajadora de salud.",
            PatientLanguages.Spanish.safetyWording
        )
        assertEquals(
            "Hii si utambuzi wa ugonjwa. Uthibitisho wa mfanyakazi wa afya unahitajika.",
            PatientLanguages.Swahili.safetyWording
        )
    }
}
