# Judge Notes

## Health And Sciences Fit

Smriti addresses a real last-mile health workflow: community health workers document visits on paper, often without internet, EHR access, or immediate clinical backup. The demo focuses on maternal-health ANC visits, longitudinal patient memory, urgent local protocol lookup, protocol-grounded referral support, local follow-up loops, patient leave-behind messages, village panel management, and end-of-day supervisor reporting.

## LiteRT Track Fit

The app is structured for local Gemma 4 through LiteRT-LM behind a replaceable `GemmaAgent` interface. The LiteRT-LM dependency is pinned and readiness is visible in-app. The filmed/local submission flow requires RealGemma text reasoning with explicit local gates and a sideloaded app-private model. If setup is missing or inference fails, the app shows setup/retry messaging instead of mock clinical output.

## Impact And Vision

- CHWs serve large patient panels in low-connectivity environments.
- Maternal danger signs require timely referral support.
- Local visit history reduces reliance on memory and paper.
- Protocol citations make recommendations reviewable by CHWs and supervisors.
- The phone becomes a local patient-memory, panel-management, and documentation aid, not a cloud chatbot.

## Video Pitch Points

- Open with the field problem: no signal, no EHR, paper records, high-stakes maternal visits.
- Show airplane mode before launching.
- Show Urgent Protocol Lookup as a local guidance check that does not save or create referral flags by itself.
- Show Community Panel to prove Smriti supports whole-roster village work, not only one patient note.
- Show Meena's prior history before generating a new visit note.
- Use the danger-sign transcript to trigger referral support.
- Show the protocol citation and CHW confirmation step.
- Show the post-save patient message as editable and user-shared only.
- End with the supervisor summary and Offline Proof.

## Technical Depth Points

- Android native app with Kotlin, Jetpack Compose, Room/SQLite, local assets, Android TTS, and local JSON export.
- Urgent Protocol Lookup uses deterministic `ProtocolRetriever` lookups over the local protocol pack with patient country/region context or `GLOBAL_CORE` fallback.
- Local follow-up scheduling and Community Panel counts are deterministic Room/local-state logic, not model inference.
- Patient leave-behind messages are generated locally from saved reviewed records and shared only by user action.
- `GemmaAgent` abstraction separates reasoning from UI.
- `RealGemmaAgent` is the app-facing visit-note and supervisor-priority reasoning engine.
- Real Gemma 4 LiteRT-LM text inference has been validated on-device through manual instrumentation with a sideloaded app-private model.
- `ManualLiteRtTextInferenceInstrumentedTest` returned `SMRITI_LITERT_OK`.
- The full RealGemmaAgent path has been validated: `RealGemmaPromptBuilder -> LiteRT sendMessage -> RealGemmaOutputParser -> RealGemmaSafetyPostProcessor -> VisitReasoningResult`.
- Accepted manual RealGemma benchmark: `totalScenarios=3`, `successCount=3`, `parserSuccessCount=3`, `referralCount=1`, `citationCount=2`, `singleCitationContractCount=3`, `averageLatencyMs=15812`, and `maxLatencyMs=26272`.
- Native LiteRT-LM function calling manually executed `log_visit` once with the expected `patientId`, `observationText`, `protocolCitation`, and `referralRequired` fields.
- Memory stress passed 10/20/40 compact prior visits with `parserSuccessCount=3/3`.
- RealGemma text UI mode exists behind build-time and app-private local gates.
- `MockGemmaAgent` remains only for deterministic unit fixtures and legacy benchmark scaffolding.
- Tests guard RealGemma-required app wiring, setup-required failure behavior, readiness safety, and repo model-artifact safety.

## Not A Generic Chatbot

Smriti is patient-contextual and workflow-bound. It starts from a local roster, urgent protocol lookup, or community panel, reads local history, retrieves local protocol snippets, generates a structured visit record, requires CHW confirmation, saves locally, creates follow-up tasks when reviewed plans require them, prepares editable patient messages after save, and builds a supervisor summary from confirmed records. Lookup-only activity remains read-only and does not create visits, referral flags, follow-up tasks, or counts.

## Not Gemini Live

Gemini Live can answer general questions when online. Smriti is designed for offline field operation with persistent local patient data, local protocol citations, structured visit/referral records, and end-of-day supervisor reporting. No core runtime feature requires cloud access.

## RealGemma Failure Behavior

The app-facing flow no longer falls back to mock-generated clinical, visit, or supervisor output. Missing model setup, timeout, invalid JSON, rejected citation, or other RealGemma failure keeps the transcript editable, shows retry/setup messaging, and blocks saving until a valid RealGemma result is reviewed and confirmed.

## What Remains Experimental

- Product hardening for Real Gemma `.litertlm` model setup and device performance.
- LiteRT-LM `Engine` creation, initialization, conversation creation, and inference outside the gated local submission path.
- Native Gemma audio/ASR.
- Productized function calling through LiteRT-LM.
- GPU backend benchmarking and broader device performance characterization.
- WER and referral-accuracy studies with clinically reviewed data.
