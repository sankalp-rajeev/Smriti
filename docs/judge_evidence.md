# Judge Evidence

This page is the concise evidence ledger for Smriti's current hackathon state. The app-facing reasoning path now requires RealGemma text reasoning; mock output is not shown when RealGemma is unavailable.

For the filmed/live runbook, use `docs/final_demo_checklist.md`.

## RealGemma-Required Demo

The filmed/local submission path is offline and RealGemma-backed:

```text
Patient Roster
-> VisitScreen
-> editable/sample transcript or offline speech fallback
-> VisitReasoningPipeline
-> ProtocolRetriever
-> RealGemmaAgent
-> ReviewScreen
-> CHW confirm/save
-> LocalVisitMemoryStore
-> Raw local counts + RealGemma priority queue attempt
```

Evidence:

- `AgentConfig.DEFAULT_MODE = AgentMode.REAL_GEMMA_REQUIRED`.
- App-facing visit generation uses `RealGemmaAgent`.
- App-facing supervisor priority generation attempts RealGemma.
- Missing model, missing gate, timeout, failed inference, invalid JSON, or citation/safety rejection displays setup/retry messaging and does not save.
- RealGemma has loaded and returned output on the emulator. The most recent observed failure mode was schema adherence: output omitted required `referralFlag`, so the hardened parser rejected it safely.
- When the gated model is present, Smriti starts a background RealGemma preload and keeps the shared engine/client warm for subsequent patient generations and supervisor priority attempts. The first RealGemma call may still be slower because model/session initialization is expensive; later calls should avoid repeated cold loads when memory allows.
- Confirm/save is a local Room/SQLite write only. It never invokes RealGemma, never re-runs retrieval, and never auto-exports JSON; the CHW confirm/save gate remains required.
- `SmritiLatency` logs timing markers for readiness, preload/init, protocol retrieval, history formatting, prompt build, generation, parser/safety/citation validation, ReviewScreen navigation, local save, and summary refresh without logging transcripts or raw clinical text.
- Measured emulator/local setup timing evidence from `SmritiLatency`: RealGemma preload/init 1.885 s; Meena RealGemma generation 21.726 s; Meena validation 31 ms; Meena Room save 49 ms; Meena summary refresh 5 ms; Lucia RealGemma generation after preload/reuse 14.434 s; Lucia validation 4 ms; protocol retrieval 1-2 ms; prompt build 1-3 ms. Device performance may vary.
- Timing interpretation: RealGemma inference dominates latency. Local retrieval, prompt build, parser/safety/citation validation, Room save, and summary refresh are negligible by comparison. The second generation was faster after preload/engine reuse.
- The visit prompt now asks for exact JSON only: `summary`, boolean `referralFlag`, `referralReason`, `dangerSigns`, `followUpPlan`, `clarificationQuestion`, `citations`, `confidence`, and `safetyNote`.
- The parser extracts close JSON from markdown fences/surrounding text and accepts safe aliases, but still rejects missing referral equivalents, diagnostic wording, invented/missing referral citations, and prose-only output.
- `MockGemmaAgent` may remain in tests/fixtures only; app screens do not use it for clinical/visit/supervisor output.
- Local patient roster and history use Room/SQLite.
- Local protocol retrieval uses JSON assets with country-aware ranking.
- Generated notes and referral support go to ReviewScreen before saving.
- CHW confirm/save is required before visits or referral flags persist.
- Raw local counts and saved urgent flags can remain visible as local data.
- Offline Proof reports local/offline status, RealGemma model status, setup state, and blocked direct audio.
- The core runtime does not require a cloud API.

## Required Local Setup

RealGemma inference requires:

```powershell
.\gradlew.bat assembleDebug -Psmriti.realGemmaSubmissionMode=true
adb shell run-as com.smriti.clinicalscribe mkdir -p files/dev
adb shell run-as com.smriti.clinicalscribe touch files/dev/enable_real_gemma_text_mode
adb shell run-as com.smriti.clinicalscribe ls -lh files/models/gemma-4-E2B-it-int4.litertlm
```

The model must be sideloaded outside git to:

```text
filesDir/models/gemma-4-E2B-it-int4.litertlm
```

No model file is committed, bundled, downloaded at runtime, or fetched from a cloud API.

## Phase B Patient Memory Intelligence

- Amara has an overdue incomplete follow-up in seeded local data, so opening her VisitScreen shows a missed follow-up alert before transcript input.
- `Mark Confirmed` updates the prior visit's follow-up completion state; `Note as Ongoing` dismisses only for the current screen session.
- Fatima has a rising BP history signal from prior readings `118/76 -> 125/80 -> 132/84 -> 138/88`.
- Grace's routine history does not trigger the rising BP signal.
- These cards are deterministic local logic over Room visit history. They are not diagnosis or prediction.

## Protocol Pack

Global Protocol Pack v1 is local JSON:

- 46 local maternal/ANC and CHW referral-support chunks.
- Required tags: `GLOBAL_CORE`, `INDIA`, `BANGLADESH`, `ETHIOPIA`, `AFRICA_REGION`, `SOUTH_AMERICA_REGION`.
- Retrieval ranks exact country first, then region, then `GLOBAL_CORE`.
- No vector DB, cloud RAG, remote search, or runtime download is used.

This is a protocol scaffold for the demo. It is not clinical validation.

## Synthetic Benchmark Evidence

The synthetic benchmark suite still exists as a deterministic fixture suite over `MockGemmaAgent`. It is retained for retrieval and local protocol-scaffold regression testing only. It is not app-facing reasoning and should not be described as the live demo engine.

## RealGemma Manual Evidence

Accepted manual RealGemma benchmark:

- `totalScenarios=3`
- `successCount=3`
- `parserSuccessCount=3`
- `referralCount=1`
- `citationCount=2`
- `singleCitationContractCount=3`
- `averageLatencyMs=15812`
- `maxLatencyMs=26272`

The 15.8s average latency reflects real on-device Gemma 4 E2B text inference on CPU backend; in the CHW field workflow, this is positioned as protocol-grounded reasoning support replacing manual paper/protocol lookup, not instant chat.

Latency tuning note: the app now preloads and reuses the RealGemma engine where supported, compacts visit prompts to recent history plus top protocol chunks, and keeps save latency separate from generation latency. The measured emulator/local setup showed preload/init at 1.885 s, a first Meena generation at 21.726 s, and a later Lucia generation at 14.434 s after preload/reuse. This is performance evidence only, not clinical validation.

Current schema-hardening note: if manual RealGemma output still fails the strict parser, the app treats that as a safe rejection, preserves the transcript for retry, logs raw output only in debug/dev Logcat under `SmritiRealGemma`, and does not fall back to mock clinical output.

## Phase C Multilingual Evidence

- Smriti demonstrates selected patient-specific local-language output: English, Hindi, Swahili, and Spanish.
- `Patient.preferredLanguage` controls the RealGemma visit-note output language.
- Patient mapping is Meena/Priya -> Hindi, Grace -> Swahili, Lucia -> Spanish, and Fatima/Amara -> English.
- Lucia is Peru/Spanish; Brazil is not used for her Spanish-language demo.
- Protocol citation IDs remain stable in English and are not translated.
- No cloud translation API is used.
- Manual multilingual RealGemma validation is required before filming or claiming a language in the video.
- The architecture can extend to more Gemma-supported languages as protocol packs and UI translations are added.

Manual multilingual harness:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.smriti.clinicalscribe.reasoning.ManualRealGemmaMultilingualInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.allowManualTextInference=true"
```

## Audio Status

Direct Gemma 4 audio is blocked by the current public LiteRT-LM Android/Kotlin path. Smriti uses offline speech/editable transcript fallback into RealGemma text reasoning.

Do not claim direct Gemma 4 audio works.

## Not Claimed

Smriti does not claim:

- clinical validation,
- autonomous diagnosis,
- autonomous treatment,
- direct Gemma 4 audio transcription,
- cloud runtime,
- mock output as RealGemma,
- model files bundled or downloaded by the app,
- real patient/PHI data in the repository.

## Validation

Required local validation commands:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

Manual RealGemma validation requires a connected target and sideloaded app-private model.
