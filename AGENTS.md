# AGENTS.md

## Project

Project name: Smriti  
Product: Offline maternal-health visit copilot for community health workers.  
Goal: Build an Android MVP for the Gemma 4 Good Hackathon.

The app must work offline. No core runtime feature may require internet.

## Core demo flow

1. Open app in airplane mode.
2. Select patient "Meena, 28F".
3. Show prior visit history.
4. Record a 30-second Hindi/English voice observation.
5. Run local reasoning pipeline.
6. Generate structured visit note.
7. Flag referral if danger signs are present.
8. Show protocol citation.
9. CHW reviews and confirms.
10. Generate end-of-day supervisor summary.

## Tech stack

- Android native app
- Kotlin
- Jetpack Compose preferred for UI
- Room / SQLite for local storage
- Android TTS for offline voice output
- LiteRT / LiteRT-LM for Gemma 4 inference
- Gemma 4 E2B as MVP baseline
- Gemma 4 E4B only as stretch benchmark
- Local JSON/keyword retrieval for protocol snippets

## Important constraints

- Do not use cloud APIs for core app logic.
- Do not use Gemini API, OpenAI API, Firebase, Supabase, or remote databases.
- Do not frame the app as diagnostic AI.
- The app provides protocol-grounded documentation and referral support only.
- CHW must confirm every generated record before saving.
- Every clinical recommendation must include a source/protocol citation or be marked uncertain.
- If uncertain, the app must ask for confirmation instead of guessing.

## Engineering conventions

- Keep code simple and demo-first.
- Prefer small, testable classes.
- Separate reasoning logic from UI.
- Do not hardcode demo outputs inside UI.
- Mock Gemma locally only behind an interface so it can be replaced by real LiteRT-LM.
- Use clear names: Patient, VisitLog, ReferralFlag, ProtocolChunk.
- After every meaningful code/config change, update CONTEXT.md with:
  - what changed,
  - files touched,
  - whether build/test was run,
  - any errors,
  - next recommended step.

## Build and test commands

Use Android Studio or Gradle wrapper when available.

Common commands:
- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew connectedAndroidTest`

If Gradle wrapper is not present yet, ask before generating Android project files.

## Definition of done

A feature is done only when:
- it works offline,
- it is connected to the demo flow,
- generated outputs are editable before save,
- error cases are handled,
- and the relevant test/demo path is documented.

## MVP priority

P0:
- patient roster
- local visit history
- voice-note input or simulated audio text input
- GemmaAgent interface
- structured visit note generation
- referral flag generation
- local storage
- review/confirm screen
- end-of-day summary

P1:
- real Gemma 4 LiteRT-LM integration
- protocol RAG citations
- Android TTS
- multilingual output

P2 / stretch:
- camera input for forms
- Whisper fallback
- E4B benchmark
