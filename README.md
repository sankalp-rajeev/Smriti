# Smriti

Smriti is an offline-first maternal-health visit copilot prototype for community health workers.

This initial Android scaffold uses Kotlin, Jetpack Compose, Room skeletons, and a local `MockGemmaAgent` to demonstrate the MVP flow:

1. Select patient Meena, 28F.
2. Review prior visit history.
3. Enter a simulated voice observation as text.
4. Generate a structured visit note locally.
5. Review referral flag, protocol citation, and follow-up.
6. Confirm/save the record.
7. Generate an end-of-day supervisor summary.

No cloud APIs, real Gemma, LiteRT, Whisper, camera, or remote database are implemented in this scaffold.

## Open In Android Studio

Open the repository root as a Gradle project. Android Studio should sync `settings.gradle.kts` and the `app` module.

## Build

Use Android Studio, or run a Gradle wrapper after one is generated:

```powershell
./gradlew assembleDebug
```

This machine did not have a standalone `gradle` command available when the scaffold was created.
