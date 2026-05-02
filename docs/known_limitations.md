# Known Limitations

## Direct Gemma 4 Audio

Direct Gemma 4 audio transcription is not enabled in the normal app flow. LiteRT-LM Android `0.10.2` exposes audio container classes, but the manual runtime path hit:

```text
Audio must be preprocessed before being used in SessionAdvanced.
```

No public audio preprocessing API was found in the local AAR inspection. The current Phase 2 voice path is therefore editable transcript plus Android offline speech fallback, not direct Gemma audio.

The current Kotlin API path also does not expose prompt-template customization needed for multimodal placeholder injection. Direct Gemma 4 audio through the public LiteRT-LM Android/Kotlin path remains blocked by upstream artifact/API limits.

## RealGemma Local Setup Required

`RealGemmaAgent` is the app-facing reasoning engine. It requires a local submission build flag, an app-private sentinel, and a sideloaded app-private `.litertlm` model. If setup is missing or inference fails, the app shows setup/retry messaging and does not show mock clinical output.

## RealGemma Schema Adherence

RealGemma can load and return text on the emulator, but output schema adherence is still being tuned. A recent RealGemma response omitted the required `referralFlag` field, so the app rejected it as invalid output. This is expected safe behavior: invalid RealGemma output is not saved, is not shown as a clinical result, and does not trigger a mock fallback.

The hardened prompt requires exact JSON only with `summary`, boolean `referralFlag`, `referralReason`, `dangerSigns`, `followUpPlan`, `clarificationQuestion`, `citations`, `confidence`, and `safetyNote`. The parser can recover valid JSON from markdown fences or surrounding text and accepts safe aliases, but still rejects prose-only output, diagnostic language, missing referral equivalents, and referral output without valid supplied protocol citations.

Debug/dev builds log the first 1500 characters of raw rejected RealGemma output and the parser reason under `SmritiRealGemma`; CHW-facing UI shows only concise retry/setup messaging.

## Offline SpeechRecognizer Dependency

Android offline speech recognition depends on the emulator/device:

- recognizer service availability,
- on-device recognizer support,
- installed offline language packs,
- language support for `en-IN`, `en-US`, or `en`.

If the offline language pack is unavailable, the UI asks the CHW to type or use the sample transcript.

## Protocol Corpus Coverage

Global Protocol Pack v1 is a local 46-chunk corpus for maternal/ANC and CHW referral support across global, country, and regional tags. It is still not clinically complete. Before broader use, country-specific content needs review against official program materials and local referral pathways.

## No Real Patient Data

The repository uses demo patients and seeded mock visit history. It must not include real patient data or PHI.

## Manual Model Requirement

Manual RealGemma tests require a sideloaded app-private `.litertlm` model. No model file is committed, downloaded at runtime, or bundled in assets.

## Not Diagnostic AI

Smriti provides protocol-grounded documentation and referral support. It does not diagnose, prescribe, or act without CHW review.
