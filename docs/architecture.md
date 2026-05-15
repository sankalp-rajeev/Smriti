# Architecture

This document shows the current RealGemma-required app architecture.

## RealGemma-Required UI Flow

```text
MainActivity / SmritiApp
-> PatientListScreen
-> VisitScreen
-> editable transcript
-> Generate Local Visit Note
-> VisitReasoningPipeline
-> ProtocolRetriever with ProtocolRetrievalContext
-> RealGemmaAgent
-> ReviewScreen
-> CHW confirm/save
-> LocalVisitMemoryStore
-> SummaryScreen
```

Important boundaries:

- `AgentConfig.DEFAULT_MODE = AgentMode.REAL_GEMMA_REQUIRED`.
- App-facing visit-note reasoning uses `RealGemmaAgent` when local setup is complete. Supervisor Summary is built from confirmed local records. RealGemma supervisor-priority reasoning remains manual/probe-only and is not part of the filmed app-facing flow.
- If the submission build flag, app-private sentinel, or app-private model is missing, the app shows setup/retry messaging instead of mock clinical output.
- Offline Proof shows RealGemma setup/readiness status.
- The normal Meena demo uses `countryCode=IN` and `region=INDIA`, with `GLOBAL_CORE` fallback.

## Manual RealGemma Instrumentation Flow

```text
developer-run androidTest
-> explicit instrumentation argument
-> app-private sideloaded .litertlm model
-> ModelAvailability
-> LiteRtEngineConfigFactory
-> LiteRtGemmaTextClient.generateTextManual(...)
-> RealGemmaAgent or direct prompt harness
-> RealGemmaOutputParser
-> RealGemmaSafetyPostProcessor
-> logged result
```

Manual tests do not write to Room and are not called by app startup.

## RealGemma Text UI Flow

```text
debug build with -Psmriti.realGemmaSubmissionMode=true
-> app-private files/dev/enable_real_gemma_text_mode
-> app-private .litertlm model status check
-> VisitScreen shows model/inference status
-> VisitReasoningPipeline
-> RealGemmaAgent
-> LiteRT text client
-> LiteRtGemmaTextClient.generateTextManual(...)
-> ReviewScreen
-> CHW confirm/save gate
```

If any required gate or model file is missing, generation is blocked with clear setup/retry messaging. If inference fails or output is rejected, the transcript remains editable and nothing is saved automatically.

## Transcript And Audio Flow

```text
manual/sample transcript
-> editable transcript field
-> VisitReasoningPipeline
```

```text
Try Offline Speech
-> AndroidOfflineSpeechRecognizerClient
-> on-device recognizer when available
-> system recognizer with EXTRA_PREFER_OFFLINE otherwise
-> language fallback: en-IN, en-US, en
-> success fills editable transcript
-> failure shows friendly message and preserves existing transcript
```

```text
Gemma audio transcription
-> local microphone audio recording
-> short 16 kHz mono PCM capture wrapped as in-memory WAV bytes
-> Conversation.sendMessage(Contents.of(Content.Text(prompt), Content.AudioBytes(audioBytes)))
-> EngineConfig.audioBackend = Backend.CPU()
-> Gemma transcription fills editable transcript
-> CHW reviews/edits transcript before generating note
```

```text
recorded .m4a voice note
-> app-private local file metadata
-> stored only after CHW confirmation
```

Gemma audio transcription is wired into the Visit screen only when RealGemma submission readiness is active. Gemma audio fills an editable transcript only. Clinical note generation still requires CHW review and a separate Generate action. No audio-only save path. No direct audio diagnosis, treatment, or referral.

## Protocol Retrieval Flow

```text
editable transcript
-> ProtocolRetrievalContext(countryCode, region)
-> local JSON Global Protocol Pack v1
-> keyword score
-> location rank: exact country, then region, then GLOBAL_CORE
-> supplied protocol chunks for GemmaAgent
```

The corpus is local JSON, not cloud RAG or a vector database. It currently includes global, India, Bangladesh, Ethiopia, Africa-region, and South-America-region maternal/ANC referral-support chunks. Recommendations must be grounded in retrieved protocol chunks or returned as uncertain.

## Urgent Protocol Lookup Flow

```text
PatientListScreen or VisitScreen
-> UrgentProtocolLookupScreen
-> danger-sign chips and optional observation text
-> ProtocolRetrievalContext(patient country/region or GLOBAL_CORE)
-> ProtocolRetriever local JSON lookup
-> guidance card with citation or no-guidance fallback
```

This flow is read-only. It does not call RealGemma, LiteRT, supervisor reasoning, Android share intents, or cloud APIs. It does not write to Room and does not create visits, referral flags, follow-up tasks, patient messages, or community-panel counts. If no local protocol chunk matches, Smriti tells the CHW to document the observation and contact a supervisor/health facility according to local practice.

## Local Storage Flow

```text
AppDatabase
-> PatientDao
-> VisitLogDao
-> ReferralFlagDao
-> FollowUpTaskDao
-> ProtocolChunkDao
```

`LocalVisitMemoryStore` owns seed demo data, confirmed visit saves, confirmed referral saves, snapshot refresh, reset demo data, and patient history filtering.

Only Review confirm/save writes generated visit data. Urgent protocol lookup is outside the save path and is intentionally non-persistent.

## Safety And Citation Enforcement Flow

```text
RealGemmaPromptBuilder
-> LiteRT text generation
-> RealGemmaOutputParser
-> ProtocolCitationValidator
-> RealGemmaSafetyPostProcessor
-> VisitReasoningResult or visible unavailable/retry state
```

Safety rules:

- The app is not diagnostic AI.
- Recommendations must be cited or uncertain.
- Diagnostic claims are rejected in RealGemma parsing.
- CHW confirmation is required before saving.
- No cloud API is part of core runtime.
