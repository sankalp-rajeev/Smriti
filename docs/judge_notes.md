# Judge Notes

## Health And Sciences Fit

Smriti addresses a real last-mile health workflow: community health workers document visits on paper, often without internet, EHR access, or immediate clinical backup. The demo focuses on maternal-health ANC visits, longitudinal patient memory, protocol-grounded referral support, and end-of-day supervisor reporting.

## LiteRT Track Fit

The app is structured for local Gemma 4 through LiteRT-LM behind a replaceable `GemmaAgent` interface. The LiteRT-LM dependency is pinned and readiness is visible in-app. Real model loading and inference are intentionally disabled until controlled device testing, so the demo remains safe and fully offline.

## Impact And Vision

- CHWs serve large patient panels in low-connectivity environments.
- Maternal danger signs require timely referral support.
- Local visit history reduces reliance on memory and paper.
- Protocol citations make recommendations reviewable by CHWs and supervisors.
- The phone becomes a local patient-memory and documentation aid, not a cloud chatbot.

## Video Pitch Points

- Open with the field problem: no signal, no EHR, paper records, high-stakes maternal visits.
- Show airplane mode before launching.
- Show Meena's prior history before generating a new visit note.
- Use the danger-sign transcript to trigger referral support.
- Show the protocol citation and CHW confirmation step.
- End with the supervisor summary and Offline Proof.

## Technical Depth Points

- Android native app with Kotlin, Jetpack Compose, Room/SQLite, local assets, Android TTS, and local JSON export.
- `GemmaAgent` abstraction separates reasoning from UI.
- `MockGemmaAgent` provides deterministic offline demo behavior.
- RealGemma scaffold includes prompt/parsing/readiness layers without runtime inference.
- LiteRT-LM dependency is present, model path detection exists, and EngineConfig is a deferred plan only.
- Tests guard default mock mode, disabled LiteRT client behavior, readiness safety, and repo model-artifact safety.

## Not A Generic Chatbot

Smriti is patient-contextual and workflow-bound. It starts from a selected patient, reads local history, retrieves local protocol snippets, generates a structured visit record, requires CHW confirmation, saves locally, and builds a supervisor summary from confirmed records.

## Not Gemini Live

Gemini Live can answer general questions when online. Smriti is designed for offline field operation with persistent local patient data, local protocol citations, structured visit/referral records, and end-of-day supervisor reporting. No core runtime feature requires cloud access.

## Why The Mock Fallback Is Demo-Safe

`MockGemmaAgent` is deterministic, offline, and easy to inspect. It lets judges verify the product workflow, safety constraints, review/confirm requirement, and local data model without pretending that real LiteRT inference has been validated. This avoids unsafe clinical or model-loading claims.

## What Remains Experimental

- Real Gemma `.litertlm` model loading.
- LiteRT-LM `Engine` creation and initialization.
- Conversation creation and inference.
- Native Gemma audio/ASR.
- Function calling through LiteRT-LM.
- Benchmarks for latency, memory, WER, and referral accuracy with the real model.
