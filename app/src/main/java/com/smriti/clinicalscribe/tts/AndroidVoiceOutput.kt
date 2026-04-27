package com.smriti.clinicalscribe.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidVoiceOutput(context: Context) : VoiceOutput {
    private var ready = false
    private var failedReason: String? = null
    private var textToSpeech: TextToSpeech? = null

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            val tts = textToSpeech
            if (status == TextToSpeech.SUCCESS && tts != null) {
                val languageResult = tts.setLanguage(Locale.getDefault())
                if (
                    languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    failedReason = "Offline TTS language is missing or not supported on this device."
                } else {
                    ready = true
                }
            } else {
                failedReason = "Android TTS engine is unavailable."
            }
        }
    }

    override fun speak(text: String): VoiceOutputResult {
        if (text.isBlank()) {
            return VoiceOutputResult.Unavailable("Nothing to read aloud.")
        }
        if (!ready) {
            return VoiceOutputResult.Unavailable(failedReason ?: "Android TTS is still initializing.")
        }

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "smriti_tts_${System.currentTimeMillis()}")
        return VoiceOutputResult.Started
    }

    override fun stop() {
        textToSpeech?.stop()
    }

    override fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
    }
}
