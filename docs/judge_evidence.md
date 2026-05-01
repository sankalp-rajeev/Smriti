# Judge Evidence

This page is the concise evidence ledger for Smriti's current hackathon state. It separates what works in the normal judge demo, what is developer-only, what is blocked, and what is not claimed.

For the filmed/live runbook, use `docs/final_demo_checklist.md`.

## Normal Demo

The normal demo path is offline and mock-backed by default:

```text
Patient Roster
-> Meena visit screen
-> editable/sample transcript or offline speech fallback
-> VisitReasoningPipeline
-> ProtocolRetriever
-> MockGemmaAgent
-> ReviewScreen
-> CHW confirm/save
-> LocalVisitMemoryStore
-> Supervisor Summary
```

Evidence:

- `AgentConfig.DEFAULT_MODE = AgentMode.MOCK`.
- `MockGemmaAgent` is the default reasoning agent.
- Local patient roster and history use Room/SQLite.
- Local protocol retrieval uses JSON assets with country-aware ranking.
- Generated notes and referral support go to ReviewScreen before saving.
- CHW confirm/save is required before visits or referral flags persist.
- Supervisor Summary reads confirmed local data.
- Offline Proof is visible in the app and reports local/offline status.
- The core runtime does not require a cloud API.

## Phase B Patient Memory Intelligence

Phase B adds deterministic local intelligence without diagnosis:

- Amara has an overdue incomplete follow-up in seeded local data, so opening her VisitScreen shows a missed follow-up alert before transcript input.
- `Mark Confirmed` updates the prior visit's follow-up completion state; `Note as Ongoing` dismisses only for the current screen session when no notes field is available.
- Fatima has a rising BP history signal from prior readings `118/76 -> 125/80 -> 132/84 -> 138/88`.
- Grace's routine history does not trigger the rising BP signal.
- These cards are local logic over Room visit history. Gemma is not required.

## Protocol Pack

Global Protocol Pack v1 is local JSON:

- 46 local maternal/ANC and CHW referral-support chunks.
- Required tags: `GLOBAL_CORE`, `INDIA`, `BANGLADESH`, `ETHIOPIA`, `AFRICA_REGION`, `SOUTH_AMERICA_REGION`.
- Retrieval ranks exact country first, then region, then `GLOBAL_CORE`.
- No vector DB, cloud RAG, remote search, or runtime download is used.

This is a protocol scaffold for the demo. It is not clinical validation.

## Synthetic Benchmark Evidence

The synthetic benchmark suite has 10 cases covering:

- India ANC danger-sign case.
- India normal ANC follow-up.
- Bangladesh maternal danger-sign case.
- Ethiopia maternal danger-sign case.
- Africa-region fallback case.
- South America-region fallback case.
- `GLOBAL_CORE` fallback case.
- Vague/incomplete observation requiring clarification.
- No-danger-sign routine visit.
- Return visit with prior history relevance.

Covered country/region contexts include `IN`, `BD`, `ET`, `KE`, `PE`, `NP`, plus the required protocol region tags.

Runner:

```text
ProtocolRetriever -> VisitReasoningPipeline -> MockGemmaAgent
```

The tests verify retrieval level, citation expectations, referral behavior, uncertainty/clarification, Meena demo preservation, `MockGemmaAgent` default mode, and RealGemma developer gating. These are synthetic protocol-scaffold tests, not clinical validation.

## RealGemma Manual And Developer-Only Evidence

RealGemma text inference has been manually validated, but it is not the default demo mode.

Current RealGemma paths:

- Manual instrumentation tests with a sideloaded app-private `.litertlm` model.
- Developer-only UI text mode guarded by all of:
  - debug/build-time gate: `-Psmriti.realGemmaDevMode=true`,
  - app-private local gate: `files/dev/enable_real_gemma_text_mode`,
  - app-private model file: `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- Output still appears on ReviewScreen.
- CHW confirm/save remains required.
- Missing model, timeout, failed inference, invalid JSON, or rejected citation returns a safe unavailable/uncertain result.
- Recorded-demo submission mode is separate and requires all of: `-Psmriti.realGemmaSubmissionMode=true`, `files/dev/enable_real_gemma_text_mode`, and `filesDir/models/gemma-4-E2B-it-int4.litertlm`.
- When submission mode is fully active, visit generation uses `RealGemmaAgent` through `VisitReasoningPipeline`; if unavailable, the app shows `On-device reasoning unavailable — please retry.` and does not silently show mock output as RealGemma.
- SummaryScreen can show a `RealGemma Priority Follow-Up Queue` from today's confirmed visits, referral flags, missed follow-ups, history signals, patient context, and supplied protocol citations.
- If RealGemma priority generation fails, the deterministic local supervisor summary remains visible as the fallback evidence.

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

Manual memory stress:

- Context sizes: 10, 20, and 40 compact prior visits.
- `parserSuccessCount=3/3`.

Manual function calling:

- Native `log_visit` tool was manually executed once.
- Tool arguments included `patientId`, `observationText`, `protocolCitation`, and `referralRequired`.
- The tool returned `savedToRoom=false`.
- Native function calling is not wired into the normal UI.

GPU backend probe:

- Not attempted for the final video pass because the stable manual RealGemma path is CPU-backed and adding GPU would require a new isolated backend path.
- CPU backend is retained for the stable demo; GPU backend benchmarking remains future work.

## Audio Status

Direct Gemma 4 audio is blocked by the current public LiteRT-LM Android/Kotlin path.

Evidence:

- LiteRT-LM Android `0.10.2` exposes audio containers such as `Content.AudioBytes`, `Content.AudioFile`, `InputData.Audio`, and `InputData.Text`.
- Runtime audio attempt failed with:

```text
Audio must be preprocessed before being used in SessionAdvanced.
```

- Local AAR inspection found no public `AudioPreprocessor`, `AudioProcessor`, `Preprocessor`, or `preprocess(...)` API.
- The current Kotlin API path does not expose prompt-template customization needed for multimodal placeholder injection.
- Smriti therefore uses offline speech/editable transcript fallback into text reasoning.

Do not claim direct Gemma 4 audio works.

## Not Claimed

Smriti does not claim:

- clinical validation,
- autonomous diagnosis,
- autonomous treatment,
- direct Gemma 4 audio transcription,
- cloud runtime,
- RealGemma as default,
- model files bundled or downloaded by the app,
- real patient/PHI data in the repository.

## Validation

Latest local validation commands:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

Latest status: all three passed on April 29, 2026. The sandboxed Gradle wrapper can hit a user-cache lock-file issue; normal-cache reruns pass.
