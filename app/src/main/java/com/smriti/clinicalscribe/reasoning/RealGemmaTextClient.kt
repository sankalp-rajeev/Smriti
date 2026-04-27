package com.smriti.clinicalscribe.reasoning

sealed class TextGenerationResult {
    data class Success(val text: String) : TextGenerationResult()
    data class Unavailable(val status: String) : TextGenerationResult()
    data class Failed(val error: String) : TextGenerationResult()
}

interface RealGemmaTextClient {
    suspend fun generateText(prompt: String): TextGenerationResult
}

class UnavailableGemmaTextClient : RealGemmaTextClient {
    override suspend fun generateText(prompt: String): TextGenerationResult {
        return TextGenerationResult.Unavailable(
            status = "Experimental Real Gemma path unavailable. No diagnosis generated. CHW confirmation required."
        )
    }
}
