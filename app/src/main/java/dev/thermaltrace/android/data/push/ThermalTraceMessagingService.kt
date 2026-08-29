package dev.thermaltrace.android.data.push

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.thermaltrace.android.DeepLinks
import dev.thermaltrace.android.MainActivity
import dev.thermaltrace.android.R
import dev.thermaltrace.android.ThermalTraceApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ThermalTraceMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val registrar = (application as? ThermalTraceApp)?.container?.pushRegistrar ?: return
        scope.launch {
            registrar.syncToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return
        val destination = message.data["deep_link"]
            ?.takeIf { it.isNotBlank() }
            ?: DeepLinks.ALERTS

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(DeepLinks.EXTRA_DESTINATION, destination)
        }
        val pending = PendingIntent.getActivity(
            this,
            destination.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, PushRegistrar.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_thermaltrace)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(this).notify((title + body).hashCode(), notification)
        } catch (error: SecurityException) {
            Log.w(TAG, "Notification permission missing", error)
        }
    }

    companion object {
        private const val TAG = "TTMessaging"
    }
}
