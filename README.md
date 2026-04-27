# Smriti

Smriti is an offline maternal-health visit copilot for community health workers. It helps a CHW select a patient, review local visit history, capture a voice-note-style observation, generate a structured visit note, surface protocol-grounded referral support, confirm the record, and produce an end-of-day supervisor summary.

The current hackathon demo is intentionally demo-safe: `MockGemmaAgent` is the default reasoning path, LiteRT-LM readiness is visible, and real model loading/inference is disabled until controlled device testing is complete.

## Problem

Community health workers often work from paper records, memory, and limited connectivity. That makes it easy to miss longitudinal context, such as a prior high blood pressure note during a later pregnancy danger-sign visit. Existing hospital scribes and cloud chatbots assume stable internet, EHR access, or clinician workflows. Smriti targets the field setting: one Android phone, local patient memory, local protocols, and no required network.

## Why Offline Matters

The core runtime must work in airplane mode. Smriti stores patient data locally, reads a local protocol asset corpus, records voice notes to app-private storage, and uses Android TTS locally when available. No cloud APIs are used for core runtime.

## Core Demo Flow

1. Turn on airplane mode.
2. Open Smriti.
3. Show Offline Proof on the Patient Roster.
4. Select `Meena, 28F`.
5. Review prior visit history.
6. Use the sample danger-sign transcript or enter an observation.
7. Generate a local structured visit note.
8. Review referral suggestion and protocol citation.
9. CHW edits/confirms before saving.
10. Open End-of-Day Supervisor Summary.
11. Show urgent case, follow-ups, Offline Proof, and optional JSON export.

See [docs/demo_flow.md](docs/demo_flow.md) for the step-by-step judge script.

## What Works Now

- Android native app in Kotlin and Jetpack Compose.
- Local patient roster with Meena and prior visit history.
- Room/SQLite local storage for patients, visits, referrals, and protocols.
- Local protocol retrieval from `app/src/main/assets/protocols/maternal_health_demo_protocols.json`.
- Mock visit-note generation through `MockGemmaAgent`.
- Referral flag generation for pregnancy danger-sign keywords.
- CHW review/edit/confirm before saving.
- End-of-day supervisor summary with concise urgent cases.
- Reset Demo Data restores original Meena history and clears saved mock referrals.
- Local JSON export for visit and summary data.
- Local app-private voice note recording metadata.
- Android TTS buttons for offline voice output when device language data is available.
- Offline Proof visible on Patient Roster and Summary.
- Repo safety checks against committed model artifacts.

## Mocked vs Real

Real now:

- Offline app flow, local storage, protocol asset retrieval, review/confirm, referral display, supervisor summary display, JSON export, TTS integration, and voice note recording.

Mocked now:

- Reasoning is deterministic through `MockGemmaAgent`.
- The sample danger-sign transcript stands in for real ASR.

Experimental and disabled:

- `RealGemmaAgent` is scaffolded behind an interface.
- LiteRT-LM dependency is present, and `EngineConfig` construction is available when a sideloaded model is found.
- Real `.litertlm` model loading in normal app flow, Conversation creation, and inference are disabled.
- Engine initialization exists only as an explicit manual developer check and is not wired into the UI.
- Text-only LiteRT inference exists only as an explicit manual developer call and is not wired into the UI.
- Real Gemma audio/ASR is not implemented yet.

## LiteRT-LM Status

- Dependency pinned: `com.google.ai.edge.litertlm:litertlm-android:0.10.2`.
- Expected future model path: `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- The app detects whether that file exists, but does not load it.
- EngineConfig is constructed with `Backend.CPU()` only when that model file exists.
- Direct LiteRT-LM API types compile after the Room KSP migration.
- Runtime Engine initialization and text inference are disabled by default and manual-only.
- `RealGemmaReadinessEvaluator` keeps model loading and inference disabled.
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

## Tests

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

The test suite covers mock reasoning, protocol retrieval, concise supervisor summary formatting, LiteRT readiness guards, disabled LiteRT client behavior, repo model-artifact safety, JSON export, and default mock mode.

## Judge Notes

For track fit, pitch points, and technical-depth framing, see [docs/judge_notes.md](docs/judge_notes.md). For a 3-minute video outline, see [docs/video_script_3min.md](docs/video_script_3min.md).
