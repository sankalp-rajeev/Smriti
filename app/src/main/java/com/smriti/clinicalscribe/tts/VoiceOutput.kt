package com.smriti.clinicalscribe.tts

interface VoiceOutput {
    fun speak(text: String): VoiceOutputResult
    fun stop()
    fun release()
}

sealed class VoiceOutputResult {
    data object Started : VoiceOutputResult()
    data class Unavailable(val reason: String) : VoiceOutputResult()
}

class NoOpVoiceOutput : VoiceOutput {
    override fun speak(text: String): VoiceOutputResult {
        return VoiceOutputResult.Unavailable("Offline TTS is not available in this build.")
    }

    override fun stop() = Unit
    override fun release() = Unit
}
