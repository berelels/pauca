package app.pauca.helper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.pauca.BuildConfig
import app.pauca.R
import app.pauca.data.Accent
import app.pauca.data.Palette

/**
 * O que muda entre o Pauca (pago) e o Pauca Lite (grátis). O Lite é o mesmo app com menos
 * recursos: o que não é dele aparece nos ajustes com o selo "Pauca" e abre o convite para a
 * versão completa.
 */
object Edition {
    val isLite: Boolean = BuildConfig.LITE

    /** O pacote do Pauca na loja, para onde o convite leva (sempre o de produção). */
    const val FULL_PACKAGE = "app.pauca"

    /** No Lite: o perfil padrão e mais um criado pela pessoa. */
    const val LITE_PROFILE_LIMIT = 2

    private val LITE_PALETTES = setOf(Palette.PAUCA.id, Palette.PAPEL.id)
    private val LITE_ACCENTS = setOf(Accent.DEFAULT, Accent.BLUE, Accent.NONE)

    fun hasPalette(id: String) = !isLite || id in LITE_PALETTES
    fun hasAccent(accent: String) = !isLite || accent in LITE_ACCENTS
    fun canAddProfile(count: Int) = !isLite || count < LITE_PROFILE_LIMIT

    /**
     * O pacote do Lite que corresponde a este app (com o mesmo sufixo de build), para a
     * versão completa importar os ajustes dele.
     */
    val litePackage: String
        get() = BuildConfig.APPLICATION_ID.replaceFirst(FULL_PACKAGE, "$FULL_PACKAGE.lite")

    /** Convite para a versão completa, mostrado ao tocar em algo que é só do Pauca. */
    fun showUpsell(context: Context, onShow: (OlDialog) -> Unit = { it.show() }) {
        onShow(context.createDialog(
            title = R.string.full_title,
            message = R.string.full_message,
            action = R.string.full_action,
            onAction = { openStore(context) },
        ))
    }

    private fun openStore(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$FULL_PACKAGE"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            context.openUrl("https://play.google.com/store/apps/details?id=$FULL_PACKAGE")
        }
    }
}
