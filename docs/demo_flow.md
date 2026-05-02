# Demo Flow

Use this for the live judge demo or screen recording. Reset Demo Data before filming if the emulator has accumulated test visits.

## Setup

1. Open Android quick settings.
2. Turn on airplane mode.
3. Launch Smriti.

## Script

1. **Show Patient Roster**
   - Start on the purpose line: `Offline CHW visit copilot`.
   - Point out `Local patient memory + local protocol pack.`
   - Point out the `Offline Proof` card.
   - Key lines: `Network required: No`, `Patient data: Local Room/SQLite`, `Protocol source: Local JSON; country-aware retrieval`, `Active reasoning mode: RealGemmaAgent`, `RealGemma text mode: Active` or `Setup required`.
   - Show the six synthetic patients and, if useful, tap `Import Supervisor Register` to demonstrate local asset import.
   - Point out patient output language labels: Meena/Priya Hindi, Grace Swahili, Lucia Spanish, Fatima/Amara English.
   - Optional: tap `Add Patient` to show offline speech registration prompts and manual fallback, then return to the roster.

2. **Optional Phase B Memory Shots**
   - Select `Amara Tesfaye, 30F`; show the missed follow-up alert and its `Mark Confirmed` / `Note as Ongoing` actions.
   - Return and select `Fatima Begum, 24F`; show the cautious BP history signal.
   - Do not describe either card as a diagnosis or prediction.

3. **Select Meena**
   - Tap `Meena Sharma, 28F`.
   - Show prior visit history: borderline blood pressure and routine ANC follow-ups are already stored locally.
   - Point out Meena's India/INDIA protocol context with GLOBAL_CORE fallback.

4. **Create Visit Observation**
   - Use `Use sample danger-sign transcript`.
   - The sample includes headache, blurred vision, high blood pressure, and reduced fetal movement.

5. **Generate Visit Note**
   - Tap the generate button.
   - Explain that the filmed/local submission flow uses RealGemma text reasoning. If setup is missing or output is invalid, the app shows retry/setup messaging instead of mock output.
   - If filming the gated RealGemma multilingual path, mention that `preferredLanguage` controls selected patient-specific output language and protocol citation IDs stay in English.

6. **Review Referral Support**
   - Show the structured note.
   - Show `Referral Support`.
   - Show protocol citation.
   - Read the safety line: `Protocol-grounded referral support, not diagnosis.`
   - Point out `CHW reviews and confirms before saving.`

7. **Confirm And Save**
   - Edit fields only if useful for the demo.
   - Confirm/save the record.
   - Mention that CHW confirmation is required before data is saved.

8. **Open End-of-Day Supervisor Summary**
   - Show total visits and referral count.
   - Show `Urgent Cases`, for example:
     `Meena - SAME_DAY - headache, blurred vision, high blood pressure, reduced fetal movement. Citation: ...`
   - In fully gated submission mode, show `RealGemma Priority Follow-Up Queue`; if it is unavailable, show the deterministic fallback message and local summary below.

9. **Show Offline Proof Again**
   - Confirm the same proof appears on Summary.
   - Emphasize no network, local Room/SQLite data, local JSON protocol retrieval, RealGemmaAgent required for reasoning, and direct Gemma audio blocked with offline speech/transcript fallback.

10. **Optional Export**
   - Tap `Export Summary JSON`.
   - Explain the export is saved locally for later sync or supervisor sharing.

11. **Optional Reset**
    - Tap `Reset Demo Data`.
    - Confirm it clears saved visits/referrals and restores the clean six-patient synthetic roster.

## Closing Line

One phone, no signal, local patient memory, local protocols, CHW confirmation, and a judge-readable supervisor brief.

## Do Not Claim

- Do not claim clinical validation.
- Do not claim direct Gemma 4 audio works.
- Do not imply autonomous diagnosis or treatment.
- Do not present mock output as RealGemma output.
- Do not claim broad all-language support.
- Do not claim Amharic, Oromo, or Bangla output; Amara/Ethiopia and Fatima/Bangladesh remain English.
- Do not present multilingual RealGemma output as video-ready until the manual multilingual harness passes for the filmed language.
