package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.ToolCall

/**
 * Compile-only probe for LiteRT-LM API types. The nullable references below prove
 * direct imports compile, but they do not create runtime LiteRT objects.
 */
data class LiteRtApiSurfaceProbe(
    val engine: Engine? = null,
    val engineConfig: EngineConfig? = null,
    val backend: Backend? = null,
    val textContent: Content.Text? = null,
    val imageBytesContent: Content.ImageBytes? = null,
    val imageFileContent: Content.ImageFile? = null,
    val audioBytesContent: Content.AudioBytes? = null,
    val audioFileContent: Content.AudioFile? = null,
    val imageInputData: InputData.Image? = null,
    val audioInputData: InputData.Audio? = null,
    val conversation: Conversation? = null,
    val conversationConfig: ConversationConfig? = null,
    val toolCall: ToolCall? = null,
    val openApiTool: OpenApiTool? = null
) {
    val verifiedTypeNames: List<String>
        get() = listOf(
            "com.google.ai.edge.litertlm.Engine",
            "com.google.ai.edge.litertlm.EngineConfig",
            "com.google.ai.edge.litertlm.Backend",
            "com.google.ai.edge.litertlm.Content.Text",
            "com.google.ai.edge.litertlm.Content.ImageBytes",
            "com.google.ai.edge.litertlm.Content.ImageFile",
            "com.google.ai.edge.litertlm.Content.AudioBytes",
            "com.google.ai.edge.litertlm.Content.AudioFile",
            "com.google.ai.edge.litertlm.InputData.Image",
            "com.google.ai.edge.litertlm.InputData.Audio",
            "com.google.ai.edge.litertlm.Conversation",
            "com.google.ai.edge.litertlm.ConversationConfig",
            "com.google.ai.edge.litertlm.ToolCall",
            "com.google.ai.edge.litertlm.OpenApiTool"
        )

    val audioPreprocessingFindings: List<String>
        get() = listOf(
            "Session.generateContent(List<InputData>) is public.",
            "Conversation.sendMessage(Contents) is public and accepts Content.AudioFile/AudioBytes through Contents.",
            "InputData.Audio and Content.AudioBytes/AudioFile are public raw-audio containers.",
            "No public AudioPreprocessor, AudioProcessor, Preprocessor, or preprocess(...) API was found in litertlm-android 0.10.2 classes.jar.",
            "Runtime raw InputData.Audio may fail with: Audio must be preprocessed before being used in SessionAdvanced."
        )

    val imageApiFindings: List<String>
        get() = listOf(
            "Content.ImageBytes and Content.ImageFile are public image content containers.",
            "InputData.Image is a public raw image container.",
            "EngineConfig exposes visionBackend and maxNumImages.",
            "Session.generateContent(List<InputData>) is public.",
            "Conversation.sendMessage(Contents) is public and accepts Content.ImageBytes/ImageFile through Contents.",
            "No public PromptTemplate, MediaPlaceholder, MultiModalTemplate, ImagePreprocessor, or preprocess(...) API was found in litertlm-android 0.10.2 classes.jar.",
            "Manual runtime probing is required before claiming a usable Gemma 4 vision path."
        )
}
