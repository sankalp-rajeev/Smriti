# Known Limitations

## Direct Gemma 4 Audio

Direct Gemma 4 audio transcription is not enabled in the normal app flow. LiteRT-LM Android `0.10.2` exposes audio container classes, but the manual runtime path hit:

```text
Audio must be preprocessed before being used in SessionAdvanced.
```

No public audio preprocessing API was found in the local AAR inspection. The current Phase 2 voice path is therefore editable transcript plus Android offline speech fallback, not direct Gemma audio.

The current Kotlin API path also does not expose prompt-template customization needed for multimodal placeholder injection. Direct Gemma 4 audio through the public LiteRT-LM Android/Kotlin path remains blocked by upstream artifact/API limits.

## RealGemma Is Not Default

`MockGemmaAgent` remains default. `RealGemmaAgent` is scaffolded, manually testable, and available only through a developer-only text mode guarded by both a build-time flag and an app-private local gate. It has no public CHW-facing toggle.

## Offline SpeechRecognizer Dependency

Android offline speech recognition depends on the emulator/device:

- recognizer service availability,
- on-device recognizer support,
- installed offline language packs,
- language support for `en-IN`, `en-US`, or `en`.

If the offline language pack is unavailable, the UI asks the CHW to type or use the sample transcript.

## Protocol Corpus Size

The local protocol corpus is demo-sized. It supports the current maternal-health danger-sign and routine ANC demo flow, but Phase 3 should expand and review the corpus before broader use.

## No Real Patient Data

The repository uses demo patients and seeded mock visit history. It must not include real patient data or PHI.

## Manual Model Requirement

Manual RealGemma tests require a sideloaded app-private `.litertlm` model. No model file is committed, downloaded at runtime, or bundled in assets.

## Not Diagnostic AI

Smriti provides protocol-grounded documentation and referral support. It does not diagnose, prescribe, or act without CHW review.
