# Offline Safety

Smriti is designed so the core demo can run in airplane mode.

## Offline Runtime

- Patient roster, visit history, referral flags, and protocol chunks are stored locally with Room/SQLite.
- Protocol retrieval uses a local country/region-aware asset JSON corpus.
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
- Referral language is framed as support for human review, not autonomous diagnosis or treatment.

## RealGemma Safety Gate

- `RealGemmaAgent` is the app-facing reasoning engine for visit notes and supervisor priority reasoning.
- `MockGemmaAgent` may remain only as a deterministic test fixture, not as app-facing clinical output.
- LiteRT-LM dependency and readiness checks are present.
- App startup does not run inference.
- RealGemma text reasoning requires the submission build flag, app-private local gate, and sideloaded app-private model.
- If setup is missing or inference fails, the app shows setup/retry messaging and does not save or display mock clinical output.
- Direct Gemma 4 audio remains blocked by the public LiteRT-LM Android/Kotlin preprocessing path.

## Data Boundary

The demo stores data on device. Exported JSON is generated only when the user taps export, and the file remains local until the user shares or syncs it outside the app.
