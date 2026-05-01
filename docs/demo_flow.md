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
   - Key lines: `Network required: No`, `Patient data: Local Room/SQLite`, `Protocol source: Local JSON; country-aware retrieval`, `Active reasoning mode: MockGemmaAgent`, `Inference: Disabled by default`.
   - Show the six synthetic patients and, if useful, tap `Load Demo Supervisor Register` to demonstrate local asset import.
   - Optional: tap `Add Patient` to show offline speech registration prompts and manual fallback, then return to the roster.

2. **Select Meena**
   - Tap `Meena Sharma, 28F`.
   - Show prior visit history: borderline blood pressure and routine ANC follow-ups are already stored locally.
   - Point out Meena's India/INDIA protocol context with GLOBAL_CORE fallback.

3. **Create Visit Observation**
   - Use `Use sample danger-sign transcript`.
   - The sample includes headache, blurred vision, high blood pressure, and reduced fetal movement.

4. **Generate Visit Note**
   - Tap the generate button.
   - Explain that the current demo uses `MockGemmaAgent` for deterministic offline reasoning.

5. **Review Referral Support**
   - Show the structured note.
   - Show `Referral Support`.
   - Show protocol citation.
   - Read the safety line: `Protocol-grounded referral support, not diagnosis.`
   - Point out `CHW reviews and confirms before saving.`

6. **Confirm And Save**
   - Edit fields only if useful for the demo.
   - Confirm/save the record.
   - Mention that CHW confirmation is required before data is saved.

7. **Open End-of-Day Supervisor Summary**
   - Show total visits and referral count.
   - Show `Urgent Cases`, for example:
     `Meena - SAME_DAY - headache, blurred vision, high blood pressure, reduced fetal movement. Citation: ...`

8. **Show Offline Proof Again**
   - Confirm the same proof appears on Summary.
   - Emphasize no network, local Room/SQLite data, local JSON protocol retrieval, mock active by default, RealGemma developer-only, and direct Gemma audio blocked with offline speech/transcript fallback.

9. **Optional Export**
   - Tap `Export Summary JSON`.
   - Explain the export is saved locally for later sync or supervisor sharing.

10. **Optional Reset**
    - Tap `Reset Demo Data`.
    - Confirm it clears saved mock visits/referrals and restores the clean six-patient synthetic roster.

## Closing Line

One phone, no signal, local patient memory, local protocols, CHW confirmation, and a judge-readable supervisor brief.

## Do Not Claim

- Do not claim clinical validation.
- Do not claim direct Gemma 4 audio works.
- Do not imply autonomous diagnosis or treatment.
- Do not present RealGemma text mode as the default demo path; it remains developer-only and gated.
- Do not present the Phase A language/country metadata as validated multilingual clinical output.
