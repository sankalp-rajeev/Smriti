# Demo Flow

Use this for the 3-minute edited judge video. Reset Demo Data before filming if the emulator has accumulated saved visits. Turn on airplane mode before opening Smriti.

## Three-Minute Sequence

1. **A. Welcome screen** - Show `Smriti` and `Offline health visit assistant`, then tap `Start visits`.
2. **B. Offline Proof / setup ready** - Tap `Check offline setup`, then show local proof: works offline after setup, patient memory on device, guidance on device, RealGemma ready/setup state, paper-note scan available, Gemma audio transcription validated, no cloud APIs. Return to the roster.
3. **C. Patient roster search and attention chips** - Show search, primary actions, smaller secondary actions, `Needs attention` before routine visits, patient chips, and patient-card `Note language` labels. The roster does not show technical proof details by default.
4. **D. Urgent protocol lookup** - Tap `Urgent protocol lookup`. Select `Severe headache` and `Blurred vision`, then tap `Check urgent guidance`. Show local health guidance, citation, safety copy, and the line that no visit/referral/follow-up is saved from lookup.
5. **E. Community panel** - Tap `Community panel`. Show `Today's focus`, `Needs attention`, follow-up counts, pregnancy stage, languages/countries, and the local priority list. Say: this is saved-on-device panel management, not prediction.
6. **F. Amara Tesfaye** - Open Amara and show the missed follow-up alert. Note this is deterministic local history, not prediction.
7. **G. Fatima Begum** - Open Fatima and show the rising BP history signal. Keep the spoken framing cautious: history signal for CHW attention, not diagnosis.
8. **H. Meena Sharma** - Select synthetic demo patient Meena, use the Hindi sample/RealGemma note path, generate a structured note, show referral suggested, local guidance citation, safety wording, CHW review, then confirm/save.
9. **I. Patient leave-behind message** - From the post-save Summary, tap `Prepare patient message`; show editable text and the user-initiated Share/Copy controls. Do not send a message during filming unless intentionally showing the Android chooser.
10. **J. Lucia Fernandez** - Show a Spanish RealGemma note for Lucia after manual validation. Keep citation IDs in English.
11. **K. Grace Achieng** - Show a Swahili routine/no-referral RealGemma note after manual validation.
12. **L. Grace paper-note scan** - Use `Use sample paper note`. Local Gemma vision extracts Grace, BP, symptoms, and follow-up from the synthetic note. CHW reviews and saves. Say: vision scan is data-entry support only, not diagnosis.
13. **M. End-of-day Summary** - Show urgent, follow-up, and routine priority lists. Synthetic demo patient Meena should appear as urgent after the confirmed referral save; `View community panel` is available from Summary.
14. **N. Close with Offline Proof** - Close on no cloud APIs, local patient memory, local guidance, RealGemma text + vision + audio transcription validated, and CHW review/confirm/save always required.

For edited waits, use an on-screen label while keeping the real generated result:

```text
On-device Gemma 4 inference - sped up for demo.
```

## Spoken Positioning

- `Smriti demonstrates local Android LiteRT-LM text reasoning and local Gemma 4 vision paper-note extraction.`
- `Selected languages demonstrated: English, Hindi, Spanish, Swahili.`
- `Patient-specific generated note languages are supported; full app UI translation is not claimed.`
- `Vision scan is data-entry support only, not diagnosis.`
- `CHW review, confirm, and save is always required.`
- `Community panel counts are local saved roster data, not AI triage or prediction.`
- `Urgent protocol lookup checks local health guidance only; it does not save a visit or create a referral flag.`
- `Patient messages are reviewed by the CHW and shared only by user action through Android's share sheet.`
- `Smriti runs one on-device Gemma request at a time so the filmed path stays stable.`
- `Gemma audio transcription fills an editable transcript only; clinical note generation still goes through text reasoning and CHW review after the worker taps Generate.`

## Safety Details To Show

- Review screen content is editable before save.
- Referral support includes a local protocol citation.
- If RealGemma is unavailable or invalid, Smriti shows retry/setup messaging, preserves the transcript, and does not display mock clinical output.
- If another RealGemma request is running, Smriti shows `Smriti is already preparing a note. Please wait.` instead of queueing another native inference call.
- Paper-note scan uses local Gemma vision and Review Scanned Note; image bytes are not persisted.
- Follow-up tasks and Community Panel counts are separate from saved visit counts.
- Urgent protocol lookup is read-only and separate from saved visit, referral, follow-up, Summary, and Community Panel counts.
- Patient leave-behind messages are generated only after confirm/save and remain editable before sharing.
- No cloud OCR/API is used.
- Existing patient note language is not overwritten by any default/new-patient language setting.

## Do Not Claim

- Do not claim clinical validation.
- Do not claim autonomous diagnosis or treatment.
- Do not claim direct audio diagnosis, treatment, or referral.
- Do not claim broad all-language support.
- Do not claim clinical image diagnosis, broad camera diagnosis, or referral decisions from images alone.
- Do not call the lookup emergency AI, AI triage, diagnosis, treatment guidance, or a risk score.
- Do not say no other team has this.
- Do not present mock output as RealGemma output.
- Do not show real patient data.
