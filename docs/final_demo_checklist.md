# Final Demo Checklist

Use this checklist for the final filmed or live judge demo. Keep the main demo on the default `MockGemmaAgent` path for reliability.

## Build And Install

| Check | Exact steps | Expected result | Problem signal |
| --- | --- | --- | --- |
| Build debug APK | Run `.\gradlew.bat assembleDebug`. | Build passes and creates `app/build/outputs/apk/debug/app-debug.apk`. | Build fails, APK missing, or Gradle cannot compile. |
| Install APK if needed | Install from Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk`. | App installs as Smriti on the emulator/device. | Install fails, app missing, or stale build opens. |
| Protect repo contents | Check that no `.litertlm`, `.wav`, `.mp3`, `.m4a`, `.flac`, `.tflite`, `.task`, or `.onnx` artifacts are staged. | Only source/docs/test files are staged. | Model/audio artifacts appear in git status. |

## Filming Setup

| Check | Exact steps | Expected result | Problem signal |
| --- | --- | --- | --- |
| Hide sensitive material | Close logs, terminal windows, and file explorers with local paths or secrets. | Recording shows only the app and safe docs/slides. | Logcat, shell history, secrets, or local model paths are visible. |
| Use synthetic data only | Confirm the app shows the six seeded demo patients, especially Meena Sharma. | No real patient names, phone numbers, locations, or PHI are visible. | Any real patient/PHI data appears. |
| Turn airplane mode on | Open Android quick settings and enable airplane mode before app flow. | Airplane mode icon/status is visible before the demo. | Wi-Fi/cellular appears active during the core flow. |
| Set claim boundary | Prepare the spoken line: `Protocol-grounded referral support, not diagnosis.` | Narration matches the product safety framing. | Narration implies diagnosis, treatment, or clinical validation. |

## Manual Demo Flow

| Check | Exact steps | Expected result | Problem signal |
| --- | --- | --- | --- |
| Launch app | Open Smriti after airplane mode is on. | Patient Roster loads locally. | App crashes, requires network, or roster does not load. |
| Reset demo data | Open End-of-Day Summary if needed, tap `Reset Demo Data`, then return to Patient Roster. | Saved demo visits/referrals are cleared and the six-patient synthetic roster is restored. | Old saved referrals remain or reset fails. |
| Optional register import | Tap `Load Demo Supervisor Register`, confirm import. | `6 synthetic patients imported from local supervisor register.` appears and no duplicate histories are created after repeat import. | Import asks for network/storage permission or duplicates patients endlessly. |
| Optional add patient | Tap `Add Patient`; try one offline speech prompt or type manually, then `Confirm and Add`. | Speech unavailable states keep fields editable; manual save creates a local Room patient. | Speech failure blocks manual entry or auto-saves before confirmation. |
| Show roster purpose | On Patient Roster, show `Smriti`, `Offline CHW visit copilot`, and patient list. | Purpose, offline status, and patient list are clear. | Screen looks empty, misleading, or lacks patient list. |
| Show Offline Proof | Point to `Offline Proof`. | It shows `Network required: No`, local Room/SQLite patient data, local JSON country-aware protocol retrieval, default mock reasoning, RealGemma model status, inference status, and direct Gemma audio blocked with offline speech/transcript fallback. | It implies cloud runtime, default RealGemma, or direct Gemma audio working. |
| Select Meena | Tap `Meena Sharma, 28F` / `Select Patient and View History`. | Visit screen opens for Meena. | Wrong patient opens or navigation fails. |
| Show prior history | Scroll or point to Prior Visit History. | Confirmed local history is visible before new transcript entry. | History missing after reset or latest confirmed order looks wrong. |
| Show protocol context | Point to Meena's India/INDIA protocol context with GLOBAL_CORE fallback. | The visit context is clear and non-intrusive. | Protocol context missing or claims cloud/vector retrieval. |
| Use sample transcript | Tap `Use sample danger-sign transcript`. | Editable transcript fills with severe headache, blurred vision, BP 150/95, and reduced fetal movement. | Transcript stays blank or sample button fails. |
| Optional offline speech | If shown, tap `Try Offline Speech` only if you want to demonstrate fallback. | If unavailable, friendly message appears and existing transcript remains. | Raw error code appears, transcript clears, or app saves/generates automatically. |
| Generate note | Tap `Generate Local Visit Note`. | Review screen opens quickly with a structured local note. | Generation fails, hangs, auto-saves, or leaves the Visit screen blank. |
| Show structured note | On Review screen, show editable Observation, Relevant history, Protocol-grounded support, and Suggested follow-up. | Generated content is editable before saving. | Fields are not editable or content is missing. |
| Show referral support | Point to `Referral Support`. | Referral support appears with urgency, danger signs, facility, and protocol basis. | Danger-sign case does not produce referral support. |
| Show citation | Point to `Protocol Citation`. | Citation is present and tied to local protocol text. | Citation is missing or invented-looking. |
| Show safety gate | Point to `Safety Gate`. | It says `Protocol-grounded referral support, not diagnosis.` and `CHW reviews and confirms before saving.` | Wording implies diagnosis, treatment, or autonomous action. |
| Confirm/save | Tap `Confirm CHW Review and Save`. | Visit is saved and Summary screen opens. | Save happens before confirmation, button fails, or summary does not open. |
| Show summary counts | On Summary screen, show Local Supervisor Brief, total visits, and referral flags. | Counts reflect confirmed local data after save. | Counts do not update after save. |
| Show urgent case | Point to `Urgent Cases`. | Meena urgent case appears with concise danger signs and citation. | Urgent case missing or contains long raw paragraphs. |
| Show Offline Proof again | Point to Offline Proof on Summary. | Same offline evidence is visible after save. | Offline Proof missing on Summary. |
| Optional export | Tap `Export Summary JSON`. | Local export path appears. | Export fails or implies remote sync/cloud upload. |
| Closing line | End with: `Local patient memory + local protocol pack + CHW confirmation + offline runtime.` | Judges hear the core product claim clearly. | Closing claim mentions clinical validation, direct Gemma audio, or cloud dependency. |

## What To Say

- `Smriti is an offline CHW visit copilot.`
- `The normal demo uses MockGemmaAgent for deterministic offline reliability.`
- `Local patient memory plus a local protocol pack grounds the visit note.`
- `This is protocol-grounded referral support, not diagnosis.`
- `CHW reviews and confirms before saving.`
- `RealGemma text inference has been manually validated and is available only in developer-gated mode.`
- `Direct Gemma 4 audio is blocked by the current public LiteRT-LM Android/Kotlin path, so Smriti uses offline speech or editable transcript fallback into text reasoning.`

## What Not To Claim

- Do not claim clinical validation.
- Do not claim autonomous diagnosis or treatment.
- Do not claim direct Gemma 4 audio works.
- Do not claim RealGemma is the default demo mode.
- Do not claim cloud runtime, cloud ASR, remote RAG, or model downloads.
- Do not show or use real patient data.

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
- Keep `MockGemmaAgent` as default.
