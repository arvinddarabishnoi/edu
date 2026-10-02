package com.mindnova.edutopia

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.json.JSONObject

/**
 * Edutopia Application class. Startup stays lightweight: Firebase is
 * auto-initialized by the ContentProvider the google-services plugin adds.
 * We only detect and record whether Firebase is actually usable so the UI can
 * show an explicit "not configured" state instead of crashing or pretending.
 */
class EdutopiaApp : Application() {

    var firebaseConfigured: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        firebaseConfigured = ensureFirebase()
        if (!firebaseConfigured) {
            Log.w(
                TAG,
                "Firebase is not configured (missing app/google-services.json). " +
                    "The app shows setup instructions instead of running against a fake backend."
            )
        }
    }

    private fun ensureFirebase(): Boolean {
        return try {
            if (FirebaseApp.getApps(this).isNotEmpty()) return true
            // The gradle plugin was skipped at build time (no config committed).
            // Optionally support a runtime-provided file for local experiments:
            // adb push google-services.json /data/data/<appId>/files/google-services.json
            val f = java.io.File(filesDir, "google-services.json")
            if (!f.exists()) return false
            val json = JSONObject(f.readText())
            val client = json.getJSONArray("client").getJSONObject(0)
            val options = FirebaseOptions.Builder()
                .setApiKey(client.getJSONArray("api_key").getJSONObject(0).getString("current_key"))
                .setApplicationId(client.getJSONObject("client_info").getString("mobilesdk_app_id"))
                .setProjectId(json.getJSONObject("project_info").getString("project_id"))
                .build()
            FirebaseApp.initializeApp(this, options)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "EdutopiaApp"
        lateinit var instance: EdutopiaApp
            private set

        fun isFirebaseConfigured(): Boolean =
            ::instance.isInitialized && instance.firebaseConfigured
    }
}
