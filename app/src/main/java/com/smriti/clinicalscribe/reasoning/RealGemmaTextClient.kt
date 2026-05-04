package com.smriti.clinicalscribe.reasoning

sealed class TextGenerationResult {
    data class Success(val text: String) : TextGenerationResult()
    data class Unavailable(val status: String) : TextGenerationResult()
    data class Failed(val error: String) : TextGenerationResult()
}

interface RealGemmaTextClient {
    suspend fun generateText(prompt: String): TextGenerationResult

    suspend fun generateText(
        prompt: String,
        requestType: RealGemmaRequestType
    ): TextGenerationResult {
        return generateText(prompt)
    }
}

class UnavailableGemmaTextClient(
    private val status: String = "On-device reasoning unavailable. Complete local model setup and retry."
) : RealGemmaTextClient {
    override suspend fun generateText(prompt: String): TextGenerationResult {
        return TextGenerationResult.Unavailable(status = status)
    }
}
