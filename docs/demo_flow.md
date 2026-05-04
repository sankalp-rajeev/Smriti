# Demo Flow

Use this for the 3-minute edited judge video. Reset Demo Data before filming if the emulator has accumulated saved visits. Turn on airplane mode before opening Smriti.

## Three-Minute Sequence

1. **A. Welcome screen** - Show `Smriti` and `Offline health visit assistant`, then tap `Start visits`.
2. **B. Offline Proof / setup ready** - Tap `Check offline setup`, then show local proof: works offline after setup, patient memory on device, guidance on device, RealGemma ready/setup state, paper-note scan available, no cloud APIs, direct Gemma audio not used. Return to the roster.
3. **C. Patient roster search and attention chips** - Show search, primary actions, smaller secondary actions, `Needs attention` before routine visits, patient chips, and patient-card `Note language` labels. The roster does not show technical proof details by default.
4. **D. Amara Tesfaye** - Open Amara and show the missed follow-up alert. Note this is deterministic local history, not prediction.
5. **E. Fatima Begum** - Open Fatima and show the rising BP history signal. Keep the spoken framing cautious: history signal for CHW attention, not diagnosis.
6. **F. Meena Sharma** - Select Meena, use the Hindi sample/RealGemma note path, generate a structured note, show referral suggested, local guidance citation, safety wording, CHW review, then confirm/save.
7. **G. Lucia Fernandez** - Show a Spanish RealGemma note for Lucia after manual validation. Keep citation IDs in English.
8. **H. Grace Achieng** - Show a Swahili routine/no-referral RealGemma note after manual validation.
9. **I. Grace paper-note scan** - Use `Use sample paper note`. Local Gemma vision extracts Grace, BP, symptoms, and follow-up from the synthetic note. CHW reviews and saves. Say: vision scan is data-entry support only, not diagnosis.
10. **J. End-of-day Summary** - Show urgent, follow-up, and routine priority lists. Meena should appear as urgent after the confirmed referral save.
11. **K. Close with Offline Proof** - Close on no cloud APIs, local patient memory, local guidance, RealGemma text and vision, direct Gemma audio blocked, and CHW review/confirm/save always required.

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
- `Smriti runs one on-device Gemma request at a time so the filmed path stays stable.`
- `Direct Gemma audio remains blocked, so Smriti uses offline speech or editable transcript into text reasoning.`

## Safety Details To Show

- Review screen content is editable before save.
- Referral support includes a local protocol citation.
- If RealGemma is unavailable or invalid, Smriti shows retry/setup messaging, preserves the transcript, and does not display mock clinical output.
- If another RealGemma request is running, Smriti shows `Smriti is already preparing a note. Please wait.` instead of queueing another native inference call.
- Paper-note scan uses local Gemma vision and Review Scanned Note; image bytes are not persisted.
- No cloud OCR/API is used.
- Existing patient note language is not overwritten by any default/new-patient language setting.

## Do Not Claim

- Do not claim clinical validation.
- Do not claim autonomous diagnosis or treatment.
- Do not claim direct Gemma audio works.
- Do not claim broad all-language support.
- Do not claim clinical image diagnosis, broad camera diagnosis, or referral decisions from images alone.
- Do not say no other team has this.
- Do not present mock output as RealGemma output.
- Do not show real patient data.
