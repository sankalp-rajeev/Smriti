# Smriti — Product Requirements Document
**Version:** 1.2
**Author:** Sankalp Rajeev
**Date:** April 26, 2026
**Project Window:** April 26 – May 18, 2026 (22 days)
**Primary Track:** Health & Sciences
**Special Tech Track:** LiteRT

---

## 1. Executive Summary

There are 2 million community health workers (CHWs) globally. They conduct door-to-door health visits in villages, slums, and remote communities — the last mile of healthcare. They document everything on paper. They have no EHR access. They have no reliable internet. They make clinical decisions from memory.

Every ambient AI scribe on the market — Abridge ($5.3B valuation), Suki, Nuance DAX — is built for hospital doctors with Epic EHR and stable WiFi. None of them work for the health worker walking between mud-brick houses in rural Odisha or peri-urban Nairobi.

**Smriti is a local-first CHW visit copilot for maternal health — focused on voice documentation, longitudinal patient memory, and protocol-grounded referral flags.**

This is a composite scenario drawn from documented ASHA workflow failures — not a single incident, but a pattern Last Mile Health, KhushiHealth, and the NHM have independently described:

An ASHA worker visits a pregnant mother complaining of a headache. She writes it on paper and moves to her next visit. Three days later the mother has a stroke. The ASHA later realizes she missed the danger signs because she could not recall the mother's blood pressure from three weeks prior. Smriti would have surfaced that history instantly and flagged an immediate referral per WHO ANC guidelines.

That pattern — not one case, but thousands — is what this app is built to break.

The CHW speaks their observations in their local language. Gemma 4 actively reasons over what it hears — surfacing prior history, retrieving protocol guidance, deciding when to flag a referral — and produces a structured visit record the CHW confirms before saving. Nothing requires internet.

> *"Not another ambient scribe. Not another CHW chatbot. A local-first maternal health copilot for the ASHA worker who has no EHR, no signal, and no backup."*

---

## 2. The Problem

### 2.1 The Scale

- **2 million+ CHWs globally** — India (ASHA workers: 1 million+), Ethiopia (Health Extension Workers: 40,000+), Kenya, Nigeria, Bangladesh, Brazil
- Each CHW serves **500–3,000 people** with no doctor backup
- More than 40,000 Ethiopian health workers are responsible for 2,500 to 3,000 people each
- CHWs conduct **1–8 home visits per day**, each generating documentation that is currently written by hand on paper forms

### 2.2 The Documentation Crisis

CHWs spend 30–40% of their field time on paperwork. The consequences:
- Paper records are lost, damaged, or illegible
- No longitudinal patient history — every visit starts from scratch
- Clinical guidelines (WHO IMCI, national protocols) run to hundreds of pages — impossible to memorize
- Mistakes happen when a CHW can't remember what a patient was prescribed 3 months ago
- Supervisors receive incomplete, unstructured reports

### 2.3 Why Existing Tools Fail

| Tool | What it does | Why it fails CHWs |
|---|---|---|
| CommCare, ODK, KoboToolbox | Structured form collection | Forms only — no reasoning, no clinical guidance, no voice |
| HEP Assist (Last Mile Health) | Cloud chatbot for clinical guidance | Requires internet — useless in the field |
| ChatCHW, ASHABot | Cloud LLM chatbot | Requires internet, no longitudinal memory |
| Abridge, Suki, Nuance DAX | Ambient scribe for hospital doctors | Cloud-only, EHR-dependent, $100+/month |
| CommCare AI (emerging) | Form automation | Still cloud-dependent, no voice |

**The gap:** No tool combines offline operation + native voice input in local languages + longitudinal patient memory + clinical RAG + structured documentation output. This combination is only practical with Gemma 4's native audio + long context + function calling on LiteRT.

### 2.4 What Gemma 4 Specifically Enables

Gemma 4 makes this practical for the first time. Prior edge models lacked the combination needed:
- **Native audio ASR** — E2B/E4B transcribe spoken Hindi, Swahili, Kannada without a separate Whisper model. Audio is processed in 30-second chunks — sufficient for a CHW's spoken observation per patient stop.
- **Long context** — Base model supports 128K tokens; current LiteRT-LM package supports 32K tokens. At ~750 tokens per compressed visit summary, 32K holds ~40 visit records — 6 months of history for a typical CHW patient load.
- **Native function calling** — structured tool invocation without prompt-engineering hacks. Gemma decides which tool to call and when.
- **Multimodal** — reads paper forms and medicine labels through the camera.
- **LiteRT-LM deployment** — GPU/NPU accelerated on Android. E2B targets mid-range devices (6GB RAM, Snapdragon 8-series ~$200–300). E4B targets flagships (8–12GB RAM).

**Device target:** E2B as the MVP baseline on mid-range Android. E4B as a stretch benchmark on flagship hardware. Neither requires internet after first-time model download.

---

## 3. The Solution

### 3.1 What Smriti Does

A CHW arrives at a patient's home. They open Smriti on their Android phone. The app loads the patient's visit history from local storage. The CHW speaks their observations in their preferred language. Gemma 4 listens, transcribes, reasons over the observation against the patient's history and locally-stored clinical guidelines, and produces a structured visit record with recommended next steps.

At end of day, Smriti generates a supervisor summary covering all visits, flagged cases, and required follow-ups.

### 3.2 Core Capabilities

**1. Voice-Driven Visit Documentation**
CHW speaks naturally: *"Meena, 28 years old, 32 weeks pregnant. Complaining of headache and blurred vision since yesterday. BP 150 over 95. Fetal movement reduced."* Gemma transcribes, structures, and immediately surfaces prior visit history.

**2. Longitudinal Patient Memory**
Every previous visit is stored locally. At next visit, Gemma automatically surfaces: *"Last visit 3 weeks ago: treated for ARI, referred to PHC. Did patient complete antibiotic course?"* The CHW never starts from scratch.

**3. Clinical RAG — Grounded Recommendations**
Locally-indexed WHO IMCI guidelines, national health protocols, and drug reference cards. When Gemma generates a recommendation, it cites the exact protocol section. It never invents clinical guidance.

**4. Structured Output via Native Function Calling**
Visit records, referral flags, supervisor reports — all generated as structured data via Gemma 4's native function calling. The app executes the action. Gemma reasons about what to do.

**5. Multilingual Voice Input and Output**
Gemma 4 E2B/E4B has strong out-of-the-box support for 35+ languages including Hindi, Swahili, and Kannada, pre-trained on 140+ languages. Smriti targets the languages most common in ASHA and CHW deployments: Hindi, Swahili, and Kannada as the demo set.

**6. Offline-First, Always**
Zero network dependency during field operation. Sync when connectivity is available. Patient data never leaves the device without explicit CHW action.

### 3.3 What Smriti Is Not

- **Not a diagnostic tool** — recommendations are protocol-grounded, always framed as "per WHO IMCI guidelines" not "the patient has X"
- **Not a replacement for clinical supervision** — escalation flags always recommend human supervisor review
- **Not an autonomous agent** — CHW confirms every generated record before saving
- **Not cloud-dependent** — every core feature works with airplane mode on

---

## 4. Competition Alignment

### 4.1 Track: Health & Sciences
*"Bridge the gap between humans and data. Build tools that accelerate discovery or democratize knowledge."*

Smriti bridges the gap between 2 million CHWs and the clinical knowledge base behind them. It democratizes access to WHO guidelines, longitudinal patient records, and structured documentation — capabilities that hospital doctors take for granted — for the workers who need them most.

### 4.2 Special Tech Track: LiteRT
Smriti runs Gemma 4 E2B via LiteRT-LM with GPU/NPU acceleration on Android. E4B is the benchmark target on flagship hardware. This is the correct deployment pathway for a tool that must run on a mid-range Android phone in a village with no internet. E2B runs on devices with 6GB RAM (Snapdragon 8-series, ~$200–300). LiteRT is not a workaround — it is the enabling technology.

### 4.3 Judging Criteria Alignment

| Criterion | Weight | How Smriti scores |
|---|---|---|
| Impact & Vision | 40 pts | 2M CHWs globally, lives-on-the-line decisions, zero extra hardware cost, offline necessity structural not optional |
| Video Pitch | 30 pts | CHW in the field, patient history surfaced instantly, referral flag generated, supervisor brief produced — cinematic and immediate |
| Technical Depth | 30 pts | Gemma 4 native audio (30s chunks) + 32K LiteRT context + function calling + local RAG + multilingual — all unique E2B/E4B capabilities used simultaneously |

### 4.4 The "Why Gemini Can't Do This" Argument

A judge opens Gemini Live. Points phone at a patient. Asks "what should I do?" Gemini answers from general knowledge. It cannot:
- Remember what this patient was treated for 3 months ago
- Cross-reference against locally-stored WHO IMCI guidelines offline
- Generate a structured supervisor brief covering 8 visits today
- Work with zero signal in a village 40km from the nearest tower
- Call structured tools to log a referral, flag a case, and update a patient record simultaneously

This is structurally impossible without a persistent local model. Smriti is the gap.

---

## 5. Waterfall Development Overview

| Phase | Dates | Output |
|---|---|---|
| 1. Stack Validation | Apr 26–28 | LiteRT + Gemma 4 audio confirmed on device |
| 2. Core Pipeline | Apr 29 – May 5 | Voice → transcription → structured record end-to-end |
| 3. Productization | May 6–12 | Longitudinal memory, RAG, multilingual, supervisor brief |
| 4. Testing + Benchmarks | May 9–12 | Accuracy tables, latency, offline confirmation |
| 5. Submission Assets | May 13–18 | APK, repo, video, writeup |

---

## 6. Requirements

### 6.1 Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | Voice input: Gemma 4 native audio ASR transcribes CHW speech in 30-second chunks; CHW taps record, speaks, taps stop | P0 |
| FR-02 | Patient selection from local roster before visit | P0 |
| FR-03 | Longitudinal patient history surfaced at start of each visit | P0 |
| FR-04 | Gemma reasons over observation + history + guidelines → structured visit record | P0 |
| FR-05 | Structured output via native function calling: log_visit(), flag_referral(), update_patient() | P0 |
| FR-06 | Local RAG over WHO IMCI + national protocol corpus | P1 |
| FR-07 | Every recommendation cites source protocol section | P1 |
| FR-08 | Referral flags: condition, urgency, recommended facility, reason | P0 |
| FR-09 | End-of-day supervisor summary generation | P1 |
| FR-10 | CHW confirms every generated record before saving | P0 |
| FR-11 | Multilingual voice output for generated recommendations | P1 |
| FR-12 | Camera input: read paper forms, medicine labels | P2 |
| FR-13 | Zero network dependency during field operation | P0 |
| FR-14 | Sync capability when WiFi available (structured JSON export) | P2 |

### 6.2 Non-Functional Requirements

| ID | Requirement | Target | Rationale |
|---|---|---|---|
| NFR-01 | Audio transcription latency | <3s for 30-second voice input | Field workflow — CHW cannot wait |
| NFR-02 | Visit record generation latency | <10s after voice input ends | Acceptable for field documentation |
| NFR-03 | App memory footprint | <2GB | Android per-app constraint |
| NFR-04 | Transcription accuracy (Hindi/English) | WER <15% on clear speech | Based on 2025 clinical ASR systematic review |
| NFR-05 | Offline operation | Zero network calls during runtime | Core product guarantee |
| NFR-06 | Patient history context | Holds ~40 compressed visit summaries per patient in 32K LiteRT context | Compressed summaries fit within LiteRT-LM 32K window; older records summarized and stored locally |
| NFR-07 | Minimum Android API | API 26 (Android 8.0) | 95%+ device coverage |
| NFR-08 | Referral flag accuracy | >80% on test cases | Safety-critical feature |

### 6.3 Out of Scope (MVP)

- EHR integration
- Multi-CHW team coordination
- Automated supervisor dashboards
- iOS version
- Play Store submission
- Biometric patient identification

---

## 7. Design

### 7.1 System Architecture

```
CHW opens patient record (local roster)
        │
        ▼
┌─────────────────────────────────────────────────┐
│  Patient History Loader                         │
│  Room/SQLite → last 6 months of visits          │
│  Presented to CHW before visit starts           │
└─────────────────┬───────────────────────────────┘
                  │ Patient context injected
                  ▼
CHW speaks observation (voice input)
        │
        ▼
┌─────────────────────────────────────────────────┐
│  GEMMA 4 E4B — CORE REASONING AGENT             │
│  LiteRT-LM, GPU/NPU accelerated                 │
│                                                 │
│  Inputs (all local, all offline):               │
│  ├── Live audio stream (native ASR)             │
│  ├── Patient history (last 6 months)            │
│  ├── Retrieved protocol snippets (RAG)          │
│  ├── CHW language preference                    │
│  └── Prior confirmed records this session       │
│                                                 │
│  Native function calling — 4 tools:             │
│  ├── log_visit()                                │
│  ├── flag_referral()                            │
│  ├── update_patient_record()                    │
│  └── generate_supervisor_summary()              │
│                                                 │
│  Gemma reasons and decides which tools to call  │
└─────────────────┬───────────────────────────────┘
                  │
        ┌─────────┼──────────────┐
        ▼         ▼              ▼
┌──────────┐ ┌─────────┐ ┌────────────────┐
│  Visit   │ │Referral │ │ Patient Record │
│  Record  │ │  Flag   │ │  Update        │
│  UI      │ │  Alert  │ │  Local Storage │
└──────────┘ └─────────┘ └────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────┐
│  CHW Review + Confirm                           │
│  Edit any field before saving                   │
│  Every recommendation shows protocol citation   │
└─────────────────────────────────────────────────┘
        │
        ▼ (end of day)
┌─────────────────────────────────────────────────┐
│  Supervisor Summary — Gemma end-of-day call     │
│  Input: all visit records today                 │
│  Output: structured brief with flagged cases,   │
│  referral count, follow-up priorities           │
└─────────────────────────────────────────────────┘
```

> **Key design framing:** Gemma 4 is the active reasoning agent — it does not just transcribe. It proactively surfaces relevant patient history, retrieves protocol guidance, decides which function to call, asks clarifying follow-up questions when observations are incomplete, and generates grounded recommendations the CHW reviews before saving. No separate transcription model needed — Gemma 4 E2B's native audio handles ASR, reasoning, and structured output in a single model. This is what makes the architecture simple enough to build in 22 days and compelling enough to win on Technical Depth.

### 7.2 Native Function Calling — Tool Schema

```json
{
  "tools": [
    {
      "name": "log_visit",
      "description": "Record a completed home visit as a structured document",
      "parameters": {
        "patient_id": "string",
        "visit_date": "ISO8601",
        "chief_complaint": "string",
        "observations": "array of strings",
        "vitals": {
          "temperature": "float | null",
          "muac_cm": "float | null",
          "weight_kg": "float | null"
        },
        "assessment": "string — protocol-grounded, never diagnostic",
        "protocol_cited": "string | null — e.g. 'WHO IMCI Section 3.2'",
        "action_taken": "string",
        "followup_required": "boolean",
        "followup_date": "ISO8601 | null"
      }
    },
    {
      "name": "flag_referral",
      "description": "Generate an urgent referral flag for supervisor and patient record",
      "parameters": {
        "patient_id": "string",
        "urgency": "IMMEDIATE | WITHIN_24H | WITHIN_WEEK",
        "reason": "string — plain language",
        "protocol_basis": "string — which guideline mandates this referral",
        "recommended_facility": "string | null",
        "danger_signs_present": "array of strings"
      }
    },
    {
      "name": "update_patient_record",
      "description": "Update persistent patient profile with new clinical information",
      "parameters": {
        "patient_id": "string",
        "updated_fields": "object — only changed fields",
        "medication_changes": "array | null",
        "condition_history_addition": "string | null"
      }
    },
    {
      "name": "generate_supervisor_summary",
      "description": "Generate end-of-day structured summary for CHW supervisor",
      "parameters": {
        "chw_id": "string",
        "summary_date": "ISO8601",
        "total_visits": "int",
        "referrals_flagged": "int",
        "urgent_cases": "array of {patient_id, reason, urgency}",
        "followups_due": "array of {patient_id, date, reason}",
        "narrative": "string — 2-3 sentence daily summary"
      }
    }
  ]
}
```

### 7.3 Gemma System Prompt

```
You are Smriti, a voice documentation agent for community health workers.

Your job: listen to a CHW's spoken observations, reason over the patient's 
history, retrieve relevant protocol guidance, and generate a structured 
visit record.

Rules you must follow:
1. Transcribe the CHW's speech accurately. Do not paraphrase their words.
2. Generate assessments grounded in WHO IMCI or national protocols only. 
   Always cite the specific protocol section.
3. Never generate a diagnosis. Use protocol language: 
   "signs consistent with severe acute malnutrition per WHO IMCI" 
   not "the child has malnutrition."
4. Flag referrals when danger signs are present per protocol. 
   Do not guess at thresholds — use the retrieved protocol definition.
5. When patient history shows a prior treatment, ask whether it was 
   completed before documenting a new course.
6. If you are uncertain, flag it. Do not fill gaps with assumptions.
7. Generate all output in: {chw_language}

Patient: {patient_name}, {patient_age}
Visit history: {last_6_months_summary}
Retrieved protocol: {rag_context}
```

### 7.4 Local RAG Corpus

**Sources (all open access / public domain):**
- WHO ANC Antenatal Care recommendations (primary corpus — maternal health MVP)
- India ASHA worker training modules, ANC section (NHM, public domain)
- WHO danger signs in pregnancy reference card (public domain)
- Kenya CHW ANC handbook (MOH Kenya, public domain)
- WHO IMCI guidelines (roadmap only — not in MVP corpus)

**Implementation:** Flat JSON keyword + section lookup over ~200 indexed protocol chunks. No vector database needed on Android — corpus is small, TF-IDF sufficient, zero additional memory cost.

### 7.5 Technology Stack — Every Choice Justified

#### Gemma 4 E2B (baseline) / E4B (target)
The only edge model combining native audio ASR + long context (128K base model, 32K via LiteRT-LM) + function calling + multilingual (35+ languages, trained on 140+) + LiteRT deployment — simultaneously on a phone. No prior edge model had this combination at this deployment scale. This is why Smriti is practical for the first time with Gemma 4.

#### LiteRT + LiteRT-LM
Only Android pathway with GPU/NPU acceleration for both audio processing and language model inference. Confirmed support for Gemma 4 E4B. Direct LiteRT special prize alignment.

#### Kotlin + Android SDK
Native LiteRT SDK access, Room, Android TTS, local notifications — no bridge layer overhead.

#### Room / SQLite
All patient data local. 6-month rolling visit history per patient queryable by patient ID. JSON export for sync. Zero cloud dependency.

#### Android TTS
Offline multilingual voice output for generated recommendations. Zero network cost. Supports Hindi, Swahili, and other required languages.

#### Whisper Tiny (fallback only)
If Gemma 4 E2B native audio is insufficient quality on baseline device, Whisper Tiny as a preprocessing ASR stage before passing text to Gemma for reasoning. Test in Phase 1 — use only if needed.

---

## 8. Implementation

### 8.1 Phase Plan (22 days from April 26)

#### Phase 1 — Stack Validation (Apr 26–28, 3 days)

| Day | Task | Exit Criteria |
|---|---|---|
| Apr 26 | LiteRT + LiteRT-LM on device. Test Gemma 4 E2B audio: speak 30 seconds of Hindi, confirm transcription | Transcription output confirmed, WER estimated <20% |
| Apr 27 | Native function calling confirmed: log_visit() called with correct schema | Valid JSON tool call returned |
| Apr 28 | Memory test: Gemma 4 E2B + Room in same process, 6-month patient history injected | Combined footprint <2GB confirmed |

**Phase 1 exit criteria:** Audio works. Function calling works. Memory fits. If Gemma 4 audio quality is poor, add Whisper Tiny and document this decision.

#### Phase 2 — Core Pipeline (Apr 29 – May 5, 7 days)

| Day | Task | Exit Criteria |
|---|---|---|
| Apr 29–30 | Patient roster UI, local Room schema, patient selection flow | Patient loaded with history before visit |
| May 1–2 | Voice input → Gemma transcription → structured visit record via log_visit() | Single end-to-end visit documented from voice |
| May 3 | Referral flag: flag_referral() triggered on danger sign keywords | Referral generated for "convulsions" test input |
| May 4 | Patient record update: update_patient_record() persists across sessions | Second visit shows first visit history |
| May 5 | CHW confirm screen: every record editable before save | Edit and save flow working |

**Phase 2 exit criteria:** Full visit cycle works end-to-end. Voice in, structured record out, saved locally, visible on next visit. Airplane mode confirmed.

#### Phase 3 — Productization (May 6–12, 7 days)

| Day | Task | Exit Criteria |
|---|---|---|
| May 6 | Local RAG: 200 indexed WHO IMCI chunks, keyword retrieval | Protocol citation appears in assessment for MUAC test case |
| May 7 | Multilingual: Hindi voice input → Hindi output recommendations | Native-speaker review of Hindi output |
| May 8 | Swahili voice input → Swahili output (stretch — skip if behind schedule) | Second language confirmed OR documented as roadmap |
| May 9–10 | Supervisor summary: generate_supervisor_summary() after 5 test visits | Structured brief with referral count and urgent cases |
| May 11 | UI polish: CHW-friendly, large text, minimal navigation | Demo-quality interface |
| May 12 | Android TTS multilingual output for recommendations | Voice output confirmed in Hindi offline |

**Phase 3 exit criteria:** Full product. Two video beats working (real-time visit documentation + end-of-day supervisor brief). Two languages confirmed. RAG citations visible.

#### Phase 4 — Testing and Benchmarks (May 9–12, parallel with Phase 3)

| Day | Task | Exit Criteria |
|---|---|---|
| May 9 | Transcription benchmark: 20 voice inputs, WER measured | WER table documented |
| May 10 | Visit record accuracy: 10 test cases with known ground truth | Precision on structured fields documented |
| May 11 | Referral flag accuracy: 10 known danger sign cases | Detection rate >80% documented |
| May 12 | Latency benchmark: voice input end → record generated, 10 runs | Mean latency documented |

#### Phase 5 — Submission (May 13–18)

| Day | Task | Exit Criteria |
|---|---|---|
| May 13 | GitHub README, architecture diagram, benchmark table | Repository judge-ready |
| May 14 | APK build and clean-device install test | Works on device it wasn't developed on |
| May 15 | Film demo sequence | Raw footage captured |
| May 16 | Edit 3-minute video | YouTube link live |
| May 17 | Kaggle writeup, media gallery, all links | Draft submitted |
| May 18 | Final review, submit 11:59 PM UTC | Submitted ✓ |

---

## 9. Testing

### 9.1 Test Scenarios

| Test ID | Requirement | Scenario | Input | Expected Output | Pass Criteria |
|---|---|---|---|---|---|
| T-01 | FR-01 | Hindi voice transcription | 30s observation in Hindi | Accurate text transcription | WER <15% |
| T-02 | FR-01 | Swahili voice transcription | 30s observation in Swahili | Accurate text transcription | WER <20% |
| T-03 | FR-03 | History surfaced | Return visit for known patient | Last visit summary shown | Correct date, chief complaint, actions |
| T-04 | FR-04 | Clean visit record | Normal visit, no danger signs | Structured log_visit() output | All fields populated, no false flags |
| T-05 | FR-08 | Danger sign flag | "Child has convulsions and high fever" | flag_referral() called: IMMEDIATE | Urgency = IMMEDIATE, danger_signs populated |
| T-06 | FR-08 | MUAC flag | "MUAC 10.5cm, bilateral pitting edema" | SAM referral flagged per WHO IMCI | Protocol citation: WHO IMCI Section 3 |
| T-07 | FR-06 | RAG citation | Any recommendation generated | Protocol section cited | Citation present in every clinical recommendation |
| T-08 | FR-09 | Supervisor summary | 5 visits completed, end-of-day trigger | Structured summary with all visits | Referral count correct, urgent cases listed |
| T-09 | FR-13 | Offline operation | Airplane mode, full pipeline | All features functional | Zero network errors in logcat |
| T-10 | NFR-01 | Transcription latency | 30s voice input, 10 runs | Transcription complete | Mean <3s |
| T-11 | NFR-02 | Record generation latency | End of input → log_visit() returned | Record generated | Mean <10s |

### 9.2 Failure Tests

| Test | Scenario | Expected Behavior |
|---|---|---|
| F-01 | Background noise during voice input | Gemma flags "unclear audio — please repeat" |
| F-02 | New patient with no history | Visit starts with empty history, no hallucinated prior records |
| F-03 | Observation contains no clinical information | Gemma requests clarification, does not generate empty record |
| F-04 | Gemma function call timeout | Timeout handled, visit saved as draft, CHW notified |
| F-05 | Unsupported language spoken | Gemma attempts transcription, flags language confidence |
| F-06 | 10 visits in one session | No memory leak, all visits saved correctly, summary accurate |

### 9.3 Demo Script (Film May 15)

1. Airplane mode on — show on screen
2. Open Smriti — zero network errors
3. Select patient "Meena, 28F" — show 3 prior visit history surfacing
4. Speak in Hindi: *"Meena, 28 years old, 7 months pregnant. Complaining of headache and blurred vision. Blood pressure 150 over 95. No fetal kick count done today."*
5. Show Gemma transcribing in real time
6. Show structured record generated: log_visit() called
7. Show flag_referral() triggered: IMMEDIATE, danger signs = hypertension in pregnancy, per ANC guidelines
8. Show protocol citation: WHO ANC Recommendation B1.2
9. CHW confirms and saves
10. Trigger end-of-day summary: show supervisor brief with 5 visits, 1 urgent referral
11. Show full offline — no network calls made throughout

---

## 10. Deployment

### 10.1 Distribution
- APK sideload via GitHub Releases
- Minimum Android 8.0 (API 26)
- Debug keystore for demo

### 10.2 Model Distribution
| Model | Source | Size | Delivery |
|---|---|---|---|
| Gemma 4 E2B | HuggingFace, first-launch WiFi download | ~2GB | On-device, one-time |
| Gemma 4 E4B | HuggingFace, benchmark target | ~5GB | Same |
| Whisper Tiny (if needed) | Bundled in APK | ~75MB | APK assets |

### 10.3 GitHub Repository Structure

```
smriti/
├── README.md                    # Story, architecture, setup, benchmarks
├── app/src/main/kotlin/
│   ├── audio/                   # GemmaAudioEngine, WhisperFallback
│   ├── reasoning/               # GemmaAgent, FunctionCallHandler, SystemPrompt
│   ├── rag/                     # ProtocolIndexer, KeywordRetriever
│   ├── data/                    # Room entities, PatientRecord, VisitLog
│   └── ui/                      # PatientSelectScreen, VisitScreen, SummaryScreen
├── benchmarks/
│   ├── transcription_wer.csv
│   ├── latency_results.csv
│   └── referral_accuracy.csv
├── corpus/
│   └── who_imci_indexed.json    # 200 protocol chunks, open access
└── docs/
    └── architecture.png
```

---

## 11. Risk Register

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Gemma 4 E2B native audio quality insufficient | Medium | High | Test Day 1. Add Whisper Tiny as preprocessing stage if WER >20%. Document in README. |
| LiteRT-LM GPU delegate fails on target device | Medium | High | Test Day 1. Fallback: XNNPACK CPU. Slower but functional. |
| 128K context fills with long patient history | Low | Medium | Cap at 6 months rolling. Summarize older history into compact representation before injection. |
| Hindi/Swahili output quality poor | Medium | High | Native-speaker review May 7-8. Fix before May 12. |
| Referral flag false negatives on danger signs | Medium | High | RAG retrieval must surface WHO danger sign definitions before Gemma reasons. Test all 8 IMCI danger signs. |
| 22-day timeline too tight | High | High | Phase 1 and 2 are non-negotiable. Phase 3 features are drop candidates if behind schedule. Supervisor summary is P1, not P0. |
| Kaggle writeup framing drifts into "diagnostic AI" | Low | High | Every output framed as "protocol-grounded documentation support." Never "diagnosis" or "treatment recommendation." |

---

## 12. The Video

### 12.1 Opening (0:00–0:25)
You, on camera:
*"An ASHA worker visits a pregnant mother with a headache. She writes it on paper and moves on. Three days later, the mother has a stroke. The ASHA realizes she missed the danger signs — she couldn't remember the blood pressure from three weeks earlier. This happens. Not once. Thousands of times. Because there are one million ASHA workers in India alone, each managing hundreds of patients, each working from memory and paper. I built something for them."*

### 12.2 Demo (0:25–2:10)
- Airplane mode on, phone screen visible
- CHW selects patient Meena, prior history shown
- Speaks in Hindi, Gemma transcribes in real time
- Referral flag fires: IMMEDIATE, hypertension in pregnancy
- Protocol citation shown: WHO ANC guidelines
- CHW confirms record
- End of day: supervisor brief generated with 5 visits, 1 urgent referral
- Export shown

### 12.3 Close (2:10–3:00)
*"One phone. No signal. A health worker who now remembers every patient, cites every guideline, and never misses a danger sign. This is what Gemma 4 can do for the last mile."*

Stack callout on screen: Gemma 4 E2B · LiteRT · Offline · 35+ languages · Open source

---

## 13. Final Positioning

**Smriti is not an ambient scribe. It is not a CHW chatbot. It is a local-first maternal health visit copilot for ASHA workers — turning a spoken voice note, patient history, and offline ANC guidelines into a source-cited visit record, referral flag, and supervisor summary, entirely on-device via Gemma 4 E2B and LiteRT.**

Every architectural decision, every benchmark, every frame of the video must defend this sentence.
