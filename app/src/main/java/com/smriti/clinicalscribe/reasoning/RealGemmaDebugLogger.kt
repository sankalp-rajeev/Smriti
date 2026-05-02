package com.smriti.clinicalscribe.reasoning

import android.util.Log
import com.smriti.clinicalscribe.BuildConfig

object RealGemmaDebugLogger {
    const val TAG = "SmritiRealGemma"
    private const val RAW_OUTPUT_LIMIT = 1_500

    fun logParserFailure(rawOutput: String, reason: String) {
        if (!shouldLog()) return
        val preview = rawOutput.take(RAW_OUTPUT_LIMIT)
        safeWarn("Parser rejected RealGemma output: $reason")
        safeWarn("Raw RealGemma output preview first ${preview.length} chars: $preview")
    }

    private fun shouldLog(): Boolean {
        return BuildConfig.DEBUG || BuildConfig.REAL_GEMMA_DEV_BUILD_GATE
    }

    private fun safeWarn(message: String) {
        try {
            Log.w(TAG, message)
        } catch (_: RuntimeException) {
            // Local JVM unit tests use Android stubs where Log methods are not implemented.
        }
    }
}
