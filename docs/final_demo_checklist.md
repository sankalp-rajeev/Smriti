# Final Demo Checklist

Use this checklist for the final filmed or live judge demo. The app-facing reasoning path now requires RealGemma text reasoning. If RealGemma setup is missing or inference fails, show setup/retry messaging; do not show mock clinical output.

## Build And Install

| Check | Exact steps | Expected result | Problem signal |
| --- | --- | --- | --- |
| Build debug APK | Run `.\gradlew.bat assembleDebug`. | Build passes and creates `app/build/outputs/apk/debug/app-debug.apk`. | Build fails, APK missing, or Gradle cannot compile. |
| Install APK if needed | Install from Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk`. | App installs as Smriti on the emulator/device. | Install fails, app missing, or stale build opens. |
| Protect repo contents | Check that no `.litertlm`, `.wav`, `.mp3`, `.m4a`, `.flac`, `.tflite`, `.task`, or `.onnx` artifacts are staged. | Only source/docs/test files are staged. | Model/audio artifacts appear in git status. |
| RealGemma submission build | Run `.\gradlew.bat assembleDebug -Psmriti.realGemmaSubmissionMode=true`, then create the app-private sentinel and confirm the app-private model exists. | Offline Proof shows RealGemmaAgent, RealGemma text active, model found, inference enabled. | Offline Proof shows setup required, missing model, or inference disabled. |
| RealGemma preload status | Open the roster with the model present and gates active. | Engine moves through `Preparing` to `Ready`, or remains `Loads on demand` if preload is not available. It never says `Found, not loaded`. | Status implies the model is missing after it generated output, or the UI blocks on preload. |
| RealGemma single-flight gate | Start one note generation, then try scan/summary paths before it finishes. | Buttons are disabled where visible, or Smriti says `Smriti is already preparing a note. Please wait.` No second native inference starts. | Multiple RealGemma calls queue behind the native runner or the app enters another `sendMessage` while busy. |
| Latency evidence | Filter Logcat for `SmritiLatency` during a gated RealGemma run. | Recent emulator/local setup evidence: preload/init 1.885 s; Meena generation 21.726 s; Lucia after preload/reuse 14.434 s; validation 4-31 ms; Room save 49 ms; protocol retrieval and prompt build 1-3 ms. Device performance may vary. | Logs include transcript/PHI, save triggers Gemma, or timing claims imply clinical validation. |
| Optional GPU experiment | Run `ManualRealGemmaBackendLatencyInstrumentedTest` only with explicit `allowExperimentalGpuBackend=true` on the target device/emulator. | CPU baseline remains available; GPU logs under `SmritiBackendLatency` and `SmritiLatency` only if it succeeds. | GPU becomes default without evidence, CPU fallback is removed, or unsupported APIs are invented. |
| RealGemma schema hardening | If testing manual RealGemma, filter Logcat for `SmritiRealGemma` after a parser failure. | Debug/dev logs show raw output preview and parser reason; UI shows only concise retry/setup text. | Raw model output is shown to the CHW, invalid output is saved, or mock output appears as RealGemma. |
| Urgent protocol lookup boundary | Open `Urgent protocol lookup`, select severe headache + blurred vision, and check guidance. | Guidance comes from local protocol assets with a citation, no save occurs, and Summary/Community Panel counts do not change from lookup alone. | Lookup calls RealGemma/network, creates a visit/referral/follow-up, or uses diagnosis/treatment/risk-score wording. |
| Paper-note scan boundary | Open Grace or another patient and use `Use sample paper note`. | Local Gemma vision extracts synthetic paper-note fields, then Review Scanned Note requires edit/review and explicit patient-record confirmation before save. | Image bytes are saved, referral/diagnosis appears, or note saves without confirmation. |

Optional RealGemma submission setup after sideloading the model outside git:

```powershell
.\gradlew.bat assembleDebug -Psmriti.realGemmaSubmissionMode=true
adb shell run-as com.smriti.clinicalscribe mkdir -p files/dev
adb shell run-as com.smriti.clinicalscribe touch files/dev/enable_real_gemma_text_mode
adb shell run-as com.smriti.clinicalscribe ls -lh files/models/gemma-4-E2B-it-int4.litertlm
```

## Filming Setup

| Check | Exact steps | Expected result | Problem signal |
| --- | --- | --- | --- |
| Hide sensitive material | Close logs, terminal windows, and file explorers with local paths or secrets. | Recording shows only the app and safe docs/slides. | Logcat, shell history, secrets, or local model paths are visible. |
| Use synthetic data only | Confirm the app shows the six seeded demo patients, especially Meena Sharma. | No real patient names, phone numbers, locations, or PHI are visible. | Any real patient/PHI data appears. |
| Turn airplane mode on | Open Android quick settings and enable airplane mode before app flow. | Airplane mode icon/status is visible before the demo. | Wi-Fi/cellular appears active during the core flow. |
| Set claim boundary | Prepare the spoken line: `Based on local health guidance, not diagnosis.` | Narration matches the product safety framing. | Narration implies diagnosis, treatment, or clinical validation. |

## Three-Minute Edited Flow

Use this as the primary final filming order:

| Segment | What to show | Keep it tight |
| --- | --- | --- |
| A. Welcome screen | `Smriti`, `Offline health visit assistant`, then `Start visits`. | 5 seconds. |
| B. Offline Proof / setup ready | Tap `Check offline setup`; show works offline after setup, local patient memory, local guidance, RealGemma ready/setup, paper-note scan available, cloud APIs none; return to roster. | 8 seconds. |
| C. Patient roster | Search, primary actions, smaller secondary actions, attention chips, `Needs attention` before routine visits, and patient-card `Note language` labels. | 10 seconds. |
| D. Urgent protocol lookup | Open `Urgent protocol lookup`; select severe headache + blurred vision; show local guidance, citation, safety copy, and no-save boundary. | 10 seconds; local lookup, not diagnosis. |
| E. Community panel | Open `Community panel`; show local caseload counts, follow-ups, languages/countries, and priority list. | 10 seconds; saved-on-device panel, not prediction. |
| F. Amara Tesfaye | Missed follow-up alert. | 8 seconds. |
| G. Fatima Begum | Rising BP history signal. | 8 seconds; say this is a local history signal, not diagnosis. |
| H. Meena Sharma | Hindi RealGemma note, referral suggested, citation/local guidance, CHW confirm/save. | Main sequence, about 50 seconds. |
| I. Patient message | From post-save Summary, open `Prepare patient message`; show editable text and Share/Copy. | 12 seconds; user-initiated share only. |
| J. Lucia Fernandez | Spanish RealGemma note after manual validation. | 15 seconds. |
| K. Grace Achieng | Swahili routine/no-referral RealGemma note after manual validation. | 15 seconds. |
| L. Grace paper-note scan | Use sample paper note; local Gemma vision extracts Grace/BP/symptoms/follow-up; CHW reviews and saves. | 20 seconds; data-entry support only. |
| M. End-of-day Summary | Urgent, follow-up, routine priority list, and `View community panel`. | 15 seconds. |
| N. Closing Offline Proof | No cloud APIs, local patient memory, local guidance, RealGemma text and vision, direct Gemma audio blocked. | 10 seconds. |

It is acceptable to shorten RealGemma waits in editing if the result is real and the video shows:

```text
On-device Gemma 4 inference - sped up for demo.
```

## Manual Demo Flow

| Check | Exact steps | Expected result | Problem signal |
| --- | --- | --- | --- |
| Launch app | Open Smriti after airplane mode is on. | Welcome appears on first launch, or Patient Roster loads locally after welcome has been seen. | App crashes, requires network, or a blank screen appears. |
| Welcome | Show `Smriti`, `Offline health visit assistant`, then tap `Start visits`. | Purpose and safety boundary are clear before roster. | Welcome implies diagnosis or cloud dependency. |
| Setup Guidance | If model is absent on first launch, show `One-time setup needed`. | It explains supervisor model setup in plain words and offers `Continue without model (demo mode)`. | It mentions internal model/runtime terms or claims full reasoning without a model. |
| Reset demo data | Open End-of-Day Summary if needed, tap `Reset Demo Data`, confirm the dialog, then return to Patient Roster. | Saved demo visits/referrals are cleared and the six-patient synthetic roster is restored. | Old saved referrals remain or reset fails. |
| Optional register import | Tap `Import register`, confirm `Add patients from file?`. | Local register import appears and existing patients are not deleted by the dialog wording. | Import asks for network, deletes existing patients unexpectedly, or duplicates histories endlessly. |
| Optional add patient | Tap `Add Patient`; try one offline speech prompt or type manually, then `Confirm and Add`. | Speech unavailable states keep fields editable; manual save creates a local Room patient. | Speech failure blocks manual entry or auto-saves before confirmation. |
| Show Amara alert | Select `Amara Tesfaye, 30F`. | Missed follow-up card appears above transcript input with `Mark Confirmed` and `Note as Ongoing`. | No alert appears after Reset Demo Data or action saves a generated visit automatically. |
| Show Fatima history signal | Select `Fatima Begum, 24F`. | History signal card appears for rising BP trend with cautious ANC monitoring wording. | Card diagnoses disease, says preeclampsia, or appears for routine Grace. |
| Show roster purpose | On Patient Roster, show `Smriti`, `Offline health visit assistant`, search, sorted sections, and patient chips. | Purpose, next step, and attention list are clear. | Screen looks empty, misleading, or lacks patient list. |
| Show community panel | Tap `Community panel` from Patient Roster. | Panel opens with `Today's focus`, `Needs attention`, `Follow-ups`, pregnancy stage, languages/countries, and priority list from local data. | Panel requires internet/model inference, uses diagnosis/risk-score wording, or counts do not match local state. |
| Show urgent protocol lookup | Tap `Urgent protocol lookup` from Patient Roster. Select `Severe headache` and `Blurred vision`, then tap `Check urgent guidance`. | Lookup opens offline, retrieves local health guidance with a citation, shows `This is not a diagnosis`, and states no visit/referral/follow-up is saved. | Lookup saves data, creates a referral/follow-up, changes Summary/Community Panel counts, calls RealGemma, or uses treatment/dose/risk-score wording. |
| Note language labels | Check patient cards for `Note language: Hindi`, `Note language: Spanish`, `Note language: Swahili`, or `Note language: English`. | Generated notes follow each patient's saved preferred language. | Roster implies the whole app UI language changed. |
| Empty states | Search for a missing patient. | Roster shows `No patient found for ...` and offers `Add patient`. | Blank list appears with no explanation. |
| Show Offline Proof | Tap `Check offline setup`. | Dedicated setup screen shows works offline after setup, patient memory saved on device, guidance stored on device, on-device Gemma ready/setup needed, paper-note scan available, cloud APIs none, direct Gemma audio not used. | Technical proof details appear by default on the roster or imply cloud runtime/direct Gemma audio. |
| Select Meena | Tap `Meena Sharma, 28F` / `Open visit`. | Visit screen opens for Meena. | Wrong patient opens or navigation fails. |
| Show visit order | On Visit screen, show patient header, alerts before input, instruction card, input card, compact history, then local setup proof. | Important alerts are visible before transcript input. | Alerts are buried below history or setup details. |
| Visit note language | On Visit screen, point to `Visit note will be prepared in ...`. | The label matches the selected patient's saved note language, not a global UI language. | Changing a default/new-patient language changes an existing patient's note language. |
| Use sample transcript | Tap `Use sample visit transcript`. | Editable transcript fills with Meena-specific danger signs. | Grace or another routine patient receives Meena's danger-sign sample. |
| Optional offline speech | Tap `Speak observation` only if you want to demonstrate fallback. | If unavailable or empty, friendly message appears and existing transcript remains. | Raw error code appears, transcript clears, or app saves/generates automatically. |
| Generate validation | Try blank input once if useful. | Inline message says `Please speak or type today's visit observation first.` | Dialog appears or app navigates away. |
| Generate note | Tap `Generate visit note`. | Loading card shows calm progress. Review opens only after valid output; invalid/unavailable output shows `Note could not be prepared` and preserves transcript. | Generation hangs, auto-saves, clears transcript, shows raw model output, or presents mock output. |
| Prevent overlap | While the note is preparing, verify `Generate visit note`, `Scan paper note`, and `Use sample paper note` cannot start another request. | The transcript remains visible and no second request starts. | A second request queues or opens another RealGemma flow. |
| Show structured note | On Review screen, show editable Observation, Relevant history, Local guidance support, and Follow-up plan. | Generated content is editable before saving. | Fields are not editable or content is missing. |
| Show referral support | Point to `Referral suggested`. | Referral card appears for Meena with danger signs and health guidance used. | Danger-sign case does not produce referral support. |
| Show source details | Expand `How was this prepared?`. | Shows today's observation, prior visit count, local country guidance, on-device note preparation, and guidance ID. | Raw citation ID is the primary display or technical wording appears. |
| Confirm/save | Tap `Confirm and save`. | Visit is saved and Summary screen opens. | Save happens before confirmation, button fails, or summary does not open. |
| Patient leave-behind | On post-save Summary, tap `Prepare patient message`. | Editable `Patient message` screen opens with Share and Copy; sharing uses Android chooser and does not auto-send. | Message appears before save, is not editable, auto-sends SMS/WhatsApp, or contains treatment/dosage claims. |
| Double save guard | Tap `Confirm and save` once; do not tap again while it says `Saving locally...`. | Button is disabled immediately and only one local visit is saved. | Duplicate saved-history cards appear for one ReviewScreen confirmation. |
| Save latency boundary | Watch the save step after Review. | The app shows local saving only briefly; save uses Room/SQLite and does not call RealGemma or re-run protocol retrieval. | Confirm/save triggers another inference wait or auto-exports JSON. |
| Show summary counts | On Summary screen, show `Today's priority list`, urgent cases, follow-ups, and routine visits. | Counts reflect confirmed local data after save. | Counts do not update after save. |
| Summary community panel link | Tap `View community panel` from Summary. | Community Panel opens using local saved roster/visit/referral/follow-up state. | Summary counts change unexpectedly or the panel triggers model inference. |
| Priority fallback | If on-device priority queue fails or another inference is running, show the fallback card. | It says `On-device priority summary unavailable. Showing saved local visit flags.` | Mock priority reasoning is presented as RealGemma. |
| Optional multilingual RealGemma output | Only after manual validation, show a Hindi, Swahili, or Spanish patient in fully gated submission mode. | User-facing generated note/safety wording appears in the selected patient language, while protocol citation IDs stay English. | A language fails manual validation, citation IDs are translated, or the video implies all-language support. |
| Lucia Spanish note | Open `Lucia Fernandez` after manual validation and generate/review a Spanish RealGemma note. | Spanish user-facing note appears; citation IDs remain English; CHW review/save gate remains visible. | Video implies all languages, translates citation IDs, or skips review. |
| Grace Swahili routine note | Open `Grace Achieng` after manual validation and generate/review a routine no-referral Swahili RealGemma note. | Routine/no-referral card appears with Swahili safety wording and review required. | Grace gets Meena danger-sign content or a false referral. |
| Grace paper-note scan | On Grace, tap `Use sample paper note`, review extracted fields, confirm patient association, and save. | Local Gemma 4 vision extracts structured paper-note data; image bytes are not persisted; scan is data entry only. | Referral, diagnosis, treatment, cloud OCR/API, or save without CHW confirmation appears. |
| Show urgent case | Point to `Urgent Cases`. | Meena urgent case appears with concise danger signs and citation. | Urgent case missing or contains long raw paragraphs. |
| Show Offline Proof again | Point to Offline Proof on Summary. | Same offline evidence is visible after save. | Offline Proof missing on Summary. |
| Optional export | Tap `Export Summary JSON`. | Local export path appears. | Export fails or implies remote sync/cloud upload. |
| Closing line | End with: `Local patient memory + local protocol pack + CHW confirmation + offline runtime.` | Judges hear the core product claim clearly. | Closing claim mentions clinical validation, direct Gemma audio, or cloud dependency. |

## What To Say

- `Smriti is an offline CHW visit copilot.`
- `Smriti demonstrates local Android LiteRT-LM text reasoning and local Gemma 4 vision paper-note extraction.`
- `The filmed/local submission flow uses on-device Gemma for app-facing reasoning.`
- `Local patient memory plus a local protocol pack grounds the visit note.`
- `This is based on local health guidance, not diagnosis.`
- `CHW reviews and confirms before saving.`
- `RealGemma text inference has been manually validated and is available only in developer-gated mode.`
- `If on-device reasoning returns invalid output, Smriti rejects it safely, keeps the transcript editable, and does not fall back to mock clinical output.`
- `Smriti runs only one on-device Gemma request at a time for stability.`
- `The first RealGemma call can be slower because the local engine is initializing; Smriti preloads and keeps the engine warm so later calls should be faster.`
- `In our emulator/local setup, Meena generation was about 21.7 seconds and Lucia after preload/reuse was about 14.4 seconds; local retrieval, validation, and save were milliseconds. Device performance may vary.`
- `Saving is local Room/SQLite only and does not invoke Gemma.`
- `Follow-up tasks and Community Panel counts are local state and do not count as saved visits.`
- `Urgent protocol lookup is a local protocol check; it does not save a visit or create a referral flag.`
- `Patient messages are editable and shared only by user action through the Android share sheet.`
- `Selected languages demonstrated: English, Hindi, Spanish, Swahili.`
- `These are patient-specific generated note languages; Smriti does not claim full app UI translation.`
- `Protocol citation IDs remain stable in English; no cloud translation API is used.`
- `Direct Gemma audio is not used, so Smriti uses offline speech or editable transcript fallback into text reasoning.`
- `Vision scan is data-entry support only, not diagnosis.`
- `Local Gemma 4 vision paper-note extraction requires CHW review before saving.`

## What Not To Claim

- Do not claim clinical validation.
- Do not claim autonomous diagnosis or treatment.
- Do not call urgent lookup emergency AI, AI triage, diagnosis, treatment guidance, or a risk score.
- Do not claim direct Gemma 4 audio works.
- Do not say no other team has this.
- Do not claim clinical image diagnosis or referral decisions from paper-note images.
- Do not claim broad camera diagnosis.
- Do not use paper-note scanning for wounds, rashes, ultrasound, medicine strips, growth charts, or photos of people.
- Do not show mock clinical output as RealGemma.
- Do not show raw rejected RealGemma output as a clinical result.
- Do not claim broad all-language support or unsupported Amharic, Oromo, or Bangla output.
- Do not claim the whole app UI switches language unless full UI localization is implemented.
- Do not claim multilingual RealGemma output in the video until the manual multilingual harness passes for the filmed language.
- Do not claim cloud runtime, cloud ASR, remote retrieval, or model downloads.
- Do not show or use real patient data.
- Do not speed up the video in a way that fakes output. It is acceptable to shorten a visible inference wait in editing if the generated result is real and the cut is disclosed or visually obvious.
- Do not present emulator latency evidence as clinical validation.

## Release Sanity Notes

Run before filming or packaging:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Repository safety:

- Do not commit `.litertlm`, `.task`, `.tflite`, `.onnx`, `.wav`, `.mp3`, `.m4a`, or `.flac` files.
- Do not add runtime model downloads.
- Do not add cloud APIs or cloud ASR to the normal demo.
- Keep `RealGemmaAgent` as the app-facing reasoning engine.
