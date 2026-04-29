# Phase 1 Stack Validation

Phase 1 validated the Gemma 4 LiteRT-LM stack and RealGemma scaffolding without changing the normal app flow. RealGemma/LiteRT-LM execution is disabled by default; later Phase 3 work added a developer-only gated text UI mode.

## LiteRT Text Inference

Manual harness:

- `app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualLiteRtTextInferenceInstrumentedTest.kt`

Result:

- Sends the non-clinical prompt `Reply with exactly: SMRITI_LITERT_OK`.
- Uses `LiteRtGemmaTextClient.generateTextManual(...)`.
- Requires `allowManualTextInference=true`.
- Requires a sideloaded app-private model at `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- Asserts non-empty returned text.
- Does not write to Room.

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtTextInferenceInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true
```

Logcat:

```powershell
adb logcat -s SmritiLiteRtManualTest:I "*:S"
```

## Structured JSON Result

Manual harness:

- `app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualRealGemmaVisitJsonInstrumentedTest.kt`

Result:

- Builds a Meena ANC prompt with `RealGemmaPromptBuilder`.
- Supplies demo prior history and local protocol chunks.
- Requires JSON-only output, non-diagnostic wording, CHW confirmation, and protocol citation.
- Parses with `RealGemmaOutputParser`.
- Logs parser success or rejection as an early behavior signal.
- Does not write to Room.

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaVisitJsonInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true
```

Logcat:

```powershell
adb logcat -s SmritiRealGemmaJsonTest:I "*:S"
```

## RealGemmaAgent Path

Manual harness:

- `app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualRealGemmaAgentInstrumentedTest.kt`

Result:

- Constructs `RealGemmaAgent` with an androidTest-only `RealGemmaTextClient` backed by manual LiteRT text inference.
- Exercises prompt builder -> LiteRT text inference -> parser -> `VisitReasoningResult`.
- Requires safety wording and cited output or clear uncertainty.
- Does not write to Room.

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaAgentInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true
```

Logcat:

```powershell
adb logcat -s SmritiRealGemmaAgentTest:I "*:S"
```

## Safety Post-Processing

`RealGemmaSafetyPostProcessor` appends missing safety language after successful parsing:

- `This is not a diagnosis.`
- `CHW confirmation is required before saving.`

This does not weaken diagnostic-language rejection. Unsafe diagnostic claims are still rejected by the parser.

## Citation Validation

`ProtocolCitationValidator` enforces:

- Supplied-protocol output must use exactly one supplied citation.
- Semicolon-joined citations are rejected.
- Invented citations are rejected.
- Model-written `No matching protocol citation` is rejected in RealGemma output.
- No-protocol uncertain output must use an empty citation and no referral.

## Benchmark Numbers

Manual benchmark harness:

- `app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualRealGemmaBenchmarkInstrumentedTest.kt`

Scenarios:

- `totalScenarios=3`
- ANC danger signs
- Normal ANC follow-up
- Incomplete observation

Logged metrics:

- `successCount`
- `parserSuccessCount`
- `referralCount`
- `citationCount`
- `singleCitationContractCount`
- `averageLatencyMs`
- `maxLatencyMs`
- `fallbackOrUncertainCases`

Latest accepted benchmark:

- `totalScenarios=3`
- `successCount=3`
- `parserSuccessCount=3`
- `referralCount=1`
- `citationCount=2`
- `singleCitationContractCount=3`
- `averageLatencyMs=15812`
- `maxLatencyMs=26272`

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaBenchmarkInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

Logcat:

```powershell
adb logcat -s SmritiRealGemmaBenchmark:I "*:S"
```

## Native Function Calling

Manual harness:

- `app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualLiteRtFunctionCallingInstrumentedTest.kt`

Captured result:

- Ran on Pixel_10_Pro AVD with app-private model size `2583085056` bytes.
- Native function calling passed.
- The model executed `log_visit` once.
- Tool arguments included `patientId`, `observationText`, `protocolCitation`, and `referralRequired`.
- The tool returned `savedToRoom=false`.
- This is not wired into normal app flow.

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtFunctionCallingInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true" "-Pandroid.testInstrumentationRunnerArguments.allowManualFunctionCalling=true"
```

Logcat:

```powershell
adb logcat -s SmritiLiteRtFunctionTest:I "*:S"
```

## Memory Stress

Manual harness:

- `app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualRealGemmaMemoryStressInstrumentedTest.kt`

Captured result:

- Context sizes: `10`, `20`, and `40` compact prior visits.
- Instrumentation test passed.
- `parserSuccessCount=3`.
- All 10/20/40 runs had citations, referral flags, safety wording, and no failure categories.
- Approximate JVM memory before/after and latency are logged by the harness.

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaMemoryStressInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

Logcat:

```powershell
adb logcat -s SmritiRealGemmaMemory:I "*:S"
```

## Audio API Result

Manual harness:

- `app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualLiteRtAudioCapabilityInstrumentedTest.kt`

Captured result:

- Audio API surface probe passed for `Content.AudioBytes`, `Content.AudioFile`, `InputData.Audio`, and `InputData.Text`.
- This only proves API surface availability.
- It does not prove Gemma 4 E2B audio transcription quality.

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtAudioCapabilityInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualAudioProbe=true"
```

Logcat:

```powershell
adb logcat -s SmritiLiteRtAudioProbe:I "*:S"
```

## Direct Gemma 4 Audio Blocker

Manual harness:

- `app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualLiteRtAudioInferenceInstrumentedTest.kt`

Current blocker:

```text
Audio must be preprocessed before being used in SessionAdvanced.
```

The local `litertlm-android-0.10.2` AAR did not expose a public `AudioPreprocessor`, `AudioProcessor`, `Preprocessor`, or `preprocess(...)` API. The manual audio inference test now tries the `Content.AudioFile` conversation route first and the raw `InputData.Audio` session route second, then logs blocked/skipped if preprocessing is still unavailable.

The current Kotlin API path also does not expose the prompt-template customization needed for multimodal placeholder injection. Direct Gemma 4 audio through the public LiteRT-LM Android/Kotlin path is therefore blocked by upstream artifact/API limits. Smriti uses offline ASR/editable transcript fallback into verified Gemma 4 text reasoning instead.

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtAudioInferenceInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualAudioInference=true" "-Pandroid.testInstrumentationRunnerArguments.manualAudioFilePath=/data/local/tmp/manual-smriti-audio.wav"
```

Logcat:

```powershell
adb logcat -s SmritiLiteRtAudioInference:I "*:S"
```

## Validation Boundary

Phase 1 does not change normal app behavior:

- No model file is committed.
- No model download code is present.
- No Hugging Face runtime code is present.
- No public CHW-facing RealGemma toggle exists.
- `MockGemmaAgent` remains default.
- RealGemma/LiteRT-LM inference remains disabled by default and is limited to manual instrumentation or developer-only gated text mode.
