# LiteRT-LM Status

This document is the current judge-facing status of Smriti's LiteRT-LM integration.

## Current State

- LiteRT-LM dependency is pinned in `app/build.gradle.kts`:
  `com.google.ai.edge.litertlm:litertlm-android:0.10.2`
- Room annotation processing uses KSP `2.3.7`; KAPT is no longer applied in the app module.
- `MockGemmaAgent` remains the default app mode.
- `RealGemmaAgent` remains experimental and disabled for the demo path.
- The app checks for the expected model path:
  `filesDir/models/gemma-4-E2B-it-int4.litertlm`
- If the model is absent, Offline Proof says `Real Gemma model: Not found`.
- If a model is present, Offline Proof says `Found, not loaded`.
- Direct LiteRT-LM API types now compile through a passive type probe:
  `Engine`, `EngineConfig`, `Backend`, `Content.Text`, and `Conversation`.
- `LiteRtEngineConfigFactory` constructs a real `EngineConfig` with `Backend.CPU()` only when the model file is found.
- `LiteRtEngineInitializationChecker` can initialize and immediately close `Engine` only when an explicit manual-test flag is true.
- No model file is committed to the repository.

## Manual-Only Engine Work

Smriti still does not run LiteRT inference in normal app behavior:

- No `.litertlm` model loading.
- No `Engine` instantiation during app startup or normal UI flow.
- No `Engine.initialize()` during app startup or normal UI flow.
- No Conversation creation.
- No inference or message sending.
- No Hugging Face or model download code.

The manual checker requires all of the following before it touches `Engine`: a found app-private model file, a prepared `EngineConfig`, and `allowManualEngineInitialization = true`. `Engine` implements `AutoCloseable`, so the checker uses `use { initialize() }` to close it immediately after initialization. This path is not wired into Patient Roster, Visit, Review, Summary, or any visible app toggle.

## Why Direct API Use Is Deferred

The LiteRT-LM artifact exposes Java 21 classfiles. The app previously used KAPT for Room, and direct references to LiteRT-LM runtime classes could trigger KAPT classfile compatibility failures. Room now uses KSP, so direct type references and `EngineConfig` construction compile. Runtime `Engine` initialization remains manual-only.

JDK 21 is required for direct LiteRT-LM API compile work.

## Readiness Guard

`RealGemmaReadinessEvaluator` is the safety gate. It reports judge-readable readiness while keeping:

- model loading disallowed in normal app flow,
- inference disallowed,
- engine creation false,
- engine initialization false,
- conversation creation false,
- sendMessage/inference false.

## Demo Position

The current hackathon demo proves the offline product flow and safety model. Real LiteRT-LM inference is a planned next step, not an active runtime path.
