package com.smriti.clinicalscribe.reasoning

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig

/**
 * Compile-only probe for LiteRT-LM API types. The nullable references below prove
 * direct imports compile, but they do not create runtime LiteRT objects.
 */
data class LiteRtApiSurfaceProbe(
    val engine: Engine? = null,
    val engineConfig: EngineConfig? = null,
    val backend: Backend? = null,
    val textContent: Content.Text? = null,
    val conversation: Conversation? = null
) {
    val verifiedTypeNames: List<String>
        get() = listOf(
            "com.google.ai.edge.litertlm.Engine",
            "com.google.ai.edge.litertlm.EngineConfig",
            "com.google.ai.edge.litertlm.Backend",
            "com.google.ai.edge.litertlm.Content.Text",
            "com.google.ai.edge.litertlm.Conversation"
        )
}
