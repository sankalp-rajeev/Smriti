# Smriti

Smriti is an offline maternal-health visit copilot for community health workers. It helps a CHW select a patient, review local visit history, capture a voice-note-style observation, generate a structured visit note, surface protocol-grounded referral support, confirm the record, and produce an end-of-day supervisor summary.

The current hackathon demo is intentionally demo-safe: `MockGemmaAgent` is the default reasoning path, LiteRT-LM readiness is visible, and RealGemma text mode is available only behind developer gates.

Developer-only RealGemma text mode exists for local validation behind two gates, but it is not the default demo path and has no public CHW-facing toggle.

Judge-facing framing: local patient memory plus a local protocol pack, protocol-grounded referral support rather than diagnosis, and CHW review/confirm before saving.

## Problem

Community health workers often work from paper records, memory, and limited connectivity. That makes it easy to miss longitudinal context, such as a prior high blood pressure note during a later pregnancy danger-sign visit. Existing hospital scribes and cloud chatbots assume stable internet, EHR access, or clinician workflows. Smriti targets the field setting: one Android phone, local patient memory, local protocols, and no required network.

## Why Offline Matters

The core runtime must work in airplane mode. Smriti stores patient data locally, reads a local country/region-aware protocol asset corpus, records voice notes to app-private storage, and uses Android TTS locally when available. No cloud APIs are used for core runtime.

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

Do not claim direct Gemma 4 audio works or that the synthetic/global protocol pack is clinical validation. RealGemma text mode remains optional, local, and developer-gated.

See [docs/demo_flow.md](docs/demo_flow.md) for the step-by-step judge script.

## Project Status Docs

For the concise current state, start with [docs/current_status.md](docs/current_status.md).

- [Judge evidence](docs/judge_evidence.md)
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
- Protocol pack: 46 local chunks across global, country, and regional tags.
- Synthetic benchmark: 10 global cases through `ProtocolRetriever -> VisitReasoningPipeline -> MockGemmaAgent`.
- RealGemma: manual text inference validated and developer-only UI mode gated; not default.
- Audio: direct Gemma 4 audio is blocked; Smriti uses offline speech/editable transcript fallback.

See [docs/judge_evidence.md](docs/judge_evidence.md).

## What Works Now

- Android native app in Kotlin and Jetpack Compose.
- Local patient roster with Meena and prior visit history.
- Room/SQLite local storage for patients, visits, referrals, and protocols.
- Local country/region-aware protocol retrieval from `app/src/main/assets/protocols/maternal_health_demo_protocols.json`.
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
- Synthetic global benchmark cases for local protocol retrieval and mock visit reasoning.

## Mocked vs Real

Real now:

- Offline app flow, local storage, protocol asset retrieval, review/confirm, referral display, supervisor summary display, JSON export, TTS integration, and voice note recording.

Mocked now:

- Reasoning is deterministic through `MockGemmaAgent`.
- The sample danger-sign transcript is the reliable demo transcript when offline speech is unavailable.

Experimental and disabled:

- `RealGemmaAgent` is scaffolded behind an interface.
- LiteRT-LM dependency is present, and `EngineConfig` construction is available when a sideloaded model is found.
- Real `.litertlm` inference is not the default normal app flow.
- Developer-only RealGemma text mode requires both a debug/build-time gate and an app-private local gate.
- Direct Gemma 4 audio remains blocked by the public LiteRT-LM Android/Kotlin audio preprocessing path.

## LiteRT-LM Status

- Dependency pinned: `com.google.ai.edge.litertlm:litertlm-android:0.10.2`.
- Expected future model path: `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- The app detects whether that file exists, but does not load it.
- EngineConfig is constructed with `Backend.CPU()` only when that model file exists.
- Direct LiteRT-LM API types compile after the Room KSP migration.
- Runtime Engine initialization and text inference are disabled by default.
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

## Tests

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

The test suite covers mock reasoning, protocol retrieval, concise supervisor summary formatting, LiteRT readiness guards, disabled LiteRT client behavior, repo model-artifact safety, JSON export, and default mock mode.

## Judge Notes

For track fit, pitch points, and technical-depth framing, see [docs/judge_notes.md](docs/judge_notes.md). For a 3-minute video outline, see [docs/video_script_3min.md](docs/video_script_3min.md).
