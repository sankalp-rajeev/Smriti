# Smriti

Smriti is an offline maternal-health visit copilot for community health workers. It helps a CHW select a patient, review local visit history, capture a voice-note-style observation, generate a structured visit note, surface protocol-grounded referral support, confirm the record, and produce an end-of-day supervisor summary.

The filmed/local submission flow now requires `RealGemmaAgent` for app-facing visit and supervisor reasoning. If the local RealGemma model or gates are missing, Smriti shows setup/retry messaging instead of falling back to mock clinical output.

Judge-facing framing: local patient memory plus a local protocol pack, protocol-grounded referral support rather than diagnosis, and CHW review/confirm before saving.

## Problem

Community health workers often work from paper records, memory, and limited connectivity. That makes it easy to miss longitudinal context, such as a prior high blood pressure note during a later pregnancy danger-sign visit. Existing hospital scribes and cloud chatbots assume stable internet, EHR access, or clinician workflows. Smriti targets the field setting: one Android phone, local patient memory, local protocols, and no required network.

## Why Offline Matters

The core runtime must work in airplane mode. Smriti stores patient data locally, reads a local country/region-aware protocol asset corpus, records voice notes to app-private storage, and uses Android TTS locally when available. No cloud APIs are used for core runtime.

## Core Demo Flow

1. Turn on airplane mode.
2. Show Welcome.
3. Tap `Check offline setup` to show Offline Proof / setup ready, then return to the roster.
4. Show Patient Roster search, attention chips, and patient-card note language labels.
5. Open `Amara Tesfaye, 30F` for missed follow-up.
6. Open `Fatima Begum, 24F` for rising BP history signal.
7. Open `Meena Sharma, 28F` for a Hindi RealGemma danger-sign note with referral suggested, local guidance citation, and CHW confirm/save.
8. Open `Lucia Fernandez` for a Spanish RealGemma note after manual validation.
9. Open `Grace Achieng` for a Swahili routine/no-referral RealGemma note after manual validation.
10. Use Grace's sample paper-note scan; local Gemma 4 vision extracts structured data for CHW review/save.
11. Open End-of-Day Summary for urgent, follow-up, and routine priority lists.
12. Close with Offline Proof: no cloud APIs, local patient memory, local guidance, RealGemma text + vision, direct Gemma audio blocked.

Smriti demonstrates local Android LiteRT-LM text reasoning and local Gemma 4 vision paper-note extraction. Selected languages demonstrated: English, Hindi, Spanish, Swahili. Do not claim direct Gemma 4 audio works, clinical validation, all-language support, or broad camera diagnosis. The input path remains offline speech or editable transcript into RealGemma text reasoning, and vision scan is data-entry support only, not diagnosis.

See [docs/demo_flow.md](docs/demo_flow.md) for the step-by-step judge script.

## Project Status Docs

For the concise current state, start with [docs/current_status.md](docs/current_status.md).

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
- Phase C multilingual demo support: selected patient-specific output languages demonstrated are English, Hindi, Spanish, and Swahili; `preferredLanguage` controls RealGemma visit-note output in fully gated submission mode after manual validation.
- RealGemma: manual text inference validated; app-facing reasoning now requires build flag + app-private sentinel + app-private model for inference.
- Supervisor priority: raw local counts remain visible, and the app attempts RealGemma priority reasoning; unavailable output shows retry/setup messaging.
- Audio: direct Gemma 4 audio is blocked; Smriti uses offline speech/editable transcript fallback.
- Vision: manual probe passed; local Gemma 4 vision extracts structured JSON from a synthetic paper note for CHW-reviewed data entry only. Image bytes are not persisted and no cloud OCR/API is used.

The 15.8s average RealGemma latency reflects real on-device Gemma 4 E2B text inference on CPU backend; in the CHW field workflow, this is positioned as protocol-grounded reasoning support replacing manual paper/protocol lookup, not instant chat.

See [docs/judge_evidence.md](docs/judge_evidence.md).

## What Works Now

- Android native app in Kotlin and Jetpack Compose.
- Local six-patient synthetic roster with Meena Sharma, Fatima Begum, Amara Tesfaye, Grace Achieng, Priya Devi, and Lucia Fernandez.
- Local supervisor-register import from `app/src/main/assets/demo/smriti_patients.json`; repeated imports upsert without duplicate histories.
- Add Patient flow with voice-first registration prompts and manual fallback fields.
- Missed follow-up card on patient open for overdue incomplete follow-ups, with Mark Confirmed and Note as Ongoing actions.
- History signal card for a cautious rising BP trend across prior visits.
- Room/SQLite local storage for patients, visits, referrals, and protocols.
- Local country/region-aware protocol retrieval from `app/src/main/assets/protocols/maternal_health_demo_protocols.json`.
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
- Direct Gemma 4 audio remains blocked by the public LiteRT-LM Android/Kotlin audio preprocessing path.

## LiteRT-LM Status

- Dependency pinned: `com.google.ai.edge.litertlm:litertlm-android:0.10.2`.
- Required model path: `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- The app detects whether that file exists; if missing or not enabled, generation is blocked with setup/retry messaging.
- EngineConfig defaults to stable `Backend.CPU()` when that model file exists. An isolated `Backend.GPU()` latency experiment exists behind explicit developer/test configuration and is not the default.
- Direct LiteRT-LM API types compile after the Room KSP migration.
- Runtime text inference is enabled only when the submission build flag, app-private local gate, and app-private model are present.
- No model files are committed.

See [docs/litert_status.md](docs/litert_status.md).

## Safety Constraints

- Smriti is not diagnostic AI.
- Outputs are protocol-grounded documentation and referral support only.
- Every clinical recommendation must include a protocol citation or be treated as uncertain.
- CHW confirmation is required before saving generated records.
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

Only claim a filmed RealGemma language after this manual harness passes for that language. No cloud translation API is used, and direct Gemma audio remains blocked.

## Judge Notes

For track fit, pitch points, and technical-depth framing, see [docs/judge_notes.md](docs/judge_notes.md). For a 3-minute video outline, see [docs/video_script_3min.md](docs/video_script_3min.md).
