# Architecture

This document shows the current architecture at the end of Phase 2.

## Normal Mock UI Flow

```text
MainActivity / SmritiApp
-> PatientListScreen
-> VisitScreen
-> editable transcript
-> Generate Local Visit Note
-> VisitReasoningPipeline
-> ProtocolRetriever with ProtocolRetrievalContext
-> MockGemmaAgent
-> ReviewScreen
-> CHW confirm/save
-> LocalVisitMemoryStore
-> SummaryScreen
```

Important boundaries:

- `AgentConfig.DEFAULT_MODE = AgentMode.MOCK`.
- `GemmaAgentFactory.create()` returns `MockGemmaAgent` by default.
- RealGemma text is exposed only through developer-only mode when both gates are enabled.
- Offline Proof shows readiness/status but does not enable inference.
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

Manual tests do not write to Room and are not called by app startup or visible screens.

## Developer-Only RealGemma Text UI Flow

```text
debug build with -Psmriti.realGemmaDevMode=true
-> app-private files/dev/enable_real_gemma_text_mode
-> app-private .litertlm model status check
-> VisitScreen shows developer warning and model/inference status
-> VisitReasoningPipeline
-> RealGemmaAgent
-> RealGemmaDeveloperTextClient
-> LiteRtGemmaTextClient.generateTextManual(...)
-> ReviewScreen
-> CHW confirm/save gate
```

If either gate is missing, the normal `MockGemmaAgent` visit path is used. If both gates are enabled but the model is missing or inference fails, RealGemma returns a safe uncertain result and does not save.

## Transcript And Audio Fallback Flow

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
recorded .m4a voice note
-> app-private local file metadata
-> stored only after CHW confirmation
```

Direct Gemma 4 audio is not wired into this flow.

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

## Local Storage Flow

```text
AppDatabase
-> PatientDao
-> VisitLogDao
-> ReferralFlagDao
-> ProtocolChunkDao
```

`LocalVisitMemoryStore` owns:

- seed demo data,
- save confirmed visit,
- save confirmed referral flag,
- refresh snapshot,
- reset demo data,
- patient history filtering.

Only Review confirm/save writes generated visit data.

## Safety And Citation Enforcement Flow

Normal mock path:

```text
ProtocolRetriever
-> MockGemmaAgent
-> cited VisitReasoningResult
-> ReviewScreen CHW confirmation
```

Manual RealGemma path:

```text
RealGemmaPromptBuilder
-> LiteRT text generation
-> RealGemmaOutputParser
-> ProtocolCitationValidator
-> RealGemmaSafetyPostProcessor
-> VisitReasoningResult or safe uncertain fallback
```

Safety rules:

- The app is not diagnostic AI.
- Recommendations must be cited or uncertain.
- Diagnostic claims are rejected in RealGemma parsing.
- CHW confirmation is required before saving.
- No cloud API is part of core runtime.
