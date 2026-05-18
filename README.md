# Smriti

Offline maternal-health visit copilot for community health workers.

Android · Kotlin · Jetpack Compose · Room/SQLite · Gemma 4 · LiteRT-LM · Offline after setup

Smriti helps a community health worker move from a home-visit observation to a reviewed, cited, locally saved visit record without depending on cloud APIs during the field workflow.

## The Problem

Community health workers often carry the continuity of care in paper notebooks, memory, phone notes, and patient-held cards. During maternal-health visits, a missed card, a forgotten follow-up, or unreliable connectivity can break the chain between prior history and today's danger signs.

That gap matters in the last mile. A CHW may need to remember who had a high blood pressure note last week, who missed an antenatal follow-up, and which symptoms need urgent review, while moving between homes without stable internet.

Smriti is built for that setting: local patient memory, local protocol guidance, and a workflow that helps document and escalate concerns without turning the app into autonomous diagnostic AI.

## The Solution

Smriti starts from the person, not a blank chat box.

```text
Roster
-> patient history
-> CHW observation
-> local protocol retrieval
-> on-device Gemma structured note
-> CHW review/edit
-> local save
-> follow-up task
-> patient leave-behind message
-> supervisor summary + Community Panel
```

The app supports a local roster, prior visit history, country/region-aware protocol snippets, reviewed visit notes, follow-up tasks, editable patient messages, a Community Panel, and an end-of-day supervisor summary. All demo data is synthetic.

## Why Gemma 4 + LiteRT-LM

Gemma 4 is used inside a bounded workflow, not exposed as open-ended chat.

`RealGemmaAgent` receives the CHW observation, local patient history, and cited protocol chunks retrieved from local JSON assets. It asks Gemma 4 E2B through LiteRT-LM 0.11.0 for strict structured output. The result then passes through a JSON parser, citation validator, and safety post-processor before it can reach the review screen.

If RealGemma setup is missing, inference fails, output is invalid, or citations do not match supplied protocol chunks, Smriti shows setup/retry messaging. It does not silently fall back to mock clinical output.

## Architecture

```text
CHW observation
-> Room/SQLite patient history
-> ProtocolRetriever over local JSON assets
-> RealGemmaAgent
-> LiteRT-LM Gemma 4 E2B
-> JSON parser + citation validator + safety post-processor
-> ReviewScreen
-> CHW confirm/save
-> Follow-up / patient message / supervisor summary / Community Panel
```

Runtime boundaries:

- Core visit data stays local in Room/SQLite.
- Protocol retrieval uses app-bundled JSON assets with country/region context.
- RealGemma inference is gated by local setup and runs through LiteRT-LM.
- Review and confirmation are required before generated content is saved.
- Supervisor summary and Community Panel are deterministic local state views, not diagnosis or prediction.

## Core Demo Flow

1. Welcome screen and local patient roster.
2. Open synthetic patient Meena and show prior antenatal visit history.
3. Enter a Hindi/English danger-sign observation.
4. Retrieve local protocol guidance and generate a cited RealGemma visit note.
5. Review/edit, confirm, and save the note locally.
6. Show follow-up task and editable patient leave-behind message.
7. Open Grace paper-note scan for CHW-reviewed data entry support.
8. Show Community Panel and end-of-day supervisor summary.

## Components

| Component | Implementation |
| --- | --- |
| Android UI | Kotlin + Jetpack Compose |
| Local memory | Room/SQLite patients, visits, referrals, follow-ups |
| Protocol retrieval | Local JSON assets with country/region context |
| Reasoning | `RealGemmaAgent` + LiteRT-LM Gemma 4 E2B |
| Safety | Strict parser, citation validator, safety post-processor, CHW confirm/save |
| Multimodal | Audio fills editable transcript only; paper-note scan supports reviewed data entry only |
| Local workflow | Follow-ups, patient messages, Community Panel, supervisor summary |

## Project Structure

```text
Smriti/
├── app/
│   ├── src/main/
│   │   ├── assets/demo/              # synthetic patient/sample assets
│   │   ├── assets/protocols/         # local maternal-health protocol JSON
│   │   ├── java/.../audio/           # local recording helpers
│   │   ├── java/.../data/            # Room entities, DAOs, local memory store
│   │   ├── java/.../pipeline/        # visit reasoning orchestration
│   │   ├── java/.../rag/             # local protocol retrieval
│   │   ├── java/.../reasoning/       # RealGemma, LiteRT, parser, safety checks
│   │   ├── java/.../transcript/      # transcript clients
│   │   ├── java/.../tts/             # Android voice output
│   │   └── java/.../ui/              # Compose screens
│   ├── src/test/                     # deterministic JVM tests
│   ├── src/androidTest/              # manual LiteRT/Gemma probes
│   └── build.gradle.kts
├── docs/
│   ├── submission_writeup.md
│   ├── technical_project_summary.md
│   ├── known_limitations.md
│   └── SOURCES.md
├── gradle/wrapper/
├── runSmriti.ps1
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Build

Open the repository in Android Studio, or build from PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

On macOS/Linux:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## RealGemma Local Setup

The Gemma model is not committed, bundled in app assets, or downloaded at runtime.

RealGemma app-facing inference requires all of:

- build flag: `-Psmriti.realGemmaSubmissionMode=true`
- app-private sentinel: `files/dev/enable_real_gemma_text_mode`
- app-private model: `filesDir/models/gemma-4-E2B-it-int4.litertlm`

Example setup after installing a submission-mode build and sideloading the model outside Git:

```powershell
.\gradlew.bat assembleDebug -Psmriti.realGemmaSubmissionMode=true
adb shell run-as com.smriti.clinicalscribe mkdir -p files/dev files/models
adb shell run-as com.smriti.clinicalscribe touch files/dev/enable_real_gemma_text_mode
adb shell run-as com.smriti.clinicalscribe ls -lh files/models/gemma-4-E2B-it-int4.litertlm
```

If the model or gates are absent, the app can still show the local workflow and setup/retry state, but it will not fabricate a clinical note through mock output.

## Validation Evidence

- `testDebugUnitTest` covers local retrieval, pipeline behavior, RealGemma gates, parser/citation/safety checks, Room-backed local state, follow-ups, summaries, and repo model-artifact safety.
- `assembleDebug` has been used for APK validation.
- LiteRT-LM dependency is pinned to `com.google.ai.edge.litertlm:litertlm-android:0.11.0`.
- Manual RealGemma text benchmarks on CPU were around 15-22 seconds depending on scenario/device.
- Speculative decoding was tested on CPU and was slightly slower in the recorded benchmark, so it is not used.
- Gemma audio probe succeeded, but audio fills an editable transcript only.
- Gemma vision/paper-note probe succeeded, but paper-note scan remains CHW-reviewed data-entry support only.
- Native LiteRT-LM protocol tool-calling was manually validated, while production visits keep deterministic `ProtocolRetriever` retrieval.

## Safety Boundaries

- Smriti is not diagnostic AI.
- It does not prescribe, calculate dosage, or claim clinical validation.
- It does not make autonomous referral decisions.
- It does not auto-save generated model output.
- CHW review/edit/confirm is required before saving.
- Every clinical recommendation must cite supplied protocol guidance or be treated as uncertain.
- Audio transcription is transcript support only.
- Paper-note scan is data-entry support only, not clinical image diagnosis.
- Urgent Protocol Lookup is read-only and creates no visit, referral, follow-up, message, or summary record.
- Community Panel and follow-up tasks are deterministic local workflow state, not diagnosis or prediction.
- The repository uses synthetic demo data only and contains no PHI.
- No cloud APIs are used for core runtime after setup.

## Submission Links

- Video: TBD
- Kaggle writeup: [docs/submission_writeup.md](docs/submission_writeup.md)
- APK: TBD
- Demo: TBD
- Repo: TBD

Additional supporting docs:

- [Technical project summary](docs/technical_project_summary.md)
- [Known limitations](docs/known_limitations.md)
- [Sources](docs/SOURCES.md)
