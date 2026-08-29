package dev.thermaltrace.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.thermaltrace.android.ui.navigation.ThermalTraceNav
import dev.thermaltrace.android.ui.theme.ThermalTraceTheme

class MainActivity : ComponentActivity() {
    private var deepLinkDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkDestination = intent?.getStringExtra(DeepLinks.EXTRA_DESTINATION)
        enableEdgeToEdge()
        val container = (application as ThermalTraceApp).container
        setContent {
            ThermalTraceTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ThermalTraceNav(
                        container = container,
                        deepLinkDestination = deepLinkDestination,
                        onDeepLinkConsumed = { deepLinkDestination = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkDestination = intent.getStringExtra(DeepLinks.EXTRA_DESTINATION)
    }
}
