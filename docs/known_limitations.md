# Known Limitations

## Direct Gemma 4 Audio

Direct Gemma 4 audio transcription is not enabled in the normal app flow. LiteRT-LM Android `0.10.2` exposes audio container classes, but the manual runtime path hit:

```text
Audio must be preprocessed before being used in SessionAdvanced.
```

No public audio preprocessing API was found in the local AAR inspection. The current Phase 2 voice path is therefore editable transcript plus Android offline speech fallback, not direct Gemma audio.

The current Kotlin API path also does not expose prompt-template customization needed for multimodal placeholder injection. Direct Gemma 4 audio through the public LiteRT-LM Android/Kotlin path remains blocked by upstream artifact/API limits.

## Gemma 4 Vision Scope

The Phase H API surface check inspected the local `litertlm-android-0.10.2` AAR/classes.jar and found:

- `Content.ImageBytes`
- `Content.ImageFile`
- `InputData.Image`
- `EngineConfig.visionBackend`
- `EngineConfig.maxNumImages`
- `Conversation.sendMessage(Contents)`
- `Session.generateContent(List<InputData>)`

The same inspection did not find a public prompt-template, media-placeholder, multimodal-template, image-preprocessor, or `preprocess(...)` API. Despite that risk, the gated `ManualRealGemmaVisionProbeInstrumentedTest` passed on emulator with the sideloaded app-private model: the engine accepted `Conversation` image input and extracted structured JSON from the synthetic paper note.

The implemented feature is intentionally narrow:

- synthetic/demo paper-note data entry only,
- CHW review and explicit patient-record confirmation before save,
- no image bytes persisted,
- no diagnosis,
- no referral advice from image alone,
- no treatment recommendations beyond text written on the paper note,
- no wounds, rashes, ultrasound, medicine strips, growth charts, or photos of people,
- no cloud OCR/API.

Real-world handwriting quality, camera quality, and model behavior still need field validation before broader use.

## RealGemma Local Setup Required

`RealGemmaAgent` is the app-facing reasoning engine. It requires a local submission build flag, an app-private sentinel, and a sideloaded app-private `.litertlm` model. If setup is missing or inference fails, the app shows setup/retry messaging and does not show mock clinical output.

## RealGemma Native Stability

A native LiteRT-LM text inference crash was observed in Logcat inside `liblitertlm_jni.so` during `Conversation.sendMessage(prompt)` after overlapping/retried RealGemma calls piled up behind the synchronized text inference runner. This was not a screen, Room, or SQLite crash.

The app now uses a global non-queueing RealGemma inference gate so only one preload, visit-note generation, supervisor priority generation, paper-note vision extraction, or manual test request can run at a time. A second request returns `Smriti is already preparing a note. Please wait.` and does not enter another native call.

This reduces the likely overlapping-call crash condition, but a true native abort can still terminate the process because Kotlin cannot catch `SIGABRT`. For filming, use the stable CPU path, avoid repeated taps during inference, and do not rely on coroutine timeout as native cancellation.

## GPU Backend Experiment

CPU is the stable documented backend for the filmed path. `Backend.GPU()` is available only through an isolated manual latency experiment and is not default. If GPU is unsupported, crashes, or does not provide meaningful stable improvement on the target device/emulator, keep CPU for filming and documentation.

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
