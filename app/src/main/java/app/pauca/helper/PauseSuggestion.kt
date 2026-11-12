package app.pauca.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.pauca.R
import app.pauca.data.Prefs

/** Notificação "quer adicionar este app à pausa?" e os dois botões dela. */
object PauseSuggestion {
    private const val CHANNEL = "pause_suggestions"
    const val ACTION_ADD = "app.pauca.PAUSE_SUGGEST_ADD"
    const val ACTION_NEVER = "app.pauca.PAUSE_SUGGEST_NEVER"
    const val EXTRA_PKG = "pkg"

    fun post(context: Context, pkg: String, label: String, count: Int) {
        val manager = NotificationManagerCompat.from(context)
        // Sem a permissão (Android 13+) ou com as notificações desligadas: não avisa
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, context.getString(R.string.pause_suggest_channel), NotificationManager.IMPORTANCE_LOW)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val text = TextUtils.expandTemplate(context.getText(R.string.pause_suggest_message), label, count.toString())
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(context.getString(R.string.pause_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.pause_suggest_add), action(context, ACTION_ADD, pkg))
            .addAction(0, context.getString(R.string.pause_suggest_never), action(context, ACTION_NEVER, pkg))
            .build()
        try {
            manager.notify(pkg.hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }

    private fun action(context: Context, action: String, pkg: String): PendingIntent {
        val intent = Intent(context, PauseSuggestionReceiver::class.java).setAction(action).putExtra(EXTRA_PKG, pkg)
        return PendingIntent.getBroadcast(
            context, (action + pkg).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class PauseSuggestionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra(PauseSuggestion.EXTRA_PKG) ?: return
        val prefs = Prefs(context)
        when (intent.action) {
            PauseSuggestion.ACTION_ADD -> prefs.pauseApps = prefs.pauseApps + pkg
            PauseSuggestion.ACTION_NEVER -> prefs.pauseNeverAsk = prefs.pauseNeverAsk + pkg
            else -> return
        }
        NotificationManagerCompat.from(context).cancel(pkg.hashCode())
    }
}
