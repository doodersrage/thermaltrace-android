package dev.thermaltrace.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.thermaltrace.android.ui.navigation.ThermalTraceNav
import dev.thermaltrace.android.ui.theme.ThermalTraceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ThermalTraceApp).container
        setContent {
            ThermalTraceTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ThermalTraceNav(container)
                }
            }
        }
    }
}
