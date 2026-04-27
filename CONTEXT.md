# Smriti Project Context

## Current Goal
Field Tool Milestone: make Smriti feel like a real offline CHW field tool while still using mock reasoning.

## Current Status
The repository now has a working Android Kotlin + Jetpack Compose mock MVP. It supports local 30-second voice-note recording to app-private storage, simulated transcript reasoning, offline JSON protocol grounding, CHW review/confirm, Android TTS readout, local JSON export, demo reset, and an Offline Proof section. It still uses `MockGemmaAgent`; no real Gemma/LiteRT/Whisper/camera/cloud features are present.

## Completed Tasks
- [x] Read `AGENTS.md`.
- [x] Read `smriti_prd.md`.
- [x] Started root-level project context tracking.
- [x] Updated `AGENTS.md` with the `CONTEXT.md` maintenance rule.
- [x] Created root Gradle files and an `app` module.
- [x] Added Room entities/DAOs for patients, visits, referral flags, and protocol chunks.
- [x] Added `GemmaAgent`, `MockGemmaAgent`, visit reasoning, and supervisor summary models.
- [x] Added a simple keyword `ProtocolRetriever`.
- [x] Added Compose screens for patient list, visit entry/history, review/confirm, and supervisor summary.
- [x] Added a no-op `VoiceOutput` interface for future offline TTS integration.
- [x] Attempted Gradle build commands.
- [x] Added root `gradle.properties` with AndroidX, Jetifier, JVM args, and Kotlin code style settings.
- [x] App launched successfully on Pixel 9 Pro API 35 emulator.
- [x] Mock supervisor summary verified on emulator.
- [x] Polished mock MVP labels for the judge demo flow.
- [x] Verified `assembleDebug` after UI copy polish.
- [x] Added focused unit tests for `MockGemmaAgent`.
- [x] Added focused unit tests for `ProtocolRetriever`.
- [x] Verified `testDebugUnitTest` passes.
- [x] Fixed ReviewScreen button layout so "Edit Observation" is a normal full-width button below the header.
- [x] Split ReviewScreen generated note into judge-readable editable sections: Observation, Relevant history, Protocol-grounded support, and Protocol citation.
- [x] Removed repetitive "Assessment support" wording from mock structured notes.
- [x] Added a SummaryScreen "Reset Demo Data" action that clears saved mock visits/referral flags and restores original demo history.
- [x] Verified `testDebugUnitTest` and `assembleDebug` after UI/demo polish.
- [x] Added offline maternal-health protocol JSON corpus in app assets.
- [x] Updated `ProtocolRetriever` to load and parse the offline asset corpus.
- [x] Updated `MockGemmaAgent` to use retrieved protocol chunks for citations/referral basis.
- [x] Added safe no-match behavior: no invented citation, uncertain result, and CHW/supervisor confirmation prompt.
- [x] Updated unit tests for asset-backed protocol retrieval and no-citation fallback.
- [x] Added VisitScreen voice-note style UI shell without real audio capture.
- [x] Added mock Start/Stop voice note state, local listening indicator, 30-second chunk label, sample danger-sign transcript button, and simulated transcript label.
- [x] Verified `testDebugUnitTest` and `assembleDebug` after voice-note UI shell.
- [x] Added Android `RECORD_AUDIO` permission and runtime permission request.
- [x] Implemented `AudioRecorder` for local app-private `.m4a` voice notes with 30-second max duration.
- [x] Added optional voice-note metadata to `VisitLog`: `audioFilePath`, `audioDurationSeconds`, and `transcriptSource`.
- [x] Persisted voice-note metadata when a CHW confirms/saves a visit.
- [x] Implemented offline Android TTS through `VoiceOutput`.
- [x] Added ReviewScreen TTS button for referral suggestions and SummaryScreen TTS button for supervisor summaries.
- [x] Added local JSON export for current visit note and end-of-day supervisor summary.
- [x] Added Offline Proof section for judge/demo verification.
- [x] Added unit tests for `VisitLog` audio metadata and JSON export contents.
- [x] Verified `testDebugUnitTest` and `assembleDebug` for the Field Tool Milestone.

## Active Tasks
- [ ] Install the latest debug build on the Pixel 9 Pro API 35 emulator and verify microphone permission, local recording, TTS, JSON exports, and Offline Proof.
- [ ] Start the LiteRT/Gemma integration spike behind the existing `GemmaAgent` interface.

## Blockers / Issues
- Gradle wrapper now exists; direct sandboxed wrapper runs can still fail until Gradle can access the normal user-level `.gradle` cache.
- A standalone `gradle` command is not installed in the current shell.
- Build was not started because `gradlew.bat` was missing and `gradle assembleDebug` failed with `gradle` not recognized.
- Android Studio build/sync reported: AndroidX dependencies are detected but `android.useAndroidX` is not enabled.
- First local `.\gradlew.bat assembleDebug` attempt in the sandbox failed because Gradle could not create a lock file under `C:\Users\CodexSandboxOffline\.gradle\wrapper\dists\...`.
- First local `.\gradlew.bat testDebugUnitTest` attempt in the sandbox failed for the same Gradle user-cache lock-file reason; rerun with normal user-level Gradle cache access worked.
- Initial unit test run found one bad "normal visit" fixture because it contained the danger-sign keyword "bleeding"; the fixture was corrected and the tests passed.
- Local `.\gradlew.bat testDebugUnitTest` still needs normal user-level Gradle cache access in this sandbox; the first sandboxed attempt failed on the known lock-file path, then passed with cache access.
- Android compile emits a warning that the no-arg `MediaRecorder()` constructor is deprecated. It still compiles for the current min API 26 MVP; this can be modernized later with API-conditional construction.
- Android TTS availability depends on the emulator/device installed TTS engine and language data. The UI reports unavailable/initializing states instead of failing.
- Real ASR is not implemented. Voice-note transcripts remain simulated and saved audio is marked `REAL_ASR_PENDING`.
- `git status` is blocked by Git dubious ownership because the repository is owned by `Sankalps-Razer/rajee` while the sandbox user is `Sankalps-Razer/CodexSandboxOffline`.

## Architecture Decisions
- Build Android native with Kotlin and Jetpack Compose to match the PRD and demo needs.
- Keep all MVP core behavior offline with local mock data and no network APIs.
- Put Gemma behavior behind a `GemmaAgent` interface so LiteRT-LM can replace `MockGemmaAgent` later.
- Use Room entities/DAOs as the local persistence boundary and seed demo data on first app launch.
- Use a simple in-app screen state machine instead of adding Navigation Compose for the first scaffold.
- Use a flat keyword `ProtocolRetriever` for protocol chunks until the P1 local RAG corpus is added.

## File Map
- `AGENTS.md` - project instructions for Codex and engineering constraints.
- `smriti_prd.md` - product requirements and demo plan.
- `CONTEXT.md` - living project memory updated after meaningful changes.
- `settings.gradle.kts` - Gradle project/module registration.
- `build.gradle.kts` - root Android/Kotlin plugin versions.
- `app/build.gradle.kts` - Android app module configuration and dependencies.
- `app/src/main/AndroidManifest.xml` - Android app manifest.
- `app/src/main/java/com/smriti/clinicalscribe/MainActivity.kt` - app entry point, local seeding, runtime permission, screen flow, save/summary wiring, TTS/export wiring, and demo reset wiring.
- `app/src/main/java/com/smriti/clinicalscribe/audio/` - local app-private voice-note recording and voice-note metadata.
- `app/src/main/java/com/smriti/clinicalscribe/data/` - Room database, entities, DAOs, and demo seed data.
- `app/src/main/java/com/smriti/clinicalscribe/export/JsonExporter.kt` - local app-private JSON export for visits and supervisor summaries.
- `app/src/main/java/com/smriti/clinicalscribe/reasoning/` - Gemma interface, mock implementation, and reasoning result models.
- `app/src/main/java/com/smriti/clinicalscribe/rag/` - protocol chunk model and keyword retriever.
- `app/src/main/java/com/smriti/clinicalscribe/ui/` - Compose MVP screens, including VisitScreen mock voice-note shell.
- `app/src/main/java/com/smriti/clinicalscribe/tts/` - Android TTS-backed offline voice output abstraction.
- `app/src/main/assets/protocols/maternal_health_demo_protocols.json` - offline deterministic maternal-health demo protocol corpus with danger-sign chunks and referral guidance.
- `app/src/test/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgentTest.kt` - deterministic unit tests for local mock visit-note/referral behavior.
- `app/src/test/java/com/smriti/clinicalscribe/rag/ProtocolRetrieverTest.kt` - deterministic unit tests for local protocol keyword retrieval.
- `app/src/test/java/com/smriti/clinicalscribe/data/VisitLogTest.kt` - unit test for optional audio metadata on visit logs.
- `app/src/test/java/com/smriti/clinicalscribe/export/JsonExporterTest.kt` - unit test for visit JSON protocol citation and referral flag export.

## Next Steps
1. Install the latest debug build on the Pixel 9 Pro API 35 emulator.
2. Verify microphone permission request, local 30-second recording, saved filename/duration, CHW review, TTS buttons, JSON export paths, and Offline Proof.
3. Confirm the sample transcript still generates the protocol-grounded referral flow.
4. Begin the LiteRT/Gemma integration spike behind `GemmaAgent`.
5. Keep `MockGemmaAgent` available as the offline demo fallback.

## Change Log
- 2026-04-27: Created `CONTEXT.md` with initial project state before Android scaffold.
- 2026-04-27: Updated `AGENTS.md` to require `CONTEXT.md` updates after meaningful changes. Files touched: `AGENTS.md`, `CONTEXT.md`. Build/test not run yet.
- 2026-04-27: Added Android Gradle project scaffold, README, manifest, resources, Room skeleton, mock reasoning layer, protocol retriever, TTS stub, and Compose MVP screens. Files touched: root Gradle files, `README.md`, `app/**`, `CONTEXT.md`. Build attempted with `gradlew.bat assembleDebug` and `gradle assembleDebug`; failed before build start because no wrapper or Gradle command exists. Next recommended step: open/sync in Android Studio and generate a wrapper.
- 2026-04-27: Fixed AndroidX build/config issue reported by Android Studio: "AndroidX dependencies are detected but android.useAndroidX is not enabled." Files touched: `gradle.properties`, `CONTEXT.md`. Fix applied: added `android.useAndroidX=true`, `android.enableJetifier=true`, `org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8`, and `kotlin.code.style=official`. Build/test not run in this shell. Next recommended step: sync Gradle and rebuild in Android Studio.
- 2026-04-27: User verified the app builds and launches on the Pixel 9 Pro API 35 emulator, including the mock supervisor summary with total visits, referral flags, urgent cases, and follow-ups. Polished only mock MVP UI copy and local mock output wording. Files touched: `app/src/main/java/com/smriti/clinicalscribe/ui/PatientListScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/VisitScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/ReviewScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/SummaryScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgent.kt`, `CONTEXT.md`. Changes: added visible "Offline demo mode" labels, renamed the summary to "End-of-Day Supervisor Summary", made roster return clearer, and clarified that referral output is a protocol-grounded suggestion rather than a diagnosis. Build result: first `.\gradlew.bat assembleDebug` failed in the sandbox because Gradle could not create its user-cache lock file; rerun with normal user-level Gradle cache access passed. Next recommended step: reinstall the latest debug build and run the polished Meena demo flow end to end on the emulator.
- 2026-04-27: Added focused deterministic unit tests for mock reasoning and protocol retrieval only. Files touched: `app/build.gradle.kts`, `app/src/main/java/com/smriti/clinicalscribe/rag/ProtocolRetriever.kt`, `app/src/test/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgentTest.kt`, `app/src/test/java/com/smriti/clinicalscribe/rag/ProtocolRetrieverTest.kt`, `CONTEXT.md`. Changes: added JUnit test dependency, removed `ProtocolRetriever` fallback that returned the first protocol for unrelated queries, tested normal mock notes, danger-sign referral suggestions, protocol citation language, diagnostic-language avoidance, danger-sign retrieval, and unrelated-query empty retrieval. Test result: first sandboxed test command failed on Gradle cache lock-file creation; first real test run compiled but failed one fixture because the normal visit text included "bleeding"; after correcting the fixture, `.\gradlew.bat testDebugUnitTest` passed with 6 tests. Next recommended step: run the app again and confirm the unchanged mock flow still feels right after the retriever no-match behavior change.
- 2026-04-27: Fixed screenshot polish issues in the mock MVP only. Files touched: `app/src/main/java/com/smriti/clinicalscribe/data/AppDatabase.kt`, `app/src/main/java/com/smriti/clinicalscribe/MainActivity.kt`, `app/src/main/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgent.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/ReviewScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/SummaryScreen.kt`, `app/src/test/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgentTest.kt`, `CONTEXT.md`. Issues fixed: moved ReviewScreen "Edit Observation" below the header as a full-width button; made structured visit note output concise and sectioned; preserved "not a diagnosis" and "CHW confirmation required"; added editable ReviewScreen sections for Observation, Relevant history, Protocol-grounded support, and Protocol citation; added demo-mode Reset Demo Data action on SummaryScreen that clears saved mock visits/referral flags and restores original demo history. Test/build result: `.\gradlew.bat testDebugUnitTest` passed after the known sandbox cache retry; `.\gradlew.bat assembleDebug` passed. Next recommended step: install the latest debug APK and verify the reset-enabled demo flow on the Pixel 9 Pro API 35 emulator.
- 2026-04-27: Added real local protocol grounding from an offline JSON asset corpus while keeping the mock MVP architecture. Files touched: `app/src/main/assets/protocols/maternal_health_demo_protocols.json`, `app/src/main/java/com/smriti/clinicalscribe/rag/ProtocolChunk.kt`, `app/src/main/java/com/smriti/clinicalscribe/rag/ProtocolRetriever.kt`, `app/src/main/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgent.kt`, `app/src/main/java/com/smriti/clinicalscribe/MainActivity.kt`, `app/src/main/java/com/smriti/clinicalscribe/data/AppDatabase.kt`, `app/src/test/java/com/smriti/clinicalscribe/rag/ProtocolRetrieverTest.kt`, `app/src/test/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgentTest.kt`, `CONTEXT.md`. Protocol corpus added: 10 maternal-health demo chunks covering severe headache, blurred vision, high blood pressure, reduced fetal movement, vaginal bleeding, convulsions, severe abdominal pain, fever, swelling of face/hands, and same-day referral guidance. Tests added/updated: asset-backed retrieval for headache + blurred vision + high BP, unrelated-query no match, referral output including retrieved source/section, and no-protocol uncertain/no-citation output. Test/build result: first sandboxed `.\gradlew.bat testDebugUnitTest` failed on the known Gradle cache lock-file path; rerun with normal cache access passed. `.\gradlew.bat assembleDebug` passed. Next recommended step: install the latest debug build and verify the judge demo shows asset-derived protocol citations in the ReviewScreen.
- 2026-04-27: Added a voice-note style UI shell without real ASR or audio recording. Files touched: `app/src/main/java/com/smriti/clinicalscribe/ui/VisitScreen.kt`, `CONTEXT.md`. Changes: added "Record Visit Note" section, mock Start/Stop voice note button, "Listening locally..." recording state, 30-second chunk label, "Simulated transcript for demo" text field label, "Use sample danger-sign transcript" fill button, and note that real Gemma 4 audio integration comes next. Test/build result: first sandboxed `.\gradlew.bat testDebugUnitTest` failed on the known Gradle cache lock-file path; rerun with normal cache access passed. `.\gradlew.bat assembleDebug` passed. Next recommended step: install the latest debug build and verify the mock voice workflow on the Pixel 9 Pro API 35 emulator.
- 2026-04-27: Completed Field Tool Milestone while keeping mock reasoning. Files touched: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/smriti/clinicalscribe/MainActivity.kt`, `app/src/main/java/com/smriti/clinicalscribe/audio/AudioRecorder.kt`, `app/src/main/java/com/smriti/clinicalscribe/audio/VoiceNoteMetadata.kt`, `app/src/main/java/com/smriti/clinicalscribe/data/AppDatabase.kt`, `app/src/main/java/com/smriti/clinicalscribe/data/TranscriptSource.kt`, `app/src/main/java/com/smriti/clinicalscribe/data/VisitLog.kt`, `app/src/main/java/com/smriti/clinicalscribe/export/JsonExporter.kt`, `app/src/main/java/com/smriti/clinicalscribe/tts/AndroidVoiceOutput.kt`, `app/src/main/java/com/smriti/clinicalscribe/tts/VoiceOutput.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/VisitScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/ReviewScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/SummaryScreen.kt`, `app/src/test/java/com/smriti/clinicalscribe/data/VisitLogTest.kt`, `app/src/test/java/com/smriti/clinicalscribe/export/JsonExporterTest.kt`, `CONTEXT.md`. Features implemented: runtime microphone permission, local app-private 30-second voice-note recording, voice-note metadata persisted on confirmed visits, Android TTS readout for referral and summary, app-private visit/summary JSON export, and Offline Proof demo section. Test/build result: initial sandboxed Gradle run hit the known user-cache lock-file issue; `.\gradlew.bat testDebugUnitTest` passed with normal cache access; `.\gradlew.bat assembleDebug` passed. Known issues: `MediaRecorder()` no-arg constructor deprecation warning; TTS depends on device language data; real ASR remains pending and transcript source is marked `REAL_ASR_PENDING` when audio is attached. Next recommended step: emulator verification of recording/TTS/export, then start the LiteRT/Gemma integration spike behind `GemmaAgent`.
