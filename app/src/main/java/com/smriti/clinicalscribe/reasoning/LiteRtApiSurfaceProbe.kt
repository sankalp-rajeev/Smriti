package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Capabilities
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.SessionConfig
import com.google.ai.edge.litertlm.ToolCall
import com.google.ai.edge.litertlm.ToolManager
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolProvider
import com.google.ai.edge.litertlm.ToolSet

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
    val openApiTool: OpenApiTool? = null,
    val toolProvider: ToolProvider? = null,
    val toolManager: ToolManager? = null,
    val toolSet: ToolSet? = null,
    val toolParam: ToolParam? = null,
    val samplerConfig: SamplerConfig? = null,
    val sessionConfig: SessionConfig? = null,
    val capabilities: Capabilities? = null
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
            "com.google.ai.edge.litertlm.OpenApiTool",
            "com.google.ai.edge.litertlm.ToolProvider",
            "com.google.ai.edge.litertlm.ToolManager",
            "com.google.ai.edge.litertlm.ToolSet",
            "com.google.ai.edge.litertlm.ToolParam",
            "com.google.ai.edge.litertlm.SamplerConfig",
            "com.google.ai.edge.litertlm.SessionConfig",
            "com.google.ai.edge.litertlm.Capabilities",
            "com.google.ai.edge.litertlm.ExperimentalFlags"
        )

    val audioPreprocessingFindings: List<String>
        get() = listOf(
            "Session.generateContent(List<InputData>) is public.",
            "Conversation.sendMessage(Contents) is public and accepts Content.AudioFile/AudioBytes through Contents.",
            "InputData.Audio and Content.AudioBytes/AudioFile are public raw-audio containers.",
            "EngineConfig.audioBackend is now available in litertlm-android 0.11.0 (was absent in 0.10.2).",
            "No public AudioPreprocessor, AudioProcessor, Preprocessor, or preprocess(...) API was found in litertlm-android 0.11.0 classes.jar.",
            "ExperimentalFlags.overwritePromptTemplate is available (may enable multimodal prompt customization).",
            "Runtime raw InputData.Audio may fail with: Audio must be preprocessed before being used in SessionAdvanced."
        )

    val speculativeApiFindings: List<String>
        get() = listOf(
            "ExperimentalFlags.enableSpeculativeDecoding is public in litertlm-android 0.11.0.",
            "Capabilities.hasSpeculativeDecodingSupport() is public in litertlm-android 0.11.0.",
            "No public draft-model, target-model, MTP-specific, or multi-token configuration class was found in the 0.11.0 classes.jar name scan.",
            "Smriti keeps speculative decoding manual-only until the gated latency probe is run on a target device."
        )

    val toolCallingApiFindings: List<String>
        get() = listOf(
            "OpenApiTool and ToolCall are public.",
            "ToolProvider, ToolManager, ToolSet, and ToolParam are public.",
            "ReflectionTool is present in the 0.11.0 AAR but is Kotlin-internal, so Smriti does not import it.",
            "ConversationConfig exposes tools and automaticToolCalling.",
            "Smriti keeps native tool-calling manual-only; production protocol retrieval remains deterministic ProtocolRetriever before RealGemma prompting."
        )

    val imageApiFindings: List<String>
        get() = listOf(
            "Content.ImageBytes and Content.ImageFile are public image content containers.",
            "InputData.Image is a public raw image container.",
            "EngineConfig exposes visionBackend and maxNumImages.",
            "Session.generateContent(List<InputData>) is public.",
            "Conversation.sendMessage(Contents) is public and accepts Content.ImageBytes/ImageFile through Contents.",
            "No public PromptTemplate, MediaPlaceholder, MultiModalTemplate, ImagePreprocessor, or preprocess(...) API was found in litertlm-android 0.11.0 classes.jar.",
            "ExperimentalFlags.overwritePromptTemplate is available (may enable multimodal prompt customization).",
            "Manual runtime probing is required before claiming a usable Gemma 4 vision path."
        )
}
