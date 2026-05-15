# Smriti Final Competition Video Script

Target runtime: 2:45-2:58  
Narration pace: calm, direct, about 145-155 words/minute  
Filming rule: keep airplane mode visible early, and use a clean phone-frame screen recording, not raw Android Studio or emulator chrome.

## 0:00-0:20 - Memory Hook

**Narration**

"If she forgets the card, we check it in our record." An ASHA worker said that while describing how records determine what care is due. For a community health worker, memory is infrastructure. Smriti is built for the health worker who shows up, even when there is no signal, no EHR, and too many visits to remember alone.

**Exact App Screen/Actions**

- Open with a phone beside a paper notebook.
- Show Android airplane mode turning on.
- Launch Smriti to Welcome.

**On-Screen Text Overlay**

For a community health worker, memory is infrastructure.

## 0:20-0:42 - Start The Day

**Narration**

Smriti starts with the day, not a chatbot. The health worker can start visits, import a supervisor-provided patient register, add one patient manually, or check offline setup. The register and patient memory are stored on this device.

**Exact App Screen/Actions**

- Show Welcome sections: Start, Set up patient list, Help & setup.
- Briefly show Import patient register or Start visits.
- Tap Start visits.

**On-Screen Text Overlay**

Set up today's patient list  
Saved on this device

## 0:42-1:02 - Roster And Local Memory

**Narration**

The roster is a daily dashboard. Search and filters help the CHW find needs attention, follow-up due, near term, or routine patients. Smriti turns one home visit into local memory: a cited note, a follow-up task, a patient message, and a community view.

**Exact App Screen/Actions**

- Show roster metrics and filter chips.
- Tap a filter, then clear or return to All.
- Show Amara or Fatima briefly for an English typed observation or local history signal.
- Open synthetic demo patient Meena.

**On-Screen Text Overlay**

Local patient memory

## 1:02-1:47 - Meena Visit Loop

**Narration**

For Meena, I type today's Hindi observation: severe headache, blurred vision, high blood pressure, and reduced fetal movement. Smriti retrieves cited local guidance, then Gemma 4 E2B runs on-device through LiteRT-LM to draft a structured note. The note is not final. The health worker reviews the citation, structure, and referral-support wording before anything can be saved.

**Exact App Screen/Actions**

- Show Meena's local prior history.
- Type or paste the Hindi/English observation into the editable transcript.
- Tap Generate visit note.
- Show ReviewScreen with generated note, local guidance/citation, referral support, and confirm/save.
- Confirm and save.

**On-Screen Text Overlay**

Cited local guidance  
Review before save

## 1:47-2:12 - Closing The Loop

**Narration**

After save, the visit becomes action. Smriti creates the follow-up task, prepares an editable patient leave-behind message, and updates supervisor and community views from local Room data. Nothing is auto-sent. Nothing is saved without CHW confirmation.

**Exact App Screen/Actions**

- Show follow-up task.
- Open Patient message for review.
- Show Copy and Share controls without sending.
- Open Summary, then Community Panel.

**On-Screen Text Overlay**

Follow-up created  
Patient message prepared  
Community panel updated

## 2:12-2:34 - Paper Note And Boundaries

**Narration**

Smriti also supports paper-note data entry. For Grace, a paper-note scan can extract draft fields for review. Paper-note scan is CHW-reviewed data-entry support only, not clinical image diagnosis. It does not recommend treatment or create a referral from an image alone.

**Exact App Screen/Actions**

- Open Grace.
- Show Scan paper note or the reviewed scan result if already prepared.
- Show Review Scanned Note briefly.

**On-Screen Text Overlay**

Vision data-entry only  
CHW review required

## 2:34-2:58 - Technical Proof And Close

**Narration**

Smriti runs after setup without cloud APIs. Local patient history lives in Room/SQLite. Local JSON protocols provide country and region-aware citations. Gemma audio fills an editable transcript only. Clinical note generation still requires CHW review and a separate Generate action. Paper-note scan is CHW-reviewed data-entry support only, not clinical image diagnosis. Every generated record goes through review before save. Smriti means memory. I built it so no health worker has to carry that memory alone.

**Exact App Screen/Actions**

- Show Offline Proof.
- End on Community Panel or roster.

**On-Screen Text Overlay**

Gemma 4 E2B on-device  
Room + local protocol JSON  
No cloud APIs after setup

## Technical Proof Note - Not Spoken Narration

Internal validation includes the RealGemma text path, parser checks, referral-safety validation, Content.AudioBytes manual probes, lookupProtocol manual probes, and build history evidence. Keep this note in the video description or presenter notes only.

## Filming Variant A - Microphone Audio Shown

Use only if the manual audio path is stable on the filming device.

**App Actions**

- On a Visit screen, tap Record observation.
- Speak the observation clearly.
- Stop recording.
- Show the transcript filling the editable transcript box.
- Make one small edit.
- Then tap Generate visit note.

**Narration Insert**

If audio is shown, say: "Gemma audio fills an editable transcript only. Clinical note generation still requires CHW review and a separate Generate action."

**Boundary**

Do not imply the recording creates referral-support wording by itself, saves by itself, or bypasses the editable transcript.

## Filming Variant B - Typed Editable Transcript

Use this if microphone audio is skipped or unstable.

**App Actions**

- Type or paste the observation into the transcript field.
- Optionally say it represents a recorded field observation.
- Tap Generate visit note.

**Narration Insert**

"For this recording, I am using the typed editable transcript path. The same safety path still applies: local guidance, cited note generation, review, and health worker confirm/save."

## Source Notes For Video Description

- ASHA quote: https://yaleglobalhealthreview.com/2017/05/14/consider-the-asha-a-qualitative-analysis-of-accredited-social-health-activists-experiences-in-udaipur-india/
- WHO maternal mortality fact sheet: https://www.who.int/news-room/fact-sheets/detail/maternal-mortality
- WHO/UN maternal mortality report: https://www.who.int/publications/i/item/9789240108462
- ITU internet use: https://www.itu.int/itu-d/reports/statistics/2024/11/10/ff24-internet-use/
- ITU rural internet gap: https://www.itu.int/itu-d/reports/statistics/2024/11/10/ff24-internet-use-in-urban-and-rural-areas/
