# Local Model Setup

Smriti is prepared for manual Gemma `.litertlm` sideloading, but the normal app does not load or run a model. The only real text inference entry point is a developer-run instrumentation test.

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

For the debug build, the actual application ID is:

```text
com.smriti.clinicalscribe
```

There is currently no debug `applicationIdSuffix`, so `run-as com.smriti.clinicalscribe` targets the debug app-private `filesDir`.

## Current App Behavior

- The app checks whether the expected app-private model file exists.
- If missing, Offline Proof reports the Real Gemma model as not found and inference disabled.
- If present, the app reports the model as found but not loaded.
- The LiteRT layer constructs `EngineConfig` with `Backend.CPU()` only when the model is found.
- Passive direct LiteRT API type references compile after the Room KSP migration.
- Engine initialization is manual-only and requires an explicit test flag.
- Normal app startup and UI screens do not create `Engine`.
- Normal app startup and UI screens do not create a conversation.
- Normal app startup and UI screens do not run inference.
- Manual text inference requires `allowManualTextInference=true`.
- `MockGemmaAgent` remains the safe default.

## Manual Text Inference Test

The developer-only instrumentation harness is:

```text
app/src/androidTest/java/com/smriti/clinicalscribe/reasoning/ManualLiteRtTextInferenceInstrumentedTest.kt
```

It uses `targetContext.filesDir`, checks `filesDir/models/gemma-4-E2B-it-int4.litertlm`, requires the explicit instrumentation argument `allowManualTextInference=true`, sends the non-clinical prompt `Reply with exactly: SMRITI_LITERT_OK`, logs the result to Logcat with tag `SmritiLiteRtManualTest`, and only asserts that returned text is non-empty. It does not touch Room patient or visit data.

Create the app-private model directory:

```powershell
adb shell run-as com.smriti.clinicalscribe mkdir -p files/models
```

Copy the local model through `/data/local/tmp` and into app-private storage:

```powershell
adb push "C:\path\to\gemma-4-E2B-it-int4.litertlm" /data/local/tmp/gemma-4-E2B-it-int4.litertlm
adb shell chmod 644 /data/local/tmp/gemma-4-E2B-it-int4.litertlm
adb shell run-as com.smriti.clinicalscribe cp /data/local/tmp/gemma-4-E2B-it-int4.litertlm files/models/gemma-4-E2B-it-int4.litertlm
adb shell run-as com.smriti.clinicalscribe ls -lh files/models/gemma-4-E2B-it-int4.litertlm
```

Run only the manual instrumentation test:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualLiteRtTextInferenceInstrumentedTest -Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true
```

View the manual test output:

```powershell
adb logcat -s SmritiLiteRtManualTest:I "*:S"
```

If the instrumentation argument is missing, the test is skipped. If the model is missing, the test is skipped with the expected app-private path in the skip reason. Normal validation should continue to use only `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug`.

## Manual Engine Check

`LiteRtEngineInitializationChecker` is a developer-only readiness helper. It can instantiate `Engine`, call `initialize()`, and immediately `close()` the engine only when:

- the model file was sideloaded outside git,
- `EngineConfig` was prepared,
- and `allowManualEngineInitialization` is explicitly `true`.

It does not create a Conversation and does not call `sendMessage`.

`LiteRtGemmaTextClient.generateTextManual(...)` is the separate manual text-inference helper. It requires a found model, prepared `EngineConfig`, and `allowManualTextInference=true`. It creates one Conversation, sends one text prompt, extracts text, and closes Conversation and Engine. This is not exposed in the app UI.

## Development Note

JDK 21 is required for LiteRT-LM compile/API work because `litertlm-android-0.10.2` exposes Java classfile version 65 API classes.
