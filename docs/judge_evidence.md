# Judge Evidence

This page is the concise evidence ledger for Smriti's current hackathon state. The app-facing reasoning path now requires RealGemma text reasoning; mock output is not shown when RealGemma is unavailable.

For the filmed/live runbook, use `docs/final_demo_checklist.md`.

## Phase D UX Evidence

The recorded-demo UI was simplified for community health workers and low-digital-literacy field use:

- Welcome screen opens first in FinalUi and explains Smriti in plain language before showing the roster.
- Welcome groups actions into `Start`, `Set up patient list`, and `Help & setup`, with `Start visits` as the clear primary action.
- About Smriti uses CHW-focused human framing: Smriti is built for the health worker who shows up even when there is no signal, no EHR, and too many visits to remember alone.
- User Guide gives six short steps: choose patient, speak/type visit, generate note, review carefully, confirm/save, end of day.
- Setup Guidance appears when the model file is absent on first launch and avoids internal runtime terms.
- Patient Roster has local search, UI-only filter chips for `All`, `Needs attention`, `Follow-up due`, `Near term`, and `Routine`, large main actions, smaller setup/support actions, attention/routine sections, empty states, patient status chips, and patient-card `Note language` labels.
- Patient Roster exposes `Urgent protocol lookup`, a local read-only health guidance check over protocol assets.
- Patient Roster exposes `Community panel`, a local caseload view for saved roster counts, follow-ups, urgent review saved, pregnancy stage, languages, countries, and today's focus.
- Status chips are deterministic from local data: Amara shows `Follow-up due`, Fatima shows `History signal`, Grace shows `Routine`, and Meena shows `Referral saved` after a confirmed referral visit.
- Visit screen places missed follow-up and history-signal cards above transcript input, then shows a simple instruction card.
- Visit screen also shows local follow-up task cards when generated or seeded follow-ups are open, with CHW actions to mark done or reschedule.
- Sample transcripts are patient-specific, so Grace never receives Meena's danger-sign sample.
- Loading copy is calm and sequential; generation is disabled while running.
- RealGemma failure shows a retry card and preserves the transcript. It does not display mock output.
- Review screen uses plain cards for `Referral suggested`, `No referral flag`, and `More information needed`, plus a collapsed source section explaining what information was used.
- Summary screen shows priority list, urgent cases, follow-ups, routine visits, `View community panel`, and a plain fallback when on-device summary reasoning is unavailable.
- After confirm/save, Summary can offer `Prepare patient message`; the CHW can review/edit/copy/share the text through Android's share sheet. Smriti does not auto-send messages.
- Urgent Protocol Lookup can also be opened from the Visit screen as `Check urgent guidance`; it does not save data, create referral flags, create follow-up tasks, or change Summary/Community Panel counts.
- Offline Proof uses CHW-facing wording and avoids confusing model/internal status labels. It is available from `Check offline setup` and is not shown by default on the roster.
- Destructive actions are confirmed before import/reset.
- No cloud APIs, runtime downloads, direct Gemma audio, PHI, or invalid-output save path was added. In FinalUi, the paper-note action is `Scan paper note`; the synthetic sample shortcut is hidden. The paper-note scan flow is local Gemma vision data entry only and requires CHW review before save.

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
-> local follow-up task creation if reviewed follow-up text exists
-> patient message/community panel options from saved local state
-> Supervisor Summary from confirmed local records
```

Evidence:

- `AgentConfig.DEFAULT_MODE = AgentMode.REAL_GEMMA_REQUIRED`.
- App-facing visit generation uses `RealGemmaAgent`.
- Supervisor Summary is built from confirmed local records. RealGemma supervisor-priority reasoning remains manual/probe-only and is not part of the filmed app-facing flow.
- Missing model, missing gate, timeout, failed inference, invalid JSON, or citation/safety rejection displays setup/retry messaging and does not save.
- RealGemma has loaded and returned output on the emulator. The most recent observed failure mode was schema adherence: output omitted required `referralFlag`, so the hardened parser rejected it safely.
- When the gated model is present, Smriti can start a background RealGemma preload and keep the shared engine/client warm for subsequent visit-note generations and manual/probe paths. The first RealGemma call may still be slower because model/session initialization is expensive; later calls should avoid repeated cold loads when memory allows.
- Confirm/save is a local Room/SQLite write only. It never invokes RealGemma, never re-runs retrieval, and never auto-exports JSON; the CHW confirm/save gate remains required.
- Follow-up scheduling, patient leave-behind messages, and Community Panel counts are deterministic local logic after save. They do not invoke RealGemma, LiteRT, protocol retrieval, cloud APIs, or mock clinical output.
- Urgent Protocol Lookup is deterministic local `ProtocolRetriever` logic before or during visit documentation. It invokes no RealGemma/LiteRT/cloud path and writes no local records.
- `SmritiLatency` logs timing markers for readiness, preload/init, protocol retrieval, history formatting, prompt build, generation, parser/safety/citation validation, ReviewScreen navigation, local save, and summary refresh without logging transcripts or raw clinical text.
- A native LiteRT-LM crash was observed after overlapping/retried RealGemma calls piled up behind `Conversation.sendMessage(prompt)`. The app now uses one global non-queueing RealGemma inference gate across visit notes, manual/probe supervisor-priority reasoning, paper-note vision extraction, manual test paths, and preload. Busy requests return a friendly wait message instead of entering another native call.
- UI actions disable overlapping generation/scan/save paths. Confirm/save remains local Room/SQLite only and is guarded against double taps.
- Measured emulator/local setup timing evidence from `SmritiLatency`: RealGemma preload/init 1.885 s; Meena RealGemma generation 21.726 s; Meena validation 31 ms; Meena Room save 49 ms; Meena summary refresh 5 ms; Lucia RealGemma generation after preload/reuse 14.434 s; Lucia validation 4 ms; protocol retrieval 1-2 ms; prompt build 1-3 ms. Device performance may vary.
- Timing interpretation: RealGemma inference dominates latency. Local retrieval, prompt build, parser/safety/citation validation, Room save, and summary refresh are negligible by comparison. The second generation was faster after preload/engine reuse.
- CPU is the stable documented text backend. GPU timing is isolated as an explicit experiment via `ManualRealGemmaBackendLatencyInstrumentedTest`; it is not default and should only be used for filming if target-device results are successful, stable, and meaningfully faster.
- Native LiteRT-LM protocol tool-calling is validated as an optional manual evidence path only. `ManualLiteRtProtocolToolCallingInstrumentedTest` registers `lookupProtocol(query, countryCode, region)` backed by local `ProtocolRetriever` and logs under `SmritiProtocolToolCall`. The connected probe passed: Gemma called the tool with `{"countryCode":"IN","query":"severe headache blurred vision high blood pressure pregnancy danger signs","region":"INDIA"}` and returned citation `Smriti Demo Maternal Health Protocol Danger Signs 1.1`. Production visits still use deterministic `ProtocolRetriever` before RealGemma prompting.
- Speculative decoding/MTP was manually benchmarked on CPU through `ManualRealGemmaSpeculativeLatencyInstrumentedTest`: CPU baseline 21787 ms, CPU speculative/MTP 22138 ms, delta +351 ms slower. CPU remains the stable default, and Smriti should not claim speculative decoding speedup.
- The visit prompt now asks for exact JSON only: `summary`, boolean `referralFlag`, `referralReason`, `dangerSigns`, `followUpPlan`, `clarificationQuestion`, `citations`, `confidence`, and `safetyNote`.
- The parser extracts close JSON from markdown fences/surrounding text and accepts safe aliases, but still rejects missing referral equivalents, diagnostic wording, invented/missing referral citations, and prose-only output.
- `MockGemmaAgent` may remain in tests/fixtures only; app screens do not use it for clinical/visit/supervisor output.
- Local patient roster and history use Room/SQLite.
- Local protocol retrieval uses JSON assets with country-aware ranking.
- Generated notes and referral support go to ReviewScreen before saving.
- CHW confirm/save is required before visits or referral flags persist.
- Summary uses CHW-facing wording: `Saved visits on this device`, urgent cases, follow-ups, routine visits, and a collapsed explanation of saved visit notes, patient history, and local health guidance.
- Community Panel uses CHW-facing wording such as `Saved on this device`, `Needs attention`, `Follow-ups`, `No recent visit`, and `Urgent review saved`; it avoids diagnosis, prediction, treatment, and risk-score claims.
- Urgent Protocol Lookup uses CHW-facing wording such as `Urgent protocol lookup`, `Check urgent guidance`, `Local guidance checked`, `Health guidance used`, and `Urgent review may be needed`; it avoids emergency-AI, AI-triage, diagnosis, treatment, dose, and risk-score claims.
- Offline Proof reports local/offline status, RealGemma model status, setup state, Gemma audio transcription validated as manual probe, and vision paper-note scan available.
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
- No vector DB, remote search, or runtime download is used.

Urgent Protocol Lookup uses this same pack directly. Patient-launched lookup passes the selected patient's country/region context; roster-launched lookup uses a `GLOBAL_CORE` fallback. If no local match exists, the UI shows a safe no-guidance state instead of guessing.

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

The 15.8s average latency reflects real on-device Gemma 4 E2B text inference on CPU backend; in the CHW field workflow, this is positioned as local-guidance reasoning support replacing manual paper/protocol lookup, not instant chat.

Latency tuning note: the app now preloads and reuses the RealGemma engine where supported, compacts visit prompts to recent history plus top protocol chunks, and keeps save latency separate from generation latency. The measured emulator/local setup showed preload/init at 1.885 s, a first Meena generation at 21.726 s, and a later Lucia generation at 14.434 s after preload/reuse. This is performance evidence only, not clinical validation.

Current schema-hardening note: if manual RealGemma output still fails the strict parser, the app treats that as a safe rejection, preserves the transcript for retry, logs raw output only in debug/dev Logcat under `SmritiRealGemma`, and does not fall back to mock clinical output.

## Phase C Multilingual Evidence

- Selected generated note languages: English, Hindi, Spanish, Swahili. Full app UI translation is not claimed.
- Smriti demonstrates selected patient-specific local-language output in those four languages after manual validation.
- Full app UI translation is not claimed. Existing patient `preferredLanguage` is not overwritten by any default/new-patient language setting.
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

Gemma audio transcription validated through LiteRT-LM 0.11.0 manual probe and wired into the app-facing Visit screen. `Conversation.sendMessage(Contents.of(Content.Text(prompt), Content.AudioBytes(audioBytes)))` with `EngineConfig.audioBackend = Backend.CPU()` succeeded on-device. Gemma audio fills an editable transcript only. Clinical note generation still requires CHW review and a separate Generate action. No audio-only save path. No direct audio diagnosis, treatment, or referral.

## Paper-Note Vision Evidence

Local Gemma 4 vision is claimed only for synthetic paper-note data extraction. Paper-note scan is CHW-reviewed data-entry support only, not clinical image diagnosis.

- The `litertlm-android-0.11.0` AAR/classes.jar surface was checked.
- Found image API holders: `Content.ImageBytes`, `Content.ImageFile`, and `InputData.Image`.
- Found related `EngineConfig` fields: `visionBackend` and `maxNumImages`.
- Found transport methods that can carry multimodal-looking inputs: `Conversation.sendMessage(Contents)` and `Session.generateContent(List<InputData>)`.
- No public prompt-template, media-placeholder, multimodal-template, image-preprocessor, or `preprocess(...)` API was found in the AAR inspection.
- `ManualRealGemmaVisionProbeInstrumentedTest` passed on emulator with a sideloaded app-private model.
- The engine accepted `Conversation` image input.
- Local Gemma 4 vision extracted structured JSON from the synthetic paper note: Grace Achieng, 02 May 2026, BP 116/74, symptoms, routine ANC follow-up, confidence HIGH, and `needsReview=true`.
- The app flow now supports `Scan paper note`, Review Scanned Note, explicit patient link/current-patient confirmation, and local save to history with `source=paper_scan`. Any synthetic sample shortcut is kept outside FinalUi/dev-only filming.
- The image bytes are not persisted.
- The scan flow does not call visit-note referral generation or supervisor priority reasoning.

Do not claim clinical image diagnosis, referral decisions from image alone, real patient image support, or cloud OCR.

Gemma audio transcription validated as manual probe. Gemma audio fills an editable transcript only. Clinical note generation still requires CHW review and a separate Generate action. The vision path is separate from audio.

## Not Claimed

Smriti does not claim:

- clinical validation,
- autonomous diagnosis,
- autonomous treatment,
- direct audio diagnosis, treatment, or referral,
- clinical image diagnosis or referral from paper-note image alone,
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
