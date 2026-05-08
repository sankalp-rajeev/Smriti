# Smriti Technical Project Summary

## 1. Executive Summary

Smriti is an offline maternal-health visit copilot for community health workers (CHWs), designed for the Gemma 4 Good Hackathon demo scope. The app helps a CHW manage a local patient roster, check urgent local protocol guidance, review visit history, capture or enter a visit observation, retrieve local protocol context, generate a structured visit note with referral support, require CHW review before saving, create local follow-up loops, prepare patient-friendly leave-behind messages, view a community panel, and produce an end-of-day supervisor summary.

The central product idea is local patient memory plus a local protocol pack plus a structured documentation workflow. Smriti is not diagnostic AI. It provides protocol-grounded documentation and referral support only, and every generated record must be reviewed and confirmed by the CHW before it is persisted.

The filmed/local submission flow uses `RealGemmaAgent` as the app-facing reasoning engine. Real Gemma 4 LiteRT-LM text inference has been validated through manual paths, and app inference requires the submission build flag, app-private sentinel, and app-private model. Gemma audio transcription is wired into the Visit screen after the LiteRT-LM 0.11.0 manual probe succeeded; audio fills an editable transcript only. Clinical note generation still goes through text reasoning, protocol citation validation, ReviewScreen, and confirm/save after the CHW manually taps Generate Visit Note. No audio-only save path. No direct audio diagnosis, treatment, or referral.

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
- Urgent Protocol Lookup from the roster or Visit screen for read-only local danger-sign guidance with citations.
- Missed follow-up alert on patient open for overdue incomplete follow-ups.
- Local follow-up task scheduling after confirmed saves, with due/upcoming/overdue roster state and mark done/reschedule actions.
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
- RealGemma priority follow-up queue attempt in the app-facing summary flow, with raw local counts retained when RealGemma supervisor reasoning is unavailable.
- Patient leave-behind message generated from saved reviewed visit data, editable before copy/share.
- Community Panel / Village Panel view derived from local patients, visits, referral flags, and follow-up tasks.
- Reset Demo Data for repeatable filming/demo runs.
- Local JSON export for visit and summary data.
- Local Gemma 4 vision paper-note extraction from synthetic paper notes for data-entry support only.
- Offline Proof display from `Check offline setup` and on the summary screen.

Out of scope for the current submission:

- Diagnosis or autonomous treatment.
- Cloud sync or cloud runtime.
- Remote databases, Firebase, Supabase, OpenAI API, Gemini API, cloud ASR, cloud RAG, or model downloads.
- Real patient data or PHI.
- Full clinical guideline validation.
- Direct audio diagnosis, treatment, or referral.
- Bundling the RealGemma model or bypassing local setup gates.
- Broad all-language support or untested Amharic, Oromo, or Bangla output.
- Broad camera diagnosis or clinical image diagnosis.

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
- `PatientListScreen`: shows the local roster, patient-card note language labels, Add Patient, local supervisor-register import, and a `Check offline setup` path to Offline Proof.
- `CommunityPanelBuilder` / `CommunityPanelScreen`: derive and show a local village-level caseload panel without model inference.
- `UrgentProtocolLookupBuilder` / `UrgentProtocolLookupScreen`: provide read-only CHW-facing local protocol lookup from danger-sign chips or typed observations without model inference or persistence.
- `AddPatientScreen`: collects a new local patient through EN/HI/ES/SW offline speech prompts or manual fields.
- `VisitScreen`: shows prior history, transcript controls, sample transcript, offline speech fallback, local reasoning context, and generate action.
- `PatientMemoryInsights`: deterministic missed follow-up and rising BP history-signal logic.
- `ReviewScreen`: displays editable generated output, referral support, protocol citation, safety gate, and confirm/save action.
- `SummaryScreen`: shows the supervisor brief, urgent cases, follow-ups, patient-message entry point after save, community panel entry point, export, reset, and Offline Proof.
- `PatientLeaveBehindMessageGenerator` / `PatientMessageScreen`: generate a safe local patient message from saved reviewed data and provide editable Share/Copy controls.
- `OfflineProofCard`: summarizes local/offline status, protocol source, active reasoning mode, RealGemma readiness, and direct-audio limitation.
- `VisitReasoningPipeline`: coordinates transcript text or local audio path, protocol retrieval, and `GemmaAgent` invocation. It writes nothing to storage.
- `ProtocolRetriever`: deterministic local keyword retrieval over JSON protocol chunks.
- `MockGemmaAgent`: deterministic test fixture only; not used for app-facing clinical/visit/supervisor output.
- `RealGemmaAgent`: required app-facing text reasoning agent backed by LiteRT-LM when local setup is complete.
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
- follow-up tasks linked to saved visits or seeded history,
- local protocol chunks from JSON assets,
- no PHI,
- no cloud runtime.

The six synthetic patients are Meena Sharma (India/Hindi danger-sign referral demo), Fatima Begum (Bangladesh rising BP trend), Amara Tesfaye (Ethiopia overdue follow-up data for Phase B), Grace Achieng (Kenya/Swahili routine no-referral history), Priya Devi (India/Hindi sparse early ANC history), and Lucia Fernandez (Peru/Spanish South America/global fallback context). Brazil is not used for Spanish-language Lucia.

The CHW confirm/save action is the only persistence gate. Speech input, note generation, and urgent protocol lookup do not save data automatically. `VisitReasoningPipeline` is intentionally UI-independent and storage-free; only the Review screen confirmation path persists records through `LocalVisitMemoryStore`. Follow-up tasks are created only after a visit is saved and remain separate from saved visit counts. Patient leave-behind messages are generated on demand from saved reviewed data and are not persisted as visits or follow-ups. Community Panel counts are read-only derivations from local state, and lookup-only activity is not counted.

## 6. Reasoning Architecture

Reasoning is behind the `GemmaAgent` interface.

`RealGemmaAgent` is the required app-facing reasoning engine. `MockGemmaAgent` may remain only as a deterministic unit-test fixture and must not be shown as clinical/visit/supervisor output.

`RealGemmaAgent` builds prompts, calls the local text client, parses strict JSON, validates citations, applies safety post-processing, and returns a `VisitReasoningResult`. If setup or inference fails, the app shows retry/setup messaging and does not save.

Phase C adds selected multilingual output support for the recorded demo. `Patient.preferredLanguage` maps to English, Hindi, Swahili, or Spanish and is passed into `RealGemmaPromptBuilder` for visit-note prompts in fully gated submission mode. Protocol citation IDs remain stable in English. `RealGemmaSafetyPostProcessor` appends required safety wording in the requested demo language if the model omits it. The architecture can extend to more Gemma-supported languages as protocol packs and UI translations are added.

`VisitReasoningPipeline` takes patient context, observation text or local audio metadata, retrieves protocol chunks, calls the configured `GemmaAgent`, and returns a structured result. It does not write to Room. The Review screen remains the save gate.

## 7. RealGemma / LiteRT-LM Integration

LiteRT-LM dependency:

```text
com.google.ai.edge.litertlm:litertlm-android:0.11.0
```

Expected app-private model path:

```text
filesDir/models/gemma-4-E2B-it-int4.litertlm
```

Repository and runtime constraints:

- No `.litertlm` model file is committed.
- No model file is bundled in app assets.
- No runtime model download code exists.
- The app path detects the model and only attempts inference after the submission build flag, local sentinel, and app-private model are present.
- `EngineConfig` defaults to `Backend.CPU()` when the app-private model is present; GPU remains an explicit experiment.
- Runtime `Engine` initialization and text inference are blocked until local setup is complete.

Manual text inference has been validated with a sideloaded app-private model. `ManualLiteRtTextInferenceInstrumentedTest` sent the non-clinical prompt `Reply with exactly: SMRITI_LITERT_OK` and returned the expected text.

The full RealGemmaAgent path has also been validated:

```text
RealGemmaPromptBuilder
-> LiteRT sendMessage
-> RealGemmaOutputParser
-> RealGemmaSafetyPostProcessor
-> VisitReasoningResult
```

RealGemma app-facing text mode is gated by all of:

- build flag: `-Psmriti.realGemmaSubmissionMode=true`,
- app-private sentinel file: `files/dev/enable_real_gemma_text_mode`,
- app-private model presence at `filesDir/models/gemma-4-E2B-it-int4.litertlm`.

If submission gates are closed, the app shows RealGemma setup-required messaging. If inference fails, times out, produces invalid JSON, or violates citation/safety rules, the app shows an unavailable/retry state, preserves the transcript, and does not save or show mock output.

Manual multilingual RealGemma validation is available through `ManualRealGemmaMultilingualInstrumentedTest`. It runs Meena/Hindi, Grace/Swahili, and Lucia/Spanish with the sideloaded app-private model, logs requested language, raw output preview, parser status, citation presence, safety wording, and a simple language heuristic. These results must pass before a language is claimed in the video.

## 8. Gemma Audio Transcription

Gemma audio transcription validated through LiteRT-LM 0.11.0 manual probe. `Conversation.sendMessage(Contents.of(Content.Text(prompt), Content.AudioBytes(audioBytes)))` with `EngineConfig.audioBackend = Backend.CPU()` succeeded on-device.

Transcript preview from the probe:

```text
Meena is seven months pregnant. She has severe headache and blurred vision and this is a demo.
```

Smriti's safe audio path:

```text
local microphone audio
-> Gemma audio transcription
-> editable transcript field
-> existing RealGemma text reasoning pipeline
-> protocol retrieval / citation validation
-> ReviewScreen
-> CHW confirm/save
```

Audio fills an editable transcript only. CHW must review/edit before generating the note. Clinical note generation still goes through text reasoning, protocol citation validation, ReviewScreen, and confirm/save. No audio-only save path. No direct audio diagnosis or treatment. Production-grade multilingual audio quality is not yet claimed. App-facing microphone recording is wired only to the editable transcript field.

History: LiteRT-LM 0.10.2 blocked audio with `Audio must be preprocessed before being used in SessionAdvanced.` The 0.11.0 upgrade added `EngineConfig.audioBackend` and the probe with `Backend.CPU()` resolved the blocker.

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

## 9A. Urgent Protocol Lookup

Phase 5 adds a read-only urgent local guidance path:

```text
PatientListScreen or VisitScreen
-> UrgentProtocolLookupScreen
-> quick danger-sign chips or optional typed observation
-> ProtocolRetriever with patient context or GLOBAL_CORE fallback
-> local guidance card with citation or safe no-guidance fallback
```

The quick-select observations are severe headache, blurred vision, high blood pressure, reduced fetal movement, bleeding, convulsions, severe abdominal pain, and fever. Patient-launched lookup uses the selected patient's `ProtocolRetrievalContext`; roster-launched lookup uses `GLOBAL_CORE` fallback. The result can say `Urgent review may be needed` only when the retrieved local chunk contains urgent/danger/referral language.

The lookup is intentionally not an emergency chatbot. It does not call RealGemma, LiteRT, supervisor reasoning, Android share intents, cloud APIs, or protocol generation. It does not write to Room and does not create visits, referral flags, follow-up tasks, patient messages, Summary counts, or Community Panel counts.

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
ProtocolRetriever -> VisitReasoningPipeline -> RealGemmaAgent
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

GPU backend is isolated as an explicit developer/test experiment through `LiteRtBackendMode.GPU_EXPERIMENTAL` and `ManualRealGemmaBackendLatencyInstrumentedTest`. CPU remains the default stable backend. Do not use GPU for the filmed build unless the manual experiment is run on the target device/emulator, produces successful RealGemma output, and shows meaningful stable latency improvement without breaking the CPU fallback.

## 12. Paper-Note Vision Status

Smriti demonstrates local Android LiteRT-LM text reasoning and local Gemma 4 vision paper-note extraction.

Manual vision probe evidence:

- `ManualRealGemmaVisionProbeInstrumentedTest` passed on emulator with the sideloaded app-private model.
- The engine accepted `Conversation` image input.
- Local Gemma 4 vision extracted structured JSON from the synthetic paper note: Grace Achieng, 02 May 2026, BP 116/74, symptoms, routine ANC follow-up, confidence HIGH, and `needsReview=true`.
- The app uses this only for paper-note data entry support.
- CHW review/edit and explicit patient-record confirmation are required before saving.
- Image bytes are not persisted.
- No cloud OCR/API is used.
- Gemma audio transcription is wired into the Visit screen; audio fills an editable transcript only. The vision path is separate from audio.

## 12A. Local Follow-Up, Patient Message, Community Panel, And Lookup

The final workflow additions are deterministic local app logic:

- `FollowUpTask` is a Room entity in `follow_up_tasks`. Tasks can be sourced from saved visits, seeded history, or manual/local flows. Active tasks drive roster chips, Visit screen cards, and Summary counts. They do not count as saved visits.
- `PatientLeaveBehindMessageGenerator` creates a patient-friendly message after a saved visit exists. It uses the saved reviewed `VisitLog`, patient context, and linked `ReferralFlag` only. The message is editable before sharing, and Android sharing is user-initiated through `ACTION_SEND` / `text/plain`.
- `CommunityPanelBuilder` derives total patients, pregnancy stage, urgent review saved, follow-up counts, history-signal count, no-recent-visit count, languages, countries, and a priority list from local state. It does not call RealGemma, LiteRT, protocol retrieval, or network APIs.
- `UrgentProtocolLookupBuilder` derives lookup results from selected CHW observations and local protocol chunks. It does not call RealGemma, LiteRT, cloud APIs, or any persistence API.

These features do not add diagnosis, treatment, dosage, clinical prediction, cloud APIs, or new model calls.

## 13. Safety Model

Smriti's safety model is explicit:

- The app is not diagnostic AI.
- Output is protocol-grounded documentation and referral support only.
- Clinical recommendations must include a protocol citation or remain uncertain.
- CHW review/edit/confirm is required before saving.
- Speech and generation never auto-save.
- Follow-up tasks, patient messages, and Community Panel counts do not bypass CHW confirm/save.
- Urgent protocol lookup is read-only local guidance and does not create saved records or counts.
- Patient message sharing is user-initiated only; Smriti does not auto-send SMS or WhatsApp.
- Data is local-only.
- Repository data is synthetic; no PHI is included.
- No cloud runtime, cloud ASR, cloud RAG, or remote database is used.

RealGemma-specific safety:

- `RealGemmaOutputParser` rejects diagnostic-language patterns.
- `ProtocolCitationValidator` rejects invented citations, semicolon-joined citations, and unsafe no-protocol citation patterns.
- If output cannot be trusted, the agent returns a safe uncertain fallback.
- `RealGemmaSafetyPostProcessor` appends required safety wording when missing. English, Hindi, Spanish, and Swahili are supported for the recorded demo:

```text
This is not a diagnosis.
CHW confirmation is required before saving.
यह निदान नहीं है। CHW की पुष्टि आवश्यक है।
Esto no es un diagnóstico. Se requiere confirmación de la trabajadora de salud.
Hii si utambuzi wa ugonjwa. Uthibitisho wa mfanyakazi wa afya unahitajika.
```

## 14. Final Validation And APK

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
- `RealGemmaAgent` is the app-facing reasoning engine.
- RealGemma requires local setup: build flag, app-private sentinel, and sideloaded model.
- Docs do not claim direct audio clinical reasoning.
- Docs do not claim clinical validation.

## 15. Demo Flow

Recommended filmed/live flow:

1. Turn on airplane mode.
2. Show Welcome.
3. Tap `Check offline setup` to show Offline Proof / setup ready, then return to the roster.
4. Show Patient Roster search, attention chips, and patient-card note language labels.
5. Open Urgent Protocol Lookup to show local danger-sign guidance with citation and no automatic save.
6. Open Community Panel to show local caseload counts, follow-ups, languages/countries, and priority list.
7. Show Amara missed follow-up alert.
8. Show Fatima rising BP history signal.
9. Show Meena Hindi RealGemma note with referral suggested, citation/local guidance, and CHW confirm/save.
10. Open the post-save patient message for review/edit/share controls.
11. Show Lucia Spanish RealGemma note after manual validation.
12. Show Grace Swahili routine/no-referral RealGemma note after manual validation.
13. Show Grace sample paper-note scan: local Gemma vision extracts structured paper-note data for CHW review/save.
14. Show End-of-Day Summary urgent/follow-up/routine priority list and Community Panel entry.
15. Close with Offline Proof: no cloud APIs, local patient memory, local guidance, RealGemma text + vision + audio transcription validated.

Core spoken claim:

```text
Local patient memory + local protocol pack + CHW confirmation + offline runtime.
```

Avoid claiming:

- diagnosis,
- autonomous treatment,
- urgent lookup as emergency AI, AI triage, treatment guidance, or a risk score,
- direct audio diagnosis, treatment, or referral,
- clinical validation,
- broad all-language support,
- broad camera diagnosis,
- missing RealGemma setup as a successful reasoning result,
- cloud runtime.

## 16. Known Limitations / Future Work

- Gemma audio transcription is wired into the app-facing Visit screen through LiteRT-LM 0.11.0; audio fills an editable transcript only and still requires a separate manual Generate Visit Note action. No direct audio diagnosis, treatment, or referral.
- Android offline speech depends on device/emulator recognizer support and installed offline language packs.
- The protocol corpus is a scaffold, not a complete reviewed guideline library.
- Urgent Protocol Lookup is limited by the scaffold protocol corpus and must not be treated as clinical validation or an emergency chatbot.
- RealGemma text reasoning is required for app-facing output; missing setup shows retry/setup messaging.
- Target-device GPU benchmark results are pending.
- GPU backend is not default; it is an isolated experiment unless target-device evidence proves stable improvement.
- Clinical review is required before any real deployment.
- RealGemma submission mode depends on a locally sideloaded model and should be smoke-tested before filming.
- Sync, supervisor dashboarding, and multi-device workflows are future work.
- Country-specific protocol content needs expert review and expansion before pilot use.

## 17. File / Doc Map

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
