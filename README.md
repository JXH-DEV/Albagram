# Albagram

Native Android (Kotlin + Jetpack Compose) companion for Albanian language learning. Offline-first with Room-seeded content and optional online dictionary lookup via the local `:dictionary-api` service.

## Requirements

- JDK 17+ (project targets JVM 17)
- Android SDK 35
- Android Studio Ladybug+ or command-line Gradle

## Build APK

Debug:

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

Release (R8 minify):

```bash
./gradlew assembleRelease
```

Unsigned release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`  
(For Play Store / sideload signing, configure a keystore in `app/build.gradle.kts`.)

Windows:

```powershell
.\gradlew.bat assembleDebug
```

## Modules

- Fjalëkryqi — intersecting crossword
- Rima — rhyme finder
- Ese — essay prompts + draft
- Konkursi — timed quiz with simulated live feed
- Libri im — saved words
- Fjalor — A–Z glossary with online lookup (requires dictionary API)

## Dictionary API (online lookup)

See [dictionary-api/README.md](dictionary-api/README.md). Start the server, then search in **Fjalor** while online.
