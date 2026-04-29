# LiteRT-LM Status

This document is the current judge-facing status of Smriti's LiteRT-LM integration.

## Current State

- LiteRT-LM dependency is pinned in `app/build.gradle.kts`:
  `com.google.ai.edge.litertlm:litertlm-android:0.10.2`
- Room annotation processing uses KSP `2.3.7`; KAPT is no longer applied in the app module.
- `MockGemmaAgent` remains the default app mode.
- `RealGemmaAgent` remains experimental and disabled for the demo path.
- The app checks for the expected model path:
  `filesDir/models/gemma-4-E2B-it-int4.litertlm`
- If the model is absent, Offline Proof says `Real Gemma model: Not found`.
- If a model is present, Offline Proof says `Found, not loaded`.
- Direct LiteRT-LM API types now compile through a passive type probe:
  `Engine`, `EngineConfig`, `Backend`, `Content.Text`, `Content.AudioBytes`, `Content.AudioFile`, `InputData.Audio`, `Conversation`, `ConversationConfig`, `ToolCall`, and `OpenApiTool`.
- `LiteRtEngineConfigFactory` constructs a real `EngineConfig` with `Backend.CPU()` only when the model file is found.
- `LiteRtEngineInitializationChecker` can initialize and immediately close `Engine` only when an explicit manual-test flag is true.
- `LiteRtGemmaTextClient.generateTextManual(...)` can run one text-only `sendMessage` call only when an explicit manual inference flag is true.
- Developer-only RealGemma text UI mode can call the same text path only when both the disabled-by-default build gate and the app-private local gate are enabled.
- `ManualLiteRtTextInferenceInstrumentedTest` is the only developer harness for the first real text inference attempt with a sideloaded model.
- `ManualRealGemmaBenchmarkInstrumentedTest` is the manual benchmark harness for real text inference reliability and latency.
- `ManualRealGemmaMemoryStressInstrumentedTest` is the manual 10/20/40 prior-visit context stress harness.
- `ManualLiteRtFunctionCallingInstrumentedTest` probes native tool/function calling with `OpenApiTool` and `ConversationConfig(tools=...)`.
- `ManualLiteRtAudioCapabilityInstrumentedTest` probes the exposed audio API surface.
- `ManualLiteRtAudioInferenceInstrumentedTest` is the separate real-audio manual inference harness.
- No model file is committed to the repository.

## Manual-Only Engine Work

Smriti still does not run LiteRT inference in normal app behavior:

- No `.litertlm` model loading.
- No `Engine` instantiation during app startup or normal UI flow.
- No `Engine.initialize()` during app startup or normal UI flow.
- No Conversation creation during app startup or normal UI flow.
- No inference or message sending during app startup or normal UI flow.
- No Hugging Face or model download code.

The manual checker requires all of the following before it touches `Engine`: a found app-private model file, a prepared `EngineConfig`, and `allowManualEngineInitialization = true`. `Engine` implements `AutoCloseable`, so the checker uses `use { initialize() }` to close it immediately after initialization. This path is not wired into Patient Roster, Visit, Review, Summary, or any visible app toggle.

Manual text inference is similarly explicit: `LiteRtGemmaTextClient.generateText(...)` still returns unavailable by default, while `generateTextManual(...)` requires a found model, prepared `EngineConfig`, and `allowManualTextInference = true`. The manual path initializes `Engine`, creates one `Conversation`, sends one text prompt, extracts `Content.Text`, and closes both Conversation and Engine. Developer-only RealGemma text UI mode reuses this path only after both local developer gates are enabled. It is not the default demo flow and has no public CHW-facing toggle.

## Function Calling Status

LiteRT-LM Android `0.10.2` exposes native function/tool classes:

- `OpenApiTool`
- `ToolCall`
- `ToolProvider`
- `ToolManager`
- `ConversationConfig(tools=..., automaticToolCalling=...)`

Smriti added a manual probe, `ManualLiteRtFunctionCallingInstrumentedTest`, that registers a native `log_visit` tool and asks the model to call it. This is a real native tool-call probe, not JSON prompting. On the current Phase 1 device run, the model executed `log_visit` once and supplied `patientId`, `observationText`, `protocolCitation`, and `referralRequired`. The app still uses the strict JSON prompt/parser path as the safe fallback until native tool calling is productized safely. It is not wired into app startup or UI.

## Memory Stress Status

`ManualRealGemmaMemoryStressInstrumentedTest` generates synthetic Room-style prior visit history at 10, 20, and 40 compressed visits. It uses `RealGemmaPromptBuilder(maxHistoryVisits = visitCount, historyFormatter = RealGemmaHistoryFormatter.Compact)` so larger contexts are included as single-line numbered entries, then runs the manual RealGemmaAgent path with an extra strict JSON-only reminder. It logs prompt length, latency, raw output length, parser status, citation/safety/referral/uncertainty flags, malformed-JSON/citation/safety failure categories, invalid-output previews for parser failures, and approximate JVM memory before/after.

The previous Phase 1 run completed all 10/20/40 contexts without crashing, but JSON reliability was only 1/3 because the 10-visit and 40-visit cases returned invalid JSON. The updated harness tightened history compression and output-format instructions, kept outputs under a compact character budget, and added narrow parser tolerance for harmless JSON numbers plus missing nullable `clarificationPrompt`. The latest manual run reached 3/3 parser success: all 10/20/40 contexts had citations, referral flags, safety wording, and no failure categories. Diagnostic-language and invented-citation rejection remain strict.

This is manual-only and does not write outputs to Room.

## Audio API Status

LiteRT-LM Android `0.10.2` exposes audio-capable classes:

- `Content.AudioBytes`
- `Content.AudioFile`
- `InputData.Audio`
- `Session.generateContent(...)` over `InputData`
- `Conversation.sendMessage(Contents)` with `Content.AudioFile` / `Content.AudioBytes`

Smriti added `ManualLiteRtAudioCapabilityInstrumentedTest` to log this API surface. It does not claim Gemma 4 E2B transcription quality by itself.

Audio preprocessing investigation for the local `litertlm-android-0.10.2` AAR found no public class or method named like `AudioPreprocessor`, `AudioProcessor`, `Preprocessor`, or `preprocess(...)` in `classes.jar`. The public audio holders are raw containers. The current Kotlin API path also does not expose prompt-template customization needed for multimodal placeholder injection. The runtime raw-audio attempt failed with:

```text
LiteRtLmJniException: Failed to generate content: INTERNAL: Audio must be preprocessed before being used in SessionAdvanced.
```

`ManualLiteRtAudioInferenceInstrumentedTest` is the separate real-audio manual harness. It requires `allowManualAudioInference=true`, `manualAudioFilePath=/data/local/tmp/manual-smriti-audio.wav`, and the sideloaded app-private Gemma model. It now tries the `Conversation.sendMessage(Contents.of(Content.Text(...), Content.AudioFile(...)))` route first, then the raw `Session.generateContent(InputData.Text(...), InputData.Audio(...))` route. If both routes hit the preprocessing requirement, the test logs and skips as blocked instead of claiming transcription works.

Current audio status: API surface available, real raw-audio runtime blocked until a public/wired LiteRT-LM audio preprocessing and prompt-template path is identified. Direct Gemma 4 audio transcription is therefore blocked by the current public Gemma 4 LiteRT-LM Android/Kotlin artifact/API limitations, not enabled in the normal app flow, and not claimed as working.

Phase 2 starts the fallback architecture for voice visits:

```text
local voice/audio source -> offline ASR or manual transcript -> transcript text -> local protocol retrieval -> Gemma 4 text reasoning -> structured visit result -> CHW review/confirm save
```

The new Phase 2 core keeps audio capture local and modular:

- `SpeechToTextClient` defines a sealed transcript result: success, unavailable, or error.
- `SimulatedTranscriptClient` keeps the demo/sample transcript path local and deterministic.
- `AndroidOfflineSpeechRecognizerClient` supports a live Android `SpeechRecognizer` fallback from the Visit screen. It requests `RecognizerIntent.EXTRA_PREFER_OFFLINE=true`, checks recognizer availability, prefers on-device recognition when available, falls back across `en-IN`, `en-US`, and `en`, returns offline speech metadata on success, treats network/server/no-match recognizer errors as unavailable/manual-transcript-needed, maps missing language packs to friendly UI text, and never uploads audio or adds cloud ASR.
- Android `SpeechRecognizer` still does not provide a reliable direct transcription route for the app-private recorded `.m4a` files in this implementation. Stored audio-file transcription therefore returns unavailable until a local file ASR engine or a verified OS offline-pack file path is wired.
- `VisitReasoningPipeline` is UI-independent and coordinates transcript text or local audio path, local `ProtocolRetriever`, injected `GemmaAgent`, and structured `VisitReasoningResult`.
- The pipeline does not write to Room. CHW review/confirm remains the only save path.

No audio file is committed, and no audio/Gemma path is wired into the normal app flow. The normal visible flow uses editable manual/sample transcript input as the reliable default, with Try Offline Speech as a safe live-device fallback that only fills the editable transcript field before `VisitReasoningPipeline`.

## Phase 2 Completion Status

Phase 2 is complete for the submission-ready local core flow:

- Local transcript path works through manual/sample input.
- Android offline speech fallback is attempted safely and falls back to manual/sample text when unavailable.
- `VisitReasoningPipeline` is integrated into the normal Generate Local Visit Note action.
- Local protocol retrieval grounds `MockGemmaAgent` visit-note/referral output.
- CHW review/confirm remains the only save path.
- Confirmed local visits and referral flags persist and appear in later patient history.
- Supervisor summary reads fresh confirmed local data and reports concise latest-per-patient urgent cases.
- Reset Demo Data clears saved visits/referrals and restores seeded Meena history.
- `MockGemmaAgent` remains default; RealGemma and LiteRT-LM inference remain manual-only.
- Developer-only RealGemma text mode is available only when both local developer gates are enabled; `MockGemmaAgent` remains the default.

## Developer-Only RealGemma Text UI Mode

Activation requires both gates:

- Build-time gate: build debug with `-Psmriti.realGemmaDevMode=true`.
- Local/internal gate: create `files/dev/enable_real_gemma_text_mode` in app-private storage.

When both gates are enabled, VisitScreen shows `RealGemmaAgent / Developer-only / Experimental`, model status, inference status, CPU backend, and the warning:

```text
Developer-only RealGemma text mode. Not default demo mode. Output must be reviewed before saving.
```

If the model is present, VisitScreen generation uses `RealGemmaAgent` through `VisitReasoningPipeline`, then shows the output on ReviewScreen. CHW review/confirm/save is still required. If the model is missing, inference fails, times out, or output is rejected, the result is safe/uncertain and nothing is saved automatically.

The instrumentation harness uses the debug application ID `com.smriti.clinicalscribe`, the app-private path `filesDir/models/gemma-4-E2B-it-int4.litertlm`, and the non-clinical prompt `Reply with exactly: SMRITI_LITERT_OK`. It must be run explicitly with:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtTextInferenceInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true
```

Logcat output is tagged:

```powershell
adb logcat -s SmritiLiteRtManualTest:I "*:S"
```

## Why Direct API Use Is Deferred

The LiteRT-LM artifact exposes Java 21 classfiles. The app previously used KAPT for Room, and direct references to LiteRT-LM runtime classes could trigger KAPT classfile compatibility failures. Room now uses KSP, so direct type references and `EngineConfig` construction compile. Runtime `Engine` initialization remains manual-only.

JDK 21 is required for direct LiteRT-LM API compile work.

## Readiness Guard

`RealGemmaReadinessEvaluator` is the safety gate. It reports judge-readable readiness while keeping:

- model loading disallowed in normal app flow,
- inference disallowed by default,
- engine creation false,
- engine initialization false,
- conversation creation false,
- sendMessage/inference false.

## Demo Position

The current hackathon demo proves the offline product flow and safety model. Real LiteRT-LM inference is a planned next step, not an active runtime path.
