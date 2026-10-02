# EDUTOPIA by MINDNOVA

A premium JEE preparation Android app (Kotlin + Jetpack Compose) with a Firebase
backend: authentication, Firestore data, Storage, courses/series, an exam-style
test engine with real scoring, PYQ practice, gamification (XP, levels, streaks,
leaderboards), tournaments, and a full admin console.

```
Composable  →  ViewModel (StateFlow)  →  Repository  →  Firebase
```

---

## 1. Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Ladybug (2024.2) or newer |
| JDK | 17 |
| Android SDK | compileSdk 34, minSdk 24 |
| Gradle | 8.6 (AGP 8.4.2 — see `gradle/wrapper/gradle-wrapper.properties`) |

## 2. Required step before the first build: Firebase configuration

The repository intentionally contains **no Firebase credentials**. The
`google-services` plugin will fail the build until you add your own:

1. Create (or open) a Firebase project.
2. Add an Android app with **package name `com.mindnova.edutopia`**
   (this is also the `applicationId`).
3. Download `google-services.json` → save it as `app/google-services.json`.
4. In Firebase console enable:
   - **Authentication → Email/Password** (password reset emails work with the default template)
   - **Cloud Firestore** (production mode)
   - **Cloud Storage**
5. Deploy rules + indexes from this repository (see §5).

A placeholder template is at `app/google-services.json.example` (it is NOT a
working config and must never be renamed & committed — it is for schema
reference only). Full instructions: [`docs/FIREBASE_SETUP.md`](docs/FIREBASE_SETUP.md).

> **Package-name note:** the original sources mixed `com.mindnova.Edutopia`
> (capital E, not a legal reverse-domain id per Java convention) with
> `com.mindnova.edutopia`. The build now uses a single lowercase id
> `com.mindnova.edutopia` for `namespace` **and** `applicationId`. If you must
> keep the old casing for an existing Firebase app, change both values in
> `app/build.gradle.kts` and register that exact id in Firebase — they must match.

## 3. Build & test

```bash
./gradlew clean
./gradlew test            # JVM unit tests (scoring, validation, gamification, readiness, counters)
./gradlew assembleDebug   # debug APK (standard Android debug signing)
./gradlew lintDebug
./gradlew installDebug    # on a connected device/emulator
```

Instrumented UI tests (require a device/emulator):

```bash
./gradlew connectedDebugAndroidTest
```

If `gradlew` reports that `gradle-wrapper.jar` is missing: open the project in
Android Studio once (it provisions the wrapper), or run
`gradle wrapper --gradle-version 8.6` with any local Gradle 8.x.

Signing: debug builds use the standard `~/.android/debug.keystore` that AGP
generates automatically. The previous custom root-level `debug.keystore`
reference was removed — **never commit keystores or signing passwords**.
Release signing must be configured by the app owner (e.g. via
`~/.gradle/gradle.properties` `signing*` keys), not in-repo.

## 4. Firestore data model

Collections (names in `core/utils/Constants.kt`):

`users`, `admins`, `series`, `lectures` (+ `lectures/{id}/progress` for legacy
cleanup), `tests` (+ `tests/{id}/questions`), `testAttempts`, `testResults`,
`pyqs` (+ `pyqs/{id}/attemptors`), `tournaments` (+ `participants`), `batches`,
`banners`, `announcements`, `dailyGoals`, `messages`, `auditLogs`, `appConfig`.

Key invariants enforced in code (`data/repository` + `domain/services/Counters.kt`):

- **Batch enrollment is idempotent** — joining the same batch twice never
  increments `studentCount`; switching batches decrements the old one; counts
  never go negative; all of it inside one Firestore transaction.
- **Lecture counts** — creating a lecture increments its series count exactly
  once; editing never does; deleting decrements; moving between series
  decrements+increments atomically.
- **View/XP idempotency** — a lecture's completion record
  (`users/{uid}/lectureProgress/{lectureId}`) is created once; repeat
  completions award no XP/views/`lecturesWatched`.
- **Test submission** is atomic and idempotent via a deterministic document id
  `userId_testId_startedAt`; double-taps cannot create duplicate results,
  attempts, XP, or `attemptsCount`.
- **PYQ tracking** — `solvedCount` counts distinct students (first attempt),
  `totalAttempts/totalCorrect/accuracy` are real aggregates; XP is awarded
  only on the user's first correct answer.
- **No fabricated analytics** — results never show invented rank/percentile
  (they are `0` until a trusted dataset provides them), the readiness panel
  says "Not enough data yet" with guidance until ≥2 tests exist, and admin
  dashboard metrics are true counts (0 means 0).

Deleting a test/PYQ/tournament also deletes its subcollections — parent
deletion never leaves orphaned children.

## 5. Security rules & indexes (server-side truth)

Client navigation checks are UX only. The real authorization boundary is
`firestore.rules` — students can only write their own docs, admin content is
admin-write, role changes are super-admin only, audit logs are append-only.

```bash
npm install -g firebase-tools   # or brew install firebase-cli
firebase login
firebase use <your-project-id>
firebase deploy --only firestore:rules,firestore:indexes,storage
```

- `firestore.rules`, `storage.rules`, `firestore.indexes.json` live at repo root;
  `firebase.json` wires them for the CLI.
- The composite indexes in `firestore.indexes.json` are required by:
  leaderboard (`users`: role+points), admin user list (`users`: role+createdAt),
  published tests (`tests`: status+createdAt), series lectures
  (`lectures`: seriesId+lectureNumber), personal results
  (`testResults`: userId+submittedAt), PYQ exam filter (`pyqs`: examType+year),
  active banners (`banners`: active+priority).
  Missing indexes surface in-app as an explicit error that says so — never as
  an empty list.

First super-admin bootstrap: create the user in Firebase Auth, then in the
Firestore console create `users/{uid}` with `role: "superAdmin"` (the app's
admin gate falls back to this doc). After that, admin management flows through
**Admin → Super Admin Management**.

## 6. Architecture map

```
app/src/main/java/com/mindnova/edutopia/
├── MainActivity.kt / EdutopiaApp.kt        entry points (edge-to-edge, single NavHost)
├── core/
│   ├── navigation/    Screen.kt (route catalog) · EdutopiaNavHost.kt (all routes + deep links)
│   │                  BottomNavBar.kt (student floating pill nav)
│   ├── theme/         Color · Type · Shape · Spacing · Theme (dark-first design system)
│   ├── components/    shared UI kit (cards, buttons, fields, states, player, ResourceView…)
│   └── utils/         Constants · Resource (Loading/Success/Empty/Error) · FirestoreFlows
├── data/
│   ├── models/        Firestore-backed immutable data classes
│   └── repository/    one repository per domain area, transactions + idempotency inside
├── domain/services/   pure logic: AuthValidator · TestEvaluationService · GamificationService
│                      ReadinessCalculator · JsonQuestionValidator · Counters
└── ui/                feature screens, each backed by a ViewModel with a
                       lifecycle-aware immutable UiState (no repos in Composables)
```

Errors and empty data are always distinct states (`Resource`); every screen
renders Loading / Success / Empty / Error(+Retry).

## 7. Design assets & licensing

`assets/` contains Freepik reference material (JPG/PNG + large PSD/AI/EPS
source files, with their `License free.txt` / `License premium.txt`). These are
**design references only** — no reference bitmap is embedded in the app. All
in-app UI is original Compose layouts, vector drawables and gradients.
Before shipping any Freepik-derived artwork, verify the applicable license and
attribution requirements; if such assets are kept in VCS long-term, prefer
Git LFS or external design storage (`git lfs install && git lfs track "*.psd"`).

## 8. Node workspace status page (`app/applet/`)

`server.js` serves a **static informational status page** (file-presence
checks computed at request time). It never claims the Android app was built or
compiled. `npm run build` is an explicit no-op that prints where the real
build lives. Optional; the Android project does not depend on it.

## 9. Quality gates

- `./gradlew test` — unit tests for auth validation, test scoring
  (exact marks/accuracy/subject math, no fake percentile), JSON importer
  strictness (item-level errors, nothing silently skipped), gamification
  levels/streaks, readiness "not enough data" semantics, counter/idempotency
  rules, route catalog integrity, error classification.
- `./gradlew lintDebug` — fix or explicitly suppress; do not ship new warnings.
- Every user-facing action surfaces success/failure; destructive admin actions
  require confirmation and are written to `auditLogs`.

## 10. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `File google-services.json is missing` at build | Add the real file (step 2). |
| In-app "Missing Firestore composite index…" | Deploy `firestore.indexes.json` (step 5). |
| In-app "Permission denied by Firestore security rules" | Deploy `firestore.rules`; confirm the signed-in account's role/admin doc. |
| `gradlew` says wrapper jar missing | Open in Android Studio once or run `gradle wrapper --gradle-version 8.6`. |
| Signed in but "profile could not be loaded" | Firestore is unreachable/denied — the app deliberately does NOT treat this as "profile missing". Fix connectivity/rules and retry. |
