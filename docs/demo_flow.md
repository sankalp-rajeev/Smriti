# Demo Flow

Use this for the live judge demo or screen recording. Reset Demo Data before filming if the emulator has accumulated test visits.

## Setup

1. Open Android quick settings.
2. Turn on airplane mode.
3. Launch Smriti.

## Script

1. **Show Patient Roster**
   - Point out the `Offline Proof` card.
   - Key lines: `Network required: No`, `Protocol source: Local asset JSON`, `Active reasoning mode: MockGemmaAgent`, `Inference: Disabled`.

2. **Select Meena**
   - Tap `Meena, 28F`.
   - Show prior visit history: borderline blood pressure and routine ANC follow-ups are already stored locally.

3. **Create Visit Observation**
   - Use `Use sample danger-sign transcript`.
   - The sample includes headache, blurred vision, high blood pressure, and reduced fetal movement.

4. **Generate Visit Note**
   - Tap the generate button.
   - Explain that the current demo uses `MockGemmaAgent` for deterministic offline reasoning.

5. **Review Referral Support**
   - Show the structured note.
   - Show referral suggestion.
   - Show protocol citation.
   - Keep wording clear: this is protocol-grounded referral support, not a diagnosis.

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
   - Emphasize no network, local protocol JSON, mock active, LiteRT-LM present, inference disabled.

9. **Optional Export**
   - Tap `Export Summary JSON`.
   - Explain the export is saved locally for later sync or supervisor sharing.

10. **Optional Reset**
    - Tap `Reset Demo Data`.
    - Confirm it clears saved mock visits/referrals and restores original Meena history.

## Closing Line

One phone, no signal, local patient memory, local protocols, CHW confirmation, and a judge-readable supervisor brief.
