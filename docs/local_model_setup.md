# Local Model Setup

Smriti is prepared for manual Gemma `.litertlm` sideloading, but the normal app does not load or run a model. The only real text inference entry points are developer-run instrumentation tests.

## Safety Rules

- Do not commit model files.
- Do not put model files in `app/src/main/assets/`.
- Do not add runtime download code for the model.
- Do not add Hugging Face download code to the app runtime.
- Field/demo runtime must remain offline.

The expected model filename is:

```text
gemma-4-E2B-it-int4.litertlm
```

The expected app-private runtime path is:

```text
filesDir/models/gemma-4-E2B-it-int4.litertlm
```

For the debug build, the actual application ID is:

```text
com.smriti.clinicalscribe
```

There is currently no debug `applicationIdSuffix`, so `run-as com.smriti.clinicalscribe` targets the debug app-private `filesDir`.

## Current App Behavior

- The app checks whether the expected app-private model file exists.
- If missing, Offline Proof reports the Real Gemma model as not found and inference disabled.
- If present, the app reports the model as found but not loaded.
- The LiteRT layer constructs `EngineConfig` with `Backend.CPU()` only when the model is found.
- Passive direct LiteRT API type references compile after the Room KSP migration.
- Engine initialization is manual-only and requires an explicit test flag.
- Normal app startup and UI screens do not create `Engine`.
- Normal app startup and UI screens do not create a conversation.
- Normal app startup and UI screens do not run inference.
- Manual text inference requires `allowManualTextInference=true`.
- `MockGemmaAgent` remains the safe default.

## Manual Text Inference Test

The simplest developer-only instrumentation harness is:

```text
app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualLiteRtTextInferenceInstrumentedTest.kt
```

It uses `targetContext.filesDir`, checks `filesDir/models/gemma-4-E2B-it-int4.litertlm`, requires the explicit instrumentation argument `allowManualTextInference=true`, sends the non-clinical prompt `Reply with exactly: SMRITI_LITERT_OK`, logs the result to Logcat with tag `SmritiLiteRtManualTest`, and only asserts that returned text is non-empty. It does not touch Room patient or visit data.

Create the app-private model directory:

```powershell
adb shell run-as com.smriti.clinicalscribe mkdir -p files/models
```

Copy the local model through `/data/local/tmp` and into app-private storage:

```powershell
adb push "C:\path\to\gemma-4-E2B-it-int4.litertlm" /data/local/tmp/gemma-4-E2B-it-int4.litertlm
adb shell chmod 644 /data/local/tmp/gemma-4-E2B-it-int4.litertlm
adb shell run-as com.smriti.clinicalscribe cp /data/local/tmp/gemma-4-E2B-it-int4.litertlm files/models/gemma-4-E2B-it-int4.litertlm
adb shell run-as com.smriti.clinicalscribe ls -lh files/models/gemma-4-E2B-it-int4.litertlm
```

Run only the manual instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtTextInferenceInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true
```

View the manual test output:

```powershell
adb logcat -s SmritiLiteRtManualTest:I "*:S"
```

If the instrumentation argument is missing, the test is skipped. If the model is missing, the test is skipped with the expected app-private path in the skip reason. Normal validation should continue to use only `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug`.

## Manual Visit JSON Inference Test

After the simple text test works, run the structured visit JSON harness:

```text
app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualRealGemmaVisitJsonInstrumentedTest.kt
```

It builds a realistic Meena ANC prompt with `RealGemmaPromptBuilder`, demo prior history, supplied local protocol chunks, JSON-only instructions, non-diagnostic wording rules, protocol-citation requirements, and CHW-confirmation language. It calls `LiteRtGemmaTextClient.generateTextManual(...)`, logs raw model output, and runs `RealGemmaOutputParser`.

The JSON test fails if the model is missing, inference fails, or output is empty. Parser rejection does not fail the test yet; it is logged as early model-behavior signal.

Run only the manual JSON instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaVisitJsonInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true
```

View the manual JSON test output:

```powershell
adb logcat -s SmritiRealGemmaJsonTest:I "*:S"
```

## Manual RealGemmaAgent End-to-End Test

After the structured JSON harness works, run the full experimental agent path:

```text
app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualRealGemmaAgentInstrumentedTest.kt
```

It constructs `RealGemmaAgent` with an androidTest-only `RealGemmaTextClient` adapter backed by `LiteRtGemmaTextClient.generateTextManual(...)`. The test feeds the Meena danger-sign scenario through `RealGemmaPromptBuilder`, real manual LiteRT text inference, `RealGemmaOutputParser`, and `VisitReasoningResult`. It requires `allowManualTextInference=true`, fails if the sideloaded model is missing, logs raw model output with tag `SmritiRealGemmaAgentTest`, and does not write to Room patient, visit, or referral tables.

Run only the manual RealGemmaAgent instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaAgentInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true
```

View the manual RealGemmaAgent test output:

```powershell
adb logcat -s SmritiRealGemmaAgentTest:I "*:S"
```

## Manual RealGemma Benchmark Test

To collect judge/demo timing and reliability metrics for the real on-device path, run the manual benchmark harness:

```text
app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualRealGemmaBenchmarkInstrumentedTest.kt
```

It runs three text scenarios through `RealGemmaAgent` backed by `LiteRtGemmaTextClient.generateTextManual(...)`: an ANC danger-sign case, a normal ANC follow-up case, and an incomplete observation case. It requires the sideloaded app-private model and `allowManualTextInference=true`. It logs prompt length, raw output length, raw model `protocolCitation`, single-citation contract adherence, parser success, referral presence, citation presence, safety wording, uncertainty/clarification, per-scenario latency, and an aggregate summary. It does not write benchmark output to Room patient, visit, or referral tables.

Run only the manual benchmark instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaBenchmarkInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

View the benchmark output:

```powershell
adb logcat -s SmritiRealGemmaBenchmark:I "*:S"
```

## Manual Function-Calling Probe

LiteRT-LM Android `0.10.2` exposes native tool/function-calling classes such as `OpenApiTool`, `ToolCall`, `ToolProvider`, and `ConversationConfig(tools=..., automaticToolCalling=...)`. Smriti keeps JSON parsing as the safe fallback until a manual probe proves the model reliably executes a native tool call.

Run only the manual function-calling probe:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtFunctionCallingInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true" "-Pandroid.testInstrumentationRunnerArguments.allowManualFunctionCalling=true"
```

View the function-calling probe output:

```powershell
adb logcat -s SmritiLiteRtFunctionTest:I "*:S"
```

## Manual Memory Stress Benchmark

The memory stress harness builds synthetic Room-style prior visit history with 10, 20, and 40 compact visits. It uses single-line numbered history entries such as `V01: date=..., issue=..., action=..., citation=...`, adds a strict JSON-only reminder, and runs the real manual RealGemmaAgent path. It logs prompt length, latency, output length, parser/citation/safety/referral/uncertainty status, malformed-JSON/citation/safety failure categories, a first-500-character invalid-output preview when parsing fails, and approximate JVM memory before/after.

Run only the manual memory stress instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaMemoryStressInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

View the memory stress output:

```powershell
adb logcat -s SmritiRealGemmaMemory:I "*:S"
```

## Manual Audio Capability Probe

LiteRT-LM Android `0.10.2` exposes `Content.AudioBytes`, `Content.AudioFile`, `InputData.Audio`, `Session.generateContent(...)`, and `Conversation.sendMessage(Contents)`. The local AAR inspection did not find a public `AudioPreprocessor`/`AudioProcessor`/`preprocess(...)` API. Smriti does not wire audio into RealGemma or the UI yet. The audio probe logs the available API surface and preprocessing finding without committing audio assets or changing the normal recording flow.

Run the API-surface-only audio probe:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtAudioCapabilityInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualAudioProbe=true"
```

View the audio probe output:

```powershell
adb logcat -s SmritiLiteRtAudioProbe:I "*:S"
```

## Manual Audio Inference Test

Real audio inference is intentionally not part of the normal flow. It requires a manually sideloaded small audio file, the sideloaded Gemma model, and explicit arguments. Do not commit the audio file.

Current status: raw `InputData.Audio` reached runtime, but LiteRT-LM returned `Audio must be preprocessed before being used in SessionAdvanced.` The manual test now tries the `Content.AudioFile` conversation route first and the raw `InputData.Audio` session route second. If no public/wired preprocessing path is available, it logs `Audio runtime blocked: LiteRT-LM requires preprocessing, but preprocessing API was not found/wired in litertlm-android 0.10.2.` and reports skipped/blocked rather than failing or claiming transcription works.

Push a tiny manually recorded WAV file to the emulator:

```powershell
adb push "C:\path\to\manual-smriti-audio.wav" /data/local/tmp/manual-smriti-audio.wav
adb shell ls -lh /data/local/tmp/manual-smriti-audio.wav
```

Run only the manual audio inference instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtAudioInferenceInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualAudioInference=true" "-Pandroid.testInstrumentationRunnerArguments.manualAudioFilePath=/data/local/tmp/manual-smriti-audio.wav"
```

View the manual audio inference output:

```powershell
adb logcat -s SmritiLiteRtAudioInference:I "*:S"
```

Normal validation should continue to use:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

## Manual Engine Check

`LiteRtEngineInitializationChecker` is a developer-only readiness helper. It can instantiate `Engine`, call `initialize()`, and immediately `close()` the engine only when:

- the model file was sideloaded outside git,
- `EngineConfig` was prepared,
- and `allowManualEngineInitialization` is explicitly `true`.

It does not create a Conversation and does not call `sendMessage`.

`LiteRtGemmaTextClient.generateTextManual(...)` is the separate manual text-inference helper. It requires a found model, prepared `EngineConfig`, and `allowManualTextInference=true`. It creates one Conversation, sends one text prompt, extracts text, and closes Conversation and Engine. This is not exposed in the app UI.

## Development Note

JDK 21 is required for LiteRT-LM compile/API work because `litertlm-android-0.10.2` exposes Java classfile version 65 API classes.
