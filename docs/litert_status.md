# LiteRT-LM Status

This document is the current judge-facing status of Smriti's LiteRT-LM integration.

## Current State

- LiteRT-LM dependency is pinned in `app/build.gradle.kts`:
  `com.google.ai.edge.litertlm:litertlm-android:0.11.0`
- Room annotation processing uses KSP `2.3.7`; KAPT is no longer applied in the app module.
- `RealGemmaAgent` is the app-facing reasoning engine.
- If RealGemma setup is missing or inference fails, the UI shows setup/retry messaging instead of mock output.
- The app checks for the expected model path:
  `filesDir/models/gemma-4-E2B-it-int4.litertlm`
- If the model is absent, Offline Proof says `Real Gemma model: Not found`.
- If a model is present, Offline Proof says `Real Gemma model: Found`, `Engine: Loads on demand`, and `Inference: Enabled; on-device RealGemma text reasoning` when gates are active. After a successful generation in the app session, engine status can show `Loaded`.
- Direct LiteRT-LM API types now compile through a passive type probe:
  `Engine`, `EngineConfig`, `Backend`, `Content.Text`, `Content.ImageBytes`, `Content.ImageFile`, `Content.AudioBytes`, `Content.AudioFile`, `InputData.Image`, `InputData.Audio`, `Conversation`, `ConversationConfig`, `ToolCall`, `OpenApiTool`, `SamplerConfig`, `SessionConfig`, `Capabilities`, and `ExperimentalFlags`.
- Local 0.11.0 AAR inspection confirms `ExperimentalFlags.enableSpeculativeDecoding` and `Capabilities.hasSpeculativeDecodingSupport()` are public. No separate public draft-model, target-model, MTP-specific, or multi-token configuration class was confirmed in the name/signature scan.
- `LiteRtEngineConfigFactory` defaults to a real `EngineConfig` with `Backend.CPU()` when the model file is found.
- `LiteRtEngineInitializationChecker` can initialize and immediately close `Engine` only when an explicit manual-test flag is true.
- `LiteRtGemmaTextClient.generateTextManual(...)` can run one text-only `sendMessage` call only when an explicit manual inference flag is true.
- RealGemma text UI mode can call the same text path only when the submission build gate, app-private local gate, and app-private model are present.
- `ManualLiteRtTextInferenceInstrumentedTest` is the only developer harness for the first real text inference attempt with a sideloaded model.
- `ManualRealGemmaBenchmarkInstrumentedTest` is the manual benchmark harness for real text inference reliability and latency.
- `ManualRealGemmaMemoryStressInstrumentedTest` is the manual 10/20/40 prior-visit context stress harness.
- `ManualLiteRtFunctionCallingInstrumentedTest` probes native tool/function calling with `OpenApiTool` and `ConversationConfig(tools=...)`.
- `ManualLiteRtProtocolToolCallingInstrumentedTest` exposes local `ProtocolRetriever` as a native manual-only protocol lookup tool.
- `ManualLiteRtAudioCapabilityInstrumentedTest` probes the exposed audio API surface.
- `ManualLiteRtAudioInferenceInstrumentedTest` is the separate real-audio manual inference harness.
- `ManualRealGemmaVisionProbeInstrumentedTest` is the separate gated image-input runtime probe.
- No model file is committed to the repository.

## Manual-Only Engine Work

Smriti runs LiteRT text inference only when required local setup is complete:

- No `.litertlm` model loading.
- No `Engine` instantiation during app startup or normal UI flow.
- No `Engine.initialize()` during app startup or normal UI flow.
- No Conversation creation during app startup or normal UI flow.
- No inference or message sending unless the CHW taps Generate and local setup is complete.
- No Hugging Face or model download code.

The manual checker requires all of the following before it touches `Engine`: a found app-private model file, a prepared `EngineConfig`, and `allowManualEngineInitialization = true`. `Engine` implements `AutoCloseable`, so the checker uses `use { initialize() }` to close it immediately after initialization. This path is not wired into Patient Roster, Visit, Review, Summary, or any visible app toggle.

Manual text inference is similarly explicit: `LiteRtGemmaTextClient.generateText(...)` still returns unavailable by default, while `generateTextManual(...)` requires a found model, prepared `EngineConfig`, and `allowManualTextInference = true`. The manual path initializes `Engine`, creates one `Conversation`, sends one text prompt, extracts `Content.Text`, and closes both Conversation and Engine. The RealGemma-required app-facing path reuses this manual text path only after the submission build flag, app-private sentinel, and app-private model are present.

## Function Calling Status

LiteRT-LM Android `0.11.0` exposes native function/tool classes:

- `OpenApiTool`
- `ToolCall`
- `ToolProvider`
- `ToolManager`
- `ToolSet`
- `ToolParam`
- `ConversationConfig(tools=..., automaticToolCalling=...)`

Smriti added a manual probe, `ManualLiteRtFunctionCallingInstrumentedTest`, that registers a native `log_visit` tool and asks the model to call it. This is a real native tool-call probe, not JSON prompting. On the current Phase 1 device run, the model executed `log_visit` once and supplied `patientId`, `observationText`, `protocolCitation`, and `referralRequired`. The app still uses the strict JSON prompt/parser path as the safe fallback until native tool calling is productized safely. It is not wired into app startup or UI.

`ReflectionTool` is present in the `0.11.0` AAR, but it is Kotlin-internal from Smriti's app code, so the manual probes use the public `OpenApiTool` route.

Smriti also added `ManualLiteRtProtocolToolCallingInstrumentedTest` as a Phase 9 manual/developer-only probe. It registers `lookupProtocol(query, countryCode, region)` through `OpenApiTool`, calls the existing local `ProtocolRetriever` over `protocols/maternal_health_demo_protocols.json`, and returns compact local guidance with citation, topic, snippet, and country/region context. It logs under `SmritiProtocolToolCall`.

Current protocol tool-call status: manual LiteRT-LM protocol tool-calling probe validated. The connected probe used `OpenApiTool + tool(...) + ConversationConfig(tools, automaticToolCalling=true)`, registered local tool `lookupProtocol`, and Gemma called it with:

```json
{"countryCode":"IN","query":"severe headache blurred vision high blood pressure pregnancy danger signs","region":"INDIA"}
```

The tool returned local citation `Smriti Demo Maternal Health Protocol Danger Signs 1.1`. Safety boundary logged: manual probe only; no diagnosis, no save, no Room write, no referral flag. Production retrieval remains deterministic `ProtocolRetriever` inside `VisitReasoningPipeline` before RealGemma prompting; tool-calling is not required for the normal app flow.

## Memory Stress Status

`ManualRealGemmaMemoryStressInstrumentedTest` generates synthetic Room-style prior visit history at 10, 20, and 40 compressed visits. It uses `RealGemmaPromptBuilder(maxHistoryVisits = visitCount, historyFormatter = RealGemmaHistoryFormatter.Compact)` so larger contexts are included as single-line numbered entries, then runs the manual RealGemmaAgent path with an extra strict JSON-only reminder. It logs prompt length, latency, raw output length, parser status, citation/safety/referral/uncertainty flags, malformed-JSON/citation/safety failure categories, invalid-output previews for parser failures, and approximate JVM memory before/after.

The previous Phase 1 run completed all 10/20/40 contexts without crashing, but JSON reliability was only 1/3 because the 10-visit and 40-visit cases returned invalid JSON. The updated harness tightened history compression and output-format instructions, kept outputs under a compact character budget, and added narrow parser tolerance for harmless JSON numbers plus missing nullable `clarificationPrompt`. The latest manual run reached 3/3 parser success: all 10/20/40 contexts had citations, referral flags, safety wording, and no failure categories. Diagnostic-language and invented-citation rejection remain strict.

This is manual-only and does not write outputs to Room.

## Audio API Status

LiteRT-LM Android `0.11.0` exposes audio-capable classes:

- `Content.AudioBytes`
- `Content.AudioFile`
- `InputData.Audio`
- `Session.generateContent(...)` over `InputData`
- `Conversation.sendMessage(Contents)` with `Content.AudioFile` / `Content.AudioBytes`
- `EngineConfig.audioBackend` — **new in 0.11.0** (was absent in 0.10.2)

Smriti added `ManualLiteRtAudioCapabilityInstrumentedTest` to log this API surface. It does not claim Gemma 4 E2B transcription quality by itself.

Audio preprocessing investigation for the local `litertlm-android-0.11.0` AAR found no public class or method named like `AudioPreprocessor`, `AudioProcessor`, `Preprocessor`, or `preprocess(...)` in `classes.jar`. However, `EngineConfig.audioBackend` is now a public field, and `ExperimentalFlags.overwritePromptTemplate` is also available. The runtime raw-audio attempt with 0.10.2 previously failed with:

```text
LiteRtLmJniException: Failed to generate content: INTERNAL: Audio must be preprocessed before being used in SessionAdvanced.
```

`ManualLiteRtAudioInferenceInstrumentedTest` is the separate real-audio manual harness. It requires `allowManualAudioInference=true`, `manualAudioFilePath=/data/local/tmp/manual-smriti-audio.wav`, and the sideloaded app-private Gemma model. It tries the `Conversation.sendMessage(Contents.of(Content.Text(...), Content.AudioFile(...)))` route first, then the raw `Session.generateContent(InputData.Text(...), InputData.Audio(...))` route.

`ManualRealGemmaAudioTranscriptInstrumentedTest` is the Phase 6 transcript-extraction probe, updated for 0.11.0. It now sets `audioBackend = Backend.CPU()` in the EngineConfig and prioritises Route 2 (Conversation+AudioBytes with raw WAV bytes) first, followed by Conversation+AudioFile, Session+InputData.Audio, and WAV PCM-only extraction. It requires `allowManualAudioInference=true`, `manualAudioFilePath=/data/local/tmp/manual-smriti-audio.wav`, and the sideloaded app-private Gemma model. If any route succeeds, it logs a transcript preview under `SmritiGemmaAudioTranscript`. If all routes are blocked, it logs the blocker and skips. The probe does not write to Room, invoke the visit reasoning pipeline, or change any default app behavior.

Current audio status: Gemma audio transcription is wired into the app-facing Visit screen after the LiteRT-LM 0.11.0 manual probe succeeded. The app records short local microphone audio as 16 kHz mono PCM, wraps it as WAV bytes in memory, and calls `Conversation.sendMessage(Contents.of(Content.Text(prompt), Content.AudioBytes(audioBytes)))` with `EngineConfig.audioBackend = Backend.CPU()`. Audio fills an editable transcript only. Clinical note generation still goes through text reasoning, protocol citation validation, ReviewScreen, and confirm/save after the CHW manually taps Generate Visit Note. No audio-only save path. No direct audio diagnosis, treatment, or referral.

## Vision API Status

LiteRT-LM Android `0.11.0` exposes image-capable classes and config fields:

- `Content.ImageBytes`
- `Content.ImageFile`
- `InputData.Image`
- `EngineConfig.visionBackend`
- `EngineConfig.maxNumImages`
- `Session.generateContent(...)` over `InputData`
- `Conversation.sendMessage(Contents)` with `Content.ImageBytes` / `Content.ImageFile`

The local AAR/classes.jar inspection did not find a public class or method named like `PromptTemplate`, `MediaPlaceholder`, `MultiModalTemplate`, `ImagePreprocessor`, or `preprocess(...)`. This is the same risk area as direct audio: multimodal Gemma paths may require prompt-template/media-placeholder handling not exposed by the current Kotlin/JNI surface.

`ManualRealGemmaVisionProbeInstrumentedTest` was added as a gated androidTest. It requires `allowManualVisionInference=true`, a sideloaded app-private model, and the synthetic `app/src/androidTest/assets/sample_paper_visit_note.png` asset. The test reads that asset from `InstrumentationRegistry.getInstrumentation().context.assets`, tries the `Conversation` image-content route and the raw `Session` image-input route, logs under `SmritiRealGemmaVision`, and never saves anything.

Current vision status: the manual probe passed on emulator. The engine accepted the `Conversation` image input path, and local Gemma 4 vision extracted structured JSON from the synthetic paper note: Grace Achieng, 02 May 2026, BP 116/74, symptoms, routine ANC follow-up, confidence HIGH, and `needsReview=true`.

The app now includes a narrow paper-note scan flow using `RealGemmaVisionPaperNoteClient` and `PaperNoteVisionParser`. It is data-entry support only. CHW review/edit and explicit patient-record confirmation are required before saving to local history with `transcriptSource=paper_scan`. The flow does not call visit-note referral generation, does not call supervisor priority reasoning, does not persist image bytes, and does not use cloud OCR/API.

## Backend Latency Experiment

Stable text inference remains CPU by default. The pinned LiteRT-LM artifact exposes `Backend.GPU()`, and Smriti now keeps it behind `LiteRtBackendMode.GPU_EXPERIMENTAL` plus the manual-only `ManualRealGemmaBackendLatencyInstrumentedTest`.

The experiment logs CPU and optional GPU timings under `SmritiBackendLatency` and `SmritiLatency`. If GPU crashes, is unsupported, or does not improve timing meaningfully, CPU remains the documented stable backend. The app does not invent generation-options APIs or remove CPU fallback.

## Speculative Decoding / MTP Probe

LiteRT-LM Android `0.11.0` exposes a usable Kotlin/Android-level speculative hook through:

- `ExperimentalFlags.enableSpeculativeDecoding`
- `Capabilities.hasSpeculativeDecodingSupport()`

Smriti added `ManualRealGemmaSpeculativeLatencyInstrumentedTest` as a manual/developer-only latency probe. It requires `allowManualTextInference=true` and `allowSpeculativeDecoding=true`, runs CPU baseline first, checks model capability support, then runs CPU with speculative/MTP enabled only if the model reports support. Optional GPU + speculative remains behind `allowExperimentalGpuBackend=true`.

The probe logs under `SmritiSpeculativeLatency` and `SmritiLatency`, uses existing safe RealGemma text scenarios, runs through the existing RealGemma parser, citation validation, and safety post-processing, and does not write to Room or update UI.

Manual CPU benchmark result:

- CPU baseline: 21787 ms
- CPU speculative/MTP: 22138 ms
- Delta: +351 ms slower

The first manual CPU speculative run did not improve latency on this emulator. CPU remains the stable default app path. Do not claim speculative decoding speedup.

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
- Local protocol retrieval grounds RealGemma visit-note/referral prompts.
- CHW review/confirm remains the only save path.
- Confirmed local visits and referral flags persist and appear in later patient history.
- Supervisor summary reads fresh confirmed local data and reports concise latest-per-patient urgent cases.
- Reset Demo Data clears saved visits/referrals and restores seeded Meena history.
- `RealGemmaAgent` is required for app-facing reasoning; missing setup shows retry/setup messaging.
- No app-facing mock fallback is shown as clinical output.

## RealGemma Required Text Mode

Activation requires local setup:

- Build-time gate: build debug with `-Psmriti.realGemmaSubmissionMode=true`.
- Local/internal gate: create `files/dev/enable_real_gemma_text_mode` in app-private storage.

When setup is complete, VisitScreen generation uses `RealGemmaAgent` through `VisitReasoningPipeline`, then shows the output on ReviewScreen. CHW review/confirm/save is still required. If the model is missing, inference fails, times out, or output is rejected, the app shows retry/setup messaging and nothing is saved automatically.

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
- inference allowed only when local setup is complete,
- engine creation false,
- engine initialization false,
- conversation creation false,
- sendMessage/inference false.

## Demo Position

The current hackathon demo proves the offline product flow and safety model with RealGemma required for app-facing reasoning. EngineConfig is constructed when the app-private model is present. Runtime text inference is attempted only after local setup is complete and the CHW requests generation.

GPU backend probing has not been added for the final video pass because the existing stable manual path is CPU-oriented and adding a new backend path would expand the pre-video test surface. CPU backend is retained for the stable demo; GPU backend benchmarking remains future work.

