# Three-Minute Video Script

## 0:00-0:20 Problem Hook

"An ASHA worker visits a pregnant mother with headache and blurred vision. She writes it on paper, then moves to the next house. Days later, the danger signs are obvious in hindsight. The problem was not effort. It was no EHR, no signal, and no reliable way to remember every prior visit."

Show: field setting, paper note, Android phone entering airplane mode.

## 0:20-0:45 Why Existing Tools Fail

"Hospital AI scribes assume WiFi, EHR access, and doctors. Cloud chatbots assume connectivity. CHWs need the opposite: local patient memory, local protocols, local documentation, and a workflow that works without internet."

Show: airplane mode still on, Smriti launching.

## 0:45-1:45 Live App Demo

1. Show Patient Roster and Offline Proof.
2. Select `Meena, 28F`.
3. Show prior ANC visit history.
4. Use the sample danger-sign transcript.
5. Generate the visit note.
6. Show structured note, referral suggestion, and protocol citation.
7. Confirm/save the visit.
8. Open End-of-Day Supervisor Summary.
9. Show concise urgent case and Offline Proof again.

Voiceover: "Every generated record is reviewed by the CHW before saving. The wording is protocol-grounded support, not diagnosis."

## 1:45-2:20 Technical Depth

"Smriti is Android native: Kotlin, Jetpack Compose, Room/SQLite, local protocol JSON, app-private voice notes, local JSON export, and Android TTS. Reasoning is behind a `GemmaAgent` interface. Today the demo-safe default is `MockGemmaAgent`; LiteRT-LM is pinned and readiness is visible, but real model loading and inference are disabled until controlled device testing."

Show: Offline Proof lines and maybe a quick architecture slide.

## 2:20-2:45 Impact

"There are millions of CHWs globally. Smriti gives them a local memory for each patient, a protocol citation for each recommendation, and a supervisor-ready summary at the end of the day, even when the phone has no signal."

Show: supervisor summary and local export.

## 2:45-3:00 Closing Line

"Smriti is not another ambient scribe and not another chatbot. It is a local-first maternal-health visit copilot for the health worker with no EHR, no signal, and no backup."

On-screen stack: `Android native`, `Room/SQLite`, `LiteRT-LM ready`, `Offline`, `CHW confirmed`.
