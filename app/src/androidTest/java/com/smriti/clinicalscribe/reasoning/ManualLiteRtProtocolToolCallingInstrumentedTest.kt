package com.smriti.clinicalscribe.reasoning

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.tool
import com.smriti.clinicalscribe.rag.ProtocolRetrievalContext
import com.smriti.clinicalscribe.rag.ProtocolRetriever
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualLiteRtProtocolToolCallingInstrumentedTest {
    @Test
    fun probesNativeProtocolLookupToolCallingWithSideloadedModel() {
        val args = InstrumentationRegistry.getArguments()
        val allowManualTextInference = args
            .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
            ?.equals("true", ignoreCase = true) == true
        val allowManualFunctionCalling = args
            .getString(ARG_ALLOW_MANUAL_FUNCTION_CALLING)
            ?.equals("true", ignoreCase = true) == true

        assumeTrue(
            "Manual LiteRT protocol tool-calling probe skipped: pass " +
                "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_TEXT_INFERENCE=true " +
                "and -Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_FUNCTION_CALLING=true",
            allowManualTextInference && allowManualFunctionCalling
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedModelFile = LiteRtModelPaths.expectedModelFile(context.filesDir)
        val modelStatus = ModelAvailability.fromFilesDir(context.filesDir).check()

        Log.i(TAG, "Expected model path: ${expectedModelFile.absolutePath}")
        Log.i(TAG, "Model status: ${modelStatus.proofLabel}; size=${modelStatus.fileSizeBytes ?: 0} bytes")
        Log.i(TAG, "Tool API: OpenApiTool + tool(...) + ConversationConfig(tools, automaticToolCalling=true)")

        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            throw AssertionError(
                "Manual LiteRT protocol tool-calling probe requires sideloaded model at ${expectedModelFile.absolutePath}."
            )
        }

        val prepared = LiteRtEngineConfigFactory().prepare(modelStatus) as? LiteRtEngineConfigPreparation.Prepared
            ?: throw AssertionError("Manual LiteRT protocol tool-calling probe could not prepare EngineConfig.")
        val protocolRetriever = ProtocolRetriever.fromAsset(context)
        val protocolTool = LocalProtocolLookupOpenApiTool(protocolRetriever)
        Log.i(TAG, "Local protocol tool registered: $TOOL_NAME")

        val lease = RealGemmaInferenceGate.tryAcquire(
            RealGemmaRequestType.MANUAL_TEST,
            RealGemmaRequestDiagnostics(
                modelExists = true,
                modelSizeBytes = modelStatus.fileSizeBytes,
                sentinelExists = null,
                backendMode = prepared.backendLabel,
                engineState = "manual_protocol_tool_call_probe",
                lastEngineFailure = RealGemmaInferenceGate.lastEngineFailure
            )
        ) ?: throw AssertionError(RealGemmaInferenceGate.BUSY_MESSAGE)

        val response = try {
            Engine(prepared.engineConfig).use { engine ->
                engine.initialize()
                Log.i(TAG, "Model loaded for manual protocol tool-calling probe.")
                val config = ConversationConfig(
                    tools = listOf(tool(protocolTool)),
                    automaticToolCalling = true
                )
                engine.createConversation(config).use { conversation ->
                    conversation.sendMessage(PROMPT)
                }
            }
        } catch (error: RuntimeException) {
            lease.fail(error.message ?: error::class.java.simpleName)
            Log.e(TAG, "Native protocol tool-calling probe failed: ${error.message}")
            throw AssertionError(error.message ?: error::class.java.simpleName)
        } catch (error: LinkageError) {
            lease.fail(error.message ?: error::class.java.simpleName)
            Log.e(TAG, "Native protocol tool-calling probe linkage failure: ${error.message}")
            throw AssertionError(error.message ?: error::class.java.simpleName)
        } finally {
            lease.release()
        }

        logResponse(response)
        Log.i(TAG, "Tool call happened: ${protocolTool.executionCount > 0}")
        Log.i(TAG, "Tool name: $TOOL_NAME")
        Log.i(TAG, "Tool arguments: ${protocolTool.lastRequestJson}")
        Log.i(TAG, "Returned citation: ${protocolTool.lastReturnedCitation.ifBlank { "<none>" }}")
        Log.i(TAG, "Safety boundary: manual probe only; no diagnosis, no save, no Room write, no referral flag.")

        assertTrue(
            "Native LiteRT protocol tool calling did not execute $TOOL_NAME. " +
                "The API surface is present, but this model/run did not produce an automatic tool call.",
            protocolTool.executionCount > 0
        )
        assertTrue(
            "Protocol tool call executed but did not return a local protocol citation.",
            protocolTool.lastReturnedCitation.isNotBlank()
        )
    }

    private fun logResponse(response: Message) {
        val text = response.contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "\n") { it.text }
            .trim()
        Log.i(TAG, "Response role: ${response.role}")
        Log.i(TAG, "Response text length: ${text.length}")
        Log.i(TAG, "Final response preview: ${text.take(MAX_RESPONSE_PREVIEW_CHARS)}")
        Log.i(TAG, "Response tool call count: ${response.toolCalls.size}")
        response.toolCalls.forEachIndexed { index, toolCall ->
            Log.i(TAG, "Response tool call ${index + 1}: name=${toolCall.name}; args=${toolCall.arguments}")
        }
    }

    private class LocalProtocolLookupOpenApiTool(
        private val protocolRetriever: ProtocolRetriever
    ) : OpenApiTool {
        var executionCount: Int = 0
            private set
        var lastRequestJson: String = ""
            private set
        var lastReturnedCitation: String = ""
            private set

        override fun getToolDescriptionJsonString(): String {
            return """
                {
                  "name": "$TOOL_NAME",
                  "description": "Looks up local offline maternal-health protocol guidance for CHW documentation support. It does not diagnose, save data, prescribe treatment, or create referral flags.",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "query": {
                        "type": "string",
                        "description": "Pregnancy observation or danger-sign phrase to look up in local protocol guidance."
                      },
                      "countryCode": {
                        "type": "string",
                        "description": "Optional ISO country code such as IN. Use null if unknown."
                      },
                      "region": {
                        "type": "string",
                        "description": "Optional local protocol region such as INDIA or GLOBAL_CORE. Use null if unknown."
                      }
                    },
                    "required": ["query"]
                  }
                }
            """.trimIndent()
        }

        override fun execute(paramsJsonString: String): String {
            executionCount += 1
            lastRequestJson = paramsJsonString
            Log.i(TAG, "$TOOL_NAME executed with JSON: $paramsJsonString")

            val params = parseParams(paramsJsonString)
            val context = ProtocolRetrievalContext(
                countryCode = normalizeCountryCode(params.countryCode),
                region = normalizeRegion(params.region)
            )
            val chunks = protocolRetriever.retrieve(params.query, context)
            val chunk = chunks.firstOrNull()
            if (chunk == null) {
                lastReturnedCitation = ""
                return compactJson(
                    "status" to "no_match",
                    "citation" to "",
                    "topic" to "",
                    "guidanceSnippet" to "No matching local protocol guidance found. Ask the CHW to review local guidance manually.",
                    "countryCode" to context.countryCode.orEmpty(),
                    "region" to context.region.orEmpty(),
                    "safetyBoundary" to "Protocol lookup only; no diagnosis, treatment, save, referral flag, or follow-up task."
                )
            }

            lastReturnedCitation = chunk.citation
            return compactJson(
                "status" to "matched",
                "citation" to chunk.citation,
                "topic" to chunk.topic,
                "guidanceSnippet" to chunk.text.compactSnippet(MAX_GUIDANCE_SNIPPET_CHARS),
                "countryCode" to (chunk.countryCode ?: context.countryCode.orEmpty()),
                "region" to chunk.region,
                "safetyBoundary" to "Protocol lookup only; no diagnosis, treatment, save, referral flag, or follow-up task."
            )
        }

        private fun parseParams(paramsJsonString: String): ToolParams {
            return runCatching {
                val json = JSONObject(paramsJsonString)
                ToolParams(
                    query = json.optString("query").ifBlank {
                        "severe headache blurred vision high blood pressure pregnancy"
                    },
                    countryCode = json.nullableString("countryCode"),
                    region = json.nullableString("region")
                )
            }.getOrElse {
                ToolParams(
                    query = paramsJsonString.ifBlank {
                        "severe headache blurred vision high blood pressure pregnancy"
                    },
                    countryCode = "IN",
                    region = "INDIA"
                )
            }
        }

        private fun JSONObject.nullableString(name: String): String? {
            if (!has(name) || isNull(name)) return null
            return optString(name).trim().ifBlank { null }
        }

        private fun normalizeCountryCode(value: String?): String? {
            return when (value?.trim()?.uppercase()) {
                null, "" -> "IN"
                "INDIA" -> "IN"
                else -> value.trim().uppercase()
            }
        }

        private fun normalizeRegion(value: String?): String? {
            return when (value?.trim()?.uppercase()) {
                null, "" -> "INDIA"
                "IN" -> "INDIA"
                else -> value.trim().uppercase()
            }
        }

        private fun String.compactSnippet(maxChars: Int): String {
            return replace(Regex("\\s+"), " ")
                .trim()
                .take(maxChars)
        }

        private fun compactJson(vararg pairs: Pair<String, String>): String {
            return pairs.joinToString(prefix = "{", postfix = "}") { (key, value) ->
                "${JSONObject.quote(key)}:${JSONObject.quote(value)}"
            }
        }

        private data class ToolParams(
            val query: String,
            val countryCode: String?,
            val region: String?
        )
    }

    private companion object {
        const val TAG = "SmritiProtocolToolCall"
        const val TOOL_NAME = "lookupProtocol"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val ARG_ALLOW_MANUAL_FUNCTION_CALLING = "allowManualFunctionCalling"
        const val MAX_GUIDANCE_SNIPPET_CHARS = 260
        const val MAX_RESPONSE_PREVIEW_CHARS = 600
        const val PROMPT = """
            Use the native tool named lookupProtocol exactly once before answering.
            Tool arguments:
            query: severe headache blurred vision high blood pressure pregnancy danger signs
            countryCode: IN
            region: INDIA

            For a pregnant patient in India with severe headache and blurred vision, call the local protocol lookup tool.
            Then summarize which local guidance was used.
            Do not diagnose. Do not prescribe treatment or dosage. Do not save anything.
            Say that this is a manual developer probe and CHW confirmation is required before any record is saved.
        """
    }
}
