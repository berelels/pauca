package app.pauca.focus

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.pauca.data.Prefs
import java.util.Objects

/**
 * Durante o foco, tira da barra as notificações dos apps fora da lista e conta quantas
 * foram, para o resumo que aparece na tela inicial quando o foco acaba. Também conta as dos
 * apps permitidos que o Não Perturbe do foco silenciou: elas ficam na barra, mas a pessoa
 * não viu chegar. Ligações, alarmes, mídia tocando e serviços em primeiro plano nunca são mexidos.
 */
class FocusListenerService : NotificationListenerService() {

    /**
     * Marca da última versão contada de cada notificação (pela chave). Apps de mensagem costumam
     * atualizar a mesma notificação sem mensagem nova, por exemplo para pôr a foto do contato, e
     * isso não pode contar de novo. Fica só na memória e não guarda o texto, só uma marca dele.
     */
    private val counted = mutableMapOf<String, Int>()

    override fun onListenerConnected() {
        instance = this
        sweep()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        // Alguns celulares desligam o ouvinte para poupar bateria; durante o foco, pede para voltar
        if (Prefs(this).focusActive) try {
            requestRebind(ComponentName(this, FocusListenerService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!hold(sbn)) countIfSilenced(sbn)
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

    /** Tira da barra se o app está fora da lista; true se tirou. */
    private fun hold(sbn: StatusBarNotification): Boolean {
        val prefs = Prefs(this)
        if (!prefs.focusActive || !prefs.focusFilter) return false
        if (!isMessage(sbn) || !FocusManager.isRestricted(this, sbn.packageName)) return false
        try {
            cancelNotification(sbn.key)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        count(sbn)
        return true
    }

    /** App permitido, mas o Não Perturbe do foco calou a notificação: entra no resumo. */
    private fun countIfSilenced(sbn: StatusBarNotification) {
        if (!Prefs(this).focusActive) return
        if (!isMessage(sbn) || !FocusManager.isLaunchable(this, sbn.packageName)) return
        if (passesDnd(sbn)) return
        count(sbn)
    }

    /**
     * Na hora de desligar o foco (antes de o Não Perturbe sair): conta o que ficou silenciado
     * na barra desde o começo do foco e ainda não foi contado. Cobre o tempo em que o Android
     * deixou o ouvinte desligado e as notificações chegaram sem ele ver.
     */
    fun collectSilenced(since: Long) {
        try {
            activeNotifications?.forEach { sbn ->
                if (sbn.postTime >= since && sbn.key !in counted) countIfSilenced(sbn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun passesDnd(sbn: StatusBarNotification): Boolean {
        val ranking = Ranking()
        return try {
            !currentRanking.getRanking(sbn.key, ranking) || ranking.matchesInterruptionFilter()
        } catch (e: Exception) {
            true
        }
    }

    private fun count(sbn: StatusBarNotification) {
        // O resumo do grupo é só um "envelope": conta as mensagens, não ele
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val mark = mark(sbn)
        if (counted[sbn.key] == mark) return
        counted[sbn.key] = mark
        val prefs = Prefs(this)
        val digest = prefs.focusDigest.toMutableMap()
        digest[sbn.packageName] = (digest[sbn.packageName] ?: 0) + 1
        prefs.focusDigest = digest
    }

    /** Muda quando chega algo novo (hora, título ou texto), não quando o app só retoca a notificação. */
    private fun mark(sbn: StatusBarNotification): Int {
        val n = sbn.notification
        return Objects.hash(
            n.`when`,
            n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        )
    }

    /** Um foco novo começa a contar do zero. */
    fun resetCounts() = counted.clear()

    /** Notificação comum, de um app de outro: não é ligação, alarme, mídia nem serviço. */
    private fun isMessage(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (sbn.packageName == packageName) return false
        if (sbn.isOngoing || !sbn.isClearable) return false
        if (n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return false
        return n.category != Notification.CATEGORY_CALL && n.category != Notification.CATEGORY_ALARM
    }

    companion object {
        @Volatile
        var instance: FocusListenerService? = null
            private set
    }
}
