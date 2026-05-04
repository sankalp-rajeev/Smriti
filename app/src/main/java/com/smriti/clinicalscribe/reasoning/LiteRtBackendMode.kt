package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Backend

enum class LiteRtBackendMode(val label: String) {
    CPU("CPU"),
    GPU_EXPERIMENTAL("GPU experimental");

    fun toBackend(): Backend {
        return when (this) {
            CPU -> Backend.CPU()
            GPU_EXPERIMENTAL -> Backend.GPU()
        }
    }

    companion object {
        fun fromName(value: String?): LiteRtBackendMode {
            return when (value?.trim()?.uppercase()) {
                "GPU", "GPU_EXPERIMENTAL", "EXPERIMENTAL_GPU" -> GPU_EXPERIMENTAL
                else -> CPU
            }
        }
    }
}
