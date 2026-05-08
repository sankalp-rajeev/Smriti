# Smriti Final Build Plan

**Purpose:** This is the phase-by-phase execution plan for the final Smriti Android push before the Gemma 4 Good submission.

**Project north star:** Smriti is for the ones who show up — the field health workers who carry care from home to home. The app should help them remember every visit, close every follow-up loop, leave patients with clear instructions, and manage a local village panel without cloud dependency.

---

## 0. Non-Negotiable Boundaries

Preserve these constraints through every phase:

- Smriti is CHW-first, phone-first, field-first.
- Starting scope remains maternal health / ANC visits.
- Smriti is not diagnostic AI.
- Output is protocol-grounded documentation and referral support only.
- CHW review/edit/confirm is required before saving.
- No fake/mock clinical output if RealGemma fails.
- Fully offline-first after setup.
- No Firebase, OpenAI, Gemini Cloud, Supabase, cloud RAG, cloud ASR, or runtime model downloads.
- No model bundled in APK.
- Model remains sideloaded into app-private storage.
- Paper-note/register image bytes must not be persisted.
- Vision stays limited to paper-note/register data extraction, not diagnosis.
- Direct Gemma audio is not claimed unless a fresh manual LiteRT-LM audio harness passes on the target device.
- CPU remains the stable default unless GPU/MTP is proven stable through repeated target-device tests.
- MockGemmaAgent remains only in tests/fixtures.
- If RealGemma setup, inference, parsing, citation validation, or safety validation fails, preserve the transcript, show retry/setup messaging, do not save, and do not show mock output.

---

## 1. Final Priority Stack

### P0 — Safety

1. Referral-language inconsistency parser fix.

### P1 — Product impact

2. Follow-up scheduling.
3. Patient leave-behind share card.
4. Community panel / village panel view.
5. Urgent Protocol Lookup.

### P2 — Gemma technical showpieces

6. Native Gemma audio to editable transcript, only if validated.
7. MTP/speculative decoding benchmark, only if validated.
8. Tool-calling protocol lookup trace, only if validated.

### Avoid before submission

9. Medication dosing.
10. Photo-based patient identification.
11. Open-ended clinical mentor chatbot.
12. Direct audio to clinical JSON.
13. Any feature that bypasses CHW review or weakens citation validation.

---

## 2. Phase 1 — Safety Hardening

### Goal

Fix the known risk where RealGemma output may contain referral-like or urgent language while `referralFlag=false`, causing Summary to show no urgent/referral case.

### Required behavior

If `referralFlag=false`, scan all user-facing fields for referral/urgent/escalation language:

- `summary`
- `referralReason`
- `dangerSigns`
- `followUpPlan`
- `clarificationQuestion`
- `safetyNote`
- any other parsed user-facing string fields

Reject inconsistent output safely. Do not convert `referralFlag=false` to `true` automatically.

If `referralFlag=true`, preserve the existing strict rule: a valid supplied protocol citation is required.

Do not weaken `ProtocolCitationValidator`.

### Multilingual phrase coverage

Start with these patterns and keep the implementation easy to extend:

**English**

- referral
- refer
- urgent
- emergency
- immediate review
- facility
- hospital
- danger sign
- escalate

**Hindi**

- रेफरल
- भेजें
- तुरंत
- आपात
- अस्पताल
- स्वास्थ्य केंद्र
- खतरे के संकेत
- तत्काल समीक्षा

**Spanish**

- derivación
- referir
- urgente
- emergencia
- hospital
- centro de salud
- signos de alarma
- revisión inmediata

**Swahili**

- rufaa
- haraka
- dharura
- hospitali
- kituo cha afya
- dalili za hatari
- mapitio ya haraka

### Likely files

Ask Codex/Cursor to inspect first, then patch the right files:

- `RealGemmaOutputParser`
- `ProtocolCitationValidator`
- `RealGemmaSafetyPostProcessor`
- `VisitReasoningResult`
- `SupervisorSummaryFormatter`
- `SummaryScreen`
- existing parser and safety tests

### Tests to add

- English `referralFlag=false` + “urgent referral” text is rejected.
- Hindi `referralFlag=false` + referral-like text is rejected.
- Spanish `referralFlag=false` + urgent/hospital text is rejected.
- Swahili `referralFlag=false` + `rufaa` / `dharura` text is rejected.
- Routine no-referral output remains accepted.
- `referralFlag=true` without citation remains rejected.
- `referralFlag=true` with valid supplied citation remains accepted.
- Summary does not bury referral-like text under routine follow-up.

### Validation

Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Then run the final UI path:

```powershell
.\runSmriti.ps1 -FinalUi
.\runSmriti.ps1 -Logs
```

Confirm no `liblitertlm_jni.so` fatal crash.

### Commit

```powershell
git add .
git commit -m "Harden RealGemma referral consistency validation"
git push
```

---

## 3. Phase 2 — Follow-Up Scheduling

### Goal

Turn Smriti’s follow-up support into an actual loop-closing workflow. The CHW should leave a visit knowing what needs to happen next, and the roster should surface due/overdue follow-ups automatically.

### Product story

Follow-up scheduling answers: **“What does the CHW do tomorrow?”**

This directly supports the brand pillar: **Close every loop.**

### Feature shape

```text
After CHW confirms/saves a visit
-> detect follow-up need from generated followUpPlan or CHW-entered follow-up action
-> create a local follow-up task
-> show Due / Upcoming / Overdue on roster
-> allow Mark Done / Reschedule
```

### Requirements

- Follow-up task creation happens only after CHW confirm/save.
- Follow-up tasks are local Room/SQLite records.
- Follow-up tasks must not require RealGemma to display.
- If RealGemma produces a follow-up date/reason, CHW can edit before save.
- If no date is generated, allow CHW to choose a simple date: tomorrow, 3 days, 1 week, 2 weeks, custom.
- The roster must prioritize overdue follow-ups above routine patients.
- Existing Amara missed-follow-up behavior must continue working.
- Summary should show follow-ups due today/overdue.
- Follow-up task completion must not create a generated clinical note by itself.

### Suggested model/data additions

Possible Room entity:

```kotlin
data class FollowUpTaskEntity(
    val id: String,
    val patientId: String,
    val createdFromVisitId: String?,
    val dueDateMillis: Long,
    val reason: String,
    val language: String,
    val status: FollowUpStatus, // UPCOMING, DUE, OVERDUE, COMPLETED, RESCHEDULED
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null
)
```

### UI additions

- ReviewScreen: optional “Follow-up to schedule” card before confirm/save.
- Patient Roster: chips such as `Follow-up due`, `Overdue follow-up`, `Upcoming follow-up`.
- Patient Detail / VisitScreen: local follow-up task card.
- SummaryScreen: follow-up list section.

### Tests

- Confirm/save with follow-up creates one local follow-up task.
- Double save does not create duplicate tasks.
- Reset demo data clears generated tasks but restores seeded demo history.
- Roster sorts overdue follow-ups above routine patients.
- Mark Done updates task status locally.
- Reschedule updates due date without creating duplicate patient history.
- No RealGemma call is triggered by Mark Done or Reschedule.

### Validation

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\runSmriti.ps1 -FinalUi
.\runSmriti.ps1 -Logs
```

### Commit

```powershell
git add .
git commit -m "Add local follow-up scheduling workflow"
git push
```

---

## 4. Phase 3 — Patient Leave-Behind Share Card

### Goal

Give the patient something understandable after the CHW visit: a CHW-reviewed, plain-language message in the patient’s note language that can be shown to family or clinic staff.

### Product story

Patient leave-behind answers: **“What does the mother get after the visit?”**

This is one of the strongest human-impact moments for the demo.

### Feature shape

```text
After CHW confirms/saves a visit
-> create patient-friendly message
-> language follows patient preferredLanguage
-> CHW reviews/edits the message
-> share via Android share sheet / WhatsApp / SMS when network is available
```

### Safety requirements

- No diagnosis.
- No treatment instructions beyond safe referral/follow-up wording already in the reviewed note.
- No automatic sending.
- CHW must review/edit before sharing.
- Sharing is user-initiated through Android share sheet.
- Generation remains local/offline.
- If RealGemma is unavailable, offer deterministic template from confirmed saved visit fields.
- Do not include raw protocol paragraphs.
- Do not include hidden metadata, local file paths, model paths, or internal logs.

### Example output

```text
Meena, today your health worker noted headache, blurred vision, and high blood pressure.
Please visit the health center for review as advised.
Show this message to the clinic staff.
This is not a diagnosis. A health worker must confirm all details.
```

### Suggested UI

- SummaryScreen or post-save screen: `Prepare patient message`.
- LeaveBehindScreen:
  - title: `Message for patient`
  - editable text block
  - safety note
  - buttons: `Copy`, `Share`, `Back`

### Tests

- Leave-behind is generated only from confirmed saved visit data.
- Leave-behind text is editable before share/copy.
- Share action uses Android intent and does not require app-owned cloud code.
- Hindi/Spanish/Swahili/English safety wording is present.
- Referral case produces urgent review wording without diagnosis.
- Routine case does not create false urgency.
- No raw citation paragraphs are included.

### Validation

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\runSmriti.ps1 -FinalUi
.\runSmriti.ps1 -Logs
```

### Commit

```powershell
git add .
git commit -m "Add CHW-reviewed patient leave-behind messages"
git push
```

---

## 5. Phase 4 — Community Panel / Village Panel View

### Goal

Make Smriti feel like a local village-memory system, not only a single-patient note app.

### Product story

Community panel answers: **“How does a CHW manage a whole village of mothers?”**

This should become a major judge-facing product moment.

### Feature shape

A deterministic Room-powered panel view:

```text
Pregnant patients: 40
Third trimester: 12
Follow-ups due: 7
Urgent reviews saved: 2
No visit in 30 days: 5
Languages covered: Hindi, Swahili, Spanish, English
```

Optional local RealGemma narrative only if gates are active and safe:

```text
Today’s panel needs attention on 7 follow-ups and 2 urgent reviews.
Prioritize Meena and Amara before routine ANC visits.
```

### Requirements

- Base counts must be deterministic Room queries.
- Panel must work with RealGemma unavailable.
- No clinical prediction.
- No diagnosis.
- No fake impact metrics.
- No cloud dashboard.
- No real patient data.
- Works with six seeded demo patients and scales conceptually to imported register patients.

### Suggested UI

- Roster action: `Village panel` or `Panel view`.
- Cards:
  - `Needs attention`
  - `Follow-ups due`
  - `Urgent reviews saved`
  - `Third trimester`
  - `No recent visit`
  - `Languages`
- Optional `Supervisor note` generated from local counts, not raw patient-sensitive text.

### Tests

- Counts match seeded demo data.
- Counts update after confirmed save.
- Counts update after follow-up Mark Done.
- Panel does not require RealGemma.
- Optional Gemma narrative failure falls back to deterministic counts.
- Reset restores expected panel values.

### Validation

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\runSmriti.ps1 -FinalUi
.\runSmriti.ps1 -Logs
```

### Commit

```powershell
git add .
git commit -m "Add local community panel view"
git push
```

---

## 6. Phase 5 — Urgent Protocol Lookup

### Goal

Give CHWs a fast safe path for urgent danger-sign guidance without turning Smriti into an emergency diagnosis chatbot.

### Product story

Urgent Protocol Lookup answers: **“What local guidance should the CHW check immediately?”**

This is a safe, buildable alternative to open-ended emergency triage.

### Naming

Use:

- `Urgent Protocol Lookup`
- `Check urgent guidance`
- `Danger sign guidance`

Do not use:

- `Emergency AI Triage`
- `What should I do right now?`
- `AI emergency assistant`
- `Diagnosis now`

### Feature shape

```text
Roster action: Urgent Protocol Lookup
-> CHW chooses or types danger sign
-> Smriti retrieves local protocol card
-> optional RealGemma simplification into CHW-friendly wording
-> citation required
-> no diagnosis
-> no treatment improvisation
-> optional: attach to selected patient only after CHW confirmation
```

### Suggested danger-sign options

- Severe headache + blurred vision
- Bleeding
- Convulsions
- Reduced fetal movement
- High BP
- Fever
- Severe abdominal pain
- Other / type observation

### Safe output shape

```text
Urgent review may be needed.

Observed danger signs:
- Severe headache
- Blurred vision
- High blood pressure

Local guidance:
Pregnancy danger signs should be referred for urgent clinical review.

What the CHW can do:
1. Do not delay referral.
2. Contact supervisor or nearest facility if available.
3. Document the observation.
4. Ask the patient/family to seek clinical review.

Health guidance used:
WHO ANC Recommendation B1.2

This is not a diagnosis. CHW confirmation is required.
```

### Requirements

- Use local ProtocolRetriever first.
- Citation required for urgent guidance.
- If no relevant protocol chunk is found, show “No matching local guidance found. Contact supervisor or facility.”
- Do not generate medication or dosage advice.
- Do not save anything automatically.
- If attaching to a patient record, require selected patient + CHW confirmation.
- Works offline.
- RealGemma failure falls back to deterministic protocol card, not mock clinical generation.

### Tests

- Each preset option retrieves a local protocol chunk or safe no-match fallback.
- Citation appears for matched urgent guidance.
- No diagnosis words appear.
- No medication dosage appears.
- No save occurs without CHW confirmation.
- RealGemma unavailable still shows deterministic protocol card.
- Multilingual display uses safe fixed labels or reviewed language resources.

### Validation

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\runSmriti.ps1 -FinalUi
.\runSmriti.ps1 -Logs
```

### Commit

```powershell
git add .
git commit -m "Add urgent protocol lookup flow"
git push
```

---

## 7. Phase 6 — Impact Estimate Line

### Goal

Give judges one clear workflow number without pretending clinical validation.

### Feature shape

Add a small deterministic line in Summary or Community Panel:

```text
Workflow estimate: 3 confirmed visits, 2 follow-ups due, 1 urgent review surfaced, about 15–25 minutes of manual lookup avoided.
```

### Safety wording

Use:

- `Workflow estimate`
- `Estimated manual lookup time avoided`
- `Based on local app activity`

Do not use:

- `lives saved`
- `mortality reduced`
- `clinical outcome improved`
- `validated impact`

### Requirements

- Deterministic calculation only.
- No clinical claims.
- Explain assumptions in a collapsed info card.
- Works without RealGemma.

### Tests

- Estimate changes with saved visits/follow-ups/referrals.
- Reset clears generated estimates.
- No clinical outcome wording appears.

### Commit

```powershell
git add .
git commit -m "Add local workflow impact estimate"
git push
```

---

## 8. Phase 7 — LiteRT Capability Probes Branch

### Goal

Explore new Gemma 4 / LiteRT-LM capabilities without risking the stable filmed app.

### Branch

```powershell
git checkout -b feature/litert-capability-probes
```

### Probe 1 — Native Gemma Audio to Editable Transcript

Official current docs show `Content.AudioBytes(audioBytes)` and `audioBackend`. This reopens the blocked-audio question, but do not claim it until the app-specific harness passes.

#### Safe target architecture

```text
recorded audio
-> decode to required ByteArray format
-> Content.AudioBytes
-> prompt: "Transcribe this speech. Output only the transcript."
-> editable transcript field
-> existing VisitReasoningPipeline
-> existing ReviewScreen
```

Do not build:

```text
audio -> clinical JSON -> save
```

#### Build first

- `ManualRealGemmaAudioTranscriptInstrumentedTest`
- manually sideloaded tiny audio sample
- no Room writes
- logs transcript only in dev logs
- skips safely if API/model/backend fails

#### Merge condition

Only merge if:

- audio harness passes on target device/emulator,
- output transcript is usable,
- no native crash after repeated runs,
- existing text/vision paths remain stable,
- claim wording is updated carefully.

### Probe 2 — MTP / Speculative Decoding

Current LiteRT-LM docs expose `ExperimentalFlags.enableSpeculativeDecoding = true`; LiteRT-LM v0.11.0 adds Gemma 4 MTP support.

#### Build first

- keep CPU baseline,
- add opt-in MTP path,
- add GPU + MTP only under explicit manual flag,
- update or re-download model only if required for speculative decoding,
- run repeated Meena/Lucia/Grace scenarios.

#### Merge condition

Only use in filmed build if:

- repeated target-device runs pass,
- no `liblitertlm_jni.so` crash,
- latency meaningfully improves,
- CPU fallback remains intact,
- Offline Proof and docs say it is validated on the target setup.

### Probe 3 — Native Tool-Calling Protocol Lookup Trace

Current LiteRT-LM Android docs expose Kotlin `ToolSet`, `@Tool`, `@ToolParam`, and OpenAPI tools. Smriti already has a manual function-calling probe; now test a protocol lookup tool.

#### Safe target architecture

Keep deterministic ProtocolRetriever mandatory. Add tool-calling as optional trace/proof:

```text
ProtocolRetriever still runs before RealGemma
+ optional lookupProtocol tool available to RealGemma
+ ReviewScreen can show: "Gemma checked local protocol lookup"
```

Do not allow the model to skip mandatory retrieval.

#### Merge condition

Only merge if:

- tool calls are reliable,
- retrieval stays local,
- mandatory citation validation remains strict,
- UI shows trace without confusing CHWs.

### Probe 4 — Constrained Decoding Check

Framework-level constrained decoding exists in LiteRT-LM docs, but confirm whether Kotlin Android exposes a usable JSON schema API in the current AAR.

Do not replace parser/safety validation. Even if JSON structure is constrained, still keep:

- referral-language consistency validation,
- diagnostic-language rejection,
- citation validation,
- CHW review/confirm/save.

---

## 9. Final Filmed Demo Flow After Product Additions

A strong 3-minute edited flow after P1 features:

1. Airplane mode on.
2. Welcome: `Smriti — For the ones who show up.`
3. Check offline setup.
4. Open Community Panel: show local village memory.
5. Show follow-ups due / urgent reviews / no recent visits.
6. Open Amara: missed follow-up.
7. Open Fatima: rising BP history signal.
8. Open Meena: generate Hindi RealGemma danger-sign note.
9. Review referral support + citation + CHW confirm/save.
10. Show follow-up task created.
11. Generate/edit patient leave-behind message.
12. Share/copy message via Android share sheet.
13. Show Urgent Protocol Lookup card.
14. Show Grace paper-note scan if still stable.
15. End on Summary + Offline Proof.

Close line:

```text
Smriti helps a health worker remember every mother, close every follow-up, leave the patient with clear instructions, manage the whole village panel, and access urgent local protocol guidance offline.
```

If audio probe passes, add one short line:

```text
On this target setup, Gemma 4 also transcribes the CHW’s voice locally into an editable visit transcript.
```

If it does not pass, keep:

```text
Direct Gemma audio is not used in this build; Smriti uses offline speech when available or an editable transcript.
```

---

## 10. Final Claim Boundaries

### Say

- Smriti is an offline CHW visit copilot.
- Smriti remembers local patient history and follow-up loops.
- Smriti uses local protocol guidance and CHW confirmation.
- Smriti demonstrates on-device Gemma text reasoning and narrow paper-note vision data extraction.
- Patient messages are CHW-reviewed before sharing.
- Community panel counts are local Room/SQLite data.
- Urgent Protocol Lookup is local guidance lookup, not diagnosis.
- Native audio / MTP / tool-calling are claimed only if validated on target setup.

### Do not say

- Clinical validation.
- Autonomous diagnosis.
- Autonomous treatment.
- Medication dosing.
- Direct Gemma audio works unless the fresh harness passes.
- Broad all-language support.
- Broad camera diagnosis.
- Face recognition or patient identification from photos.
- Cloud runtime.
- Mock output as RealGemma.
- Lives saved / mortality reduced.

---

## 11. Phase-by-Phase Cursor / Codex Master Prompt

Use this as the main prompt in Cursor/Codex. Run one phase at a time. Do not ask it to implement all phases in one pass.

```text
You are continuing the Smriti Android project as a senior Kotlin/Android + LiteRT-LM engineer.

Your job is to implement the final build plan phase by phase. Do not jump ahead. Do not weaken safety. Do not introduce cloud services, runtime model downloads, mock clinical fallbacks, medication dosing, photo-based patient identification, open-ended clinical chat, or direct audio-to-clinical-JSON.

Project context:
Smriti is an offline maternal-health visit copilot for CHWs/ASHAs. It is a Kotlin + Jetpack Compose Android app using Room/SQLite, local JSON protocol retrieval, and LiteRT-LM Gemma 4 E2B for on-device reasoning. It is CHW-first, phone-first, field-first. It is not diagnostic AI. It provides protocol-grounded documentation and referral support only. CHW review/edit/confirm is required before saving.

Architecture to preserve:
PatientListScreen -> VisitScreen -> editable transcript/manual input/offline speech fallback -> VisitReasoningPipeline -> ProtocolRetriever local JSON -> RealGemmaAgent / LiteRT-LM Gemma text inference -> ReviewScreen -> CHW confirm/save -> LocalVisitMemoryStore / Room -> SummaryScreen.

Core constraints:
- Fully offline-first after setup.
- No cloud APIs.
- No Firebase/OpenAI/Gemini Cloud/Supabase.
- No runtime model downloads.
- No model bundled in APK.
- Model is sideloaded manually into app-private storage.
- RealGemma is required for app-facing visit reasoning.
- MockGemmaAgent can remain only in tests/fixtures.
- If RealGemma setup/inference/parser/citation validation fails, show retry/setup messaging, preserve transcript, do not save, and do not show mock output.
- Vision is limited to paper-note/register data extraction only, not diagnosis.
- Paper-note/register image bytes must not be persisted.
- Direct Gemma audio is not claimed unless a new manual harness passes.
- CPU remains the stable default unless GPU/MTP is proven through repeated target-device tests.

Implementation order:
P0 Safety:
1. Referral-language inconsistency parser fix.

P1 Product impact:
2. Follow-up scheduling.
3. Patient leave-behind share card.
4. Community panel / village panel view.
5. Urgent Protocol Lookup.
6. Workflow impact estimate line.

P2 Isolated LiteRT capability probes on a separate branch:
7. Native Gemma audio -> editable transcript only.
8. MTP/speculative decoding benchmark.
9. Optional tool-calling ProtocolRetriever trace.
10. Constrained decoding API check only; do not remove existing validators.

For every phase:
- Inspect existing files before editing.
- Prefer small cohesive changes.
- Add or update unit tests.
- Preserve citation validation.
- Preserve CHW review/confirm/save.
- Preserve local-only data flow.
- Do not use real patient data.
- Do not stage model/audio artifacts.
- After meaningful implementation, run:
  .\gradlew.bat testDebugUnitTest
  .\gradlew.bat assembleDebug
- When relevant, also run:
  .\gradlew.bat :app:compileDebugAndroidTestKotlin
  .\runSmriti.ps1 -FinalUi
  .\runSmriti.ps1 -Logs
- Report exact files changed, tests added, validation results, and any remaining risks.
- Recommend a git commit only after each phase passes.

Start with Phase 1 only:
Fix referral-language inconsistency. Reject RealGemma output when referralFlag=false but any user-facing field contains referral/urgent/escalation language in English, Hindi, Spanish, or Swahili. Do not convert false to true automatically. If referralFlag=true, valid supplied citation is still required. Do not weaken ProtocolCitationValidator. Add tests for English, Hindi, Spanish, Swahili false-flag referral language rejection, routine no-referral acceptance, referral true without citation rejection, and referral true with valid citation acceptance.
```

---

## 12. Per-Phase Mini Prompts

### Phase 1 prompt — Safety hardening

```text
Implement Phase 1 only: referral-language inconsistency hardening.

Reject RealGemma output if referralFlag=false but user-facing fields contain referral/urgent/escalation language. Scan summary, referralReason, dangerSigns, followUpPlan, clarificationQuestion, safetyNote, and any other parsed user-facing string fields. Cover English, Hindi, Spanish, and Swahili. Do not convert referralFlag=false to true. Reject safely. If referralFlag=true, keep valid supplied citation mandatory. Do not weaken ProtocolCitationValidator.

Add tests for multilingual false-flag referral language rejection, routine no-referral acceptance, referral true without citation rejection, and referral true with valid citation acceptance.

Run .\gradlew.bat testDebugUnitTest and .\gradlew.bat assembleDebug. Report files changed and validation results.
```

### Phase 2 prompt — Follow-up scheduling

```text
Implement Phase 2 only: local follow-up scheduling.

After CHW confirm/save, create a local follow-up task when the reviewed follow-up plan indicates a follow-up is needed. The CHW must be able to review/edit date and reason before save. Store tasks in Room. Show due/upcoming/overdue follow-up chips on Patient Roster and follow-up cards on Visit/Summary screens. Allow Mark Done and Reschedule. Do not call RealGemma for Mark Done or Reschedule. Prevent duplicate tasks on double save. Reset should clear generated follow-up tasks while restoring seeded demo state.

Add unit tests for task creation, double-save guard, sorting, Mark Done, Reschedule, Summary counts, and reset behavior.

Run validation and report files changed.
```

### Phase 3 prompt — Patient leave-behind

```text
Implement Phase 3 only: CHW-reviewed patient leave-behind message.

After a confirmed saved visit, add a screen/action to prepare a plain-language patient message in the patient’s preferred note language. It must be editable before copy/share. Use confirmed saved visit data only. No diagnosis, no treatment improvisation, no raw protocol paragraphs. Use Android share sheet / copy action; no cloud service is added. If RealGemma is unavailable, use a deterministic template. Include required safety wording.

Add tests for referral case wording, routine case wording, edit-before-share, language-specific safety wording, and no cloud/runtime dependency.

Run validation and report files changed.
```

### Phase 4 prompt — Community panel

```text
Implement Phase 4 only: local community/village panel view.

Add a roster action for Community Panel / Village Panel. Base cards must be deterministic Room queries and work without RealGemma: pregnant patients, third trimester, follow-ups due, urgent reviews saved, no recent visit, languages covered. Optional Gemma narrative can be attempted only if gates are active and must fall back to deterministic counts. No diagnosis or clinical prediction.

Add tests for seeded counts, count changes after save/follow-up completion, reset behavior, and RealGemma-unavailable fallback.

Run validation and report files changed.
```

### Phase 5 prompt — Urgent Protocol Lookup

```text
Implement Phase 5 only: Urgent Protocol Lookup.

Add a safe roster action called Urgent Protocol Lookup / Check urgent guidance. CHW selects or types a danger sign. Smriti retrieves local protocol guidance through ProtocolRetriever first and displays a cited protocol card. Optional RealGemma simplification is allowed only after retrieval and must preserve citation. If no protocol match, show safe no-match fallback. No diagnosis, no medication/dosage advice, no treatment improvisation, no automatic save. If attaching to a patient record, require selected patient and CHW confirmation.

Add tests for preset danger signs, citation required, no-match fallback, no diagnosis/medication wording, no auto-save, and RealGemma-unavailable deterministic fallback.

Run validation and report files changed.
```

### Phase 6 prompt — Impact estimate

```text
Implement Phase 6 only: deterministic workflow impact estimate line.

Add a small Summary/Community Panel line such as: "Workflow estimate: 3 confirmed visits, 2 follow-ups due, 1 urgent review surfaced, about 15–25 minutes of manual lookup avoided." Make it deterministic from local app activity and explain assumptions in a collapsed info card. Do not claim lives saved, mortality reduction, clinical validation, or outcome improvement.

Add tests for estimate updates and forbidden wording.

Run validation and report files changed.
```

### Phase 7 prompt — LiteRT capability probes

```text
Create or switch to branch feature/litert-capability-probes. Implement probes only; do not change the stable filmed flow.

Probe A: ManualRealGemmaAudioTranscriptInstrumentedTest using Content.AudioBytes/audioBackend if available. Goal is audio -> transcript only, not audio -> clinical JSON. No Room writes. Skip safely if unsupported.

Probe B: MTP/speculative decoding benchmark using ExperimentalFlags.enableSpeculativeDecoding if available. Keep CPU baseline, add GPU/MTP only behind explicit manual flags. Do repeated target-device tests.

Probe C: Optional native tool-calling protocol lookup trace using ToolSet/@Tool/@ToolParam or OpenApiTool if available. Keep deterministic ProtocolRetriever mandatory and citation validation strict.

Probe D: Check whether Android Kotlin exposes a usable constrained decoding JSON schema API. Do not remove parser/safety/citation validation.

Report exact dependency/API changes, validation results, latency, crashes, and merge/no-merge recommendation.
```
