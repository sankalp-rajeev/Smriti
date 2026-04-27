package com.smriti.clinicalscribe.tts

interface VoiceOutput {
    fun speak(text: String)
    fun stop()
    fun release()
}

class NoOpVoiceOutput : VoiceOutput {
    override fun speak(text: String) = Unit
    override fun stop() = Unit
    override fun release() = Unit
}
