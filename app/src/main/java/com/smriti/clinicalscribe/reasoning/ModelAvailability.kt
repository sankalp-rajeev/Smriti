package com.smriti.clinicalscribe.reasoning

import java.io.File

private const val MODEL_DIRECTORY_NAME = "models"
private const val GEMMA_E2B_MODEL_FILE_NAME = "gemma-4-E2B-it-int4.litertlm"

enum class ModelStatusKind {
    NOT_FOUND,
    FOUND_NOT_LOADED
}

data class ModelStatus(
    val kind: ModelStatusKind,
    val expectedPath: String,
    val fileSizeBytes: Long? = null
) {
    val proofLabel: String
        get() = when (kind) {
            ModelStatusKind.NOT_FOUND -> "Not found (inference disabled)"
            ModelStatusKind.FOUND_NOT_LOADED -> "Found ${formatFileSize(fileSizeBytes ?: 0L)} (not loaded)"
        }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val mib = bytes / (1024.0 * 1024.0)
        return "%.1f MB".format(mib)
    }
}

class ModelAvailability private constructor(
    private val expectedModelFile: File
) {
    fun check(): ModelStatus {
        return if (expectedModelFile.isFile) {
            ModelStatus(
                kind = ModelStatusKind.FOUND_NOT_LOADED,
                expectedPath = expectedModelFile.absolutePath,
                fileSizeBytes = expectedModelFile.length()
            )
        } else {
            ModelStatus(
                kind = ModelStatusKind.NOT_FOUND,
                expectedPath = expectedModelFile.absolutePath
            )
        }
    }

    companion object {
        fun fromFilesDir(filesDir: File): ModelAvailability {
            return ModelAvailability(
                expectedModelFile = File(
                    File(filesDir, MODEL_DIRECTORY_NAME),
                    GEMMA_E2B_MODEL_FILE_NAME
                )
            )
        }
    }
}
