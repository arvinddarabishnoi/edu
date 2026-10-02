package com.mindnova.edutopia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindnova.edutopia.core.navigation.EdutopiaNavHost
import com.mindnova.edutopia.core.theme.BackgroundDark
import com.mindnova.edutopia.core.theme.EdutopiaTheme
import com.mindnova.edutopia.core.theme.TextWhiteMuted
import com.mindnova.edutopia.core.theme.TextWhitePrimary

/**
 * Single-activity Compose host.
 *
 * - Edge-to-edge via enableEdgeToEdge(); screens apply system-bar insets
 *   through shared components (AppTopBar / floating bottom nav / imePadding),
 *   so content never hides behind status bars, nav bars, or the keyboard.
 * - All navigation lives in EdutopiaNavHost (Navigation Compose), including
 *   deep links; the auth-aware BrandIntro owns routing so Login/Home can never
 *   be pushed as duplicates. Back uses the NavController; sign-out clears the
 *   whole stack.
 * - Without app/google-services.json the app renders an explicit
 *   configuration guide rather than crashing or faking data (see README §2).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            EdutopiaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    if (EdutopiaApp.isFirebaseConfigured()) {
                        EdutopiaNavHost()
                    } else {
                        FirebaseNotConfiguredScreen()
                    }
                }
            }
        }
    }
}

/**
 * Not a placeholder UI: this is the real error state for a missing Firebase
 * configuration, with exact recovery steps.
 */
@Composable
private fun FirebaseNotConfiguredScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "EDUTOPIA",
            color = TextWhitePrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )
        Text(
            "Firebase configuration missing",
            color = MaterialTheme.colorScheme.error,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            "The app intentionally refuses to run against fake data.\n\n" +
                "1. Open the Firebase console for the MINDNOVA EDUTOPIA project.\n" +
                "2. Project settings → Your apps → Android app with package " +
                "com.mindnova.edutopia.\n" +
                "3. Download google-services.json.\n" +
                "4. Save it as app/google-services.json and rebuild.\n\n" +
                "Full guide: docs/FIREBASE_SETUP.md",
            color = TextWhiteMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
