# Smriti

Smriti is an offline maternal-health visit copilot for community health workers. It helps a CHW manage a local village roster, check urgent local protocol guidance, review patient history, capture a voice-note-style observation, generate a structured visit note, surface protocol-grounded referral support, confirm the record, close follow-up loops, prepare patient-friendly messages, view a community panel, and produce an end-of-day supervisor summary.

The filmed/local submission flow now requires `RealGemmaAgent` for app-facing visit and supervisor reasoning. If the local RealGemma model or gates are missing, Smriti shows setup/retry messaging instead of falling back to mock clinical output.

Judge-facing framing: local patient memory plus a local protocol pack, protocol-grounded referral support rather than diagnosis, and CHW review/confirm before saving.

## Problem

If she forgets the card, "we check it in our record," an ASHA worker in Udaipur told researchers. But if the patient was outside her area, she could not know what care was due. That is the gap Smriti is built for: the moment when care depends on memory, paper, and whether the right record is available.

Every two minutes, a woman dies from pregnancy or childbirth complications. Most maternal deaths are preventable. Existing hospital scribes and cloud chatbots assume stable internet, EHR access, or clinician workflows. Smriti targets the field setting: one Android phone, local patient memory, local protocols, and no required network.

Sources: Yale Global Health Review, ["Consider the ASHA"](https://yaleglobalhealthreview.com/2017/05/14/consider-the-asha-a-qualitative-analysis-of-accredited-social-health-activists-experiences-in-udaipur-india/); WHO, ["Maternal mortality"](https://www.who.int/news-room/fact-sheets/detail/maternal-mortality).

## Why Offline Matters

The core runtime must work in airplane mode. Smriti stores patient data locally, reads a local country/region-aware protocol asset corpus, records voice notes to app-private storage, uses Android TTS locally when available, and supports local Gemma audio transcription to fill an editable transcript. No cloud APIs are used for core runtime.

## Core Demo Flow

1. Turn on airplane mode.
2. Show Welcome: `Start`, `Set up patient list`, and `Help & setup`.
3. Optionally import the local supervisor register, then tap `Start visits`.
4. Show Patient Roster search, filter chips, attention chips, and patient-card language labels.
5. Open an English visit observation for Amara or Fatima to show local history/follow-up signals.
6. Open synthetic demo patient `Meena Sharma, 28F` for a Hindi typed observation and a RealGemma danger-sign note with cited local guidance and CHW confirm/save.
7. Show the follow-up task and editable patient leave-behind message.
8. Open `Grace Achieng` for `Scan paper note`; local Gemma 4 vision extracts structured paper-note data for CHW review/save only.
9. Open End-of-Day Summary and Community Panel for saved local follow-ups, urgent review support, and caseload visibility.
10. Close with Offline Proof: no cloud APIs after setup, local patient memory, local guidance, Gemma 4 on device, and CHW review required.

Smriti demonstrates local Android LiteRT-LM text reasoning, local Gemma 4 vision paper-note extraction, and Gemma audio transcription wired to the editable transcript field after LiteRT-LM 0.11.0 manual validation. Audio fills an editable transcript only. CHW must review/edit before generating the note. Clinical note generation still goes through text reasoning, protocol citation validation, ReviewScreen, and confirm/save. No audio-only save path. No direct audio diagnosis or treatment. Selected languages demonstrated: English, Hindi, Spanish, Swahili. Do not claim clinical validation, all-language support, or broad camera diagnosis. Vision scan is data-entry support only, not diagnosis.

See [docs/demo_flow.md](docs/demo_flow.md) and [docs/video_script_final.md](docs/video_script_final.md) for the step-by-step judge script.

## Project Status Docs

For the concise current state, start with [docs/current_status.md](docs/current_status.md).

- [Final video script](docs/video_script_final.md)
- [Media-gallery architecture visual](docs/smriti_architecture_diagram.md)
- [Judge evidence](docs/judge_evidence.md)
- [Technical project summary](docs/technical_project_summary.md)
- [Final demo checklist](docs/final_demo_checklist.md)
- [Phase 1 stack validation](docs/phase_1_stack_validation.md)
- [Phase 2 core pipeline](docs/phase_2_core_pipeline.md)
- [Phase 3 protocol pack](docs/phase_3_protocol_pack.md)
- [Phase 3 synthetic benchmarks](docs/phase_3_benchmarks.md)
- [Architecture](docs/architecture.md)
- [Known limitations](docs/known_limitations.md)
- [LiteRT-LM status](docs/litert_status.md)

## Current Evidence

- Normal demo: local roster/history, local protocol JSON, required `RealGemmaAgent`, review/confirm/save, raw local supervisor counts, and Offline Proof from `Check offline setup` or Summary.
- Patient infrastructure: six synthetic demo patients with prior histories, local supervisor-register import from app assets, and add-patient registration with offline speech/manual fallback.
- Protocol pack: 46 local chunks across global, country, and regional tags.
- Synthetic benchmark: 10 legacy fixture cases through `ProtocolRetriever -> VisitReasoningPipeline -> MockGemmaAgent`; these are tests only, not app-facing reasoning.
- Phase B memory intelligence: missed follow-up alert for Amara and rising BP trend signal for Fatima are deterministic local logic.
- Local follow-up scheduling: reviewed follow-up plans create Room/SQLite follow-up tasks after confirm/save; tasks do not count as saved visits.
- Patient leave-behind: saved visits can produce editable patient messages that are copied/shared only by user action.
- Community panel: local caseload counts and priority list from patients, visits, referral flags, and follow-up tasks without model inference.
- Urgent protocol lookup: roster and Visit screen can open a read-only local guidance lookup over the protocol pack; lookup alone creates no visit, referral flag, follow-up task, patient message, or community-panel count.
- Phase C multilingual demo support: selected patient-specific output languages demonstrated are English, Hindi, Spanish, and Swahili; `preferredLanguage` controls RealGemma visit-note output in fully gated submission mode after manual validation.
- RealGemma: manual text inference validated; app-facing reasoning now requires build flag + app-private sentinel + app-private model for inference.
- Supervisor priority: raw local counts remain visible, and the app attempts RealGemma priority reasoning; unavailable output shows retry/setup messaging.
- Audio: App-facing microphone recording now uses local Gemma audio transcription to fill the editable transcript field when RealGemma submission gates and the app-private model are active. Clinical note generation still requires the CHW to tap Generate Visit Note and still goes through text reasoning, protocol citation validation, ReviewScreen, and confirm/save. No audio-only save path. No direct audio diagnosis, treatment, or referral.
- Vision: manual probe passed; local Gemma 4 vision extracts structured JSON from a synthetic paper note for CHW-reviewed data entry only. Image bytes are not persisted and no cloud OCR/API is used.
- Protocol tool-calling: manual LiteRT-LM probe passed; Gemma called local `lookupProtocol` and returned cited maternal danger-sign guidance. Production visits still use deterministic `ProtocolRetriever`.
- Speculative decoding: LiteRT-LM 0.11.0 exposes speculative APIs, but the first manual CPU benchmark was slightly slower, so CPU remains the stable default and no speedup is claimed.

The 15.8s average RealGemma latency reflects real on-device Gemma 4 E2B text inference on CPU backend; in the CHW field workflow, this is positioned as protocol-grounded reasoning support replacing manual paper/protocol lookup, not instant chat.

See [docs/judge_evidence.md](docs/judge_evidence.md).

## What Works Now

- Android native app in Kotlin and Jetpack Compose.
- Local six-patient synthetic roster with Meena Sharma, Fatima Begum, Amara Tesfaye, Grace Achieng, Priya Devi, and Lucia Fernandez.
- Local supervisor-register import from `app/src/main/assets/demo/smriti_patients.json`; repeated imports upsert without duplicate histories.
- Add Patient flow with voice-first registration prompts and manual fallback fields.
- Welcome-first FinalUi flow with grouped `Start`, `Set up patient list`, and `Help & setup` actions.
- Roster search plus UI-only filters for `All`, `Needs attention`, `Follow-up due`, `Near term`, and `Routine`.
- Missed follow-up card on patient open for overdue incomplete follow-ups, with Mark Confirmed and Note as Ongoing actions.
- History signal card for a cautious rising BP trend across prior visits.
- Room/SQLite local storage for patients, visits, referrals, and protocols.
- Local country/region-aware protocol retrieval from `app/src/main/assets/protocols/maternal_health_demo_protocols.json`.
- Urgent protocol lookup screen using the same local protocol pack for CHW-facing danger-sign guidance, with no model call and no automatic save.
- RealGemma visit-note generation through `RealGemmaAgent`; missing setup or invalid output shows retry/setup messaging and does not save.
- Referral flag generation for pregnancy danger-sign keywords.
- CHW review/edit/confirm before saving.
- End-of-day supervisor summary with concise urgent cases.
- Reset Demo Data restores the clean six-patient synthetic roster and clears saved demo referrals.
- Local JSON export for visit and summary data.
- Local app-private voice note recording metadata.
- Android TTS buttons for offline voice output when device language data is available.
- Patient note-language labels on the roster and Visit screen: English, Hindi, Swahili, and Spanish. Existing patient `preferredLanguage` controls generated note language.
- Offline Proof available from `Check offline setup` and Summary, not shown by default on the roster.
- Repo safety checks against committed model artifacts.
- Synthetic global benchmark cases for local protocol retrieval and legacy mock fixtures.

## RealGemma Required

- Offline app flow, local storage, protocol asset retrieval, review/confirm, referral display, supervisor summary display, JSON export, TTS integration, and voice note recording.
- The sample danger-sign transcript is the reliable demo transcript when offline speech is unavailable.
- `RealGemmaAgent` is the app-facing reasoning engine.
- In recorded-demo submission mode, `RealGemmaPromptBuilder` asks for patient-specific local-language output from `preferredLanguage`; citation IDs remain stable in English.
- Safety post-processing appends required wording in English, Hindi, Spanish, or Swahili when missing.
- LiteRT-LM dependency is present, and `EngineConfig` construction is available when a sideloaded model is found.
- RealGemma inference requires `-Psmriti.realGemmaSubmissionMode=true`, `files/dev/enable_real_gemma_text_mode`, and `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- RealGemma failures, timeouts, invalid JSON, or citation/safety rejection show an unavailable/retry state instead of silently displaying mock output as RealGemma.
- `MockGemmaAgent` may remain in the repo for deterministic unit fixtures only; app screens do not use it for visit or supervisor output.
- Gemma audio transcription is wired into the app-facing Visit screen behind RealGemma submission readiness using `Conversation.sendMessage(Contents.of(Content.Text(prompt), Content.AudioBytes(audioBytes)))` with `EngineConfig.audioBackend = Backend.CPU()`. Audio fills an editable transcript only. No audio-only save path. No direct audio diagnosis, treatment, or referral.

## LiteRT-LM Status

- Dependency pinned: `com.google.ai.edge.litertlm:litertlm-android:0.11.0`.
- Required model path: `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- The app detects whether that file exists; if missing or not enabled, generation is blocked with setup/retry messaging.
- EngineConfig defaults to stable `Backend.CPU()` when that model file exists. An isolated `Backend.GPU()` latency experiment exists behind explicit developer/test configuration and is not the default.
- Manual CPU speculative/MTP probe result: baseline 21787 ms, speculative 22138 ms, delta +351 ms slower. CPU remains the stable default.
- Manual protocol tool-calling probe validated `OpenApiTool + tool(...) + ConversationConfig(tools, automaticToolCalling=true)` for local protocol lookup only. It is not production visit reasoning and does not save data.
- Direct LiteRT-LM API types compile after the Room KSP migration.
- Runtime text inference is enabled only when the submission build flag, app-private local gate, and app-private model are present.
- No model files are committed.

See [docs/litert_status.md](docs/litert_status.md).

## Safety Constraints

- Smriti is not diagnostic AI.
- Outputs are protocol-grounded documentation and referral support only.
- Every clinical recommendation must include a protocol citation or be treated as uncertain.
- CHW confirmation is required before saving generated records.
- Urgent protocol lookup is read-only local guidance; it does not create visits, referral flags, follow-up tasks, patient messages, or summary/community counts.
- The app-facing agent is `RealGemmaAgent`; no mock clinical output is shown when RealGemma is unavailable.
- No cloud APIs, remote databases, model download code, or Hugging Face runtime code are used for core runtime.

See [docs/offline_safety.md](docs/offline_safety.md).

## Build And Run

Open the repository root in Android Studio and run the `app` debug configuration, or use PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

On macOS/Linux:

```bash
./gradlew assembleDebug
```

Demo-tested path: Pixel 9 Pro API 35 emulator. Turn on airplane mode before the demo and use Reset Demo Data if repeated test saves have accumulated.

Recorded-demo RealGemma submission build setup, after sideloading the model outside git:

```powershell
.\gradlew.bat assembleDebug -Psmriti.realGemmaSubmissionMode=true
adb shell run-as com.smriti.clinicalscribe mkdir -p files/dev
adb shell run-as com.smriti.clinicalscribe touch files/dev/enable_real_gemma_text_mode
adb shell run-as com.smriti.clinicalscribe ls -lh files/models/gemma-4-E2B-it-int4.litertlm
```

## Tests

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

The test suite covers RealGemma-required app wiring, legacy mock fixtures, protocol retrieval, six-patient seed/import behavior, add-patient registration helpers, concise supervisor summary formatting, LiteRT readiness guards, disabled LiteRT client behavior, repo model-artifact safety, and JSON export.

Manual multilingual RealGemma validation is optional and requires a sideloaded app-private model:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaMultilingualInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

Only claim a filmed RealGemma language after this manual harness passes for that language. No cloud translation API is used. Gemma audio transcription fills the editable transcript only; CHW review and manual note generation are still required.

## Media Gallery Plan

Recommended submission media:

- Phone-frame recording of the FinalUi flow from Welcome to roster, Meena note review/save, patient message, Summary, Community Panel, and Offline Proof.
- Architecture image exported from [docs/smriti_architecture_diagram.md](docs/smriti_architecture_diagram.md).
- One clean roster screenshot showing filters and patient memory.
- One ReviewScreen screenshot showing cited guidance and CHW confirm/save.

## Judge Notes

For track fit, pitch points, and technical-depth framing, see [docs/judge_notes.md](docs/judge_notes.md). For a 3-minute video outline, see [docs/video_script_3min.md](docs/video_script_3min.md).
