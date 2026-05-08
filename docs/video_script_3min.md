# Three-Minute Video Script

## 0:00-0:20 Problem Hook

"An ASHA worker visits a pregnant mother with headache and blurred vision. She writes it on paper, then moves to the next house. Days later, the danger signs are obvious in hindsight. The problem was not effort. It was no EHR, no signal, and no reliable way to remember every prior visit."

Show: field setting, paper note, Android phone entering airplane mode.

## 0:20-0:45 Why Existing Tools Fail

"Hospital AI scribes assume WiFi, EHR access, and doctors. Cloud chatbots assume connectivity. CHWs need the opposite: local patient memory, local protocols, local documentation, and a workflow that works without internet."

Show: airplane mode still on, Smriti launching.

## 0:45-1:45 Live App Demo

1. Show Patient Roster and Offline Proof.
2. Open `Urgent protocol lookup`; select severe headache and blurred vision; show local health guidance with citation and no automatic save.
3. Open `Community panel` and show local caseload counts: follow-ups, urgent review saved, pregnancy stage, languages, and today's focus.
4. Select `Meena, 28F`.
5. Show prior ANC visit history.
6. Use the sample danger-sign transcript.
7. Generate the visit note.
8. Show structured note, referral suggestion, and protocol citation.
9. Confirm/save the visit.
10. Open the patient message, show it is editable, then return.
11. Open End-of-Day Supervisor Summary.
12. Show concise urgent case, `View community panel`, and Offline Proof again.

Voiceover: "Every generated record is reviewed by the CHW before saving. The wording is protocol-grounded support, not diagnosis."

## 1:45-2:20 Technical Depth

"Smriti is Android native: Kotlin, Jetpack Compose, Room/SQLite, local protocol JSON, app-private voice notes, local JSON export, and Android TTS. The filmed local flow uses RealGemma text reasoning through LiteRT-LM after local setup. If the model or gates are missing, Smriti shows a retry/setup state instead of mock clinical output."

Show: Offline Proof lines and maybe a quick architecture slide.

## 2:20-2:45 Impact

"There are millions of CHWs globally. Smriti gives them local urgent guidance lookup, a local memory for each patient, a protocol citation for each recommendation, a patient message after the visit, a village panel for follow-up loops, and a supervisor-ready summary at the end of the day, even when the phone has no signal."

Show: community panel, supervisor summary, and local export.

## 2:45-3:00 Closing Line

"Smriti is not another ambient scribe and not another chatbot. It is a local-first maternal-health visit copilot for the health worker with no EHR, no signal, and no backup."

On-screen stack: `Android native`, `Room/SQLite`, `LiteRT-LM ready`, `Offline`, `CHW confirmed`.
