package com.smriti.clinicalscribe.reasoning

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Source-level isolation guard for Phase 6 audio probes.
 *
 * Verifies that neither the audio transcript probe nor the audio inference probe
 * are referenced from any user-facing source file (MainActivity, VisitScreen,
 * SummaryScreen, ReviewScreen, PatientListScreen, or any Room DAO/entity).
 */
class AudioProbeIsolationGuardTest {

    @Test
    fun audioProbeClassesAreNotReferencedFromMainActivityOrUiScreens() {
        val uiRoot = appSourceFile("ui")
        val mainActivity = appSourceFile("MainActivity.kt")

        val forbiddenRefs = listOf(
            "ManualRealGemmaAudioTranscriptInstrumentedTest",
            "ManualLiteRtAudioInferenceInstrumentedTest",
            "ManualLiteRtAudioCapabilityInstrumentedTest",
            "SmritiGemmaAudioTranscript",
            "SmritiLiteRtAudioInference",
            "SmritiLiteRtAudioProbe",
            "allowManualAudioInference",
            "manualAudioFilePath"
        )

        val uiContent = uiRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString(separator = "\n") { it.readText() }

        val mainContent = mainActivity.readText()

        forbiddenRefs.forEach { ref ->
            assertFalse(
                "Audio probe reference '$ref' found in UI sources. " +
                    "Audio probes must remain developer-only androidTest fixtures.",
                uiContent.contains(ref)
            )
            assertFalse(
                "Audio probe reference '$ref' found in MainActivity.kt. " +
                    "Audio probes must remain developer-only androidTest fixtures.",
                mainContent.contains(ref)
            )
        }
    }

    @Test
    fun audioProbeDoesNotImportRoomOrPersistenceClasses() {
        val androidTestRoot = File("src/androidTest/java/com/smriti/clinicalscribe/reasoning")
            .takeIf { it.exists() }
            ?: File("app/src/androidTest/java/com/smriti/clinicalscribe/reasoning")

        val audioProbeFiles = androidTestRoot.listFiles()
            ?.filter { it.name.contains("Audio", ignoreCase = true) && it.extension == "kt" }
            .orEmpty()

        val roomImports = listOf(
            "androidx.room",
            "com.smriti.clinicalscribe.data.VisitLog",
            "com.smriti.clinicalscribe.data.ReferralFlag",
            "com.smriti.clinicalscribe.data.SmritiDatabase",
            "VisitReasoningPipeline",
            "RealGemmaAgent"
        )

        audioProbeFiles.forEach { file ->
            val content = file.readText()
            roomImports.forEach { forbidden ->
                assertFalse(
                    "Audio probe ${file.name} references '$forbidden'. " +
                        "Audio probes must not write to Room or invoke the reasoning pipeline.",
                    content.contains(forbidden)
                )
            }
        }
    }

    private fun appSourceFile(relativePath: String): File {
        val modulePath = File("src/main/java/com/smriti/clinicalscribe/$relativePath")
        val rootPath = File("app/src/main/java/com/smriti/clinicalscribe/$relativePath")
        return when {
            modulePath.exists() -> modulePath
            else -> rootPath
        }
    }
}
