package dev.thermaltrace.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
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
    private var deepLinkEventId by mutableStateOf<Long?>(null)
    private var oauthExchangeToken by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        val container = (application as ThermalTraceApp).container
        setContent {
            ThermalTraceTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ThermalTraceNav(
                        container = container,
                        deepLinkDestination = deepLinkDestination,
                        deepLinkEventId = deepLinkEventId,
                        oauthExchangeToken = oauthExchangeToken,
                        onDeepLinkConsumed = {
                            deepLinkDestination = null
                            deepLinkEventId = null
                        },
                        onOAuthExchangeConsumed = { oauthExchangeToken = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val uri = intent.data
        val exchange = oauthExchangeFrom(uri)
        if (exchange != null) {
            oauthExchangeToken = exchange
            return
        }
        deepLinkDestination = intent.getStringExtra(DeepLinks.EXTRA_DESTINATION)
        deepLinkEventId = intent.getStringExtra(DeepLinks.EXTRA_EVENT_ID)
            ?.toLongOrNull()
            ?: intent.getLongExtra(DeepLinks.EXTRA_EVENT_ID, -1L).takeIf { it >= 0 }
    }

    private fun oauthExchangeFrom(uri: Uri?): String? {
        if (uri == null) return null
        val customScheme =
            uri.scheme == DeepLinks.OAUTH_SCHEME && uri.host == DeepLinks.OAUTH_HOST
        val httpsAppLink =
            uri.scheme == "https" &&
                uri.host == DeepLinks.OAUTH_HTTPS_HOST &&
                (uri.path == DeepLinks.OAUTH_HTTPS_PATH || uri.path?.startsWith("${DeepLinks.OAUTH_HTTPS_PATH}/") == true)
        if (!customScheme && !httpsAppLink) return null
        return uri.getQueryParameter(DeepLinks.OAUTH_EXCHANGE_PARAM)?.takeIf { it.isNotBlank() }
    }

    fun openOAuthUrl(url: String) {
        // Custom Tabs keep the app underneath; after Google/YubiKey, /app/oauth
        // can hand back via intent:// more reliably than a detached Chrome tab.
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(this, Uri.parse(url))
    }
}
