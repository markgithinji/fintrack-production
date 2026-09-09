package com.fintrack.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.fintrack.shared.feature.navigation.ui.MainScreen


class MainActivity : FragmentActivity() {
    private var transactionIdState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        transactionIdState = intent?.getStringExtra("transactionId")

        setContent {
            MainScreen(
                initialTransactionId = transactionIdState,
                onTransactionIdConsumed = { transactionIdState = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        transactionIdState = intent.getStringExtra("transactionId")
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    MainScreen()
}
