# LiteRT-LM / Gemma 4 Android Integration Notes

Date: 2026-04-27

## Scope

This is a documentation-only integration spike for Smriti. No LiteRT dependency, model file, runtime behavior, or default agent mode is changed by these notes. `MockGemmaAgent` remains the demo-safe default.

## Official Sources Researched

- LiteRT-LM overview: https://ai.google.dev/edge/litert-lm/overview
- LiteRT-LM Android Kotlin guide: https://ai.google.dev/edge/litert-lm/android
- Gemma 4 E2B LiteRT-LM model card: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Google AI Edge Gallery: https://github.com/google-ai-edge/gallery

## Android Integration Path

The official Android guide exposes LiteRT-LM through the Kotlin package `com.google.ai.edge.litertlm`.

The documented Gradle dependency is:

```kotlin
implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")
```

For Smriti, prefer pinning an explicit version after Android Studio/Gradle resolves the current Google Maven artifact. The existing repository declarations already include the likely required repositories:

```kotlin
repositories {
    google()
    mavenCentral()
}
```

The basic runtime shape is:

```kotlin
val engineConfig = EngineConfig(
    modelPath = "/path/to/your/model.litertlm",
    backend = Backend.CPU(),
)

val engine = Engine(engineConfig)
engine.initialize()

val conversation = engine.createConversation()
val response = conversation.sendMessage("...")
```

The Android guide also documents async message APIs, conversation config, tool registration, multimodal content, and native-layer error handling. LiteRT-LM calls can throw `LiteRtLmJniException` or lifecycle exceptions, so Smriti should wrap all real calls in timeout/error handling and fall back to `MockGemmaAgent`.

## Minimum Android Requirements

The LiteRT-LM Android guide does not clearly state a single `minSdk` requirement on the page reviewed. The Google AI Edge Gallery README says the Gallery app requires Android 12 and up.

Current Smriti state:

- `compileSdk = 34`
- `targetSdk = 34`
- `minSdk = 26`
- emulator verified: Pixel 9 Pro API 35

Recommendation: treat Android 12 / API 31+ as the known-good target for the LiteRT-LM spike until dependency resolution proves otherwise. Do not raise Smriti's `minSdk` during the dependency-only test unless the LiteRT artifact requires it.

## Model Format

Gemma 4 E2B for LiteRT-LM is distributed in `.litertlm` format. The official LiteRT-LM overview lists Gemma4-E2B as a supported chat model with size around 2583 MB. The Hugging Face model card describes the Android/iOS/Desktop/IoT/Web-ready model and says the model file size is 2.58 GB.

Expected model:

```text
gemma-4-E2B-it-int4.litertlm
```

The Android guide shows examples using a filesystem `modelPath`. Some examples for older models mention `.task` files, but the Gemma 4 E2B LiteRT-LM model card identifies `.litertlm` as the relevant format for this spike.

## Model Packaging and Loading Options

LiteRT-LM expects a local filesystem path to the model. Practical Smriti options:

1. App-private sideload/import path
   - Best first implementation target.
   - Keep the 2.58 GB model out of git and out of the APK.
   - Store path in local config once the user/developer places the model on device.

2. App-private downloaded file
   - Useful for development or a setup flow.
   - Not acceptable as a core runtime requirement because Smriti must work offline in the field.
   - Any download should happen before the offline demo/field use.

3. Bundled asset
   - Technically simple conceptually, but not recommended because the model is multi-GB.
   - May still need copying from assets to app-private storage before native loading.

4. Hugging Face download
   - The model is hosted under `litert-community/gemma-4-E2B-it-litert-lm`.
   - This is a distribution/source option, not a runtime dependency for the offline demo.
   - License/model access terms must be checked before shipping.

Recommendation: add a model loader that checks for an app-private `.litertlm` path and reports unavailable if missing. Do not commit model files.

## Text, Audio, and Multimodal Input

The Android guide documents `Content.Text`, `Content.ImageBytes`, `Content.ImageFile`, `Content.AudioBytes`, and `Content.AudioFile` for multimodal messages. It also shows separate `visionBackend` and `audioBackend` configuration for supported multimodal models.

The Gemma 4 E2B model card says the model includes text, vision, and audio components, with vision/audio models loaded as needed.

For Smriti, the first real integration should be text-only:

- Use the existing typed/simulated transcript as input.
- Keep local voice recording as metadata until ASR/audio behavior is proven.
- Add audio input only after text prompt, local protocol retrieval, structured parsing, timeout, and fallback behavior are stable.

## Structured Outputs and Function Calling

The Android guide documents tool/function calling through Kotlin `ToolSet` classes and OpenAPI-style tool specs. It also shows manual tool calling where a response message exposes `toolCalls` with names and arguments.

For Smriti's first RealGemma path, use a stricter text-to-JSON contract before relying on tools:

- Build a prompt that asks for a compact JSON object matching `VisitReasoningResult`.
- Parse JSON locally into existing data models.
- Reject invalid, missing, non-cited, or diagnostic-language output.
- Fall back to `MockGemmaAgent` or return the existing unavailable/uncertain result on timeout or parse failure.

Tool/function calling may be useful later for protocol retrieval or JSON schema enforcement, but it should not be the first coding step unless the selected Gemma 4 model and Android artifact are confirmed to support the needed behavior reliably.

## Open Risks

- LiteRT-LM artifact version and API surface may change; pin an explicit version after dependency resolution.
- Minimum Android requirement is not clear from the Android guide; Gallery's Android 12+ requirement may force a product decision later.
- The Gemma 4 E2B model is about 2.58 GB, so packaging, storage, install size, and first-load time are major demo risks.
- Runtime memory is substantial; test on the actual Pixel 9 Pro API 35 emulator/device before enabling any UI mode.
- Direct audio input is documented for supported models, but Smriti still needs text transcript behavior first for safe clinical review.
- Structured output needs local validation. Do not trust raw model text without schema checks and safety checks.
- Model download from Hugging Face is not compatible with offline field runtime unless done during setup before field use.
- Android AI Core/Gemini Nano is mentioned as recommended for production on supported Android devices, but Smriti's hackathon path remains LiteRT-LM/Gemma behind `GemmaAgent`.

## Recommended Implementation Sequence

1. Add LiteRT-LM dependency only and verify Gradle sync/build on the existing app.
2. Add a model availability loader that checks an app-private `.litertlm` path without loading the model by default.
3. Implement text-only `RealGemmaAgent` behind `AgentMode.REAL_GEMMA_EXPERIMENTAL`, with strict timeout, JSON parsing, citation validation, and fallback.
4. Benchmark model load, memory, and generation latency on the emulator/device.
5. Explore direct audio input only after text-only reasoning is stable.

Next coding step: **A. add LiteRT dependency only**.
