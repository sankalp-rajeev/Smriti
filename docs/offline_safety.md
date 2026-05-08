# Offline Safety

Smriti is designed so the core demo can run in airplane mode.

## Offline Runtime

- Patient roster, visit history, referral flags, follow-up tasks, and protocol chunks are stored locally with Room/SQLite.
- Protocol retrieval uses a local country/region-aware asset JSON corpus.
- Urgent Protocol Lookup uses that same local asset corpus for danger-sign guidance and does not call a model or network service.
- Community Panel counts are derived from local patients, visits, referral flags, and follow-up tasks without model inference.
- Patient leave-behind messages are generated after confirm/save from saved reviewed local data.
- Voice notes are recorded to app-private local storage.
- Visit and summary JSON exports are written locally.
- Android TTS is used locally when device language data is available.
- No cloud APIs are used for core runtime.

## Clinical Safety

- Smriti is not diagnostic AI.
- The app provides protocol-grounded documentation and referral support only.
- Clinical recommendations must cite a local protocol section or be treated as uncertain.
- Generated records are editable before save.
- CHW confirmation is required before any generated visit record is saved.
- Follow-up tasks are created only after confirm/save and do not count as saved visits.
- Patient leave-behind messages are editable before sharing and are shared only by user action through Android's share sheet.
- Community Panel wording is local caseload support, not clinical prediction, risk scoring, diagnosis, or treatment planning.
- Urgent Protocol Lookup wording is local health guidance only. Lookup alone does not create visits, referral flags, follow-up tasks, patient messages, summary counts, or community-panel counts.
- Referral language is framed as support for human review, not autonomous diagnosis or treatment.

## RealGemma Safety Gate

- `RealGemmaAgent` is the app-facing reasoning engine for visit notes and supervisor priority reasoning.
- `MockGemmaAgent` may remain only as a deterministic test fixture, not as app-facing clinical output.
- LiteRT-LM dependency and readiness checks are present.
- App startup does not run inference.
- RealGemma text reasoning requires the submission build flag, app-private local gate, and sideloaded app-private model.
- If setup is missing or inference fails, the app shows setup/retry messaging and does not save or display mock clinical output.
- Gemma audio transcription is wired into the Visit screen behind RealGemma submission readiness. Audio fills an editable transcript only. Clinical note generation still goes through text reasoning, protocol citation validation, ReviewScreen, and confirm/save after a manual Generate action. No audio-only save path. No direct audio diagnosis, treatment, or referral.

## Data Boundary

The demo stores data on device. Exported JSON is generated only when the user taps export, and the file remains local until the user shares or syncs it outside the app.
