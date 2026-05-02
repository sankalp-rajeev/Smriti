package com.smriti.clinicalscribe.reasoning

import android.util.Log
import kotlin.system.measureTimeMillis

object SmritiLatencyLogger {
    const val TAG = "SmritiLatency"

    fun log(label: String, durationMillis: Long, scenario: String? = null) {
        safeInfo("${label}Ms=$durationMillis${scenario?.let { "; scenario=$it" }.orEmpty()}")
    }

    fun mark(message: String, scenario: String? = null) {
        safeInfo("$message${scenario?.let { "; scenario=$it" }.orEmpty()}")
    }

    inline fun <T> measure(label: String, scenario: String? = null, block: () -> T): T {
        var value: T? = null
        val duration = measureTimeMillis {
            value = block()
        }
        log(label, duration, scenario)
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private fun safeInfo(message: String) {
        try {
            Log.i(TAG, message)
        } catch (_: RuntimeException) {
            // Android Log is not implemented in local JVM unit tests.
        }
    }
}
