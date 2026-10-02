# Firebase Project Setup — EDUTOPIA

This document is the checklist a developer must complete once. The repository
does not and will not contain real credentials.

## 1. Create the Firebase project

1. console.firebase.google.com → **Add project** (name e.g. `edutopia-mindnova`).
2. Under **Build → Authentication → Sign-in method**: enable **Email/Password**.
3. **Build → Firestore Database**: create a database (production mode, pick a
   region close to your users).
4. **Build → Storage**: get started (default bucket).
5. **Project settings → Your apps → Add app (Android)**:
   - Package name: `com.mindnova.edutopia` (must match `applicationId`).
   - Nickname: `EDUTOPIA`.
   - Download `google-services.json`.

## 2. Place the config file

```
app/google-services.json     ← the downloaded file goes HERE (git-ignored)
```

`app/google-services.json.example` in the repo is a schema template only.
Never commit the real file; never paste real API keys into source, README, or
CI logs.

## 3. Deploy rules & indexes

From the repository root, with the target project selected:

```bash
firebase use <project-id>
firebase deploy --only firestore:rules,firestore:indexes,storage
```

- `firestore.rules` — client navigation is UX; **these rules are the real
  permission boundary** (student vs admin vs superAdmin; self-writable
  attempts/progress; super-admin-only role writes; append-only audit logs).
- `firestore.indexes.json` — every composite query the app relies on.
  Missing index errors are shown in-app verbatim, never swallowed as "empty".
- `storage.rules` — private profile photos, public content assets, admin-write.

## 4. Bootstrap the first super admin

1. Sign up in the app (or create the user in Auth console).
2. Firestore console → `users/{uid}` → create document with at least:

   ```
   uid:  <the auth uid>
   name: Platform Owner
   email: owner@example.com
   studentClass: ""
   role: superAdmin
   xp: 0   points: 0   level: 1   streak: 1
   testsCompleted: 0  lecturesWatched: 0  pyqsSolved: 0
   createdAt: <ms timestamp>   lastActiveAt: <ms timestamp>
   ```

3. (Recommended) also create `admins/{uid}` with
   `{ uid, name, email, role: "superAdmin", status: "active", permissions: [ …all… ] }`
   so the admin gate resolves through the normal path.
4. Sign in again → the app routes to the Admin Console. Create further admins
   from **Admin → Super Admin Management**.

## 5. What the app expects in Firestore

Minimal bootstrap data for a usable install:

| Collection | Sample doc | Notes |
|---|---|---|
| `batches` | name, targetClass, accessType "Free", joinCode "EDU-1234", studentCount 0, status "active" | counts maintained by enrollment transactions |
| `series` | title, subject, order 1, lectureCount 0, targetClass "All" | lectureCount is derived, don't hand-edit |
| `lectures` | seriesId, lectureNumber, videoUrl (YouTube), publishStatus "published" | views/XP handled by completion transactions |
| `tests` | title, status "published", durationMinutes, marksPerQuestion 4, negativeMarks 1 | |
| `tests/{id}/questions` | questionNumber, questionText, options A-D, correctAnswer, subject, marks, negativeMarks, order | bulk-loaded by the admin JSON importer |
| `pyqs` | examType, year, subject, questionText, options, correctAnswer | solvedCount/totalAttempts/accuracy auto-update |
| `dailyGoals` | dateKey "YYYY-MM-DD", lecture{…}, test{…}, isActive | empty DB → home shows nothing (by design) |

Empty collections are fine — every screen distinguishes **Empty** from
**Error**, so an unseeded database displays "nothing here yet", not a crash
and not fabricated demo data.

## 6. Analytics / storage bucket URLs

`firebase-analytics` is initialized by default; no custom events are required
for the app to function. Banner/announcement `imageUrl`s in admin screens
should be Storage `gs://` download URLs or HTTPS links you control.
