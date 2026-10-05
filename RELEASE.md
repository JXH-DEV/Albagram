# Albagram release signing

Do **not** commit keystores or passwords.

Add to `local.properties` (gitignored) or export env vars:

```
ALBAGRAM_STORE_FILE=C:\\path\\to\\albagram-release.jks
ALBAGRAM_STORE_PASSWORD=...
ALBAGRAM_KEY_ALIAS=albagram
ALBAGRAM_KEY_PASSWORD=...
```

Then:

```
./gradlew assembleRelease
```

Without these keys, `assembleRelease` still builds an unsigned/minify release APK using the default debug-less signing config omission (unsigned until store file is set).

## Database upgrades

Room uses explicit migrations (`1→2`, `2→3`). Destructive fallback is **off** so user data (saved words, drafts, progress, crossword progress, daily visit streak) is preserved across upgrades.

## v1.6.0 Go/No-Go

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] Cold start: Home shows **Sot** daily card with 4 challenge rows
- [ ] Daily challenges rotate by date (word, crossword, quiz, ese)
- [ ] Home: **Seri kuizi** vs **Seri ditore** labels are distinct
- [ ] Home: resume row for in-progress crossword / essay draft
- [ ] Badge celebration dialog appears on new badge unlock
- [ ] Konkursi: personal stats panel (no mock feed)
- [ ] Konkursi: daily quiz mode (5 questions, marks complete at ≥30 pts)
- [ ] Rima: **Luaj** rhyme quiz (5 rounds, scoring)
- [ ] Ese: draft/completed badges on list; **Dorëzo** at ≥80 words + suggested word
- [ ] Libri: flashcard **Rishiko** when ≥3 saved words
- [ ] Glossary: daily word MCQ banner (+5 pts, marks word daily complete)
- [ ] Crossword: completion overlay + deep link from Home daily row
- [ ] Weekly words on Home/Libri rotate by week hash

## v1.5.0 Go/No-Go

- [ ] `./gradlew :dictionary-api:test`
- [ ] `./gradlew :dictionary-api:run` (keep server running for device smoke)
- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew assembleRelease`
- [ ] Cold start: seed gate reaches Ready and shows Home
- [ ] Glossary: local search still works offline
- [ ] Glossary: online section appears when API is running and device has network
- [ ] Glossary: airplane mode shows offline hint, no crash
- [ ] Remote word detail: source link opens browser; save-to-Libri works
- [ ] Crossword: resume progress, hints reduce points, solved cells stay locked
- [ ] Ese: draft hydrates and auto-saves latest text
- [ ] Konkursi: pending save carries across questions and can be saved at end
- [ ] Rima: blank validation + IME search + result list opens word detail
- [ ] Libri: reviewed counter and weekly recommended words are visible
- [ ] Home: goal cards show score/streak targets and update after play
- [ ] Seed QA: links/integrity tests pass with no broken references

## v1.4.0 Go/No-Go (archived)

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] `./gradlew assembleRelease`
- [ ] Cold start: seed gate reaches Ready and shows Home
- [ ] Crossword: resume progress, hints reduce points, solved cells stay locked
- [ ] Ese: draft hydrates and auto-saves latest text
- [ ] Konkursi: pending save carries across questions and can be saved at end
- [ ] Rima: blank validation + IME search + result list opens word detail
- [ ] Libri: reviewed counter and weekly recommended words are visible
- [ ] Home: goal cards show score/streak targets and update after play
- [ ] Seed QA: links/integrity tests pass with no broken references
