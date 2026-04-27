package com.smriti.clinicalscribe.reasoning

import java.io.File

object LiteRtModelPaths {
    const val MODELS_DIRECTORY_NAME = "models"
    const val GEMMA_E2B_MODEL_FILE_NAME = "gemma-4-E2B-it-int4.litertlm"

    fun expectedModelFile(filesDir: File): File {
        return File(File(filesDir, MODELS_DIRECTORY_NAME), GEMMA_E2B_MODEL_FILE_NAME)
    }
}
