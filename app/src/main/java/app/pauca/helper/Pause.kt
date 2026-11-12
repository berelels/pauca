package app.pauca.helper

import android.content.Context
import app.pauca.data.AppModel
import app.pauca.data.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pedido de pausa: o app que ia abrir e em qual abertura do dia. */
class PauseRequest(val app: AppModel, val count: Int)

/**
 * Contagem de aberturas por app no dia (hora local) e a decisão de mostrar a pausa.
 * Só conta o que passa pelo Pauca; zera sozinho quando o dia muda.
 */
object Pause {

    val LIMITS = listOf(3, 5, 10, 15, 20)

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Novo dia: esquece contagens e permissões do anterior. */
    private fun rollDay(prefs: Prefs) {
        val day = today()
        if (prefs.pauseDay == day) return
        prefs.pauseDay = day
        prefs.pauseCounts = emptyMap()
        prefs.pauseAllowed = emptySet()
        prefs.pauseAsked = emptySet()
    }

    /** Se [pkg] deve pausar agora, devolve o número desta abertura (a que ainda não aconteceu). */
    fun check(prefs: Prefs, pkg: String): Int? {
        if (!prefs.pauseOn || pkg !in prefs.pauseApps) return null
        rollDay(prefs)
        if (pkg in prefs.pauseAllowed) return null
        val next = (prefs.pauseCounts[pkg] ?: 0) + 1
        return next.takeIf { it >= prefs.pauseLimit }
    }

    fun countOpen(context: Context, prefs: Prefs, pkg: String, label: String) {
        if (!prefs.pauseOn) return
        rollDay(prefs)
        val count = (prefs.pauseCounts[pkg] ?: 0) + 1
        prefs.pauseCounts = prefs.pauseCounts + (pkg to count)
        // App fora da lista que já passou do limite: sugere uma vez por dia (e nunca mais se a pessoa pediu)
        if (prefs.pauseSuggest && pkg !in prefs.pauseApps && count >= prefs.pauseLimit &&
            pkg !in prefs.pauseNeverAsk && pkg !in prefs.pauseAsked
        ) {
            prefs.pauseAsked = prefs.pauseAsked + pkg
            PauseSuggestion.post(context, pkg, label, count)
        }
    }

    fun allowToday(prefs: Prefs, pkg: String) {
        rollDay(prefs)
        prefs.pauseAllowed = prefs.pauseAllowed + pkg
    }
}
