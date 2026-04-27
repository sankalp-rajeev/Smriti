# Smriti Project Context

## Current Goal
Create the initial offline-first Android Kotlin project skeleton for the Smriti MVP demo flow.

## Current Status
The repository now has a working Android Kotlin + Jetpack Compose mock MVP. The app builds, launches on the Pixel 9 Pro API 35 emulator, reaches the End-of-Day Supervisor Summary screen, has focused unit coverage for mock reasoning/protocol retrieval, and includes demo reset support for repeated judge walkthroughs.

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

## Active Tasks
- [ ] Reinstall the latest debug build on the emulator and click through the reset-enabled polished judge flow.
- [ ] Decide whether to add a small Room reset/seed instrumentation test later.

## Blockers / Issues
- Gradle wrapper now exists; direct sandboxed wrapper runs can still fail until Gradle can access the normal user-level `.gradle` cache.
- A standalone `gradle` command is not installed in the current shell.
- Build was not started because `gradlew.bat` was missing and `gradle assembleDebug` failed with `gradle` not recognized.
- Android Studio build/sync reported: AndroidX dependencies are detected but `android.useAndroidX` is not enabled.
- First local `.\gradlew.bat assembleDebug` attempt in the sandbox failed because Gradle could not create a lock file under `C:\Users\CodexSandboxOffline\.gradle\wrapper\dists\...`.
- First local `.\gradlew.bat testDebugUnitTest` attempt in the sandbox failed for the same Gradle user-cache lock-file reason; rerun with normal user-level Gradle cache access worked.
- Initial unit test run found one bad "normal visit" fixture because it contained the danger-sign keyword "bleeding"; the fixture was corrected and the tests passed.
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
- `app/src/main/java/com/smriti/clinicalscribe/MainActivity.kt` - app entry point, local seeding, screen flow, save/summary wiring, and demo reset wiring.
- `app/src/main/java/com/smriti/clinicalscribe/data/` - Room database, entities, DAOs, and demo seed data.
- `app/src/main/java/com/smriti/clinicalscribe/reasoning/` - Gemma interface, mock implementation, and reasoning result models.
- `app/src/main/java/com/smriti/clinicalscribe/rag/` - protocol chunk model and keyword retriever.
- `app/src/main/java/com/smriti/clinicalscribe/ui/` - Compose MVP screens.
- `app/src/main/java/com/smriti/clinicalscribe/tts/VoiceOutput.kt` - future offline TTS abstraction stub.
- `app/src/test/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgentTest.kt` - deterministic unit tests for local mock visit-note/referral behavior.
- `app/src/test/java/com/smriti/clinicalscribe/rag/ProtocolRetrieverTest.kt` - deterministic unit tests for local protocol keyword retrieval.

## Next Steps
1. Open the root folder in Android Studio and run Gradle sync.
2. Run Build -> Make Project.
3. Generate a Gradle wrapper from Android Studio or a local Gradle install.
4. Install the latest debug build on the Pixel 9 Pro API 35 emulator and run the reset-enabled polished Meena demo flow end to end.
5. Consider a small Room reset/seed instrumentation test if reset behavior becomes important for automated demo validation.

## Change Log
- 2026-04-27: Created `CONTEXT.md` with initial project state before Android scaffold.
- 2026-04-27: Updated `AGENTS.md` to require `CONTEXT.md` updates after meaningful changes. Files touched: `AGENTS.md`, `CONTEXT.md`. Build/test not run yet.
- 2026-04-27: Added Android Gradle project scaffold, README, manifest, resources, Room skeleton, mock reasoning layer, protocol retriever, TTS stub, and Compose MVP screens. Files touched: root Gradle files, `README.md`, `app/**`, `CONTEXT.md`. Build attempted with `gradlew.bat assembleDebug` and `gradle assembleDebug`; failed before build start because no wrapper or Gradle command exists. Next recommended step: open/sync in Android Studio and generate a wrapper.
- 2026-04-27: Fixed AndroidX build/config issue reported by Android Studio: "AndroidX dependencies are detected but android.useAndroidX is not enabled." Files touched: `gradle.properties`, `CONTEXT.md`. Fix applied: added `android.useAndroidX=true`, `android.enableJetifier=true`, `org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8`, and `kotlin.code.style=official`. Build/test not run in this shell. Next recommended step: sync Gradle and rebuild in Android Studio.
- 2026-04-27: User verified the app builds and launches on the Pixel 9 Pro API 35 emulator, including the mock supervisor summary with total visits, referral flags, urgent cases, and follow-ups. Polished only mock MVP UI copy and local mock output wording. Files touched: `app/src/main/java/com/smriti/clinicalscribe/ui/PatientListScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/VisitScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/ReviewScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/SummaryScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgent.kt`, `CONTEXT.md`. Changes: added visible "Offline demo mode" labels, renamed the summary to "End-of-Day Supervisor Summary", made roster return clearer, and clarified that referral output is a protocol-grounded suggestion rather than a diagnosis. Build result: first `.\gradlew.bat assembleDebug` failed in the sandbox because Gradle could not create its user-cache lock file; rerun with normal user-level Gradle cache access passed. Next recommended step: reinstall the latest debug build and run the polished Meena demo flow end to end on the emulator.
- 2026-04-27: Added focused deterministic unit tests for mock reasoning and protocol retrieval only. Files touched: `app/build.gradle.kts`, `app/src/main/java/com/smriti/clinicalscribe/rag/ProtocolRetriever.kt`, `app/src/test/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgentTest.kt`, `app/src/test/java/com/smriti/clinicalscribe/rag/ProtocolRetrieverTest.kt`, `CONTEXT.md`. Changes: added JUnit test dependency, removed `ProtocolRetriever` fallback that returned the first protocol for unrelated queries, tested normal mock notes, danger-sign referral suggestions, protocol citation language, diagnostic-language avoidance, danger-sign retrieval, and unrelated-query empty retrieval. Test result: first sandboxed test command failed on Gradle cache lock-file creation; first real test run compiled but failed one fixture because the normal visit text included "bleeding"; after correcting the fixture, `.\gradlew.bat testDebugUnitTest` passed with 6 tests. Next recommended step: run the app again and confirm the unchanged mock flow still feels right after the retriever no-match behavior change.
- 2026-04-27: Fixed screenshot polish issues in the mock MVP only. Files touched: `app/src/main/java/com/smriti/clinicalscribe/data/AppDatabase.kt`, `app/src/main/java/com/smriti/clinicalscribe/MainActivity.kt`, `app/src/main/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgent.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/ReviewScreen.kt`, `app/src/main/java/com/smriti/clinicalscribe/ui/SummaryScreen.kt`, `app/src/test/java/com/smriti/clinicalscribe/reasoning/MockGemmaAgentTest.kt`, `CONTEXT.md`. Issues fixed: moved ReviewScreen "Edit Observation" below the header as a full-width button; made structured visit note output concise and sectioned; preserved "not a diagnosis" and "CHW confirmation required"; added editable ReviewScreen sections for Observation, Relevant history, Protocol-grounded support, and Protocol citation; added demo-mode Reset Demo Data action on SummaryScreen that clears saved mock visits/referral flags and restores original demo history. Test/build result: `.\gradlew.bat testDebugUnitTest` passed after the known sandbox cache retry; `.\gradlew.bat assembleDebug` passed. Next recommended step: install the latest debug APK and verify the reset-enabled demo flow on the Pixel 9 Pro API 35 emulator.
