# Demo Flow

Use this for the 3-minute edited judge video. Reset Demo Data before filming if the emulator has accumulated saved visits. Turn on airplane mode before opening Smriti.

## Final Filmed Sequence

1. **Welcome / setup screen** - Show `Smriti`, `Set up today's patient list`, `Start`, `Set up patient list`, and `Help & setup`.
2. **Import or start visits** - Briefly show `Import patient register`, then tap `Start visits`.
3. **Roster** - Show `Today's focus`, search, filter chips (`All`, `Needs attention`, `Follow-up due`, `Near term`, `Routine`), main actions, setup actions, support actions, and patient cards.
4. **English typed observation** - Open Amara or Fatima to show local history/follow-up context and an editable typed observation path.
5. **Hindi typed Meena observation** - Open synthetic demo patient Meena, type the danger-sign observation, generate a cited Gemma 4 note, review the citation/referral-support wording, then confirm/save.
6. **Swahili paper-note/image scan with Grace** - Open Grace and use `Scan paper note` or a pre-prepared reviewed scan result. Say clearly: paper-note vision is data-entry support only.
7. **Follow-up task** - Show the saved follow-up task created after CHW confirmation.
8. **Patient leave-behind** - Open the editable patient message. Show Copy/Share controls, but do not auto-send.
9. **Supervisor summary / Community Panel** - Show saved local visits, follow-ups, urgent review support, patient messages, and Community Panel counts.
10. **Close with Offline Proof** - Show local Room database, local protocol pack, LiteRT-LM Gemma 4, CHW review required, and no cloud APIs after setup.

For edited waits, use an on-screen label while keeping the real generated result:

```text
On-device Gemma 4 inference - sped up for demo.
```

## Spoken Positioning

- `Smriti is an offline CHW visit copilot.`
- `Local patient memory plus cited local guidance shapes the note.`
- `Gemma 4 E2B runs on-device through LiteRT-LM after setup.`
- `CHW reviews, edits, confirms, and saves.`
- `No cloud APIs are required after setup for the filmed runtime.`
- `Gemma audio transcription fills an editable transcript only.`
- `Vision scan is paper-note data-entry support only, not diagnosis.`
- `Community Panel counts are saved local roster data, not prediction or AI triage.`
- `Urgent lookup checks local health guidance only; it does not save a visit or create a referral flag.`
- `Patient messages are reviewed by the CHW and shared only by user action.`

## Safety Details To Show

- Review screen content is editable before save.
- Referral support includes a local protocol citation.
- If on-device reasoning is unavailable or invalid, Smriti shows retry/setup messaging, preserves the transcript, and does not display mock clinical output.
- Paper-note scan uses local Gemma vision and Review Scanned Note; image bytes are not persisted.
- Follow-up tasks and Community Panel counts are separate from saved visit counts.
- Urgent protocol lookup is read-only and separate from saved visit, referral, follow-up, Summary, and Community Panel counts.
- Patient leave-behind messages are generated only after confirm/save and remain editable before sharing.
- No cloud OCR/API is used.

## Do Not Claim

- Do not claim clinical validation.
- Do not claim autonomous diagnosis or treatment.
- Do not claim treatment or dosage support.
- Do not claim direct audio diagnosis, treatment, or referral.
- Do not claim broad all-language support.
- Do not claim clinical image diagnosis, broad camera diagnosis, or referral decisions from images alone.
- Do not call lookup emergency AI, AI triage, diagnosis, treatment guidance, or a risk score.
- Do not present mock output as RealGemma output.
- Do not show real patient data.
