# Smriti

Smriti is an offline maternal-health visit copilot for community health workers. It helps a CHW select a patient, review local visit history, capture a voice-note-style observation, generate a structured visit note, surface protocol-grounded referral support, confirm the record, and produce an end-of-day supervisor summary.

The current hackathon demo is intentionally demo-safe: `MockGemmaAgent` is the default reasoning path, LiteRT-LM readiness is visible, and RealGemma text mode is available only behind explicit developer/submission gates.

Developer-only RealGemma text mode exists for local validation, and the recorded-demo submission mode can use RealGemma only when the build flag, app-private sentinel, and app-private model are all present. Neither mode is a public CHW-facing toggle.

Judge-facing framing: local patient memory plus a local protocol pack, protocol-grounded referral support rather than diagnosis, and CHW review/confirm before saving.

## Problem

Community health workers often work from paper records, memory, and limited connectivity. That makes it easy to miss longitudinal context, such as a prior high blood pressure note during a later pregnancy danger-sign visit. Existing hospital scribes and cloud chatbots assume stable internet, EHR access, or clinician workflows. Smriti targets the field setting: one Android phone, local patient memory, local protocols, and no required network.

## Why Offline Matters

The core runtime must work in airplane mode. Smriti stores patient data locally, reads a local country/region-aware protocol asset corpus, records voice notes to app-private storage, and uses Android TTS locally when available. No cloud APIs are used for core runtime.

## Core Demo Flow

1. Turn on airplane mode.
2. Open Smriti.
3. Show Offline Proof on the Patient Roster.
4. Optionally select `Amara Tesfaye, 30F` to show the missed follow-up alert.
5. Optionally select `Fatima Begum, 24F` to show the rising BP history signal.
6. Select `Meena Sharma, 28F`.
7. Review prior visit history.
8. Use the sample danger-sign transcript or enter an observation.
9. Generate a local structured visit note.
10. Review referral suggestion and protocol citation.
11. CHW edits/confirms before saving.
12. Open End-of-Day Supervisor Summary.
13. Show urgent case, follow-ups, Offline Proof, RealGemma priority queue when gated, and optional JSON export.

Do not claim direct Gemma 4 audio works or that the synthetic/global protocol pack is clinical validation. RealGemma text mode remains optional, local, and gated for developer or recorded submission use.

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

- Normal demo: local roster/history, local protocol JSON, `MockGemmaAgent`, review/confirm/save, supervisor summary, and Offline Proof.
- Patient infrastructure: six synthetic demo patients with prior histories, local supervisor-register import from app assets, and add-patient registration with offline speech/manual fallback.
- Protocol pack: 46 local chunks across global, country, and regional tags.
- Synthetic benchmark: 10 global cases through `ProtocolRetriever -> VisitReasoningPipeline -> MockGemmaAgent`.
- Phase B memory intelligence: missed follow-up alert for Amara and rising BP trend signal for Fatima are deterministic local logic.
- Phase C multilingual demo support: selected patient-specific output languages are English, Hindi, Swahili, and Spanish; `preferredLanguage` controls RealGemma visit-note output in fully gated submission mode. The architecture can extend to more Gemma-supported languages as protocol packs and UI translations are added.
- RealGemma: manual text inference validated, developer-only UI mode gated, and recorded-demo submission mode gated by build flag + app-private sentinel + app-private model; not default.
- Supervisor priority: deterministic local summary always remains, with a RealGemma priority queue only when submission mode is fully active.
- Audio: direct Gemma 4 audio is blocked; Smriti uses offline speech/editable transcript fallback.

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
- Mock visit-note generation through `MockGemmaAgent`.
- Referral flag generation for pregnancy danger-sign keywords.
- CHW review/edit/confirm before saving.
- End-of-day supervisor summary with concise urgent cases.
- Reset Demo Data restores the clean six-patient synthetic roster and clears saved mock referrals.
- Local JSON export for visit and summary data.
- Local app-private voice note recording metadata.
- Android TTS buttons for offline voice output when device language data is available.
- Patient language labels on the roster and Visit screen: EN / English, हिंदी / Hindi, Kiswahili / Swahili, and Español / Spanish.
- Offline Proof visible on Patient Roster and Summary.
- Repo safety checks against committed model artifacts.
- Synthetic global benchmark cases for local protocol retrieval and mock visit reasoning.

## Mocked vs Real

Real now:

- Offline app flow, local storage, protocol asset retrieval, review/confirm, referral display, supervisor summary display, JSON export, TTS integration, and voice note recording.

Mocked now:

- Reasoning is deterministic through `MockGemmaAgent`.
- The sample danger-sign transcript is the reliable demo transcript when offline speech is unavailable.

Experimental and disabled:

- `RealGemmaAgent` is implemented behind an interface and has been manually validated with sideloaded local LiteRT-LM text inference.
- In recorded-demo submission mode, `RealGemmaPromptBuilder` asks for patient-specific local-language output from `preferredLanguage`; citation IDs remain stable in English.
- Safety post-processing appends required wording in English, Hindi, Spanish, or Swahili when missing.
- LiteRT-LM dependency is present, and `EngineConfig` construction is available when a sideloaded model is found.
- Real `.litertlm` inference is not the default normal app flow.
- Developer-only RealGemma text mode requires both a debug/build-time gate and an app-private local gate.
- RealGemma submission mode requires `-Psmriti.realGemmaSubmissionMode=true`, `files/dev/enable_real_gemma_text_mode`, and `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- RealGemma failures, timeouts, invalid JSON, or citation/safety rejection show an unavailable/retry state instead of silently displaying mock output as RealGemma.
- Direct Gemma 4 audio remains blocked by the public LiteRT-LM Android/Kotlin audio preprocessing path.

## LiteRT-LM Status

- Dependency pinned: `com.google.ai.edge.litertlm:litertlm-android:0.10.2`.
- Expected future model path: `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- The normal default app path detects whether that file exists but does not load it; manual/developer-gated paths can initialize it explicitly.
- EngineConfig is constructed with `Backend.CPU()` only when that model file exists.
- Direct LiteRT-LM API types compile after the Room KSP migration.
- Runtime Engine initialization and text inference are disabled by default, but manual/developer-gated RealGemma text inference has been validated.
- Developer-only RealGemma text mode can run only when the build gate, app-private local gate, and app-private model are present.
- No model files are committed.

See [docs/litert_status.md](docs/litert_status.md).

## Safety Constraints

- Smriti is not diagnostic AI.
- Outputs are protocol-grounded documentation and referral support only.
- Every clinical recommendation must include a protocol citation or be treated as uncertain.
- CHW confirmation is required before saving generated records.
- The default agent remains `MockGemmaAgent`.
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

The test suite covers mock reasoning, protocol retrieval, six-patient seed/import behavior, add-patient registration helpers, concise supervisor summary formatting, LiteRT readiness guards, disabled LiteRT client behavior, repo model-artifact safety, JSON export, and default mock mode.

Manual multilingual RealGemma validation is optional and requires a sideloaded app-private model:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaMultilingualInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

Only claim a filmed RealGemma language after this manual harness passes for that language. No cloud translation API is used, and direct Gemma audio remains blocked.

## Judge Notes

For track fit, pitch points, and technical-depth framing, see [docs/judge_notes.md](docs/judge_notes.md). For a 3-minute video outline, see [docs/video_script_3min.md](docs/video_script_3min.md).
