# Current Status

Smriti is an offline Android maternal-health visit copilot prototype for community health workers. The normal demo is complete through Phase 2 and remains demo-safe: `MockGemmaAgent` is the default, RealGemma/LiteRT-LM inference is manual-only, and the core app path does not require internet.

## Phase 1 Status

Phase 1 validated the local Gemma 4 LiteRT-LM stack behind manual instrumentation only.

- LiteRT-LM Android dependency is pinned to `com.google.ai.edge.litertlm:litertlm-android:0.10.2`.
- Room annotation processing uses KSP, so LiteRT-LM API references compile without KAPT classfile failures.
- `EngineConfig` can be prepared with `Backend.CPU()` only when an app-private `.litertlm` model is found.
- Manual text inference exists through `LiteRtGemmaTextClient.generateTextManual(...)`.
- `RealGemmaAgent` has prompt building, strict JSON parsing, citation validation, and safety post-processing.
- The latest accepted manual RealGemma benchmark reported `totalScenarios=3`, `successCount=3`, `parserSuccessCount=3`, `referralCount=1`, `citationCount=2`, `singleCitationContractCount=3`, `averageLatencyMs=15812`, and `maxLatencyMs=26272`.
- Manual probes confirmed native function calling API behavior and long-context memory stress behavior with a sideloaded model.
- Direct Gemma 4 audio remains blocked by LiteRT-LM audio preprocessing requirements.

## Phase 2 Status

Phase 2 is complete for the local core visit flow:

- Patient roster and prior visit history are local.
- Manual/sample transcript entry is the reliable default path.
- Try Offline Speech uses Android `SpeechRecognizer` as a safe offline-preferred fallback.
- `VisitReasoningPipeline` is wired into the normal Generate Local Visit Note action.
- Local protocol retrieval grounds `MockGemmaAgent` output.
- Generated notes, referrals, and citations are editable on the Review screen.
- Nothing is saved until CHW confirm/save.
- Confirmed visits and referral flags persist locally.
- Later Meena visits show the latest confirmed visit first in history.
- Supervisor Summary reads fresh confirmed local data and keeps urgent cases concise.
- Reset Demo Data clears saved visits/referrals and restores seeded Meena history.

## Phase 3 Next

Phase 3 has started with a developer-only RealGemma text UI mode. This is not the default demo path and is not CHW-facing.

Recommended order:

1. Expand and harden the local protocol corpus.
2. Improve offline speech setup guidance and device diagnostics.
3. Capture more developer-mode RealGemma runs and failure cases.
4. Add stronger structured-output telemetry for manual RealGemma runs.
5. Revisit direct Gemma 4 audio only if LiteRT-LM exposes or documents a usable preprocessing and prompt-template path.

## What Is Real

- Android Kotlin + Jetpack Compose app.
- Room/SQLite local storage.
- Local patient roster, local visit history, local referral flags.
- Local JSON protocol corpus and keyword retrieval.
- Review/edit/confirm save gate.
- Supervisor summary from confirmed local records.
- Reset Demo Data.
- Android TTS integration when device language data is available.
- Android `SpeechRecognizer` offline-preferred live speech fallback.
- LiteRT-LM dependency, EngineConfig preparation, and manual instrumentation harnesses.
- Developer-only RealGemma text UI mode when both gates are enabled locally and the app-private model is present.

## What Is Mock Or Default

- `MockGemmaAgent` is the default reasoning agent.
- The sample danger-sign transcript is the reliable demo transcript.
- The normal app UI does not run RealGemma unless the developer-only build gate and local sentinel-file gate are both enabled.
- Offline Proof reports RealGemma readiness without enabling inference.

## What Is Blocked

- Direct Gemma 4 audio transcription is blocked by the current LiteRT-LM public audio preprocessing path.
- The current LiteRT-LM Android/Kotlin path also does not expose the prompt-template customization needed for multimodal placeholder injection.
- Android offline speech recognition depends on device/emulator recognizer support and installed offline language packs.
- The protocol corpus is still demo-sized and needs Phase 3 expansion.

## Manual-Only

- `ManualLiteRtTextInferenceInstrumentedTest`
- `ManualRealGemmaVisitJsonInstrumentedTest`
- `ManualRealGemmaAgentInstrumentedTest`
- `ManualRealGemmaBenchmarkInstrumentedTest`
- `ManualRealGemmaMemoryStressInstrumentedTest`
- `ManualLiteRtFunctionCallingInstrumentedTest`
- `ManualLiteRtAudioCapabilityInstrumentedTest`
- `ManualLiteRtAudioInferenceInstrumentedTest`

These tests require explicit instrumentation arguments and, for inference, a sideloaded app-private model. They are not wired into Patient Roster, Visit, Review, Summary, startup, or visible toggles.

## Next Recommended Work

- Run one full emulator smoke test of the Phase 2 judge loop in airplane mode.
- Keep `MockGemmaAgent` as default for submission.
- Capture fresh manual RealGemma benchmark Logcat metrics only if a sideloaded model is available.
- Start Phase 3 with protocol-corpus expansion and offline speech diagnostics, not RealGemma UI wiring.
- Use developer-only RealGemma text mode only for local validation; keep `MockGemmaAgent` as the submission default.
