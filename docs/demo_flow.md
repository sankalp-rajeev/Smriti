# Demo Flow

Use this for the recorded judge demo. Reset Demo Data before filming if the emulator has accumulated saved visits.

## Setup

1. Turn on airplane mode.
2. Launch Smriti.
3. If the Welcome screen appears, show it briefly and tap `Start visits`.
4. If `One-time setup needed` appears, explain that the model is sideloaded by a supervisor; for a setup demo, tap `Continue without model (demo mode)`. For the RealGemma filmed path, install the model first so this screen does not appear.

## Script

1. **Welcome**
   - Show `Smriti` and `Offline health visit assistant`.
   - Say: Smriti helps health workers remember patient history, check local health guidance, prepare visit notes, and decide follow-up. It does not diagnose.
   - Optional: open `View user guide`, then return.

2. **Patient Roster**
   - Show search near the top.
   - Show primary actions: `Add patient`, `End-of-day summary`, `Import register`, and `User guide`.
   - Show language selector. Selecting a language changes the next visit output language without changing saved records.
   - Point out sorted sections: `Needs attention` before `Routine visits`.
   - Confirm chips:
     - Amara: `Follow-up due`
     - Fatima: `History signal`
     - Grace: `Routine`
     - Meena after saving a referral: `Referral saved`

3. **Alert Cards**
   - Open Amara and show `Missed follow-up` above the visit input.
   - Open Fatima and show `History signal` above the visit input.
   - Open Grace and confirm no false history signal appears.

4. **Meena Visit**
   - Open `Meena Sharma, 28F`.
   - Show patient header, then any alert cards, then `What to do now`.
   - Tap `Use sample visit transcript`. The sample is Meena-specific and includes severe headache, blurred vision, BP 150/95, and reduced fetal movement.
   - Prior history is compact and lower on the screen; expand `Show patient history` if useful.

5. **Optional Paper Note Scan**
   - Open `Grace Achieng, 26F` if you want to show the safe scan path.
   - Tap `Use sample paper note`.
   - Loading should show `Reading paper note...` and `Extracting visit details...`.
   - Review Scanned Note should show patient name, date, BP, symptoms, follow-up plan, and the confidence message without raw enum text.
   - Confirm the association before saving. Say: this is data entry from a synthetic paper note, not diagnosis or referral advice.

6. **Generate Note**
   - Tap `Generate visit note`.
   - Loading card should show calm progress: reading history, checking local guidance, running on-device Gemma, preparing note.
   - If RealGemma is unavailable or invalid, Smriti shows `Note could not be prepared`, preserves the transcript, and does not show mock output.

7. **Review**
   - Show `Review visit note`.
   - Read the safety line: `Smriti does not diagnose. Review before saving.`
   - For danger signs, show `Referral suggested`.
   - For routine visits, show `No referral flag`.
   - For incomplete observations, show `More information needed`.
   - Expand `How was this prepared?` to show today's observation, prior visit count, local guidance country, on-device Gemma, and raw guidance ID.

8. **Confirm And Save**
   - Edit fields if useful.
   - Tap `Confirm and save`.
   - Say: nothing is saved until the health worker reviews and confirms.

9. **End-Of-Day Summary**
   - Show `Today's priority list`.
   - Show `Urgent cases`, `Follow-ups`, and `Routine visits`.
   - If the on-device priority queue is unavailable, the screen must show `On-device summary unavailable. Local visit counts are shown below.`
   - Expand `Local proof` if needed.

10. **Offline Proof**
   - Point out:
     - `Works offline after setup`
     - `Patient memory: saved on this device`
     - `Health guidance: stored on this device`
     - `On-device Gemma: ready` or `Setup needed`
     - `Cloud APIs: none`
     - `Direct Gemma audio: not used`
     - `Paper note scan: Available`
     - `Cloud OCR: none`
   - Paper-note scan is local Gemma vision data entry only. Do not claim image diagnosis.

11. **Optional Reset**
    - Tap `Reset Demo Data`.
    - Confirm the dialog says it will clear saved visits and restore the original patient list.

## Closing Line

One phone, no signal, local patient memory, local health guidance, on-device Gemma reasoning, and health-worker review before saving.

## Do Not Claim

- Do not claim clinical validation.
- Do not claim autonomous diagnosis or treatment.
- Do not claim direct Gemma audio works.
- Do not claim clinical image diagnosis or referral decisions from paper-note images.
- Do not use paper-note scanning for wounds, rashes, ultrasound, medicine strips, growth charts, or photos of people.
- Do not present mock output as RealGemma output.
- Do not show real patient data.
