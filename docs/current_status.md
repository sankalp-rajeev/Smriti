# Current Status

Smriti is an offline Android maternal-health visit copilot prototype for community health workers. The app-facing visit and supervisor reasoning path now requires `RealGemmaAgent`; if local RealGemma setup is missing or inference fails, Smriti shows setup/retry messaging instead of mock clinical output. The core app path does not require internet.

## Pre-Video Stability Update

After a manual Logcat review on 2026-05-03, a native LiteRT-LM text inference crash was observed inside `liblitertlm_jni.so` during `Conversation.sendMessage(prompt)` after multiple RealGemma requests piled up behind the synchronized inference runner. This was not a Compose, Room, SQLite, VisitScreen, ReviewScreen, or SummaryScreen crash.

Stability changes now in place:

- `RealGemmaInferenceGate` allows only one active LiteRT-LM request across visit-note generation, supervisor priority generation, paper-note vision extraction, manual text inference, and preload.
- The gate uses non-queueing acquisition. A second request returns `Smriti is already preparing a note. Please wait.` and does not enter another `sendMessage` call.
- Gate logs use `SmritiRealGemmaGate` with request types `VISIT_NOTE`, `SUPERVISOR_SUMMARY`, `PAPER_NOTE_SCAN`, `MANUAL_TEST`, and `PRELOAD`.
- Diagnostics log model presence, model size, sentinel state, backend mode, engine state, and last engine failure before a request starts.
- Native-call-adjacent failures mark the cached text engine session failed; Smriti does not retry inside the same request.
- Visit generation and paper-note scan actions disable each other while running. Back navigation during generation asks the CHW to wait instead of relying on unsafe native cancellation.
- Confirm/save is guarded against double taps and remains a local Room/SQLite write only.
- CPU remains the stable default backend. GPU remains isolated as an explicit manual experiment and is off by default.
- Saved-history and Summary wording now use CHW-facing labels such as `Patient history checked`, `Local health guidance checked`, `Health guidance used`, and `Saved visits on this device`.
- No mock fallback, parser/safety/citation weakening, CHW review bypass, cloud API, direct Gemma audio, or clinical image diagnosis was added.

## Phase D UX Polish

Phase D is implemented as a low-digital-literacy polish pass for the recorded demo:

- First launch now starts with a plain Welcome screen: `Smriti`, `Offline health visit assistant`, `Start visits`, `View user guide`, and `Check offline setup`.
- If the model file is absent after Welcome, Smriti shows one-time Setup Guidance in non-technical language. It explains supervisor-installed model setup and allows limited demo exploration without claiming full on-device reasoning.
- User Guide is available from Welcome and Patient Roster.
- Patient Roster now includes search by name, country, or village; section headers for `Needs attention` and `Routine visits`; full-width primary actions; a `Community panel` action; smaller secondary actions for import/user guide/offline setup; and patient-card `Note language` labels.
- Roster chips are deterministic local UI logic: `Referral saved`, `Follow-up due`, `History signal`, `Near term`, `Overdue`, and `Routine`.
- Sorting prioritizes saved urgent referrals, missed follow-ups, history signals, near-term/overdue pregnancies, then routine patients.
- The old roster language pill was removed because it did not translate the full app UI. Existing patients keep their saved `preferredLanguage`, which remains the source of truth for generated note language.
- Visit screen order is now patient header, alert cards, what-to-do instruction, input, compact history, and lower local setup proof.
- Patient-specific sample transcripts prevent Grace or routine patients from receiving Meena's danger-sign transcript.
- Generation has a calm loading card, duplicate generation prevention, blank-input inline error, short-input inline warning, and an on-device reasoning failure card that preserves the transcript.
- Review screen now uses plain cards: `Referral suggested`, `No referral flag`, `More information needed`, and a collapsed `How was this prepared?` source section.
- Summary screen shows `Saved visits on this device`, `Today's priority list`, urgent cases, follow-ups, routine visits, empty state, a collapsed preparation explanation, local-proof expansion, `View community panel`, and a plain fallback when on-device priority reasoning is unavailable.
- Destructive actions now use confirmation dialogs for Reset Demo Data and register import.
- Offline Proof wording was changed to `Works offline after setup`, `Patient memory: saved on this device`, `Health guidance: stored on this device`, `On-device Gemma: ready/setup needed`, `Cloud APIs: none`, and `Direct Gemma audio: not used`. These details now live behind `Check offline setup` instead of appearing by default on the roster.
- Source-level unit tests were added/updated for roster filtering/sorting/chips, sample transcript mapping, first-launch screens, validation/error states, review source section, summary fallback, destructive dialogs, and forbidden UI wording.
- Validation status for this pass: `.\gradlew.bat testDebugUnitTest` passed after the RealGemma gate and UI wording updates. Full build validation is listed in `CONTEXT.md`.

## Final Plan Phase 2-5 Additions

The latest build adds four local workflow features that do not require RealGemma or cloud services:

- Local follow-up tasks are stored in Room/SQLite through `follow_up_tasks`. Confirm/save creates deterministic open tasks from reviewed follow-up plans, seeded Amara follow-up state is restored on reset, roster chips show due/upcoming/overdue state, Visit screen supports Mark done and Reschedule, and Summary counts open/overdue/due-upcoming follow-ups. Follow-up tasks do not count as saved visits.
- Patient leave-behind messages are generated on demand from the saved reviewed visit, patient context, and local referral flag. The CHW can review/edit, copy, or share through Android's `ACTION_SEND` chooser. Smriti does not auto-send SMS/WhatsApp, does not call RealGemma/LiteRT during save/share, and does not persist message records.
- Community Panel is a deterministic local caseload view reached from the roster or Summary. It shows patients in roster, pregnancy stage, urgent review saved, follow-ups, history signals, no recent visit, countries, note languages, and a local priority list. It creates no records and does not invoke model inference.
- Urgent Protocol Lookup is a read-only local protocol-pack flow reached from the roster or Visit screen. The CHW can select danger-sign chips or type an observation, then see local health guidance with a citation or the safe no-guidance fallback. Lookup alone creates no visit, referral flag, follow-up task, leave-behind message, summary count, or community-panel count.

## Phase 1 Status

Phase 1 validated the local Gemma 4 LiteRT-LM stack behind manual instrumentation only.

- LiteRT-LM Android dependency is pinned to `com.google.ai.edge.litertlm:litertlm-android:0.10.2`.
- Room annotation processing uses KSP, so LiteRT-LM API references compile without KAPT classfile failures.
- `EngineConfig` can be prepared with default `Backend.CPU()` when an app-private `.litertlm` model is found.
- Manual text inference exists through `LiteRtGemmaTextClient.generateTextManual(...)`.
- `RealGemmaAgent` has prompt building, hardened JSON extraction/parsing, safe alias tolerance, citation validation, and safety post-processing.
- RealGemma inference has loaded and returned text on the emulator; the latest observed blocker was schema/parser rejection because the model omitted the required `referralFlag` field, not a missing model/runtime path.
- The visit prompt now requires exact JSON only with `summary`, boolean `referralFlag`, `referralReason`, `dangerSigns`, `followUpPlan`, `clarificationQuestion`, `citations`, `confidence`, and `safetyNote`.
- Offline Proof and Local Reasoning now describe model/engine state as `Real Gemma model: Found`, `Engine: Loads on demand`, and `Inference: Enabled; on-device RealGemma text reasoning` when the RealGemma gates are active. After a successful app-session generation, the engine state can show `Loaded`.
- When a fully gated app-private model is present, the app starts a non-blocking RealGemma engine preload and reports `Engine: Preparing`, `Ready`, `Failed`, or `Loads on demand`. A shared RealGemma text client keeps the engine/conversation warm, while the global inference gate prevents overlapping native text/vision requests.
- `SmritiLatency` Logcat timing markers cover model readiness, preload/init, protocol retrieval, patient history formatting, prompt build, RealGemma generation, parser/safety/citation validation, ReviewScreen navigation, local confirm/save, and summary refresh. These logs contain timings and synthetic scenario IDs only, not transcript text or PHI.
- Latest `SmritiLatency` emulator/local setup timings: RealGemma preload/init 1.885 s; Meena RealGemma generation 21.726 s; Meena parse/safety/citation validation 31 ms; Meena Room save 49 ms; Meena summary refresh 5 ms; Lucia RealGemma generation after preload/reuse 14.434 s; Lucia parse/safety/citation validation 4 ms; protocol retrieval 1-2 ms; prompt build 1-3 ms. Measured on emulator/local setup; device performance may vary.
- Interpretation: RealGemma inference is the main latency cost. Local protocol retrieval, prompt build, validation, save, and summary refresh are negligible by comparison. The second generation was faster after preload/engine reuse.
- Visit prompts are compacted for latency: selected patient profile, capped prior history, top retrieved protocol chunks, strict JSON schema, language instruction, allowed citations, and non-diagnostic safety wording are retained while unrelated history/protocol bulk is trimmed.
- The current LiteRT-LM `Conversation.sendMessage(prompt)` app path has no wired generation-options object in this codebase, so Smriti does not invent unsupported temperature or token-limit calls. Output size is constrained through the prompt contract and parser validation.
- Stable app text inference uses CPU by default. A GPU backend path is isolated behind explicit developer/test configuration via `LiteRtBackendMode.GPU_EXPERIMENTAL`; it is not the filmed default unless manual target-device testing proves it works and improves latency without breaking CPU fallback.
- Routine no-danger-sign RealGemma outputs can pass safely with `referralFlag=false`, `dangerSigns=[]`, and empty citations when no supplied protocol directly supports a protocol-specific recommendation. Referral outputs still require a supplied citation.
- Parser failures log the first 1500 characters of raw RealGemma output plus the rejection reason to `SmritiRealGemma` in debug/dev builds only. The UI keeps a concise retry/setup error and never shows raw model text as a clinical result.
- The latest accepted manual RealGemma benchmark reported `totalScenarios=3`, `successCount=3`, `parserSuccessCount=3`, `referralCount=1`, `citationCount=2`, `singleCitationContractCount=3`, `averageLatencyMs=15812`, and `maxLatencyMs=26272`.
- Manual probes confirmed native function calling API behavior and long-context memory stress behavior with a sideloaded model.
- Direct Gemma 4 audio remains blocked by LiteRT-LM audio preprocessing requirements.

## Phase H Paper-Note Scan Status

Phase H adds a narrow paper-note scan flow for synthetic paper visit notes only. It is data-entry support, not clinical image diagnosis.

- The local `litertlm-android-0.10.2` AAR/classes.jar surface was inspected for image and multimodal APIs.
- Found public image holders: `Content.ImageBytes`, `Content.ImageFile`, and `InputData.Image`.
- Found image-related config fields on `EngineConfig`: `visionBackend` and `maxNumImages`.
- Found usable-looking transport methods: `Conversation.sendMessage(Contents)` and `Session.generateContent(List<InputData>)`.
- No public class or method named like `PromptTemplate`, `MediaPlaceholder`, `MultiModalTemplate`, `ImagePreprocessor`, or `preprocess(...)` was found in `classes.jar`.
- `ManualRealGemmaVisionProbeInstrumentedTest` passed on emulator with the sideloaded app-private model. The engine accepted the `Conversation` image input path and local Gemma 4 vision extracted structured JSON from the synthetic paper note: Grace Achieng, 02 May 2026, BP 116/74, symptoms, routine ANC follow-up, confidence HIGH, and `needsReview=true`.
- The app now exposes `Scan paper note` and `Use sample paper note` from the Visit screen, below the primary visit-note actions.
- The flow uses local Gemma vision through LiteRT-LM, then `PaperNoteVisionParser` validates the exact paper-note JSON schema.
- CHW review/edit and explicit patient-record confirmation are required before saving.
- Scanned notes are stored as local `VisitLog` history with `transcriptSource=paper_scan`.
- Image bytes are not persisted.
- The scan flow does not call visit-note referral reasoning, supervisor priority reasoning, or RealGemmaOutputParser.
- The scan flow does not generate diagnosis, referral advice, or treatment recommendations from image alone.
- No cloud OCR/API, ML Kit OCR, runtime download, PHI, or real patient image was added.
- Direct Gemma audio remains blocked; the vision path does not change the audio limitation.

## Phase 2 Status

Phase 2 is complete for the local core visit flow:

- Patient roster and prior visit history are local.
- Manual/sample transcript entry is the reliable default path.
- `Speak observation` uses Android `SpeechRecognizer` as a safe offline-preferred fallback.
- `VisitReasoningPipeline` is wired into the normal `Generate visit note` action.
- Local protocol retrieval grounds the RealGemma prompt.
- Generated notes, referrals, and citations are editable on the Review screen.
- Nothing is saved until CHW confirm/save.
- Confirmed visits and referral flags persist locally.
- Confirm/save runs only a local Room/SQLite write on `Dispatchers.IO`; it does not call RealGemma, rebuild protocol retrieval, or export JSON automatically.
- Measured local save latency was 49 ms on the emulator/local setup, with a 5 ms summary refresh. This is local Room/SQLite behavior and does not invoke Gemma.
- Later Meena visits show the latest confirmed visit first in history.
- Supervisor Summary reads fresh confirmed local data and keeps urgent cases concise.
- Reset Demo Data clears saved visits/referrals and restores the six-patient synthetic roster.

## Phase 3 Status

Phase 3 has completed the current judge-ready pass with four controlled additions:

- RealGemma-required app-facing text mode, guarded by the submission build flag, app-private local gate, and sideloaded app-private model.
- Global Protocol Pack v1, a local 46-chunk maternal/ANC corpus with country/region-aware keyword retrieval for `GLOBAL_CORE`, `INDIA`, `BANGLADESH`, `ETHIOPIA`, `AFRICA_REGION`, and `SOUTH_AMERICA_REGION`.
- A 10-case synthetic global benchmark suite for protocol retrieval, grounding, referral behavior, uncertainty handling, and country/region/global fallback through the mock local pipeline.
- Judge-ready normal demo copy now emphasizes offline CHW workflow, local patient memory, local health guidance, referral/follow-up support, CHW confirm/save, and concise local proof.
- A consolidated judge evidence ledger is available at `docs/judge_evidence.md`.

## Phase A Patient Infrastructure

Phase A for the final recorded demo is implemented:

- Seeded six synthetic demo patients: Meena Sharma, Fatima Begum, Amara Tesfaye, Grace Achieng, Priya Devi, and Lucia Fernandez.
- Patient records now include country, country code, preferred language, protocol region, scenario preview, and optional notes for localization/protocol context.
- Prior visit history includes Meena's danger-sign setup, Fatima's rising BP trend, Amara's overdue/uncompleted follow-up data for Phase B, Grace's routine/no-referral history, Priya's sparse early ANC history, and Lucia's Peru/South America fallback context.
- Local supervisor-register import loads `app/src/main/assets/demo/smriti_patients.json` offline and re-imports without duplicate patient histories.
- Patient Roster includes Add Patient, End-of-Day Summary, and Import Supervisor Register actions.
- Add Patient supports EN/HI/ES/SW offline speech prompts for name, age, pregnancy weeks, and village, while preserving editable manual fallback.
## Phase B Intelligence Features

Phase B is implemented for the final recorded demo while preserving the safe normal default:

- VisitScreen shows a deterministic missed follow-up alert when a prior visit has `followUpDueDateMillis` before today and `followUpCompleted == false`. Amara triggers this from seeded data.
- The missed follow-up card supports `Mark Confirmed`, which persists `followUpCompleted=true`, and `Note as Ongoing`, which dismisses only for the current screen session when no notes schema is available.
- VisitScreen shows a cautious history signal for rising BP when recent prior readings clearly increase. Fatima triggers from `118/76 -> 125/80 -> 132/84 -> 138/88`; Grace does not trigger.
- RealGemma submission mode is gated by all of: `-Psmriti.realGemmaSubmissionMode=true`, app-private `files/dev/enable_real_gemma_text_mode`, and app-private `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- Fully active submission mode sends visit generation through `VisitReasoningPipeline` with `RealGemmaAgent`; failures show `On-device reasoning unavailable - please retry.` and do not save or silently display mock output as RealGemma.
- SummaryScreen keeps saved local counts/referral flags visible and attempts RealGemma priority reasoning only when the global gate is free. Failure or busy state shows `On-device priority summary unavailable. Showing saved local visit flags.` without mock priority output.
- CommunityPanelScreen uses only local patients, visits, referral flags, and follow-up tasks. It does not call RealGemma, LiteRT, protocol retrieval, or supervisor priority reasoning.
- Offline Proof reports active reasoning mode, RealGemma text mode, submission mode, inference, model found/missing, and direct Gemma audio blocked with offline speech/transcript fallback.

## Phase C Multilingual Output

Phase C supports selected patient-specific local-language output for the recorded demo:

- Demo languages are English, Hindi, Swahili, and Spanish only.
- Selected languages demonstrated: English, Hindi, Spanish, Swahili.
- Patient mapping: Meena/Priya -> Hindi, Grace -> Swahili, Lucia -> Spanish, Fatima/Amara -> English.
- Lucia remains Peru/Spanish; the app does not use Brazil for a Spanish-language Lucia demo.
- Amara/Ethiopia and Fatima/Bangladesh remain English because Amharic/Oromo/Bangla are not implemented or tested.
- Patient Roster and VisitScreen show each patient's output language label.
- Patient cards now say `Note language: ...`, and VisitScreen says `Visit note will be prepared in ...` so CHWs do not confuse note language with full app UI language.
- Fully gated RealGemma submission mode passes `preferredLanguage` into the visit-note prompt and asks for all user-facing output in that language.
- Protocol citation IDs remain stable/English, for example `WHO ANC Recommendation B1.2`.
- `RealGemmaSafetyPostProcessor` appends required safety wording in English, Hindi, Spanish, or Swahili if missing.
- Lightweight localized resources exist for key demo-visible strings, but there is no risky app-wide runtime locale switch.
- Full app UI translation is not claimed.
- No cloud translation API, runtime downloads, vector DB, model/audio artifact, PHI, or direct Gemma audio wiring was added.
- Manual multilingual RealGemma output must be verified before filming; if a language fails manual validation, remove it from the filmed claim.
- The architecture can extend to more Gemma-supported languages as protocol packs and UI translations are added.

Recommended order:

1. Review and refine the protocol pack and synthetic cases against official country program materials.
2. Run a final emulator demo smoke test in airplane mode.
3. If filming RealGemma, sideload the model outside git and verify Offline Proof shows submission mode active before recording.
4. Smoke-test Urgent Protocol Lookup in airplane mode and verify lookup-only activity does not change Summary or Community Panel counts.
5. Improve offline speech setup guidance and device diagnostics.
6. Revisit direct Gemma 4 audio only if LiteRT-LM exposes or documents a usable preprocessing and prompt-template path.
7. Keep the paper-note scan flow limited to synthetic/demo paper notes and CHW-reviewed data entry.

## What Is Real

- Android Kotlin + Jetpack Compose app.
- Room/SQLite local storage.
- Local patient roster, local visit history, local referral flags.
- Local six-patient synthetic caseload and local supervisor-register import from app assets.
- Manual add-patient path plus Android offline-speech registration fallback.
- Local JSON protocol corpus and country/region-aware keyword retrieval.
- Local urgent protocol lookup for danger-sign observations, using the same protocol corpus without model inference or automatic persistence.
- Synthetic global benchmark cases remain as legacy deterministic fixtures; they are not app-facing reasoning.
- Judge-facing normal app flow: Patient Roster -> Meena -> sample transcript/offline speech fallback -> local note generation -> Review confirm/save -> Supervisor Summary.
- Review/edit/confirm save gate.
- Supervisor summary from confirmed local records.
- Reset Demo Data.
- Android TTS integration when device language data is available.
- Android `SpeechRecognizer` offline-preferred live speech fallback.
- LiteRT-LM dependency, EngineConfig preparation, and manual instrumentation harnesses.
- RealGemma text UI mode when the submission build flag, local sentinel, and app-private model are present.
- RealGemma-required filmed/local flow with setup/retry behavior when gates or model readiness are missing.
- Local Gemma 4 vision paper-note extraction from synthetic images for data entry only.
- Local urgent protocol lookup over the protocol pack for CHW-facing guidance only.

## RealGemma Required

- `RealGemmaAgent` is the app-facing reasoning agent.
- The sample danger-sign transcript is the reliable input demo when offline speech is unavailable.
- Inference requires the submission build flag, app-private sentinel, and sideloaded app-private model.
- Missing setup, timeout, invalid JSON, or citation/safety rejection shows retry/setup messaging and does not save.
- Missing required schema fields, including all `referralFlag` aliases, is rejected safely. The parser tolerates harmless aliases such as `referral_required`, `referral_flag`, `needsReferral`, boolean `referral`, `protocolCitations`, `citation`, `follow_up_plan`, `clarification_question`, and `danger_signs`.
- `MockGemmaAgent` may remain only for deterministic unit fixtures and legacy benchmarks.

## What Is Blocked

- Direct Gemma 4 audio transcription is blocked by the current LiteRT-LM public audio preprocessing path.
- The current LiteRT-LM Android/Kotlin path also does not expose the prompt-template customization needed for multimodal placeholder injection.
- Paper-note scan is limited to text/data extraction. It must not be used for wounds, rashes, ultrasound, medicine strips, growth charts, photos of people, diagnosis, treatment, or referral decisions from image alone.
- Android offline speech recognition depends on device/emulator recognizer support and installed offline language packs.
- Global Protocol Pack v1 is not clinically complete and needs expert/country-program review before broader use.
- Synthetic global benchmark cases are protocol-scaffold tests, not clinical validation.
- Judge/demo copy must not claim autonomous diagnosis, treatment, direct Gemma audio, or clinical validation.

## Manual-Only

- `ManualLiteRtTextInferenceInstrumentedTest`
- `ManualRealGemmaVisitJsonInstrumentedTest`
- `ManualRealGemmaAgentInstrumentedTest`
- `ManualRealGemmaBenchmarkInstrumentedTest`
- `ManualRealGemmaMultilingualInstrumentedTest`
- `ManualRealGemmaMemoryStressInstrumentedTest`
- `ManualLiteRtFunctionCallingInstrumentedTest`
- `ManualLiteRtAudioCapabilityInstrumentedTest`
- `ManualLiteRtAudioInferenceInstrumentedTest`
- `ManualRealGemmaVisionProbeInstrumentedTest`
- `ManualRealGemmaBackendLatencyInstrumentedTest`

These tests require explicit instrumentation arguments and, for inference, a sideloaded app-private model. They are separate from startup and do not add cloud or runtime download behavior.

## Next Recommended Work

- Run one full emulator smoke test of the Phase A roster/import/add-patient loop in airplane mode.
- Sideload the model and run the manual RealGemma agent and multilingual harnesses before filming.
- Capture fresh manual RealGemma benchmark Logcat metrics if a sideloaded model is available.
- Harden the Global Protocol Pack v1 with reviewed country-specific sources and more coverage.
- Re-run full Phase 5 validation and manual lookup smoke test when Gradle/script escalation is available.
- Expand the synthetic benchmark suite after protocol content review.
- If RealGemma fails manual validation, remove that path or language from filmed claims.
