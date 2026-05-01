# Current Status

Smriti is an offline Android maternal-health visit copilot prototype for community health workers. The normal demo remains demo-safe: `MockGemmaAgent` is the default, RealGemma/LiteRT-LM inference is gated, and the core app path does not require internet.

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
- Reset Demo Data clears saved visits/referrals and restores the six-patient synthetic roster.

## Phase 3 Status

Phase 3 has completed the current judge-ready pass with four controlled additions:

- Developer-only RealGemma text UI mode, guarded by both a build-time gate and an app-private local gate. This is not the default demo path and is not CHW-facing.
- Global Protocol Pack v1, a local 46-chunk maternal/ANC corpus with country/region-aware keyword retrieval for `GLOBAL_CORE`, `INDIA`, `BANGLADESH`, `ETHIOPIA`, `AFRICA_REGION`, and `SOUTH_AMERICA_REGION`.
- A 10-case synthetic global benchmark suite for protocol retrieval, grounding, referral behavior, uncertainty handling, and country/region/global fallback through the mock local pipeline.
- Judge-ready normal demo copy now emphasizes offline CHW workflow, local patient memory, local protocol pack, protocol-grounded referral support, CHW confirm/save, and concise Offline Proof.
- A consolidated judge evidence ledger is available at `docs/judge_evidence.md`.

## Phase A Patient Infrastructure

Phase A for the final recorded demo is implemented:

- Seeded six synthetic demo patients: Meena Sharma, Fatima Begum, Amara Tesfaye, Grace Achieng, Priya Devi, and Lucia Fernandez.
- Patient records now include country, country code, preferred language, protocol region, scenario preview, and optional notes for localization/protocol context.
- Prior visit history includes Meena's danger-sign setup, Fatima's rising BP trend, Amara's overdue/uncompleted follow-up data for Phase B, Grace's routine/no-referral history, Priya's sparse early ANC history, and Lucia's Peru/South America fallback context.
- Local supervisor-register import loads `app/src/main/assets/demo/smriti_patients.json` offline and re-imports without duplicate patient histories.
- Patient Roster includes Add Patient and Load Demo Supervisor Register actions.
- Add Patient supports EN/HI/ES/SW offline speech prompts for name, age, pregnancy weeks, and village, while preserving editable manual fallback.
## Phase B Intelligence Features

Phase B is implemented for the final recorded demo while preserving the safe normal default:

- VisitScreen shows a deterministic missed follow-up alert when a prior visit has `followUpDueDateMillis` before today and `followUpCompleted == false`. Amara triggers this from seeded data.
- The missed follow-up card supports `Mark Confirmed`, which persists `followUpCompleted=true`, and `Note as Ongoing`, which dismisses only for the current screen session when no notes schema is available.
- VisitScreen shows a cautious history signal for rising BP when recent prior readings clearly increase. Fatima triggers from `118/76 -> 125/80 -> 132/84 -> 138/88`; Grace does not trigger.
- RealGemma submission mode is gated by all of: `-Psmriti.realGemmaSubmissionMode=true`, app-private `files/dev/enable_real_gemma_text_mode`, and app-private `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- Fully active submission mode sends visit generation through `VisitReasoningPipeline` with `RealGemmaAgent`; failures show `On-device reasoning unavailable — please retry.` and do not save or silently display mock output as RealGemma.
- SummaryScreen keeps the deterministic local supervisor brief and adds a RealGemma priority queue only when submission mode is fully active. RealGemma summary failure shows `On-device summary unavailable — deterministic local summary shown below.`
- Offline Proof reports active reasoning mode, RealGemma text mode, submission mode, inference, model found/missing, and direct Gemma audio blocked with offline speech/transcript fallback.

Recommended order:

1. Review and refine the protocol pack and synthetic cases against official country program materials.
2. Run a final emulator demo smoke test in airplane mode.
3. If filming RealGemma, sideload the model outside git and verify Offline Proof shows submission mode active before recording.
4. Improve offline speech setup guidance and device diagnostics.
5. Revisit direct Gemma 4 audio only if LiteRT-LM exposes or documents a usable preprocessing and prompt-template path.

## What Is Real

- Android Kotlin + Jetpack Compose app.
- Room/SQLite local storage.
- Local patient roster, local visit history, local referral flags.
- Local six-patient synthetic caseload and local supervisor-register import from app assets.
- Manual add-patient path plus Android offline-speech registration fallback.
- Local JSON protocol corpus and country/region-aware keyword retrieval.
- Synthetic global benchmark cases that run locally through `ProtocolRetriever + VisitReasoningPipeline + MockGemmaAgent`.
- Judge-facing normal app flow: Patient Roster -> Meena -> sample transcript/offline speech fallback -> local note generation -> Review confirm/save -> Supervisor Summary.
- Review/edit/confirm save gate.
- Supervisor summary from confirmed local records.
- Reset Demo Data.
- Android TTS integration when device language data is available.
- Android `SpeechRecognizer` offline-preferred live speech fallback.
- LiteRT-LM dependency, EngineConfig preparation, and manual instrumentation harnesses.
- Developer-only RealGemma text UI mode when both gates are enabled locally and the app-private model is present.
- RealGemma submission mode for filmed demos only when all submission gates and model readiness are satisfied.

## What Is Mock Or Default

- `MockGemmaAgent` is the default reasoning agent.
- The sample danger-sign transcript is the reliable demo transcript.
- The normal app UI does not run RealGemma unless the developer-only gates or the stricter recorded-demo submission gates are enabled.
- Offline Proof reports RealGemma readiness without enabling inference.

## What Is Blocked

- Direct Gemma 4 audio transcription is blocked by the current LiteRT-LM public audio preprocessing path.
- The current LiteRT-LM Android/Kotlin path also does not expose the prompt-template customization needed for multimodal placeholder injection.
- Android offline speech recognition depends on device/emulator recognizer support and installed offline language packs.
- Global Protocol Pack v1 is not clinically complete and needs expert/country-program review before broader use.
- Synthetic global benchmark cases are protocol-scaffold tests, not clinical validation.
- Judge/demo copy must not claim autonomous diagnosis, treatment, direct Gemma audio, or clinical validation.

## Manual-Only

- `ManualLiteRtTextInferenceInstrumentedTest`
- `ManualRealGemmaVisitJsonInstrumentedTest`
- `ManualRealGemmaAgentInstrumentedTest`
- `ManualRealGemmaBenchmarkInstrumentedTest`
- `ManualRealGemmaMemoryStressInstrumentedTest`
- `ManualLiteRtFunctionCallingInstrumentedTest`
- `ManualLiteRtAudioCapabilityInstrumentedTest`
- `ManualLiteRtAudioInferenceInstrumentedTest`

These tests require explicit instrumentation arguments and, for inference, a sideloaded app-private model. They are separate from the developer-only UI mode and are not wired into startup or public CHW-facing toggles.

## Next Recommended Work

- Run one full emulator smoke test of the Phase A roster/import/add-patient loop in airplane mode.
- Keep `MockGemmaAgent` as default for submission.
- Capture fresh manual RealGemma benchmark Logcat metrics only if a sideloaded model is available.
- Harden the Global Protocol Pack v1 with reviewed country-specific sources and more coverage.
- Expand the synthetic benchmark suite after protocol content review.
- Use developer-only RealGemma text mode only for local validation; keep `MockGemmaAgent` as the submission default.
