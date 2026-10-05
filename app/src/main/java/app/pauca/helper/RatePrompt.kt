package app.pauca.helper

import android.content.Context
import app.pauca.BuildConfig
import app.pauca.R
import app.pauca.data.Prefs

/**
 * Pedido de avaliação na Play Store, no máximo uma vez por semana e nunca na primeira semana.
 * Depois que a pessoa vai avaliar (ou diz que já avaliou), não aparece mais.
 *
 * No Lite é uma janela, só nos ajustes. No Pauca, que a pessoa já pagou, é um cartão discreto
 * no topo da tela principal dos ajustes, sem interromper nada.
 */
object RatePrompt {

    private const val WEEK = 7L * 24 * 60 * 60 * 1000

    fun isDue(context: Context): Boolean {
        val prefs = Prefs(context)
        if (prefs.ratingDone) return false
        var next = prefs.ratingNextAt
        if (next == 0L) {
            // Primeira vez: conta uma semana a partir da instalação (ou de agora, em quem atualizou)
            val start = prefs.firstOpenTime.takeIf { it > 0 } ?: System.currentTimeMillis()
            next = start + WEEK
            prefs.ratingNextAt = next
        }
        return System.currentTimeMillis() >= next
    }

    /** "Agora não": volta daqui a uma semana. */
    fun snooze(context: Context) {
        Prefs(context).ratingNextAt = System.currentTimeMillis() + WEEK
    }

    fun markDone(context: Context) {
        Prefs(context).ratingDone = true
    }

    /** Abre a página deste app (Pauca ou Lite) na Play Store. */
    fun rate(context: Context) {
        markDone(context)
        Edition.openStore(context, BuildConfig.APPLICATION_ID.removeSuffix(".debug"))
    }

    /** Lite: a janela. Já conta como vista ao abrir, então fechar no X espera uma semana. */
    fun dialog(context: Context): OlDialog {
        snooze(context)
        return context.createDialog(
            title = context.getString(R.string.rate_title_lite),
            message = context.getString(R.string.rate_message_lite),
            action = context.getString(R.string.rate_action),
            onAction = { rate(context) },
            neutral = context.getString(R.string.rate_already),
            onNeutral = { markDone(context) },
        )
    }
}
