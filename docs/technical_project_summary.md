# Smriti Technical Project Summary

## 1. Executive Summary

Smriti is an offline maternal-health visit copilot for community health workers (CHWs), designed for the Gemma 4 Good Hackathon demo scope. The app helps a CHW select a patient, review local visit history, capture or enter a visit observation, retrieve local protocol context, generate a structured visit note with referral support, require CHW review before saving, and produce an end-of-day supervisor summary.

The central product idea is local patient memory plus a local protocol pack plus a structured documentation workflow. Smriti is not diagnostic AI. It provides protocol-grounded documentation and referral support only, and every generated record must be reviewed and confirmed by the CHW before it is persisted.

The normal build uses `MockGemmaAgent` by default for deterministic offline behavior. Real Gemma 4 LiteRT-LM text inference has been validated through manual/developer-gated paths, and Phase B adds an optional recorded-demo submission mode that uses RealGemma only when all local gates and model readiness are satisfied. Direct Gemma 4 audio is blocked by the current public LiteRT-LM Android/Kotlin artifact/API path and is not claimed as working.

## 2. Problem And Use Case

CHWs and ASHAs often work from paper records, memory, and phone-based notes. They may not have EHR access, reliable connectivity, or immediate clinician backup during field visits. For maternal-health visits, this makes it easy to miss longitudinal context such as a prior borderline blood pressure note when a later visit includes danger signs.

The core demo case is `Meena Sharma, 28F`, a pregnant patient with prior ANC history. The CHW enters a danger-sign observation: severe headache, blurred vision, blood pressure around 150/95, and reduced fetal movement. Smriti retrieves relevant local protocol guidance, generates a structured visit note, flags protocol-grounded referral support, shows a citation, and requires CHW confirmation before saving.

The workflow is built around memory, retrieval, structure, and safety:

- remember the patient's local visit history,
- retrieve relevant local protocol text,
- produce an editable structured note,
- flag referral support when danger signs are present,
- require human review and confirmation.

## 3. Product Scope

The demo includes:

- Patient roster with six seeded synthetic patients and realistic synthetic prior histories.
- Local supervisor-register JSON import from app assets.
- Add-patient registration with voice-first offline speech prompts and manual fallback.
- Patient detail flow with local prior visit history.
- Missed follow-up alert on patient open for overdue incomplete follow-ups.
- Cautious rising BP history signal card from prior visit readings.
- Manual transcript entry and sample danger-sign transcript.
- Android offline speech fallback through `SpeechRecognizer` when device support and language packs are available.
- Local protocol retrieval from JSON assets.
- UI-independent reasoning pipeline.
- Structured visit note, referral support, citation, uncertainty, and follow-up output.
- Review/edit screen before persistence.
- CHW confirm/save gate.
- Local Room/SQLite persistence for confirmed visits and referral flags.
- Return-visit history that shows newly confirmed visits.
- End-of-day supervisor summary from confirmed local records.
- Optional RealGemma priority follow-up queue in fully gated submission mode, with deterministic local summary retained as fallback evidence.
- Reset Demo Data for repeatable filming/demo runs.
- Local JSON export for visit and summary data.
- Offline Proof display on the roster and summary screens.

Out of scope for the current submission:

- Diagnosis or autonomous treatment.
- Cloud sync or cloud runtime.
- Remote databases, Firebase, Supabase, OpenAI API, Gemini API, cloud ASR, cloud RAG, or model downloads.
- Real patient data or PHI.
- Full clinical guideline validation.
- Direct Gemma 4 audio through the current public LiteRT-LM Android/Kotlin path.
- Making RealGemma the default normal demo agent.
- Multilingual UI/output beyond the existing add-patient prompt support.

## 4. System Architecture

Normal submission flow:

```text
PatientListScreen
-> VisitScreen
-> transcript/manual input or offline speech fallback
-> VisitReasoningPipeline
-> ProtocolRetriever
-> GemmaAgent
-> ReviewScreen
-> LocalVisitMemoryStore
-> SummaryScreen
```

Key modules:

- `MainActivity`: wires the app state, patient selection, visit generation, review/save flow, summary flow, model status, and RealGemma developer gates.
- `PatientListScreen`: shows the local roster, Offline Proof, Add Patient, and local supervisor-register import.
- `AddPatientScreen`: collects a new local patient through EN/HI/ES/SW offline speech prompts or manual fields.
- `VisitScreen`: shows prior history, transcript controls, sample transcript, offline speech fallback, local reasoning context, and generate action.
- `PatientMemoryInsights`: deterministic missed follow-up and rising BP history-signal logic.
- `ReviewScreen`: displays editable generated output, referral support, protocol citation, safety gate, and confirm/save action.
- `SummaryScreen`: shows the supervisor brief, urgent cases, follow-ups, export, reset, and Offline Proof.
- `OfflineProofCard`: summarizes local/offline status, protocol source, active reasoning mode, RealGemma readiness, and direct-audio limitation.
- `VisitReasoningPipeline`: coordinates transcript text or local audio path, protocol retrieval, and `GemmaAgent` invocation. It writes nothing to storage.
- `ProtocolRetriever`: deterministic local keyword retrieval over JSON protocol chunks.
- `MockGemmaAgent`: deterministic default reasoning agent for the submission demo.
- `RealGemmaAgent`: developer-only/gated text reasoning agent backed by LiteRT-LM manual text inference when enabled.
- `LocalVisitMemoryStore`: persistence service for patients, confirmed visits, referral flags, reset, and supervisor-register import.
- `DemoSupervisorRegisterImporter`: loads the local synthetic supervisor register asset; repeated imports upsert without duplicate history.
- Room entities/DAOs: local patients, visits, referral flags, and protocol chunks.
- `SpeechToTextClient`: abstraction for transcript generation.
- `AndroidOfflineSpeechRecognizerClient`: offline-preferred Android speech fallback.
- `LiteRtGemmaTextClient`: manual/developer-gated LiteRT-LM text generation client.
- `RealGemmaPromptBuilder`: builds structured RealGemma prompts with patient history and protocol chunks.
- `RealGemmaOutputParser`: parses strict JSON output and rejects unsafe output.
- `RealGemmaSubmissionMode`: recorded-demo RealGemma gate evaluator requiring the build flag, local sentinel, and app-private model.
- `SupervisorPriorityPromptBuilder` / `SupervisorPriorityParser`: strict JSON RealGemma priority queue prompt and parser for supervisor follow-up ranking.
- `RealGemmaSafetyPostProcessor`: enforces non-diagnostic and CHW-confirmation wording.
- `ProtocolCitationValidator`: ensures RealGemma output uses supplied protocol citations or marks uncertainty.

## 5. Offline-First Data Model

Smriti uses Room/SQLite for local storage. The repository includes seeded synthetic patients and visit history for demo repeatability. The normal data path is local only:

- seeded six-patient synthetic roster,
- local supervisor-register import from `app/src/main/assets/demo/smriti_patients.json`,
- patient country/language/protocol-region metadata for localization and protocol context,
- confirmed visit logs,
- referral flags linked to saved visits,
- local protocol chunks from JSON assets,
- no PHI,
- no cloud runtime.

The six synthetic patients are Meena Sharma (India/Hindi danger-sign referral demo), Fatima Begum (Bangladesh rising BP trend), Amara Tesfaye (Ethiopia overdue follow-up data for Phase B), Grace Achieng (Kenya/Swahili routine no-referral history), Priya Devi (India/Hindi sparse early ANC history), and Lucia Fernandez (Peru/Spanish South America/global fallback context). Brazil is not used for Spanish-language Lucia.

The CHW confirm/save action is the only persistence gate. Speech input and note generation do not save data automatically. `VisitReasoningPipeline` is intentionally UI-independent and storage-free; only the Review screen confirmation path persists records through `LocalVisitMemoryStore`.

## 6. Reasoning Architecture

Reasoning is behind the `GemmaAgent` interface.

`MockGemmaAgent` is the deterministic submission default. It supports the complete demo flow offline and produces protocol-grounded note/referral output from local retrieved chunks. This keeps the filmed/live demo stable and avoids dependency on sideloaded model setup.

`RealGemmaAgent` is implemented as a developer-only/gated validation path. It builds prompts, calls a text client, parses strict JSON, validates citations, applies safety post-processing, and returns a `VisitReasoningResult`. It is not the default demo agent.

`VisitReasoningPipeline` takes patient context, observation text or local audio metadata, retrieves protocol chunks, calls the configured `GemmaAgent`, and returns a structured result. It does not write to Room. The Review screen remains the save gate.

## 7. RealGemma / LiteRT-LM Integration

LiteRT-LM dependency:

```text
com.google.ai.edge.litertlm:litertlm-android:0.10.2
```

Expected app-private model path:

```text
filesDir/models/gemma-4-E2B-it-int4.litertlm
```

Repository and runtime constraints:

- No `.litertlm` model file is committed.
- No model file is bundled in app assets.
- No runtime model download code exists.
- The normal default app path detects the model but does not load it.
- `EngineConfig` is constructed with `Backend.CPU()` only when the app-private model is present.
- Runtime `Engine` initialization and text inference are disabled by default.

Manual text inference has been validated with a sideloaded app-private model. `ManualLiteRtTextInferenceInstrumentedTest` sent the non-clinical prompt `Reply with exactly: SMRITI_LITERT_OK` and returned the expected text.

The full RealGemmaAgent path has also been validated:

```text
RealGemmaPromptBuilder
-> LiteRT sendMessage
-> RealGemmaOutputParser
-> RealGemmaSafetyPostProcessor
-> VisitReasoningResult
```

Developer-only RealGemma text UI mode is gated by all of:

- build flag: `-Psmriti.realGemmaDevMode=true`,
- app-private sentinel file: `files/dev/enable_real_gemma_text_mode`,
- app-private model presence at the expected `.litertlm` path.

Recorded-demo RealGemma submission mode is gated by all of:

- build flag: `-Psmriti.realGemmaSubmissionMode=true`,
- app-private sentinel file: `files/dev/enable_real_gemma_text_mode`,
- app-private model presence at `filesDir/models/gemma-4-E2B-it-int4.litertlm`.

If submission gates are closed, the app uses `MockGemmaAgent`. If RealGemma is active but inference fails, times out, produces invalid JSON, or violates citation/safety rules, the app shows an unavailable/retry state, preserves the transcript, and does not save or silently show mock output as RealGemma.

## 8. Direct Audio Limitation

LiteRT-LM Android `0.10.2` exposes audio-related public types:

- `Content.AudioBytes`
- `Content.AudioFile`
- `InputData.Audio`
- `InputData.Text`

However, the manual raw-audio runtime attempt hit:

```text
Audio must be preprocessed before being used in SessionAdvanced.
```

Local inspection of the `litertlm-android-0.10.2` AAR did not find a public `AudioPreprocessor`, `AudioProcessor`, `Preprocessor`, or `preprocess(...)` API. The current Kotlin API path also does not expose the prompt-template customization needed for multimodal placeholder injection.

Therefore, direct Gemma 4 audio through the current public LiteRT-LM Android/Kotlin path is blocked. Smriti uses this fallback architecture instead:

```text
Android offline speech or editable transcript
-> transcript text
-> local protocol retrieval
-> Gemma text reasoning
-> CHW review/confirm
```

Do not claim direct Gemma 4 audio works.

## 9. Protocol Retrieval

Smriti uses a local JSON protocol pack stored in app assets. The current pack contains 46 maternal/ANC and CHW referral-support chunks.

Protocol tags include:

- `GLOBAL_CORE`
- `INDIA`
- `BANGLADESH`
- `ETHIOPIA`
- `AFRICA_REGION`
- `SOUTH_AMERICA_REGION`

`ProtocolRetrievalContext` provides country/region context to the retriever. Ranking is deterministic:

```text
exact country -> region -> GLOBAL_CORE
```

Retrieval is keyword-based and fully local. There is no cloud RAG, remote search, vector database, or runtime download. The selected protocol citation is shown on the Review screen and carried into saved visit records when the CHW confirms.

The protocol corpus is a demo scaffold. It is not clinically complete and is not clinical validation.

## 10. Synthetic Global Benchmark Suite

The Phase 3 synthetic benchmark suite contains 10 local test cases covering:

- India ANC danger signs.
- India normal ANC follow-up.
- Bangladesh maternal danger signs.
- Ethiopia maternal danger signs.
- Africa-region fallback.
- South America-region fallback.
- `GLOBAL_CORE` fallback.
- Vague/incomplete observation requiring clarification.
- No-danger-sign routine visit.
- Return visit with prior history.

Country/region contexts include `IN`, `BD`, `ET`, `KE`, `PE`, `NP`, plus global and regional fallback coverage.

Runner:

```text
ProtocolRetriever -> VisitReasoningPipeline -> MockGemmaAgent
```

The suite verifies retrieval level, citation expectations, danger-sign referral behavior, routine no-false-referral behavior, uncertainty handling, clarification prompts, prior-history flow, and fallback retrieval. It proves scaffold behavior for the demo. It is not clinical validation.

## 11. Manual RealGemma Benchmark Evidence

Accepted manual RealGemma text benchmark:

- `totalScenarios=3`
- `successCount=3`
- `parserSuccessCount=3`
- `referralCount=1`
- `citationCount=2`
- `singleCitationContractCount=3`
- `averageLatencyMs=15812`
- `maxLatencyMs=26272`

Manual memory stress:

- Context sizes: 10, 20, and 40 compact prior visits.
- `parserSuccessCount=3/3`.

Native function calling:

- LiteRT-LM native `log_visit` tool was manually executed once.
- Tool fields included `patientId`, `observationText`, `protocolCitation`, and `referralRequired`.
- Function calling is not wired into the normal UI.

Latency framing:

The 15.8s average latency reflects real on-device Gemma 4 E2B text inference on CPU backend. In the CHW field workflow, this is positioned as protocol-grounded reasoning support replacing manual paper/protocol lookup, not instant chat.

GPU backend has not been validated for the final demo. CPU remains the stable documented backend; GPU benchmarking is future work.

## 12. Safety Model

Smriti's safety model is explicit:

- The app is not diagnostic AI.
- Output is protocol-grounded documentation and referral support only.
- Clinical recommendations must include a protocol citation or remain uncertain.
- CHW review/edit/confirm is required before saving.
- Speech and generation never auto-save.
- Data is local-only.
- Repository data is synthetic; no PHI is included.
- No cloud runtime, cloud ASR, cloud RAG, or remote database is used.

RealGemma-specific safety:

- `RealGemmaOutputParser` rejects diagnostic-language patterns.
- `ProtocolCitationValidator` rejects invented citations, semicolon-joined citations, and unsafe no-protocol citation patterns.
- If output cannot be trusted, the agent returns a safe uncertain fallback.
- `RealGemmaSafetyPostProcessor` appends required safety wording when missing:

```text
This is not a diagnosis.
CHW confirmation is required before saving.
```

## 13. Final Validation And APK

Final validation passed:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

APK size:

```text
55,882,255 bytes
```

Repository safety scan findings:

- No tracked `.litertlm` model files.
- No tracked audio files.
- No PHI or real patient data found.
- No cloud runtime dependency added.
- No runtime model download code.
- `MockGemmaAgent` remains default.
- RealGemma remains developer-only/gated.
- Docs do not claim direct Gemma audio works.
- Docs do not claim clinical validation.

## 14. Demo Flow

Recommended filmed/live flow:

1. Turn on airplane mode.
2. Open Smriti.
3. Show Patient Roster and Offline Proof.
4. Select `Meena Sharma, 28F`.
5. Show prior visit history.
6. Tap the sample danger-sign transcript or enter the observation manually.
7. Generate the local visit note.
8. On Review screen, show structured note, referral support, protocol citation, and safety gate.
9. Confirm CHW review and save.
10. Show End-of-Day Supervisor Summary.
11. Show urgent case and Offline Proof again.

Core spoken claim:

```text
Local patient memory + local protocol pack + CHW confirmation + offline runtime.
```

Avoid claiming:

- diagnosis,
- autonomous treatment,
- direct Gemma audio,
- clinical validation,
- RealGemma as the default normal demo mode,
- cloud runtime.

## 15. Known Limitations / Future Work

- Direct Gemma 4 audio is blocked by the current public LiteRT-LM Android/Kotlin audio preprocessing and prompt-template path.
- Android offline speech depends on device/emulator recognizer support and installed offline language packs.
- The protocol corpus is a scaffold, not a complete reviewed guideline library.
- RealGemma text mode is developer-only/gated and not the submission default.
- GPU backend benchmarking is future work.
- Clinical review is required before any real deployment.
- RealGemma submission mode depends on a locally sideloaded model and should be smoke-tested before filming.
- Sync, supervisor dashboarding, and multi-device workflows are future work.
- Country-specific protocol content needs expert review and expansion before pilot use.

## 16. File / Doc Map

Primary entry points:

- [README.md](../README.md): project overview and build/test entry point.
- [docs/judge_evidence.md](judge_evidence.md): concise evidence ledger.
- [docs/final_demo_checklist.md](final_demo_checklist.md): filming and release checklist.
- [docs/litert_status.md](litert_status.md): LiteRT-LM integration status.
- [docs/phase_1_stack_validation.md](phase_1_stack_validation.md): Phase 1 stack and RealGemma validation.
- [docs/phase_2_core_pipeline.md](phase_2_core_pipeline.md): Phase 2 local visit flow.
- [docs/phase_3_protocol_pack.md](phase_3_protocol_pack.md): protocol pack details.
- [docs/phase_3_benchmarks.md](phase_3_benchmarks.md): synthetic benchmark suite.
- [docs/demo_flow.md](demo_flow.md): judge demo script.
- [docs/offline_safety.md](offline_safety.md): offline and safety constraints.
- [docs/known_limitations.md](known_limitations.md): limitations and future work.
- [docs/local_model_setup.md](local_model_setup.md): sideloaded model and manual RealGemma setup.
