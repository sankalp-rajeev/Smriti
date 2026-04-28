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
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualLiteRtFunctionCallingInstrumentedTest {
    @Test
    fun probesNativeFunctionCallingWithSideloadedModel() {
        val args = InstrumentationRegistry.getArguments()
        val allowManualTextInference = args
            .getString(ARG_ALLOW_MANUAL_TEXT_INFERENCE)
            ?.equals("true", ignoreCase = true) == true
        val allowManualFunctionCalling = args
            .getString(ARG_ALLOW_MANUAL_FUNCTION_CALLING)
            ?.equals("true", ignoreCase = true) == true

        assumeTrue(
            "Manual LiteRT function-calling probe skipped: pass " +
                "-Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_TEXT_INFERENCE=true " +
                "and -Pandroid.testInstrumentationRunnerArguments.$ARG_ALLOW_MANUAL_FUNCTION_CALLING=true",
            allowManualTextInference && allowManualFunctionCalling
        )

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedModelFile = LiteRtModelPaths.expectedModelFile(context.filesDir)
        val modelStatus = ModelAvailability.fromFilesDir(context.filesDir).check()

        Log.i(TAG, "Expected model path: ${expectedModelFile.absolutePath}")
        Log.i(TAG, "Model status: ${modelStatus.proofLabel}; size=${modelStatus.fileSizeBytes ?: 0} bytes")
        Log.i(TAG, "Native API surface: OpenApiTool, ToolProvider, ToolCall, ConversationConfig(tools, automaticToolCalling)")

        if (modelStatus.kind != ModelStatusKind.FOUND_NOT_LOADED) {
            throw AssertionError(
                "Manual LiteRT function-calling probe requires sideloaded model at ${expectedModelFile.absolutePath}."
            )
        }

        val prepared = LiteRtEngineConfigFactory().prepare(modelStatus) as? LiteRtEngineConfigPreparation.Prepared
            ?: throw AssertionError("Manual LiteRT function-calling probe could not prepare EngineConfig.")

        val logVisitTool = LogVisitOpenApiTool()
        val response = try {
            Engine(prepared.engineConfig).use { engine ->
                engine.initialize()
                val config = ConversationConfig(
                    tools = listOf(tool(logVisitTool)),
                    automaticToolCalling = true
                )
                engine.createConversation(config).use { conversation ->
                    conversation.sendMessage(PROMPT)
                }
            }
        } catch (error: RuntimeException) {
            Log.e(TAG, "Native function-calling probe failed: ${error.message}")
            throw AssertionError(error.message ?: error::class.java.simpleName)
        } catch (error: LinkageError) {
            Log.e(TAG, "Native function-calling probe linkage failure: ${error.message}")
            throw AssertionError(error.message ?: error::class.java.simpleName)
        }

        logResponse(response)
        Log.i(TAG, "Tool execution count: ${logVisitTool.executionCount}")
        Log.i(TAG, "Last tool request JSON: ${logVisitTool.lastRequestJson}")

        assertTrue(
            "Native LiteRT function calling did not execute log_visit. " +
                "The API surface is present, but this model/run did not produce an automatic tool call.",
            logVisitTool.executionCount > 0
        )
    }

    private fun logResponse(response: Message) {
        val text = response.contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "\n") { it.text }
            .trim()
        Log.i(TAG, "Response role: ${response.role}")
        Log.i(TAG, "Response text length: ${text.length}")
        Log.i(TAG, "Response text: $text")
        Log.i(TAG, "Response tool call count: ${response.toolCalls.size}")
        response.toolCalls.forEachIndexed { index, toolCall ->
            Log.i(TAG, "Response tool call ${index + 1}: name=${toolCall.name}; args=${toolCall.arguments}")
        }
    }

    private class LogVisitOpenApiTool : OpenApiTool {
        var executionCount: Int = 0
            private set
        var lastRequestJson: String = ""
            private set

        override fun getToolDescriptionJsonString(): String {
            return """
                {
                  "name": "log_visit",
                  "description": "Records a structured maternal-health visit support draft for CHW review.",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "patientId": {"type": "string"},
                      "observationText": {"type": "string"},
                      "protocolCitation": {"type": "string"},
                      "referralRequired": {"type": "boolean"}
                    },
                    "required": ["patientId", "observationText", "protocolCitation", "referralRequired"]
                  }
                }
            """.trimIndent()
        }

        override fun execute(paramsJsonString: String): String {
            executionCount += 1
            lastRequestJson = paramsJsonString
            Log.i(TAG, "log_visit executed with JSON: $paramsJsonString")
            return """{"status":"accepted_for_manual_test","savedToRoom":false}"""
        }
    }

    private companion object {
        const val TAG = "SmritiLiteRtFunctionTest"
        const val ARG_ALLOW_MANUAL_TEXT_INFERENCE = "allowManualTextInference"
        const val ARG_ALLOW_MANUAL_FUNCTION_CALLING = "allowManualFunctionCalling"
        const val PROMPT = """
            Use the native tool named log_visit exactly once.
            Arguments:
            patientId: patient-meena
            observationText: Severe headache, blurred vision, BP 150/95, reduced fetal movement.
            protocolCitation: WHO ANC Recommendation B1.2
            referralRequired: true
            Do not save data anywhere else. This is not a diagnosis.
        """
    }
}
