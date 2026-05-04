package com.smriti.clinicalscribe.reasoning

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

enum class RealGemmaRequestType {
    VISIT_NOTE,
    SUPERVISOR_SUMMARY,
    PAPER_NOTE_SCAN,
    MANUAL_TEST,
    PRELOAD
}

data class RealGemmaRequestDiagnostics(
    val modelExists: Boolean,
    val modelSizeBytes: Long?,
    val sentinelExists: Boolean?,
    val backendMode: String,
    val engineState: String,
    val lastEngineFailure: String?
) {
    fun asLogText(): String {
        return "modelExists=$modelExists; modelSizeBytes=${modelSizeBytes ?: "unknown"}; " +
            "sentinelExists=${sentinelExists?.toString() ?: "unknown"}; backendMode=$backendMode; " +
            "engineState=$engineState; lastEngineFailure=${lastEngineFailure ?: "none"}"
    }
}

class RealGemmaInferenceLease internal constructor(
    private val requestType: RealGemmaRequestType
) {
    fun release() {
        RealGemmaInferenceGate.release(requestType)
    }

    fun fail(reason: String) {
        RealGemmaInferenceGate.markFailed(requestType, reason)
    }
}

object RealGemmaInferenceGate {
    const val TAG = "SmritiRealGemmaGate"
    const val BUSY_MESSAGE = "Smriti is already preparing a note. Please wait."

    private val active = AtomicBoolean(false)
    private val currentRequestType = AtomicReference<RealGemmaRequestType?>(null)
    private val lastFailure = AtomicReference<String?>(null)

    val isBusy: Boolean
        get() = active.get()

    val lastEngineFailure: String?
        get() = lastFailure.get()

    fun tryAcquire(
        requestType: RealGemmaRequestType,
        diagnostics: RealGemmaRequestDiagnostics
    ): RealGemmaInferenceLease? {
        log(
            "requestStart requestType=$requestType; ${diagnostics.asLogText()}"
        )
        return if (active.compareAndSet(false, true)) {
            currentRequestType.set(requestType)
            log("acquired requestType=$requestType")
            RealGemmaInferenceLease(requestType)
        } else {
            log(
                "busy requestType=$requestType; activeRequestType=${currentRequestType.get()}; " +
                    "lastEngineFailure=${lastFailure.get() ?: "none"}"
            )
            null
        }
    }

    internal fun release(requestType: RealGemmaRequestType) {
        currentRequestType.set(null)
        active.set(false)
        log("released requestType=$requestType")
    }

    internal fun markFailed(requestType: RealGemmaRequestType, reason: String) {
        lastFailure.set(reason)
        log("failed requestType=$requestType reason=${reason.take(160)}")
    }

    fun resetForTests() {
        currentRequestType.set(null)
        active.set(false)
        lastFailure.set(null)
    }

    private fun log(message: String) {
        try {
            Log.i(TAG, message)
        } catch (_: RuntimeException) {
            // Android Log is not implemented in local JVM unit tests.
        }
    }
}
