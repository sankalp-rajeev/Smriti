package com.smriti.clinicalscribe.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoSupervisorRegisterImporterTest {
    @Test
    fun parsesBundledSupervisorRegisterJsonOffline() {
        val repoRelative =
            File("app/src/main/assets/${DemoSupervisorRegisterImporter.ASSET_PATH}")
        val moduleRelative =
            File("src/main/assets/${DemoSupervisorRegisterImporter.ASSET_PATH}")
        val json = when {
            repoRelative.exists() -> repoRelative.readText()
            moduleRelative.exists() -> moduleRelative.readText()
            else ->
                File("../app/src/main/assets/${DemoSupervisorRegisterImporter.ASSET_PATH}")
                    .takeIf { it.exists() }?.readText()
                    ?: error(
                        "Expected ${DemoSupervisorRegisterImporter.ASSET_PATH} " +
                            "under app/src/main/assets (repo or module-relative cwd)."
                    )
        }

        assertTrue(json.contains("\"patients\""))
        assertTrue(json.contains("\"patient-meena\""))

        val now = 1_800_000_000_000L
        val register = DemoSupervisorRegisterImporter.fromJson(json, nowMillis = now)

        assertEquals(6, register.patients.size)
        assertTrue(register.priorVisits.isNotEmpty())

        val meenaPrior = register.priorVisits.filter { it.patientId == "patient-meena" }
        assertEquals(3, meenaPrior.size)
        assertTrue(meenaPrior.all { it.confirmed })

        val amaraFollowUp = register.priorVisits.firstOrNull {
            it.patientId == "patient-amara" && it.id == 3001L
        }
        assertEquals(false, amaraFollowUp?.followUpCompleted)
        assertTrue((amaraFollowUp?.followUpDueDateMillis ?: 0L) < now)

        register.priorVisits.forEach { visit ->
            assertTrue(visit.audioFilePath.isNullOrBlank())
        }
    }
}
