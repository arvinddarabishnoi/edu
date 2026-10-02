/**
 * EDUTOPIA — static workspace status page.
 *
 * This server does NOT build, compile, or run the Android app, and it does
 * not claim that it does. It renders repository metadata that is true at
 * request time (branch state on disk, file counts, presence of required
 * configuration files). Android builds happen locally with Gradle/Android
 * Studio or in CI — see README.md.
 */
const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const port = process.env.PORT || 3000;
const repoRoot = path.resolve(__dirname, '..', '..');

function fileExists(rel) {
  try {
    return fs.existsSync(path.join(repoRoot, rel));
  } catch (e) {
    return false;
  }
}

function countFiles(dir, ext) {
  let count = 0;
  const stack = [path.join(repoRoot, dir)];
  while (stack.length) {
    const cur = stack.pop();
    let entries;
    try {
      entries = fs.readdirSync(cur, { withFileTypes: true });
    } catch (e) {
      continue;
    }
    for (const ent of entries) {
      const full = path.join(cur, ent.name);
      if (ent.isDirectory()) stack.push(full);
      else if (!ext || ent.name.endsWith(ext)) count++;
    }
  }
  return count;
}

function statusPage() {
  const checks = [
    {
      label: 'Kotlin sources present',
      ok: countFiles('app/src/main/java', '.kt') > 0,
      detail: `${countFiles('app/src/main/java', '.kt')} .kt files under app/src/main/java`,
    },
    {
      label: 'Unit tests present',
      ok: countFiles('app/src/test', '.kt') > 0,
      detail: `${countFiles('app/src/test', '.kt')} .kt files under app/src/test`,
    },
    {
      label: 'Firebase config (app/google-services.json)',
      ok: fileExists('app/google-services.json'),
      detail: fileExists('app/google-services.json')
        ? 'Found (values are never displayed here).'
        : 'MISSING — copy the real file from your Firebase console. The Gradle build intentionally fails until you add it.',
    },
    {
      label: 'Firestore rules & indexes',
      ok: fileExists('firestore.rules') && fileExists('firestore.indexes.json'),
      detail: 'Deploy with: firebase deploy --only firestore',
    },
    {
      label: 'Gradle build executed by this server?',
      ok: false,
      detail:
        'NO. This is a static informational page. Run ./gradlew test assembleDebug locally to compile and test the app.',
    },
  ];

  const rows = checks
    .map(
      (c) => `
      <tr>
        <td class="${c.ok ? 'ok' : 'warn'}">${c.ok ? '✓' : '!'}</td>
        <td>${c.label}</td>
        <td class="detail">${c.detail}</td>
      </tr>`
    )
    .join('');

  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>EDUTOPIA — Workspace Status (informational only)</title>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
         background:#0B0F19; color:#E2E8F0; margin:0; padding:40px 16px; }
  .wrap { max-width: 760px; margin: 0 auto; }
  h1 { font-size: 22px; margin: 0 0 4px; }
  .sub { color:#94A3B8; font-size: 13px; margin-bottom: 20px; }
  .notice { background:#1E293B; border:1px solid #334155; border-radius:10px;
            padding:12px 14px; font-size:13px; line-height:1.5; margin-bottom:22px; }
  table { width:100%; border-collapse: collapse; background:#121B2E;
          border:1px solid #2B3C62; border-radius:12px; overflow:hidden; }
  td { padding: 10px 12px; border-bottom: 1px solid #1E293B; font-size: 13px; vertical-align: top; }
  tr:last-child td { border-bottom: none; }
  td.ok { color:#10B981; font-weight:700; width:28px; }
  td.warn { color:#F59E0B; font-weight:700; width:28px; }
  .detail { color:#94A3B8; }
  code { background:#1E293B; padding:1px 5px; border-radius:4px; font-size:12px; }
</style>
</head>
<body>
  <div class="wrap">
    <h1>EDUTOPIA by MINDNOVA — Workspace Status</h1>
    <div class="sub">Static informational page served by Node.js. This is <b>not</b> a running Android app.</div>
    <div class="notice">
      Native Android cannot execute inside a browser frame. This page only reports repository
      facts computed at request time. To actually build and run the app: open the project in
      Android Studio (or run <code>./gradlew assembleDebug</code>) on a machine with the Android
      SDK, after adding <code>app/google-services.json</code> from your Firebase project.
    </div>
    <table>${rows}</table>
  </div>
</body>
</html>`;
}

app.get('/', (req, res) => {
  res.type('html').send(statusPage());
});

app.get('/healthz', (req, res) => {
  res.json({ status: 'informational-only', androidBuildPerformed: false });
});

if (require.main === module) {
  app.listen(port, '0.0.0.0', () => {
    console.log(`EDUTOPIA workspace status page listening on 0.0.0.0:${port}`);
  });
}

module.exports = app;
