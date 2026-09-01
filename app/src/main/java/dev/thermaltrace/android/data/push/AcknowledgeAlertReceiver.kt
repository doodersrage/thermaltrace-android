package dev.thermaltrace.android.data.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.thermaltrace.android.DeepLinks
import dev.thermaltrace.android.ThermalTraceApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AcknowledgeAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(DeepLinks.EXTRA_EVENT_ID, -1L)
        if (eventId < 0) return
        val container = (context.applicationContext as? ThermalTraceApp)?.container ?: return
        val pending = goAsync()
        scope.launch {
            try {
                container.alertsInboxRepository.acknowledge(eventId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
