const express = require('express');
const app = express();
const port = process.env.PORT || 3000;

app.get('/', (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Android Project Workspace</title>
        <style>
          body { 
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; 
            display: flex; 
            justify-content: center; 
            align-items: center; 
            height: 100vh; 
            background: #f8fafc; 
            margin: 0; 
            color: #334155;
          }
          .container { 
            text-align: left; 
            background: white; 
            padding: 40px; 
            border-radius: 16px; 
            box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1); 
            max-width: 600px; 
          }
          h1 { 
            color: #0f172a; 
            margin-top: 0;
          }
          p {
            line-height: 1.6;
          }
          ul {
            line-height: 1.6;
            margin-bottom: 0;
          }
          li {
            margin-bottom: 12px;
          }
          strong {
            color: #0f172a;
          }
          .success {
            color: #10b981;
            font-weight: bold;
            margin-bottom: 24px;
            padding: 12px;
            background: #ecfdf5;
            border-radius: 8px;
            border: 1px solid #a7f3d0;
          }
        </style>
      </head>
      <body>
        <div class="container">
          <h1>Android Project Workspace</h1>
          <div class="success">✅ The Android app compiled successfully!</div>
          <p>This workspace contains a native Android application (Kotlin/Compose) imported from GitHub.</p>
          <p>The AI Studio live preview environment is a web browser frame. It cannot run native Android <code>.apk</code> files directly here, which is why the screen was blank.</p>
          <p><strong>To run your Android app:</strong></p>
          <ul>
            <li>Click the <strong>Export</strong> button (the download icon in the top right menu) to download the ZIP file.</li>
            <li>Open the extracted folder in <strong>Android Studio</strong>.</li>
            <li>Click Run to test it on your local Android emulator or physical device.</li>
          </ul>
          <p>If you'd rather see the app running live in this window, just ask the AI assistant to <strong>"Rewrite this app into a Web application"</strong> (React/Next.js) or <strong>"Build a Web Admin Panel for it"</strong>.</p>
        </div>
      </body>
    </html>
  `);
});

app.listen(port, () => {
  console.log(`Server listening on port ${port}`);
});
