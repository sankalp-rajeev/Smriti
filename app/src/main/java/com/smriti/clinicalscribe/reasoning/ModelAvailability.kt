package com.smriti.clinicalscribe.reasoning

import java.io.File

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
            ModelStatusKind.NOT_FOUND -> "Not found"
            ModelStatusKind.FOUND_NOT_LOADED -> "Found"
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
            return ModelAvailability(expectedModelFile = LiteRtModelPaths.expectedModelFile(filesDir))
        }
    }
}
