package app.pauca.focus

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.pauca.data.Prefs

/**
 * Durante o foco, tira da barra as notificações dos apps fora da lista e conta quantas
 * foram, para o resumo que aparece na tela inicial quando o foco acaba.
 * Ligações, alarmes, mídia tocando e serviços em primeiro plano nunca são mexidos.
 */
class FocusListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        sweep()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        hold(sbn)
    }

    /** Passa pelas notificações que já estavam na barra quando o foco começou. */
    fun sweep() {
        if (!Prefs(this).focusActive) return
        try {
            activeNotifications?.forEach { hold(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hold(sbn: StatusBarNotification) {
        val prefs = Prefs(this)
        if (!prefs.focusActive || !prefs.focusFilter) return
        if (!shouldHold(sbn)) return
        try {
            cancelNotification(sbn.key)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        // O resumo do grupo é só um "envelope": conta as mensagens, não ele
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val digest = prefs.focusDigest.toMutableMap()
        digest[sbn.packageName] = (digest[sbn.packageName] ?: 0) + 1
        prefs.focusDigest = digest
    }

    private fun shouldHold(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (sbn.isOngoing || !sbn.isClearable) return false
        if (n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return false
        if (n.category == Notification.CATEGORY_CALL || n.category == Notification.CATEGORY_ALARM) return false
        return FocusManager.isRestricted(this, sbn.packageName)
    }

    companion object {
        @Volatile
        var instance: FocusListenerService? = null
            private set
    }
}
