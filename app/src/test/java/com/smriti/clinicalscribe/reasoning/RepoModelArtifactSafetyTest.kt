package com.smriti.clinicalscribe.reasoning

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class RepoModelArtifactSafetyTest {
    @Test
    fun repositoryDoesNotContainCommittedLiteRtModelArtifacts() {
        val repoRoot = findRepoRoot()
        val forbidden = repoRoot
            .walkTopDown()
            .onEnter { dir -> !dir.shouldSkipDirectory() }
            .filter { file ->
                file.isFile && listOf(".litertlm", ".task", ".tflite", ".onnx").any { suffix ->
                    file.name.endsWith(suffix, ignoreCase = true)
                }
            }
            .map { it.relativeTo(repoRoot).path }
            .toList()

        assertTrue("Model artifacts must not be committed: $forbidden", forbidden.isEmpty())
    }

    private fun findRepoRoot(): File {
        var current = File(".").canonicalFile
        while (current.parentFile != null && !File(current, "settings.gradle.kts").isFile) {
            current = current.parentFile!!
        }
        return current
    }

    private fun File.shouldSkipDirectory(): Boolean {
        val path = relativePathFromRepoRoot()
        return path == ".git" ||
            path == ".gradle" ||
            path == "build" ||
            path.endsWith("${File.separator}build")
    }

    private fun File.relativePathFromRepoRoot(): String {
        val repoRoot = generateSequence(this) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: return name
        return relativeTo(repoRoot).path.ifBlank { "." }
    }
}
