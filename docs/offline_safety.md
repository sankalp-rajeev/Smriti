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

- `MockGemmaAgent` remains the default mode.
- `RealGemmaAgent` is experimental and developer-gated.
- LiteRT-LM dependency and readiness checks are present.
- Normal app startup does not load or run a model.
- Developer-only RealGemma text mode requires both the build-time gate and app-private local gate.
- Direct Gemma 4 audio remains blocked by the public LiteRT-LM Android/Kotlin preprocessing path.

## Data Boundary

The demo stores data on device. Exported JSON is generated only when the user taps export, and the file remains local until the user shares or syncs it outside the app.
