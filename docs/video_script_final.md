# Smriti Final Competition Video Script

Target runtime: 2:40-2:55  
Narration pace: calm, direct, about 145-155 words/minute  
Filming rule: keep airplane mode visible early, and keep the app screen moving. Use a clean phone-frame screen recording, not raw Android Studio or emulator chrome.

## 0:00-0:20 - Memory Hook

**Narration**

"If she forgets the card, we check it in our record." An ASHA worker said that while describing how records determine what care is due. For a community health worker, memory is infrastructure. Smriti is built for that moment: when care depends on what the worker can remember, carry, and confirm.

**Exact App Screen/Actions**

- Open with a phone beside a paper notebook.
- Show Android airplane mode turning on.
- Launch Smriti.

**On-Screen Text Overlay**

For a community health worker, memory is infrastructure.

**Visual Direction**

Quiet, close-up, human. Start with the phone and paper, not a logo.

**Notes For Filming**

Use the quote as text on screen. Source in small footer: Yale Global Health Review.

## 0:20-0:38 - Stakes And Offline Reality

**Narration**

Every two minutes, a woman dies from pregnancy or childbirth complications. Most maternal deaths are preventable. But in 2024, 2.6 billion people were offline, and 1.8 billion of them lived in rural areas. A field tool cannot assume the cloud will arrive first.

**Exact App Screen/Actions**

- Keep airplane mode visible.
- Show Smriti opening successfully offline.
- Briefly show Offline Proof.

**On-Screen Text Overlay**

Offline first. Local memory. CHW confirmed.

**Visual Direction**

Cut from source/stat overlay to the live app, still offline.

**Notes For Filming**

Do not linger on statistics. The phone working offline is the proof shot.

## 0:38-0:55 - Product Loop Anchor

**Narration**

Smriti is an offline maternal-health visit copilot for community health workers. Smriti turns one home visit into local memory: a cited note, a follow-up task, a patient message, and a community view.

**Exact App Screen/Actions**

- Show the roster in a clean phone-frame recording.
- Scroll the local patient list.
- Pause on "Meena, 28F" as a synthetic demo patient.

**On-Screen Text Overlay**

Local patient memory

**Visual Direction**

Keep taps deliberate and readable. Make the roster feel like the beginning of one visit loop, not a feature list.

**Notes For Filming**

Make clear that Meena is synthetic demo data.

## 0:55-1:52 - One Visit, One Loop

**Narration**

I open Meena from the roster and see her prior visit history stored locally on the device. Today, the observation is severe headache and blurred vision. Smriti brings up cited local guidance for danger signs, then drafts a structured visit note with on-device Gemma 4 E2B. The note is not final. The health worker reviews the citation, the wording, and the referral support, edits anything that needs correction, and only then confirms the record.

**Exact App Screen/Actions**

- Tap synthetic demo patient "Meena, 28F."
- Show prior visit history.
- Enter or capture today's observation.
- Show local protocol guidance/citation.
- Tap Generate visit note.
- Show generated structured note.
- Show ReviewScreen.
- Tap confirm/save.

**On-Screen Text Overlay**

Local patient memory  
Cited local guidance  
Review before save

**Visual Direction**

This is the longest continuous app sequence. Keep it inside the phone frame. If generation takes too long, use a short cut while keeping the before/after state obvious.

**Notes For Filming**

Use the typed transcript variant unless the microphone path is stable on the filming device. Never imply audio bypasses transcript review.

## 1:52-2:28 - After The Save

**Narration**

The visit does not end when the note is saved. Smriti creates the follow-up task, prepares an editable patient leave-behind message, and updates the Community Panel from local patient, visit, referral, and follow-up data. The result is one closed loop: patient memory, cited guidance, reviewed documentation, next action, patient communication, and supervisor visibility.

**Exact App Screen/Actions**

- Show follow-up task created.
- Open patient leave-behind message.
- Show editable Copy/Share screen.
- Open Community Panel.
- Briefly show the updated local summary or supervisor view if useful.

**On-Screen Text Overlay**

Follow-up created  
Patient message prepared  
Community panel updated

**Visual Direction**

Use fast but readable cuts. This section should feel like the day closing and the system remembering what needs to happen next.

**Notes For Filming**

Show user-initiated sharing only. Do not show auto-send behavior.

## 2:28-2:52 - Technical Proof And Close

**Narration**

Gemma 4 E2B runs on-device through LiteRT-LM. Smriti uses local patient history and cited local guidance to draft the note, then checks the structure, citation, and referral wording before anything can be saved. Smriti means memory. I built it so no health worker has to carry that memory alone.

**Exact App Screen/Actions**

- Show Offline Proof again.
- Show the confirmed note or Community Panel inside the phone frame.
- End on roster or Community Panel.

**On-Screen Text Overlay**

Gemma 4 E2B on-device  
Room + local protocol JSON  
CHW confirmed

**Visual Direction**

Close with the app in hand, not a slide deck. Let the final line breathe.

**Technical Proof Note - Not Spoken Narration**

Internal validation includes the RealGemma text path, parser checks, referral-safety validation, Content.AudioBytes manual probes, lookupProtocol manual probes, and build history evidence. Keep this note in the video description or presenter notes only.

## Filming Variant A - Microphone Audio Shown

Use only if the manual audio path is stable on the filming device.

**App Actions**

- On Meena's visit screen, tap Record with Gemma.
- Speak the observation clearly.
- Stop recording.
- Show the transcript filling the editable transcript box.
- Make one small edit.
- Then tap Generate visit note.

**Narration Insert**

If audio is shown, say: "Audio fills an editable transcript. The health worker still reviews the text before Gemma drafts the note."

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

**Boundary**

This is the recommended filming path if the device audio behavior is inconsistent.

## Source Notes For Video Description

- ASHA quote: https://yaleglobalhealthreview.com/2017/05/14/consider-the-asha-a-qualitative-analysis-of-accredited-social-health-activists-experiences-in-udaipur-india/
- WHO maternal mortality fact sheet: https://www.who.int/news-room/fact-sheets/detail/maternal-mortality
- WHO/UN maternal mortality report: https://www.who.int/publications/i/item/9789240108462
- ITU internet use: https://www.itu.int/itu-d/reports/statistics/2024/11/10/ff24-internet-use/
- ITU rural internet gap: https://www.itu.int/itu-d/reports/statistics/2024/11/10/ff24-internet-use-in-urban-and-rural-areas/
