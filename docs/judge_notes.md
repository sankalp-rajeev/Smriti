# Judge Notes

## Health And Sciences Fit

Smriti addresses a real last-mile health workflow: community health workers document visits on paper, often without internet, EHR access, or immediate clinical backup. The demo focuses on maternal-health ANC visits, longitudinal patient memory, protocol-grounded referral support, and end-of-day supervisor reporting.

## LiteRT Track Fit

The app is structured for local Gemma 4 through LiteRT-LM behind a replaceable `GemmaAgent` interface. The LiteRT-LM dependency is pinned and readiness is visible in-app. Real model loading and inference are disabled by default; developer-only text mode requires explicit local gates and a sideloaded app-private model. The normal judge demo remains safe, mock-backed, and fully offline.

## Impact And Vision

- CHWs serve large patient panels in low-connectivity environments.
- Maternal danger signs require timely referral support.
- Local visit history reduces reliance on memory and paper.
- Protocol citations make recommendations reviewable by CHWs and supervisors.
- The phone becomes a local patient-memory and documentation aid, not a cloud chatbot.

## Video Pitch Points

- Open with the field problem: no signal, no EHR, paper records, high-stakes maternal visits.
- Show airplane mode before launching.
- Show Meena's prior history before generating a new visit note.
- Use the danger-sign transcript to trigger referral support.
- Show the protocol citation and CHW confirmation step.
- End with the supervisor summary and Offline Proof.

## Technical Depth Points

- Android native app with Kotlin, Jetpack Compose, Room/SQLite, local assets, Android TTS, and local JSON export.
- `GemmaAgent` abstraction separates reasoning from UI.
- `MockGemmaAgent` provides deterministic offline demo behavior.
- Real Gemma 4 LiteRT-LM text inference has been validated on-device through manual instrumentation with a sideloaded app-private model.
- `ManualLiteRtTextInferenceInstrumentedTest` returned `SMRITI_LITERT_OK`.
- The full RealGemmaAgent path has been validated: `RealGemmaPromptBuilder -> LiteRT sendMessage -> RealGemmaOutputParser -> RealGemmaSafetyPostProcessor -> VisitReasoningResult`.
- Accepted manual RealGemma benchmark: `totalScenarios=3`, `successCount=3`, `parserSuccessCount=3`, `referralCount=1`, `citationCount=2`, `singleCitationContractCount=3`, `averageLatencyMs=15812`, and `maxLatencyMs=26272`.
- Native LiteRT-LM function calling manually executed `log_visit` once with the expected `patientId`, `observationText`, `protocolCitation`, and `referralRequired` fields.
- Memory stress passed 10/20/40 compact prior visits with `parserSuccessCount=3/3`.
- Developer-only RealGemma text UI mode exists behind build-time and app-private local gates.
- `MockGemmaAgent` remains default for the stable submission demo.
- Tests guard default mock mode, RealGemma developer gating, readiness safety, and repo model-artifact safety.

## Not A Generic Chatbot

Smriti is patient-contextual and workflow-bound. It starts from a selected patient, reads local history, retrieves local protocol snippets, generates a structured visit record, requires CHW confirmation, saves locally, and builds a supervisor summary from confirmed records.

## Not Gemini Live

Gemini Live can answer general questions when online. Smriti is designed for offline field operation with persistent local patient data, local protocol citations, structured visit/referral records, and end-of-day supervisor reporting. No core runtime feature requires cloud access.

## Why The Mock Fallback Is Demo-Safe

`MockGemmaAgent` is deterministic, offline, and easy to inspect. It lets judges verify the product workflow, safety constraints, review/confirm requirement, and local data model while RealGemma remains developer-only and gated. This avoids making the filmed submission depend on sideloaded model setup or variable device performance.

## What Remains Experimental

- Real Gemma `.litertlm` model loading in the default submission demo path.
- LiteRT-LM `Engine` creation, initialization, conversation creation, and inference outside manual/developer-gated paths.
- Native Gemma audio/ASR.
- Productized function calling through LiteRT-LM.
- GPU backend benchmarking and broader device performance characterization.
- WER and referral-accuracy studies with clinically reviewed data.
