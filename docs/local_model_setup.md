# Local Model Setup

Smriti is prepared for manual Gemma `.litertlm` sideloading. In fully gated RealGemma submission mode, the app can preload and run the sideloaded app-private model for local text reasoning. Manual instrumentation tests remain available for direct developer validation.

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
- If present, the app reports the model as found. Engine status says `Loads on demand`, `Preparing`, `Ready`, `Failed`, or `Loaded`; it should not say `Found, not loaded`.
- The LiteRT layer defaults to stable `EngineConfig` with `Backend.CPU()` when the model is found.
- Experimental `Backend.GPU()` timing is available only through explicit developer/test configuration; it is not the default app path and must keep CPU fallback intact.
- Passive direct LiteRT API type references compile after the Room KSP migration.
- With all RealGemma gates active, app startup/Patient Roster can start a non-blocking background preload. It initializes the RealGemma engine/session without generating clinical output and reuses that shared client for visit reasoning and supervisor priority attempts.
- If preload fails, the UI reports `Engine: Failed` and generation still follows the existing RealGemma unavailable/retry path; the app must not crash or fall back to mock output.
- The first RealGemma call may be slower because model initialization is expensive. Subsequent Meena, Grace, Lucia, Priya, and supervisor priority calls should be faster when the process keeps the engine alive.
- Manual text inference requires `allowManualTextInference=true`.
- RealGemma text UI mode requires `-Psmriti.realGemmaSubmissionMode=true` at build time and an app-private sentinel file at `files/dev/enable_real_gemma_text_mode`.
- Recorded-demo RealGemma visit-note prompts use the selected patient's `preferredLanguage` for English, Hindi, Swahili, or Spanish output; citation IDs remain English/stable.
- `RealGemmaAgent` is the app-facing reasoning engine; missing setup returns unavailable/retry messaging instead of mock clinical output.
- Confirm/save is local Room/SQLite only. It does not call RealGemma, rebuild protocol retrieval, or automatically export JSON.
- `SmritiLatency` logs readiness, preload/init, retrieval, prompt, generation, validation, navigation, save, and summary-refresh timings without transcript text or PHI.
- Latest measured emulator/local setup timings from `SmritiLatency`: preload/init 1.885 s; Meena RealGemma generation 21.726 s; Meena parser/safety/citation validation 31 ms; Meena Room save 49 ms; Meena summary refresh 5 ms; Lucia RealGemma generation after preload/reuse 14.434 s; Lucia validation 4 ms; protocol retrieval 1-2 ms; prompt build 1-3 ms. Device performance may vary.
- Interpretation: RealGemma inference is the main latency cost. Local protocol retrieval, prompt build, validation, and save are milliseconds. The second generation was faster after preload/engine reuse.
- Generation options note: the current app path calls LiteRT-LM through `Conversation.sendMessage(prompt)` and does not have a wired, tested API for temperature or max-output-token settings. Smriti constrains output with compact prompts, a strict JSON schema, and parser/safety/citation validation rather than inventing unsupported API calls.

## RealGemma Text UI Mode

This is the local submission path for app-facing RealGemma text reasoning. It has no public CHW-facing toggle; it is controlled by the build flag, app-private sentinel, and app-private model file.

Build with the submission build gate enabled:

```powershell
.\gradlew.bat assembleDebug -Psmriti.realGemmaSubmissionMode=true
```

Create the app-private local gate after installing the debug build:

```powershell
adb shell run-as com.smriti.clinicalscribe mkdir -p files/dev
adb shell run-as com.smriti.clinicalscribe touch files/dev/enable_real_gemma_text_mode
```

With both gates enabled and the app-private model present, VisitScreen generation uses `RealGemmaAgent` through `VisitReasoningPipeline`. The output still appears on ReviewScreen and must be confirmed before saving. If the model is missing or inference fails, the app shows setup/retry messaging, preserves the transcript, and does not save or display mock clinical output.

Remove the local gate to force RealGemma setup-required behavior:

```powershell
adb shell run-as com.smriti.clinicalscribe rm files/dev/enable_real_gemma_text_mode
```

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

It builds a realistic Meena ANC prompt with `RealGemmaPromptBuilder`, demo prior history, supplied local protocol chunks, exact-JSON schema instructions, non-diagnostic wording rules, protocol-citation requirements, and CHW-confirmation language. It calls `LiteRtGemmaTextClient.generateTextManual(...)`, logs raw model output, and runs `RealGemmaOutputParser`.

The JSON test fails if the model is missing, inference fails, output is empty, parser rejection occurs, referral support is absent for the Meena danger-sign case, citation is absent, or safety wording is missing. If parser rejection happens, the app behavior is still safe: invalid output is rejected and not saved.

Run only the manual JSON instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaVisitJsonInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true
```

View the manual JSON test output:

```powershell
adb logcat -s SmritiRealGemma:I "*:S"
```

## Manual RealGemmaAgent End-to-End Test

After the structured JSON harness works, run the full RealGemma agent path:

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

## Manual CPU/GPU Backend Latency Experiment

CPU remains the stable backend. The GPU experiment is isolated and opt-in; do not use it for the filmed build unless it succeeds on the target device/emulator and shows meaningful stable improvement.

Run CPU baseline only:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaBackendLatencyInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

Run CPU plus experimental GPU for one or two RealGemma calls:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaBackendLatencyInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true" "-Pandroid.testInstrumentationRunnerArguments.allowExperimentalGpuBackend=true" "-Pandroid.testInstrumentationRunnerArguments.backendScenarioLimit=2"
```

View logs:

```powershell
adb logcat -s SmritiBackendLatency:I SmritiLatency:I "*:S"
```

If GPU crashes, is unsupported, or has no meaningful improvement, keep CPU documented as stable. The experiment uses real `Backend.GPU()` from the pinned LiteRT-LM artifact and does not invent unsupported generation APIs.

## Manual Multilingual RealGemma Test

Phase C adds a manual multilingual validation harness for the recorded demo:

```text
app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualRealGemmaMultilingualInstrumentedTest.kt
```

It requires the sideloaded app-private model and `allowManualTextInference=true`. The harness runs Meena/Hindi, Grace/Swahili, and Lucia/Spanish scenarios, builds prompts from each patient's `preferredLanguage`, runs manual LiteRT text inference, parses the result, applies language-specific safety post-processing through `RealGemmaAgent`, verifies a protocol citation is present, logs a raw output preview, logs parser status, logs the requested language, and logs a simple heuristic for whether output appears to use the requested language.

Run only the manual multilingual instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaMultilingualInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

View the multilingual output:

```powershell
adb logcat -s SmritiRealGemmaLang:I "*:S"
```

Do not claim a filmed language until this harness passes for that language. If a language fails manual validation, remove it from the filmed demo and docs claim. Protocol citation IDs should remain English/stable. No cloud translation API is used.

## Recorded-Demo Submission Mode

RealGemma submission mode is stricter than the normal default and is intended only for filmed validation with a sideloaded local model. All three gates must be present:

- build flag: `-Psmriti.realGemmaSubmissionMode=true`,
- app-private sentinel: `files/dev/enable_real_gemma_text_mode`,
- app-private model: `filesDir/models/gemma-4-E2B-it-int4.litertlm`.

Setup commands after sideloading the model outside git:

```powershell
.\gradlew.bat assembleDebug -Psmriti.realGemmaSubmissionMode=true
adb shell run-as com.smriti.clinicalscribe mkdir -p files/dev
adb shell run-as com.smriti.clinicalscribe touch files/dev/enable_real_gemma_text_mode
adb shell run-as com.smriti.clinicalscribe ls -lh files/models/gemma-4-E2B-it-int4.litertlm
```

If RealGemma fails, times out, returns invalid JSON, or fails citation/safety validation, the app preserves the transcript and shows `On-device reasoning unavailable - please retry.` It does not silently replace the recorded RealGemma path with mock output.

## Manual Supervisor Priority Queue Test

The supervisor priority harness sends today's confirmed local visits, referral flags, missed follow-ups, history signals, patient context, and supplied protocol citations through the RealGemma priority prompt/parser path. It is manual-only and does not run in standard validation.

Run only the manual supervisor priority instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaSupervisorPriorityInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

View the priority output:

```powershell
adb logcat -s SmritiRealGemmaPriority:I "*:S"
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

## Manual Vision Probe

Current status: the `litertlm-android-0.10.2` AAR/classes.jar exposes `Content.ImageBytes`, `Content.ImageFile`, `InputData.Image`, `EngineConfig.visionBackend`, and `EngineConfig.maxNumImages`, but no public prompt-template, media-placeholder, multimodal-template, image-preprocessor, or `preprocess(...)` API was found. The manual probe passed on emulator with a sideloaded app-private model: the engine accepted `Conversation` image input and local Gemma 4 vision extracted structured JSON from the synthetic paper note.

Smriti now exposes a narrow paper-note scan flow for data entry only. It requires CHW review before local save, does not save image bytes, and does not generate diagnosis or referral advice from the image.

The probe uses only the synthetic androidTest asset `sample_paper_visit_note.png` and reads it from instrumentation context assets, not target app assets.

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaVisionProbeInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualVisionInference=true"
```

Logcat:

```powershell
adb logcat -s SmritiRealGemmaVision:I "*:S"
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

`LiteRtGemmaTextClient.generateTextManual(...)` is the separate text-inference helper. It requires a found model, prepared `EngineConfig`, and `allowManualTextInference=true`. It creates one Conversation, sends one text prompt, extracts text, and closes Conversation and Engine. It is used by manual instrumentation tests and by the RealGemma-required app-facing path only when the submission build flag, local gate, and app-private model are present.

## Development Note

JDK 21 is required for LiteRT-LM compile/API work because `litertlm-android-0.10.2` exposes Java classfile version 65 API classes.
