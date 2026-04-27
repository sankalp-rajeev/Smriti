# Local Model Setup

Smriti is prepared for future manual Gemma `.litertlm` sideloading, but the current app does not load or run a model.

## Safety Rules

- Do not commit model files.
- Do not put model files in `app/src/main/assets/`.
- Do not add runtime download code for the model.
- Do not add Hugging Face download code to the app runtime.
- Field/demo runtime must remain offline.

The expected model filename is:

```text
gemma-4-E2B-it-int4.litertlm
```

The expected app-private runtime path is:

```text
filesDir/models/gemma-4-E2B-it-int4.litertlm
```

## Current App Behavior

- The app checks whether the expected app-private model file exists.
- If missing, Offline Proof reports the Real Gemma model as not found and inference disabled.
- If present, the app reports the model as found but not loaded.
- The LiteRT layer prepares `EngineConfig` only when the model is found.
- Engine initialization is intentionally disabled.
- No `Engine` is created.
- No conversation is created.
- No inference is run.
- `MockGemmaAgent` remains the safe default.

## Development Note

JDK 21 is required for LiteRT-LM compile/API work because `litertlm-android-0.10.2` exposes Java classfile version 65 API classes.
