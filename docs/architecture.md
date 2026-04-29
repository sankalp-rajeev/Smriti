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
-> ProtocolRetriever
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

If either gate is missing, the normal `MockGemmaAgent` visit path is used. If the model is missing or inference fails, RealGemma returns a safe uncertain result and does not save.

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
